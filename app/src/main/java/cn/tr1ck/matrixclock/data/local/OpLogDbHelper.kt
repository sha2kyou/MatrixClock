package cn.tr1ck.matrixclock.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cn.tr1ck.matrixclock.data.model.OpLogEntry

class OpLogDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_OP_LOG (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time_millis INTEGER NOT NULL,
                action TEXT NOT NULL,
                detail TEXT NOT NULL,
                ip TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_op_log_time ON $TABLE_OP_LOG(time_millis DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(entry: OpLogEntry) {
        val v = ContentValues()
        v.put("time_millis", entry.timeMillis)
        v.put("action", entry.action)
        v.put("detail", entry.detail)
        v.put("ip", entry.ip)
        writableDatabase.insert(TABLE_OP_LOG, null, v)
    }

    fun trim(retentionMs: Long, maxRows: Int) {
        val db = writableDatabase
        val cutoff = System.currentTimeMillis() - retentionMs
        db.delete(TABLE_OP_LOG, "time_millis < ?", arrayOf(cutoff.toString()))
        db.execSQL(
            """
            DELETE FROM $TABLE_OP_LOG
            WHERE id NOT IN (
                SELECT id FROM $TABLE_OP_LOG
                ORDER BY time_millis DESC, id DESC
                LIMIT $maxRows
            )
            """.trimIndent()
        )
    }

    fun queryRecent(limit: Int): List<OpLogEntry> {
        return queryPaged(offset = 0, limit = limit)
    }

    fun queryPaged(offset: Int, limit: Int): List<OpLogEntry> {
        val db = readableDatabase
        val list = mutableListOf<OpLogEntry>()
        db.rawQuery(
            "SELECT time_millis, action, detail, ip FROM $TABLE_OP_LOG ORDER BY time_millis DESC, id DESC LIMIT ? OFFSET ?",
            arrayOf(limit.toString(), offset.coerceAtLeast(0).toString())
        ).use { c ->
            val idxTime = c.getColumnIndexOrThrow("time_millis")
            val idxAction = c.getColumnIndexOrThrow("action")
            val idxDetail = c.getColumnIndexOrThrow("detail")
            val idxIp = c.getColumnIndexOrThrow("ip")
            while (c.moveToNext()) {
                list += OpLogEntry(
                    timeMillis = c.getLong(idxTime),
                    action = c.getString(idxAction) ?: "",
                    detail = c.getString(idxDetail) ?: "",
                    ip = c.getString(idxIp) ?: ""
                )
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "matrixclock.db"
        private const val DB_VERSION = 1
        private const val TABLE_OP_LOG = "op_log"
    }
}
