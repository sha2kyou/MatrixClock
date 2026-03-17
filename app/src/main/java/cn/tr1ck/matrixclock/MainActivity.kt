package cn.tr1ck.matrixclock

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import kotlinx.serialization.decodeFromString
import java.util.*
import java.util.concurrent.CompletableFuture
import cn.tr1ck.matrixclock.data.api.MatrixServer
import cn.tr1ck.matrixclock.data.local.OpLogDbHelper
import cn.tr1ck.matrixclock.data.model.AuthRecord
import cn.tr1ck.matrixclock.data.model.DeviceInfo
import cn.tr1ck.matrixclock.data.model.DisplayMode
import cn.tr1ck.matrixclock.data.model.OpLogEntry
import cn.tr1ck.matrixclock.data.model.PomodoroConfig
import cn.tr1ck.matrixclock.data.model.PomodoroSession
import cn.tr1ck.matrixclock.ui.screen.AuthDialog
import cn.tr1ck.matrixclock.ui.screen.AuthManagementDialog
import cn.tr1ck.matrixclock.ui.screen.MatrixClockApp
import cn.tr1ck.matrixclock.ui.screen.SettingsDialog
import cn.tr1ck.matrixclock.util.getLocalIpAddress
import cn.tr1ck.matrixclock.ui.display.CYCLE_TOTAL

class MainActivity : ComponentActivity() {
    private val displayText = mutableStateOf("")
    private val dateText = mutableStateOf("")
    private val displayColor = mutableStateOf(Color.Red)
    private val displayMode = mutableStateOf(DisplayMode.CLOCK)
    private val clockTemplate = mutableStateOf(1)
    private val clockT3TimeMillis = mutableStateOf(0L)
    private val amPmText = mutableStateOf("")
    private val remainingSeconds = mutableIntStateOf(0)
    private val totalDurationSeconds = mutableIntStateOf(60)
    
    private val showColon = mutableStateOf(true)
    
    private lateinit var prefs: SharedPreferences
    private lateinit var opLogDb: OpLogDbHelper
    private var server: MatrixServer? = null

    private var bindRequestIp by mutableStateOf<String?>(null)
    private var currentBindFuture: CompletableFuture<String?>? = null
    private val authRecords = mutableStateListOf<AuthRecord>()
    private var isWebControlEnabled by mutableStateOf(true)
    private var is24HourFormatEnabled by mutableStateOf(true)

    private var showSettingsDialog by mutableStateOf(false)
    private var showAuthManagementDialog by mutableStateOf(false)
    private val volumeKeyHandler = Handler(Looper.getMainLooper())
    private var volumeLongPressRunnable: Runnable? = null
    private var volumeLongPressTriggered = false
    private var volumeKeyWasRepeated = false
    private var currentVolumeKeyCode = 0
    private val volumeLongPressMs = 500L
    private var volumeShortPressRunnable: Runnable? = null
    private var pendingShortKeyCode = 0
    private val volumeDoubleTapMs = 350L

    private val pomodoroConfigs = mutableStateListOf<PomodoroConfig>()
    private var currentPomoIndex by mutableIntStateOf(0)
    private var countdownStyle by mutableIntStateOf(1)

    private val opLogEntries = mutableListOf<OpLogEntry>()
    private val maxOpLogEntries = 500

    private fun pruneOpLogsLocked(now: Long = System.currentTimeMillis()): Boolean {
        val cutoff = now - OP_LOG_RETENTION_MS
        val before = opLogEntries.size
        opLogEntries.removeAll { it.timeMillis < cutoff }
        if (opLogEntries.size > maxOpLogEntries) {
            opLogEntries.subList(maxOpLogEntries, opLogEntries.size).clear()
        }
        return opLogEntries.size != before
    }

    private fun loadOpLogs() {
        synchronized(opLogEntries) {
            opLogDb.trim(OP_LOG_RETENTION_MS, maxOpLogEntries)
            opLogEntries.clear()
            opLogEntries.addAll(opLogDb.queryRecent(maxOpLogEntries))
            pruneOpLogsLocked()
        }
    }

    private val pomodoroSessions = mutableListOf<PomodoroSession>()
    private var sessionConfigId: String? = null
    private var sessionConfigText: String? = null
    private var sessionStartTime = 0L
    private val maxSessions = 500
    private val pomodoroSessionsVersion = mutableIntStateOf(0)

