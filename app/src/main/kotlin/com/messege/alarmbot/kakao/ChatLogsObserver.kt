package com.messege.alarmbot.kakao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.os.FileObserver
import com.google.gson.Gson
import com.messege.alarmbot.kakao.model.ChatLog
import com.messege.alarmbot.kakao.model.ChatMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

const val INIT_LOG_ID = 0L

class ChatLogsObserver : CoroutineScope {
    private var lastLogId: Long = INIT_LOG_ID
    private val dbName = "KakaoTalk.db"
    private val dbPath = "${Environment.getExternalStorageDirectory()}/$dbName"
    private val dbWalPath = "$dbPath-wal"
    private val dbFile = File(dbPath)
    private var database: SQLiteDatabase? = null

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun observeChatLogs(): Flow<ChatLog> = channelFlow  {
        if (!dbFile.exists()) {
            send(ChatLog(INIT_LOG_ID, null, 0L, 0L))
            return@channelFlow
        }

        database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        lastLogId = getLatestLogId()

        val observer = object : FileObserver(dbWalPath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    launch {
                        fetchNewLogs()?.let { logs ->
                            logs.forEach { send(it) }
                        }
                    }
                }
            }
        }

        observer.startWatching()

        awaitClose { observer.stopWatching() }
    }

    private fun getLatestLogId() : Long {
        return database?.rawQuery(
            "SELECT _id FROM chat_logs ORDER BY _id DESC LIMIT 1",
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            } else {
                INIT_LOG_ID
            }
        } ?: INIT_LOG_ID
    }


    private fun fetchNewLogs(): List<ChatLog>? {
        println("WILLY >> Call $lastLogId ")
        return database?.rawQuery(
            "SELECT * FROM chat_logs WHERE _id > ? ORDER BY _id ASC",
            arrayOf(lastLogId.toString())
        )?.use { cursor ->  // use{} 블록으로 자동 닫기
            val logs = mutableListOf<ChatLog>()

            while (cursor.moveToNext()) {
                val logId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                println("WILLY >> $lastLogId -> $logId")
                lastLogId = logId
                val chatLog = cursor.getChatLog()
                chatLog.takeIf {
                    it.userId != null && it.v?.enc != null && it.message != null && it.createdAt != null
                }?.run {
                    val decryptMessage = KakaoDecrypt.decrypt(userId!!, v!!.enc, message!!)

                    println("WILLY >> ${copy(message = decryptMessage)}")
                    logs.add(copy(message = decryptMessage))
                }
            }
            logs
        }
    }

    private fun Cursor.getChatLog(): ChatLog {
        val gson = Gson()
        val vJson = getStringOrNull("v")
        val chatMetadata = vJson?.let { gson.fromJson(it, ChatMetadata::class.java) }
        return ChatLog(
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