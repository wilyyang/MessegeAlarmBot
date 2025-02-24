package com.messege.alarmbot.kakao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import com.google.gson.Gson
import com.messege.alarmbot.kakao.model.ChatLog
import com.messege.alarmbot.kakao.model.ChatMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

const val INIT_LOG_ID = 0L
class ChatLogsObserver {
    private var lastLogId: Long = INIT_LOG_ID
    private val dbName = "KakaoTalk.db"

    fun observeChatLogs(): Flow<ChatLog> = flow {
        val dbPath = "${Environment.getExternalStorageDirectory()}/$dbName"
        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            emit(ChatLog(INIT_LOG_ID, null, 0L, 0L))
            return@flow
        }

        val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)

        while (true) {
            val cursor: Cursor = database.rawQuery("SELECT * FROM chat_logs ORDER BY _id DESC LIMIT 1", null)

            if (cursor.moveToFirst()) {
                val logId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))

                if (logId != lastLogId) {
                    lastLogId = logId
                    val chatLog = cursor.getChatLog()

                    chatLog?.takeIf {
                        it.userId != null && it.v?.enc != null && it.message != null && it.createdAt != null
                    }?.run {
                        val decryptMessage = KakaoDecrypt.decrypt(userId!!,  v!!.enc, message!!)
                        emit(
                            chatLog.copy(
                                message = decryptMessage,
                            )
                        )
                    }
                }
            }

            cursor.close()
            delay(500)
        }
    }

    private fun Cursor.getChatLog(): ChatLog? {
        return if (moveToFirst()) {
            val gson = Gson()
            val vJson = getStringOrNull("v")
            val chatMetadata = vJson?.let { gson.fromJson(it, ChatMetadata::class.java) }
            val chatLog = ChatLog(
                id = getLong(getColumnIndexOrThrow("id")),
                type = getIntOrNull("type"),
                chatId = getLong(getColumnIndexOrThrow("chat_id")),
                userId = getLongOrNull("user_id"),
                message = getStringOrNull("message"),
                attachment = getStringOrNull("attachment"),
                createdAt = getLongOrNull("created_at"),
                deletedAt = getLongOrNull("deleted_at"),
                clientMessageId = getLongOrNull("client_message_id"),
                prevId = getLongOrNull("prev_id"),
                referer = getIntOrNull("referer"),
                supplement = getStringOrNull("supplement"),
                v = chatMetadata
            )
            close()
            chatLog
        } else {
            close()
            null
        }
    }

    private fun Cursor.getIntOrNull(columnName: String): Int? {
        val index = getColumnIndex(columnName)
        return if (index != -1 && !isNull(index)) getInt(index) else null
    }

    private fun Cursor.getLongOrNull(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index != -1 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index != -1 && !isNull(index)) getString(index) else null
    }
}