    private fun addOpLog(action: String, detail: String = "", ip: String = "") {
        synchronized(opLogEntries) {
            val entry = OpLogEntry(timeMillis = System.currentTimeMillis(), action = action, detail = detail, ip = ip)
            opLogDb.insert(entry)
            opLogDb.trim(OP_LOG_RETENTION_MS, maxOpLogEntries)
            opLogEntries.add(0, entry)
            pruneOpLogsLocked()
        }
    }

    private fun buildDeviceInfo(): DeviceInfo {
        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val statusCode = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val statusStr = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        val dm = resources.displayMetrics
        return DeviceInfo(
            model = Build.MODEL ?: "—",
            manufacturer = Build.MANUFACTURER ?: "—",
            device = Build.DEVICE ?: "—",
            androidVersion = Build.VERSION.RELEASE ?: "—",
            sdkInt = Build.VERSION.SDK_INT,
            batteryLevel = level.coerceIn(-1, 100),
            batteryStatus = statusStr,
            screenWidthPx = dm.widthPixels,
            screenHeightPx = dm.heightPixels,
            screenDensity = dm.density,
            isCharging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING || statusCode == BatteryManager.BATTERY_STATUS_FULL
        )
    }

    private var deviceInfoCache: DeviceInfo? = null

    private fun loadAuthRecords() {
        val legacyToken = prefs.getString("auth_token", null)
        if (legacyToken != null) {
            authRecords.clear()
            authRecords.add(AuthRecord(token = legacyToken, ip = ""))
            saveAuthRecords()
            prefs.edit { remove("auth_token") }
        } else {
            val json = prefs.getString("auth_records_json", null)
            if (json != null) {
                try {
                    val list = Json.decodeFromString<List<AuthRecord>>(json)
                    authRecords.clear()
                    authRecords.addAll(list)
                } catch (_: Exception) { }
            }
        }
    }

    private fun saveAuthRecords() {
        prefs.edit { putString("auth_records_json", Json.encodeToString(authRecords.toList())) }
    }

    private fun addAuthRecord(record: AuthRecord) {
        authRecords.add(record)
        saveAuthRecords()
    }

    private fun removeAuthByToken(token: String) {
        authRecords.removeAll { it.token == token }
        saveAuthRecords()
    }

    private fun updateAuthDeviceInfo(token: String, deviceName: String?, deviceModel: String?, systemVersion: String?, batteryLevel: Int?) {
        val idx = authRecords.indexOfFirst { it.token == token }
        if (idx >= 0) {
            val r = authRecords[idx]
            authRecords[idx] = r.copy(
                deviceName = deviceName ?: r.deviceName,
                deviceModel = deviceModel ?: r.deviceModel,
                systemVersion = systemVersion ?: r.systemVersion,
                batteryLevel = batteryLevel ?: r.batteryLevel
            )
            saveAuthRecords()
        }
    }

    private fun loadPomodoroConfigs() {
        val json = prefs.getString("pomodoro_configs_json", null)
        if (json != null) {
            try {
                val list = Json.decodeFromString<List<PomodoroConfig>>(json)
                pomodoroConfigs.clear()
                pomodoroConfigs.addAll(list)
            } catch (e: Exception) {
                setupDefaultPomodoro()
            }
        } else {
            setupDefaultPomodoro()
        }
    }

    private fun setupDefaultPomodoro() {
        pomodoroConfigs.clear()
        pomodoroConfigs.add(PomodoroConfig("default", "FOCUS", 1500, "#FF0000", true))
        savePomodoroConfigs()
    }

    private fun savePomodoroConfigs() {
        val json = Json.encodeToString(pomodoroConfigs.toList())
        prefs.edit { putString("pomodoro_configs_json", json) }
    }

    private fun getKeyBinding(keyCode: Int, longPress: Boolean, doubleTap: Boolean = false): String {
        val key = when {
            keyCode == KeyEvent.KEYCODE_VOLUME_UP && doubleTap -> "key_vol_up_double"
            keyCode == KeyEvent.KEYCODE_VOLUME_UP && !longPress -> "key_vol_up_short"
            keyCode == KeyEvent.KEYCODE_VOLUME_UP && longPress -> "key_vol_up_long"
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && doubleTap -> "key_vol_down_double"
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && !longPress -> "key_vol_down_short"
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && longPress -> "key_vol_down_long"
            else -> return "none"
        }
        val stored = prefs.getString(key, null)
        return when {
            stored in listOf("menu", "pomodoro", "none", "switch_pomodoro") -> stored ?: if (key.endsWith("_double")) "switch_pomodoro" else if (key.endsWith("_long")) "menu" else "pomodoro"
            key.endsWith("_long") -> "menu"
            key.endsWith("_double") -> "switch_pomodoro"
            else -> "pomodoro"
        }
    }

