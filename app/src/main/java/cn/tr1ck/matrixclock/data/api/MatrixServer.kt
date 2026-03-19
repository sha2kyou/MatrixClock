package cn.tr1ck.matrixclock.data.api

import android.content.Context
import cn.tr1ck.matrixclock.data.model.AuthRecord
import cn.tr1ck.matrixclock.data.model.DeviceInfo
import cn.tr1ck.matrixclock.data.model.DisplayMode
import cn.tr1ck.matrixclock.data.model.OpLogEntry
import cn.tr1ck.matrixclock.data.model.PomodoroConfig
import cn.tr1ck.matrixclock.data.model.PomodoroSession
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class DeviceInfoUpdate(
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val systemVersion: String? = null,
    val batteryLevel: Int? = null
)

@Serializable
private data class OpLogPageResponse(
    val items: List<OpLogEntry>,
    val hasMore: Boolean
)

class MatrixServer(
    private val context: Context,
    port: Int,
    private val onUpdateText: (text: String, color: String?, duration: Int?, style: Int?, icon: String?) -> Unit,
    private val onSetMode: (DisplayMode) -> Unit,
    private val onSetClockTemplate: (Int) -> Unit,
    private val onBindRequest: (String) -> String?,
    private val getAuthRecords: () -> List<AuthRecord>,
    private val hasValidToken: (String?) -> Boolean,
    private val removeAuthByToken: (String) -> Unit,
    private val updateAuthDeviceInfo: (String, String?, String?, String?, Int?) -> Unit,
    private val getClockTemplate: () -> Int,
    private val getPomodoroConfigs: () -> List<PomodoroConfig>,
    private val setPomodoroConfigs: (List<PomodoroConfig>) -> Unit,
    private val getKeySettings: () -> List<String>,
    private val setKeySettings: (volUpShort: String, volUpLong: String, volUpDouble: String, volDownShort: String, volDownLong: String, volDownDouble: String) -> Unit,
    private val getDeviceInfo: () -> DeviceInfo,
    private val getPomodoroSessions: () -> List<PomodoroSession>,
    private val onLogOperation: (action: String, detail: String?, ip: String?) -> Unit,
    private val getOpLogEntries: (offset: Int, limit: Int) -> List<OpLogEntry>
) : NanoHTTPD(port) {

    var isWebInterfaceEnabled: Boolean = true


    private fun loadWebHtml(): String {
        return runCatching {
            context.assets.open("web/index.html").bufferedReader().use { it.readText() }
        }.getOrElse { "<html><body><h3>Failed to load web UI</h3></body></html>" }
    }

    /**
     * 从 assets/web/ 提供 Vite 打包的 JS/CSS（请求路径形如 /assets/index-xxxxx.js）。
     */
    private fun serveWebStatic(uri: String): Response? {
        val trimmed = uri.trimStart('/')
        if (!trimmed.startsWith("assets/")) return null
        val assetPath = "web/$trimmed"
        return try {
            val stream = context.assets.open(assetPath)
            val mime = when {
                trimmed.endsWith(".js", ignoreCase = true) -> "application/javascript"
                trimmed.endsWith(".mjs", ignoreCase = true) -> "application/javascript"
                trimmed.endsWith(".css", ignoreCase = true) -> "text/css"
                trimmed.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
                trimmed.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
                trimmed.endsWith(".woff", ignoreCase = true) -> "font/woff"
                trimmed.endsWith(".ttf", ignoreCase = true) -> "font/ttf"
                trimmed.endsWith(".ico", ignoreCase = true) -> "image/x-icon"
                trimmed.endsWith(".png", ignoreCase = true) -> "image/png"
                trimmed.endsWith(".json", ignoreCase = true) -> "application/json"
                else -> "application/octet-stream"
            }
            newChunkedResponse(Response.Status.OK, mime, stream)
        } catch (_: Exception) {
            null
        }
    }

    private fun diffKeySettings(old: List<String>, new: List<String>): List<String> {
        val labels = listOf("volUpShort", "volUpLong", "volUpDouble", "volDownShort", "volDownLong", "volDownDouble")
        return labels.mapIndexedNotNull { idx, label ->
            val before = old.getOrElse(idx) { "-" }
            val after = new.getOrElse(idx) { "-" }
            if (before != after) "$label: $before -> $after" else null
        }
    }

    private fun diffPomodoroConfigs(old: List<PomodoroConfig>, new: List<PomodoroConfig>): List<String> {
        val lines = mutableListOf<String>()
        if (old.size != new.size) lines += "count: ${old.size} -> ${new.size}"

        val oldPrimary = old.firstOrNull { it.isPrimary }?.id ?: "-"
        val newPrimary = new.firstOrNull { it.isPrimary }?.id ?: "-"
        if (oldPrimary != newPrimary) lines += "primaryId: $oldPrimary -> $newPrimary"

        val oldById = old.associateBy { it.id }
        val newById = new.associateBy { it.id }
        val oldIndexById = old.mapIndexed { idx, c -> c.id to idx }.toMap()
        val newIndexById = new.mapIndexed { idx, c -> c.id to idx }.toMap()
        fun cfgTag(id: String, cfg: PomodoroConfig?, newListFirst: Boolean = true): String {
            val idx = if (newListFirst) {
                newIndexById[id] ?: oldIndexById[id] ?: -1
            } else {
                oldIndexById[id] ?: newIndexById[id] ?: -1
            }
            val no = if (idx >= 0) "#${idx + 1}" else "#?"
            val text = (cfg?.text ?: "").ifBlank { id }.take(24)
            return "[$no $text]"
        }

        val removed = oldById.keys - newById.keys
        val added = newById.keys - oldById.keys
        removed.sorted().forEach { id ->
            lines += "${cfgTag(id, oldById[id], newListFirst = false)} removed"
        }
        added.sorted().forEach { id ->
            val c = newById[id] ?: return@forEach
            lines += "${cfgTag(id, c)} added"
        }

        (oldById.keys intersect newById.keys).sorted().forEach { id ->
            val o = oldById[id] ?: return@forEach
            val n = newById[id] ?: return@forEach
            val tag = cfgTag(id, n)
            if (o.text != n.text) lines += "$tag text: ${o.text} -> ${n.text}"
            if (o.durationSec != n.durationSec) lines += "$tag durationSec: ${o.durationSec} -> ${n.durationSec}"
            if (o.colorHex != n.colorHex) lines += "$tag colorHex: ${o.colorHex} -> ${n.colorHex}"
            if (o.countdownStyle != n.countdownStyle) lines += "$tag countdownStyle: ${o.countdownStyle} -> ${n.countdownStyle}"
            if (o.cycleTotal != n.cycleTotal) lines += "$tag cycleTotal: ${o.cycleTotal} -> ${n.cycleTotal}"
            if (o.isPrimary != n.isPrimary) lines += "$tag isPrimary: ${o.isPrimary} -> ${n.isPrimary}"
        }

        return lines
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parameters
        val method = session.method
        fun maskToken(token: String?): String {
            if (token.isNullOrBlank()) return "-"
            val t = token.trim()
            return if (t.length <= 8) "****" else "${t.take(4)}...${t.takeLast(4)}"
        }
        fun resolveDevice(token: String?): String {
            if (token.isNullOrBlank()) return "-"
            val name = getAuthRecords().firstOrNull { it.token == token }?.deviceName
            return if (!name.isNullOrBlank()) name else maskToken(token)
        }
        fun logDetailMax(text: String?, max: Int = 80): String {
            val v = (text ?: "").replace("\n", " ").trim()
            return if (v.length > max) "${v.take(max)}…" else v
        }
        val reqIp = session.remoteIpAddress ?: ""

        if (uri == "/" && method == Method.GET) {
            if (!isWebInterfaceEnabled) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Web interface is disabled in settings.")
            }
            val html = loadWebHtml()
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
        }

        if (uri == "/api/auth/bind" && method == Method.POST) {
            val token = onBindRequest(reqIp)
            if (token != null) onLogOperation("auth_bind", "token=${maskToken(token)}", reqIp)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, token ?: "DENIED")
        }

        if (uri == "/api/auth/reset" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (clientToken != null && hasValidToken(clientToken)) {
                removeAuthByToken(clientToken)
                onLogOperation("auth_reset", "token=${maskToken(clientToken)}", reqIp)
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
            }
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        }

        if (uri == "/api/auth/list" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            return newFixedLengthResponse(Response.Status.OK, "application/json", Json.encodeToString(getAuthRecords()))
        }

        if (uri == "/api/auth/device-info" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val map = HashMap<String, String>()
            session.parseBody(map)
            val jsonBody = map["postData"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body")
            try {
                val d = Json.decodeFromString<DeviceInfoUpdate>(jsonBody)
                updateAuthDeviceInfo(clientToken!!, d.deviceName, d.deviceModel, d.systemVersion, d.batteryLevel)
                onLogOperation(
                    "auth_device_info",
                    "token=${maskToken(clientToken)},name=${logDetailMax(d.deviceName, 24)},model=${logDetailMax(d.deviceModel, 24)},battery=${d.batteryLevel ?: -1}",
                    reqIp
                )
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
            } catch (e: Exception) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, e.message ?: "Bad request")
            }
        }

        if (uri == "/api/auth/revoke" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            val revokeToken = params["revokeToken"]?.get(0)
            if (!hasValidToken(clientToken) || revokeToken.isNullOrBlank()) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val revokeDeviceName = resolveDevice(revokeToken)
            removeAuthByToken(revokeToken)
            onLogOperation("auth_revoke", "by=${resolveDevice(clientToken)},target=$revokeDeviceName", reqIp)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
        }

        if (uri == "/api/mode" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val mode = params["mode"]?.get(0) ?: "CLOCK"
            val parsedMode = runCatching { DisplayMode.valueOf(mode) }.getOrNull()
                ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid mode")
            onSetMode(parsedMode)
            onLogOperation("mode", "mode=$mode,by=${resolveDevice(clientToken)}", reqIp)
            return newFixedLengthResponse("OK")
        }

        if (uri == "/api/clock/template" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val tpl = getClockTemplate()
            val json = """{"template":$tpl}"""
            return newFixedLengthResponse(Response.Status.OK, "application/json", json)
        }
        if (uri == "/api/clock/template" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val oldTemplate = getClockTemplate()
            val template = params["template"]?.get(0)?.toIntOrNull()?.coerceIn(1, 3) ?: 1
            onSetClockTemplate(template)
            val detailLines = mutableListOf<String>()
            if (oldTemplate != template) detailLines += "template: $oldTemplate -> $template"
            if (detailLines.isNotEmpty()) onLogOperation("clock_template", detailLines.joinToString("\n"), reqIp)
            return newFixedLengthResponse("OK")
        }

        if (uri == "/api/display" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val text = params["text"]?.get(0) ?: ""
            val color = params["color"]?.get(0)
            // duration <= 0 means persistent display (no countdown).
            val duration = params["duration"]?.get(0)?.toIntOrNull()
            val style = params["style"]?.get(0)?.toIntOrNull()?.coerceIn(1, 3)
            val icon = params["icon"]?.get(0)
            onUpdateText(text, color, duration, style, icon)
            onLogOperation(
                "display_text",
                "text=${logDetailMax(text, 40)},len=${text.length},color=${color ?: "-"},duration=${duration ?: -1},style=${style ?: 1},icon=${icon ?: "-"},by=${resolveDevice(clientToken)}",
                reqIp
            )
            return newFixedLengthResponse("OK")
        }

        if (uri == "/api/pomodoro/configs" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            return newFixedLengthResponse(Response.Status.OK, "application/json", Json.encodeToString(getPomodoroConfigs()))
        }
        if (uri == "/api/pomodoro/sessions" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            return newFixedLengthResponse(Response.Status.OK, "application/json", Json.encodeToString(getPomodoroSessions()))
        }

        if (uri == "/api/pomodoro/configs" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val oldConfigs = getPomodoroConfigs()
            val map = HashMap<String, String>()
            session.parseBody(map)
            val jsonBody = map["postData"]
            if (jsonBody != null) {
                try {
                    val configs = Json.decodeFromString<List<PomodoroConfig>>(jsonBody)
                    setPomodoroConfigs(configs)
                    val detailLines = diffPomodoroConfigs(oldConfigs, configs).toMutableList()
                    if (detailLines.isNotEmpty()) onLogOperation("pomodoro_configs", detailLines.joinToString("\n"), reqIp)
                    return newFixedLengthResponse("OK")
                } catch (e: Exception) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message)
                }
            }
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body")
        }

        if (uri == "/api/device/info" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            return try {
                val info = getDeviceInfo()
                newFixedLengthResponse(Response.Status.OK, "application/json", Json.encodeToString(info))
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message ?: "Error")
            }
        }

        if (uri == "/api/keys/settings" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val list = getKeySettings()
            val json = """{"volUpShort":"${list.getOrElse(0) { "pomodoro" }}","volUpLong":"${list.getOrElse(1) { "menu" }}","volUpDouble":"${list.getOrElse(2) { "switch_pomodoro" }}","volDownShort":"${list.getOrElse(3) { "pomodoro" }}","volDownLong":"${list.getOrElse(4) { "menu" }}","volDownDouble":"${list.getOrElse(5) { "switch_pomodoro" }}"}}"""
            return newFixedLengthResponse(Response.Status.OK, "application/json", json)
        }
        if (uri == "/api/keys/settings" && method == Method.POST) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            fun normShort(p: String?) = p?.takeIf { it in listOf("menu", "pomodoro") } ?: "pomodoro"
            fun normLong(p: String?) = p?.takeIf { it in listOf("menu", "pomodoro") } ?: "menu"
            fun normDouble(p: String?) = p?.takeIf { it in listOf("menu", "pomodoro", "none", "switch_pomodoro") } ?: "switch_pomodoro"
            val oldSettings = getKeySettings()
            val upShort = normShort(params["volUpShort"]?.get(0))
            val upLong = normLong(params["volUpLong"]?.get(0))
            val upDouble = normDouble(params["volUpDouble"]?.get(0))
            val downShort = normShort(params["volDownShort"]?.get(0))
            val downLong = normLong(params["volDownLong"]?.get(0))
            val downDouble = normDouble(params["volDownDouble"]?.get(0))
            val newSettings = listOf(upShort, upLong, upDouble, downShort, downLong, downDouble)
            setKeySettings(
                upShort,
                upLong,
                upDouble,
                downShort,
                downLong,
                downDouble
            )
            val detailLines = diffKeySettings(oldSettings, newSettings).toMutableList()
            if (detailLines.isNotEmpty()) onLogOperation("keys_settings", detailLines.joinToString("\n"), reqIp)
            return newFixedLengthResponse("OK")
        }

        if (uri == "/api/oplog" && method == Method.GET) {
            val clientToken = params["token"]?.get(0)
            if (!hasValidToken(clientToken)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            val page = params["page"]?.get(0)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val pageSize = params["pageSize"]?.get(0)?.toIntOrNull()?.coerceIn(10, 100) ?: 50
            val offset = page * pageSize
            val rows = getOpLogEntries(offset, pageSize + 1)
            val hasMore = rows.size > pageSize
            val items = if (hasMore) rows.take(pageSize) else rows
            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                Json.encodeToString(OpLogPageResponse(items = items, hasMore = hasMore))
            )
        }

        // Vite 构建的前端静态资源：/assets/*
        if (method == Method.GET) {
            serveWebStatic(uri)?.let { return it }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
}
