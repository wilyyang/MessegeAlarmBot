package com.messege.alarmbot.kakao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.os.FileObserver
import com.google.gson.Gson
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.kakao.model.ChatLog
import com.messege.alarmbot.kakao.model.ChatMetadata
import com.messege.alarmbot.util.log.Logger
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

const val INIT_CREATED_AT = 0L
const val MESSAGE_LENGTH_LIMIT = 1200

data class ColumnIndex(
    val id: Int,
    val type : Int,
    val chatId : Int,
    val userId : Int,
    val message : Int,
    val attachment : Int,
    val createdAt : Int,
    val deletedAt : Int,
    val v : Int
)

class ChatLogsObserver(
    val getName : suspend (Long) -> String
) : CoroutineScope {
    private var lastCreateAt: Long = INIT_CREATED_AT
    private val dbName = "KakaoTalk.db"
    private val dbPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}/ChatBot/kakao/$dbName"
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
                v = getCheckedColumnIndex("v")
            )
        }
    }

    private val isObserving = AtomicBoolean(false)

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val mutex = Mutex()

    fun observeChatLogs(): Flow<Message> = channelFlow  {
        if (!isObserving.compareAndSet(false, true)) return@channelFlow
        if (!dbFile.exists()) {
            send(Message.None())
            return@channelFlow
        }
        lastCreateAt = getLatestLogCreateAt()

        val observer = object : FileObserver(dbWalPath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    launch (Dispatchers.IO) {
                        mutex.withLock {
                            fetchNewLogs()?.let { logs ->
                                logs.forEach {
                                    val message = it.toMessage(
                                        getName = getName,
                                        getLogText = { logId ->
                                            database.rawQuery(
                                                "SELECT * FROM chat_logs WHERE id=$logId",
                                                null
                                            )?.use { cursor ->
                                                if (cursor.moveToFirst()) {
                                                    val chatLog = cursor.getChatLog()
                                                    if(chatLog.userId != null && chatLog.v != null && chatLog.message != null){
                                                        KakaoDecrypt.decrypt(chatLog.userId, chatLog.v.enc, chatLog.message)
                                                    }else ""
                                                } else {
                                                    ""
                                                }
                                            }?:""
                                        }
                                    )
                                    send(message)
                                }
                            }
                        }
                    }
                }
            }
        }

        observer.startWatching()

        awaitClose { observer.stopWatching() }
    }

    private fun getLatestLogCreateAt() : Long {
        return database.rawQuery(
            "SELECT created_at FROM chat_logs ORDER BY created_at DESC LIMIT 1",
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            } else {
                INIT_CREATED_AT
            }
        } ?: INIT_CREATED_AT
    }


    private fun fetchNewLogs(): List<ChatLog>? {
        return database.rawQuery(
            "SELECT * FROM chat_logs WHERE created_at > ? ORDER BY created_at ASC",
            arrayOf(lastCreateAt.toString())
        )?.use { cursor ->
            val logs = mutableListOf<ChatLog>()

            while (cursor.moveToNext()) {
                val chatLog = cursor.getChatLog()
                chatLog.takeIf {
                    it.type != null && it.createdAt != null && it.v?.enc != null && it.userId != null
                            && it.message != null && it.message.length < MESSAGE_LENGTH_LIMIT
                }?.run {
                    try {
                        val decryptMessage = KakaoDecrypt.decrypt(userId!!, v!!.enc, message!!)
                        val decryptAttachment = if(!attachment.isNullOrBlank() && attachment != "{}"){
                            KakaoDecrypt.decrypt(userId, v.enc, attachment)
                        } else null

                        logs.add(copy(message = decryptMessage, attachment = decryptAttachment))
                    }catch (e : Exception){
                        Logger.e("[error] decrypt error : ${e.message}")
                    }
                    lastCreateAt = this.createdAt!!
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
            v = chatMetadata
        )
    }
}