    private fun runKeyAction(action: String) {
        if (action == "none") return
        when (action) {
            "menu" -> showSettingsDialog = true
            "pomodoro" -> {
                if (displayMode.value == DisplayMode.TEXT) {
                    endPomodoroSessionIfAny(completed = false)
                    displayMode.value = DisplayMode.CLOCK
                    displayColor.value = Color.Red
                } else {
                    val primary = pomodoroConfigs.find { it.isPrimary } ?: pomodoroConfigs.firstOrNull() ?: return
                    currentPomoIndex = pomodoroConfigs.indexOf(primary).coerceAtLeast(0)
                    applyPomodoroConfig(primary)
                }
            }
            "switch_pomodoro" -> {
                if (pomodoroConfigs.size <= 1) return
                if (displayMode.value != DisplayMode.TEXT) return
                switchPomodoro(1)
            }
        }
    }

    private fun endPomodoroSessionIfAny(completed: Boolean) {
        val cid = sessionConfigId ?: return
        val text = sessionConfigText ?: ""
        val planned = totalDurationSeconds.intValue
        val actual = if (completed) planned else (planned - remainingSeconds.intValue).coerceAtLeast(0)
        pomodoroSessions.add(PomodoroSession(cid, text, sessionStartTime, planned, completed, actual))
        if (pomodoroSessions.size > maxSessions) pomodoroSessions.removeAt(0)
        savePomodoroSessions()
        sessionConfigId = null
        sessionConfigText = null
        sessionStartTime = 0L
    }

    private fun loadPomodoroSessions() {
        val json = prefs.getString("pomodoro_sessions_json", null) ?: return
        try {
            pomodoroSessions.clear()
            pomodoroSessions.addAll(Json.decodeFromString<List<PomodoroSession>>(json))
        } catch (_: Exception) { }
    }

    private fun savePomodoroSessions() {
        prefs.edit { putString("pomodoro_sessions_json", Json.encodeToString(pomodoroSessions.toList())) }
        pomodoroSessionsVersion.intValue++
    }

    /** Number of fully completed pomodoros today (only counts sessions that ran to the end and finished normally). */
    private fun getTodayCompletedPomodoroCount(configId: String?): Int {
        val cal = java.util.Calendar.getInstance()
        val todayYear = cal.get(java.util.Calendar.YEAR)
        val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
        return pomodoroSessions.count {
            (configId == null || it.configId == configId) &&
            it.completed &&
            it.actualDurationSec >= it.plannedDurationSec &&
            java.util.Calendar.getInstance().apply { timeInMillis = it.startTimeMillis }.let { c ->
                c.get(java.util.Calendar.YEAR) == todayYear && c.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
            }
        }.coerceAtMost(99)
    }

    private fun applyPomodoroConfig(config: PomodoroConfig) {
        if (displayMode.value == DisplayMode.TEXT && sessionConfigId != null) endPomodoroSessionIfAny(completed = false)
        sessionConfigId = config.id
        sessionConfigText = config.text
        sessionStartTime = System.currentTimeMillis()
        countdownStyle = config.countdownStyle.coerceIn(1, 3)
        displayText.value = config.text.uppercase(Locale.ROOT)
        displayMode.value = DisplayMode.TEXT
        totalDurationSeconds.intValue = config.durationSec
        remainingSeconds.intValue = config.durationSec
        displayColor.value = resolvePomodoroColor(config.colorHex)
    }

