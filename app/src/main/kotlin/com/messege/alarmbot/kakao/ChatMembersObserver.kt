package com.messege.alarmbot.kakao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.os.FileObserver
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.DECRYPT_MEMBER_KEY
import com.messege.alarmbot.kakao.model.ChatMember
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

data class MemberColumnIndex(
    val userId: Int,
    val type: Int,
    val nickName: Int,
    val chatId: Int,
    val privilege: Int,
    val enc: Int
)

class ChatMembersObserver : CoroutineScope {
    private val dbName = "KakaoTalk2.db"
    private val dbPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}/ChatBot/kakao/$dbName"
    private val dbWalPath = "$dbPath-wal"
    private val dbFile = File(dbPath)
    private val database: SQLiteDatabase by lazy {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    private val columnIndex: MemberColumnIndex by lazy {
        database.rawQuery("SELECT * FROM open_chat_member LIMIT 1", null).use { cursor ->
            require(cursor.moveToFirst()) { "open_chat_member 테이블이 비어 있음" }

            fun getCheckedColumnIndex(columnName: String): Int {
                return cursor.getColumnIndex(columnName).also { index ->
                    require(index != -1) { "컬럼 '$columnName'이 존재하지 않음" }
                }
            }

            MemberColumnIndex(
                userId = getCheckedColumnIndex("user_id"),
                type = getCheckedColumnIndex("profile_type"),
                nickName = getCheckedColumnIndex("nickname"),
                chatId = getCheckedColumnIndex("involved_chat_id"),
                privilege = getCheckedColumnIndex("privilege"),
                enc  = getCheckedColumnIndex("enc"),
            )
        }
    }

    private val isObserving = AtomicBoolean(false)

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val mutex = Mutex()

    private val latestJob = AtomicReference<Job?>(null)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Complete
     */
    fun observeChatMembers(): Flow<List<ChatMember>> = channelFlow  {
        if (!isObserving.compareAndSet(false, true)) return@channelFlow
        if (!dbFile.exists()) {
            send(emptyList())
            return@channelFlow
        }

        val initMembers = getGroupMembers()
        send(initMembers)

        val eventChannel = Channel<Unit>(Channel.CONFLATED)

        val observer = object : FileObserver(dbWalPath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    eventChannel.trySend(Unit)
                }
            }
        }

        observer.startWatching()

        val eventJob = launch {
            for (event in eventChannel) {
                delay(300)

                mutex.withLock {
                    val members = getGroupMembers()
                    send(members)
                }
            }
        }

        awaitClose {
            observer.stopWatching()
            eventJob.cancel()
        }
    }

    private fun getGroupMembers(): List<ChatMember> {
        val members = mutableListOf<ChatMember>()
        database.rawQuery(
            "SELECT * FROM open_chat_member WHERE involved_chat_id = ${ChatRoomType.GroupRoom1.roomKey} OR involved_chat_id = ${ChatRoomType.GroupRoom2.roomKey}",
            null
        ).use { cursor ->

            if(cursor.moveToFirst()){
                do {
                    val mem = cursor.getChatMember()
                    val decryptMem = mem.copy(
                        nickName = KakaoDecrypt.decrypt(userId = DECRYPT_MEMBER_KEY, encType = mem.enc, b64Ciphertext = mem.nickName)
                    )
                    members.add(decryptMem)
                }while (cursor.moveToNext())
            }
        }
        return members
    }

    private fun Cursor.getChatMember(): ChatMember {
        return ChatMember(
            userId = getLong(columnIndex.userId),
            type = getInt(columnIndex.type),
            nickName = getString(columnIndex.nickName),
            chatId = getLong(columnIndex.chatId),
            privilege = getInt(columnIndex.privilege),
            enc = getInt(columnIndex.enc),
        )
    }

    suspend fun walCheckpoint(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                database.rawQuery("PRAGMA wal_checkpoint(FULL);", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val totalPages = cursor.getInt(0)
                        val checkPointedPages = cursor.getInt(1)
                        val busyFlag = cursor.getInt(2)
                        Logger.i("WAL checkpoint: total=$totalPages, done=$checkPointedPages, busy=$busyFlag")
                        true
                    } else {
                        Logger.w("WAL checkpoint: 커서 이동 실패")
                        false
                    }
                }
            } catch (e: Exception) {
                Logger.e("[error] WAL checkpoint 실패: ${e.message}")
                false
            }
        }
    }

    suspend fun getChatMember(userId: Long) : ChatMember? {
        return database.rawQuery(
            "SELECT * FROM open_chat_member WHERE user_id = $userId",
            null
        ).use { cursor ->
            if(cursor.moveToFirst()){
                val mem = cursor.getChatMember()
                val decryptMem = mem.copy(
                    nickName = KakaoDecrypt.decrypt(userId = DECRYPT_MEMBER_KEY, encType = mem.enc, b64Ciphertext = mem.nickName)
                )
                decryptMem
            } else null
        }
    }
}