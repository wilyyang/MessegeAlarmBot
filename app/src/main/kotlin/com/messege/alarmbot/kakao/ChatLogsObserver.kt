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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

const val INIT_LOG_ID = 0L

data class ColumnIndex(
    val id: Int,
    val type : Int,
    val chatId : Int,
    val userId : Int,
    val message : Int,
    val attachment : Int,
    val createdAt : Int,
    val deletedAt : Int,
    val clientMessageId : Int,
    val prevId : Int,
    val referer : Int,
    val supplement : Int,
    val v : Int
)

class ChatLogsObserver : CoroutineScope {
    private var lastLogId: Long = INIT_LOG_ID
    private val dbName = "KakaoTalk.db"
    private val dbPath = "${Environment.getExternalStorageDirectory()}/$dbName"
    private val dbWalPath = "$dbPath-wal"
    private val dbFile = File(dbPath)
    private val database: SQLiteDatabase by lazy {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    private val columnIndex: ColumnIndex by lazy {
        database.rawQuery("SELECT * FROM chat_logs LIMIT 1", null).use { cursor ->
            require(cursor.moveToFirst()) { "chat_logs 테이블이 비어 있음" }

            fun getCheckedColumnIndex(columnName: String): Int {
                return cursor.getColumnIndex(columnName).also { index ->
                    require(index != -1) { "컬럼 '$columnName'이 존재하지 않음" }
                }
            }

            ColumnIndex(
                id = getCheckedColumnIndex("id"),
                type = getCheckedColumnIndex("type"),
                chatId = getCheckedColumnIndex("chat_id"),
                userId = getCheckedColumnIndex("user_id"),
                message = getCheckedColumnIndex("message"),
                attachment = getCheckedColumnIndex("attachment"),
                createdAt = getCheckedColumnIndex("created_at"),
                deletedAt = getCheckedColumnIndex("deleted_at"),
                clientMessageId = getCheckedColumnIndex("client_message_id"),
                prevId = getCheckedColumnIndex("prev_id"),
                referer = getCheckedColumnIndex("referer"),
                supplement = getCheckedColumnIndex("supplement"),
                v = getCheckedColumnIndex("v")
            )
        }
    }

    private val isObserving = AtomicBoolean(false)

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val mutex = Mutex()

    fun observeChatLogs(): Flow<ChatLog> = channelFlow  {
        if (!isObserving.compareAndSet(false, true)) return@channelFlow
        if (!dbFile.exists()) {
            send(ChatLog(INIT_LOG_ID, null, 0L, 0L))
            return@channelFlow
        }
        lastLogId = getLatestLogId()

        val observer = object : FileObserver(dbWalPath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    launch (Dispatchers.IO) {
                        mutex.withLock {
                            fetchNewLogs()?.let { logs ->
                                logs.forEach { send(it) }
                            }
                        }
                    }
                }
            }
        }

        observer.startWatching()

        awaitClose { observer.stopWatching() }
    }

    private fun getLatestLogId() : Long {
        return database.rawQuery(
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
        return database.rawQuery(
            "SELECT * FROM chat_logs WHERE _id > ? ORDER BY _id ASC",
            arrayOf(lastLogId.toString())
        )?.use { cursor ->
            val logs = mutableListOf<ChatLog>()

            while (cursor.moveToNext()) {
                val chatLog = cursor.getChatLog()
                chatLog.takeIf {
                    it.userId != null && it.v?.enc != null && it.message != null && it.createdAt != null
                }?.run {
                    val decryptMessage = KakaoDecrypt.decrypt(userId!!, v!!.enc, message!!)
                    logs.add(copy(message = decryptMessage))
                    //TODO CREATE_AT으로 변경
                    lastLogId = this.id
                }
            }
            logs
        }
    }

    private fun Cursor.getChatLog(): ChatLog {
        val gson = Gson()
        val vJson = getString(columnIndex.v)
        val chatMetadata = vJson?.let { gson.fromJson(it, ChatMetadata::class.java) }
        return ChatLog(
            id = getLong(columnIndex.id),
            type = getInt(columnIndex.type),
            chatId = getLong(columnIndex.chatId),
            userId = getLong(columnIndex.userId),
            message = getString(columnIndex.message),
            attachment = getString(columnIndex.attachment),
            createdAt = getLong(columnIndex.createdAt),
            deletedAt = getLong(columnIndex.deletedAt),
            clientMessageId = getLong(columnIndex.clientMessageId),
            prevId = getLong(columnIndex.prevId),
            referer = getInt(columnIndex.referer),
            supplement = getString(columnIndex.supplement),
            v = chatMetadata
        )
    }
}