    private fun resolvePomodoroColor(colorHex: String): Color {
        if (colorHex.equals("random", ignoreCase = true)) {
            return Color(android.graphics.Color.parseColor(POMODORO_PRESET_COLORS.random()))
        }
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Red
        }
    }

    companion object {
        private const val OP_LOG_RETENTION_MS = 14L * 24 * 60 * 60 * 1000
        private val POMODORO_PRESET_COLORS = listOf(
            "#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF", "#8B00FF"
        )
    }

    private fun switchPomodoro(delta: Int) {
        if (pomodoroConfigs.isEmpty()) return
        currentPomoIndex = (currentPomoIndex + delta + pomodoroConfigs.size) % pomodoroConfigs.size
        applyPomodoroConfig(pomodoroConfigs[currentPomoIndex])
    }

    private fun initServer(isWebEnabled: Boolean) {
        if (server == null) {
            server = MatrixServer(this, 6574,
                onUpdateText = { text, colorHex, duration, style ->
                    runOnUiThread {
                        if (displayMode.value == DisplayMode.TEXT && sessionConfigId != null) endPomodoroSessionIfAny(completed = false)
                        sessionConfigId = null
                        sessionConfigText = null
                        sessionStartTime = 0L
                        countdownStyle = style?.coerceIn(1, 3) ?: 1
                        displayText.value = text.uppercase(Locale.ROOT)
                        displayMode.value = DisplayMode.TEXT
                        val seconds = duration ?: 0
                        if (seconds <= 0) {
                            // Persistent display (no countdown).
                            totalDurationSeconds.intValue = 0
                            remainingSeconds.intValue = -1
                        } else {
                            totalDurationSeconds.intValue = seconds
                            remainingSeconds.intValue = seconds
                        }
                        colorHex?.let {
                            try {
                                displayColor.value = Color(android.graphics.Color.parseColor(it))
                            } catch (e: Exception) {
                                displayColor.value = Color.Red
                            }
                        }
                    }
                },
                onSetMode = { mode ->
                    runOnUiThread {
                        if (mode == DisplayMode.CLOCK && displayMode.value == DisplayMode.TEXT && sessionConfigId != null) endPomodoroSessionIfAny(completed = false)
                        if (mode == DisplayMode.CLOCK) { sessionConfigId = null; sessionConfigText = null; sessionStartTime = 0L }
                        displayMode.value = mode
                        if (mode == DisplayMode.CLOCK) displayColor.value = Color.Red
                    }
                },
                onSetClockTemplate = { template ->
                    runOnUiThread {
                        val t = template.coerceIn(1, 3)
                        clockTemplate.value = t
                        prefs.edit { putInt("clock_template", t) }
                    }
                },
                onBindRequest = { clientIp -> handleBindRequest(clientIp) },
                getAuthRecords = { authRecords.toList() },
                hasValidToken = { token -> authRecords.any { it.token == token } },
                removeAuthByToken = { runOnUiThread { removeAuthByToken(it) }; Unit },
                updateAuthDeviceInfo = { token, name, model, system, battery ->
                    runOnUiThread { updateAuthDeviceInfo(token, name, model, system, battery) }
                },
                getClockTemplate = { prefs.getInt("clock_template", 1).coerceIn(1, 3) },
                getPomodoroConfigs = { pomodoroConfigs.toList() },
                setPomodoroConfigs = { configs ->
                    runOnUiThread {
                        pomodoroConfigs.clear()
                        pomodoroConfigs.addAll(configs)
                        savePomodoroConfigs()
                    }
                },
                getKeySettings = {
                    fun getShort(key: String) = prefs.getString(key, null).takeIf { it in listOf("menu", "pomodoro") } ?: "pomodoro"
                    fun getLong(key: String) = prefs.getString(key, null).takeIf { it in listOf("menu", "pomodoro") } ?: "menu"
                    fun getDouble(key: String) = prefs.getString(key, null).takeIf { it in listOf("menu", "pomodoro", "none", "switch_pomodoro") } ?: "switch_pomodoro"
                    listOf(
                        getShort("key_vol_up_short"),
                        getLong("key_vol_up_long"),
                        getDouble("key_vol_up_double"),
                        getShort("key_vol_down_short"),
                        getLong("key_vol_down_long"),
                        getDouble("key_vol_down_double")
                    )
                },
                setKeySettings = { volUpShort, volUpLong, volUpDouble, volDownShort, volDownLong, volDownDouble ->
                    fun normShort(s: String) = s.takeIf { it in listOf("menu", "pomodoro") } ?: "pomodoro"
                    fun normLong(s: String) = s.takeIf { it in listOf("menu", "pomodoro") } ?: "menu"
                    fun normDouble(s: String) = s.takeIf { it in listOf("menu", "pomodoro", "none", "switch_pomodoro") } ?: "switch_pomodoro"
                    prefs.edit {
                        putString("key_vol_up_short", normShort(volUpShort))
                        putString("key_vol_up_long", normLong(volUpLong))
                        putString("key_vol_up_double", normDouble(volUpDouble))
                        putString("key_vol_down_short", normShort(volDownShort))
                        putString("key_vol_down_long", normLong(volDownLong))
                        putString("key_vol_down_double", normDouble(volDownDouble))
                    }
                },
                getDeviceInfo = { deviceInfoCache!! },
                getPomodoroSessions = { pomodoroSessions.toList() },
                onLogOperation = { action, detail, ip -> runOnUiThread { addOpLog(action, detail ?: "", ip ?: "") } },
                getOpLogEntries = { offset, limit ->
                    synchronized(opLogEntries) {
                        if (pruneOpLogsLocked()) loadOpLogs()
                        val from = offset.coerceAtLeast(0).coerceAtMost(opLogEntries.size)
                        val to = (from + limit).coerceAtMost(opLogEntries.size)
                        opLogEntries.subList(from, to).toList()
                    }
                }
            )
            try { server?.start() } catch (e: Exception) { e.printStackTrace() }
        }
        server?.isWebInterfaceEnabled = isWebEnabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        hideSystemBars()

        prefs = getSharedPreferences("matrix_prefs", Context.MODE_PRIVATE)
        opLogDb = OpLogDbHelper(this)
        loadAuthRecords()
        clockTemplate.value = prefs.getInt("clock_template", 1).coerceIn(1, 3)
        isWebControlEnabled = prefs.getBoolean("web_control_enabled", true)
        is24HourFormatEnabled = prefs.getBoolean("time_24h_enabled", DateFormat.is24HourFormat(this))
        loadPomodoroConfigs()
        loadPomodoroSessions()
        loadOpLogs()

        initServer(isWebControlEnabled)
        deviceInfoCache = buildDeviceInfo()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF1E293B),
                    onSurface = Color.White,
                    primary = Color(0xFF38BDF8)
                )
            ) {
                var currentIp by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        currentIp = getLocalIpAddress()
                        delay(5000)
                    }
                }

                val context = LocalContext.current
                LaunchedEffect(displayMode.value, remainingSeconds.intValue, is24HourFormatEnabled) {
                    if (displayMode.value == DisplayMode.CLOCK) {
                        while (true) {
                            val now = Date()
                            val format = if (is24HourFormatEnabled) {
                                if (showColon.value) "HH:mm" else "HH mm"
                            } else {
                                if (showColon.value) "hh:mm" else "hh mm"
                            }
                            displayText.value = SimpleDateFormat(format, Locale.ROOT).format(now)
                            amPmText.value = if (is24HourFormatEnabled) "" else SimpleDateFormat("a", Locale.ENGLISH).format(now).uppercase(Locale.ROOT)
                            dateText.value = SimpleDateFormat("MM-dd EEE", Locale.ENGLISH).format(now).uppercase()
                            delay(1000)
                            showColon.value = !showColon.value
                        }
                    } else if (displayMode.value == DisplayMode.TEXT) {
                        if (remainingSeconds.intValue > 0) {
                            delay(1000)
                            remainingSeconds.intValue -= 1
                        } else if (remainingSeconds.intValue == 0) {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(200)
                            }
                            delay(5000)
                            endPomodoroSessionIfAny(completed = true)
                            displayMode.value = DisplayMode.CLOCK
                            displayColor.value = Color.Red
                        } else {
                            // remainingSeconds < 0 => persistent display; do nothing.
                        }
                    }
                }
                LaunchedEffect(clockTemplate.value) {
                    if (clockTemplate.value == 3) {
                        clockT3TimeMillis.value = System.currentTimeMillis()
                        while (true) {
                            clockT3TimeMillis.value = System.currentTimeMillis()
                            delay(1000)
                        }
                    }
                }

                val activeCycleConfigId = if (displayMode.value == DisplayMode.TEXT && sessionConfigId != null) {
                    sessionConfigId
                } else null
                val todayCompleted = remember(pomodoroSessionsVersion.intValue, activeCycleConfigId) {
                    getTodayCompletedPomodoroCount(activeCycleConfigId)
                }
                val activeCycleTotal = if (displayMode.value == DisplayMode.TEXT && sessionConfigId != null) {
                    pomodoroConfigs.getOrNull(currentPomoIndex)?.cycleTotal?.coerceIn(1, 99) ?: CYCLE_TOTAL
                } else 0
                Box(modifier = Modifier.fillMaxSize()) {
                    MatrixClockApp(
                        text = displayText.value,
                        dateInfo = dateText.value,
                        amPm = amPmText.value,
                        color = displayColor.value,
                        mode = displayMode.value,
                        clockTemplate = clockTemplate.value,
                        clockT3TimeMillis = clockT3TimeMillis.value,
                        progress = if (displayMode.value == DisplayMode.TEXT) {
                            val total = totalDurationSeconds.intValue
                            val remain = remainingSeconds.intValue
                            if (total > 0 && remain >= 0) {
                                remain.toFloat() / total.toFloat()
                            } else {
                                1f
                            }
                        } else 0f,
                        remainingSeconds = remainingSeconds.intValue,
                        countdownStyle = countdownStyle,
                        isBound = authRecords.isNotEmpty(),
                        ipAddress = currentIp,
                        cycleCompleted = todayCompleted,
                        cycleTotal = activeCycleTotal
                    )
                    
                    bindRequestIp?.let { ip ->
                        AuthDialog(
                            ip = ip,
                            onAllow = {
                                val newToken = UUID.randomUUID().toString()
                                addAuthRecord(AuthRecord(token = newToken, ip = bindRequestIp ?: ""))
                                currentBindFuture?.complete(newToken)
                                bindRequestIp = null
                            },
                            onDeny = {
                                currentBindFuture?.complete(null)
                                bindRequestIp = null
                            }
                        )
                    }
                    if (showSettingsDialog) {
                        SettingsDialog(
                            ipAddress = currentIp,
                            isWebControlEnabled = isWebControlEnabled,
                            onToggleWebControl = { enabled ->
                                isWebControlEnabled = enabled
                                prefs.edit { putBoolean("web_control_enabled", enabled) }
                                server?.isWebInterfaceEnabled = enabled
                            },
                            is24HourFormat = is24HourFormatEnabled,
                            onToggle24HourFormat = { enabled ->
                                is24HourFormatEnabled = enabled
                                prefs.edit { putBoolean("time_24h_enabled", enabled) }
                            },
                            onDismiss = { showSettingsDialog = false },
                            onClearAuth = {
                                authRecords.clear()
                                saveAuthRecords()
                                showSettingsDialog = false
                            },
                            authRecords = authRecords.toList(),
                            onRevokeAuth = { token ->
                                removeAuthByToken(token)
                            },
                            onOpenAuthManagement = { showAuthManagementDialog = true }
                        )
                    }
                    if (showAuthManagementDialog) {
                        AuthManagementDialog(
                            authRecords = authRecords.toList(),
                            onRevoke = { removeAuthByToken(it) },
                            onDismiss = { showAuthManagementDialog = false }
                        )
                    }
                }
            }
        }
    }

    private fun handleBindRequest(clientIp: String): String? {
        val future = CompletableFuture<String?>()
        currentBindFuture = future
        runOnUiThread {
            bindRequestIp = clientIp
        }
        return try { future.get() } catch (e: Exception) { null }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0) {
                currentVolumeKeyCode = keyCode
                volumeLongPressTriggered = false
                volumeKeyWasRepeated = false
                volumeLongPressRunnable = Runnable {
                    volumeLongPressTriggered = true
                    runKeyAction(getKeyBinding(currentVolumeKeyCode, longPress = true))
                }
                volumeKeyHandler.postDelayed(volumeLongPressRunnable!!, volumeLongPressMs)
            } else {
                volumeKeyWasRepeated = true
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeLongPressRunnable?.let { volumeKeyHandler.removeCallbacks(it) }
            volumeLongPressRunnable = null
            if (!volumeLongPressTriggered && !volumeKeyWasRepeated) {
                if (pendingShortKeyCode == keyCode) {
                    volumeShortPressRunnable?.let { volumeKeyHandler.removeCallbacks(it) }
                    volumeShortPressRunnable = null
                    pendingShortKeyCode = 0
                    runKeyAction(getKeyBinding(keyCode, longPress = false, doubleTap = true))
                } else {
                    volumeShortPressRunnable?.let { volumeKeyHandler.removeCallbacks(it) }
                    val key = keyCode
                    volumeShortPressRunnable = Runnable {
                        runKeyAction(getKeyBinding(key, longPress = false))
                        pendingShortKeyCode = 0
                    }
                    pendingShortKeyCode = key
                    volumeKeyHandler.postDelayed(volumeShortPressRunnable!!, volumeDoubleTapMs)
                }
            }
            volumeLongPressTriggered = false
            volumeKeyWasRepeated = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        deviceInfoCache = buildDeviceInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        if (::opLogDb.isInitialized) opLogDb.close()
    }
}
