package com.messege.alarmbot.kakao

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import com.google.gson.Gson
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

data class ChatLog(
    val id: Long,
    val type: Int?,
    val chatId: Long,
    val userId: Long?,
    val message: String?,
    val attachment: String?,
    val createdAt: Long?,
    val deletedAt: Long?,
    val clientMessageId: Long?,
    val prevId: Long?,
    val referer: Int?,
    val supplement: String?,
    val v: ChatMetadata?
)

data class ChatMetadata(
    val notDecoded: Boolean,
    val origin: String,
    val c: String,
    val modifyRevision: Int,
    val isSingleDefaultEmoticon: Boolean,
    val defaultEmoticonsCount: Int,
    val isMine: Boolean,
    val enc: Int
)

data class MessageData(
    val time: Long,
    val userid: Long? = null,
    val enc : Int? = null,
    val message: String
)

class ChatLogsObserver(private val applicationContext: Context, private val dbName: String) {
    private var lastLogId: Int = -1

    fun observeChatLogs(): Flow<MessageData> = flow {
        val dbPath = "${Environment.getExternalStorageDirectory()}/$dbName"
        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            emit(MessageData(0L, null, null, ""))
            return@flow
        }

        val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)

        while (true) {
            val cursor: Cursor = database.rawQuery("SELECT * FROM chat_logs ORDER BY _id DESC LIMIT 1", null)

            if (cursor.moveToFirst()) {
                val logId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))

                if (logId != lastLogId) {
                    lastLogId = logId
                    val chatLog = cursor.getChatLog()

                    chatLog?.takeIf {
                        it.userId != null && it.v?.enc != null && it.message != null && it.createdAt != null
                    }?.run {
                        val decryptMessage = KakaoDecrypt.decrypt(userId!!,  v!!.enc, message!!)
                        emit(MessageData(createdAt!! * 1000, userId, v.enc, decryptMessage))
                    }
                }
            }

            cursor.close()
            delay(500)
        }
    }

    fun Cursor.getChatLog(): ChatLog? {

        return if (moveToFirst()) {
            val gson = Gson()
            val vJson = getStringOrNull("v") // JSON 문자열 가져오기
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