package com.messege.alarmbot.kakao

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

data class MessageData(
    val time: Long,
    val message: String
)

class ChatLogsObserver(private val applicationContext: Context, private val dbName: String) {
    private var lastLogId: Int = -1

    fun observeChatLogs(): Flow<MessageData> = flow {
        val dbFile = File(applicationContext.getDatabasePath(dbName).absolutePath)
        if (!dbFile.exists()) {
            emit(MessageData(0L, ""))
            return@flow
        }

        val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)

        while (true) {
            val cursor: Cursor = database.rawQuery("SELECT * FROM chat_logs ORDER BY _id DESC LIMIT 1", null)

            if (cursor.moveToFirst()) {
                val logId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))

                if (logId != lastLogId) {
                    lastLogId = logId

                    val time = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    val message = cursor.getString(cursor.getColumnIndexOrThrow("message"))

                    emit(MessageData(time * 1000, message))
                }
            }

            cursor.close()
            delay(500)
        }
    }
}