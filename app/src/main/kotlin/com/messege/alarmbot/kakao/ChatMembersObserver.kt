package com.messege.alarmbot.kakao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.os.FileObserver
import com.messege.alarmbot.kakao.model.ChatMember
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

    /**
     * Complete
     */
    fun observeChatMembers(): Flow<List<ChatMember>> = channelFlow  {
        if (!isObserving.compareAndSet(false, true)) return@channelFlow
        if (!dbFile.exists()) {
            send(listOf())
            return@channelFlow
        }


        updateAllMembers()
        val observer = object : FileObserver(dbWalPath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY) {
                    launch (Dispatchers.IO) {
                        mutex.withLock {

                        }
                    }
                }
            }
        }

        observer.startWatching()

        awaitClose { observer.stopWatching() }
    }

    private fun updateMember() {
        // open_chat_member 테이블의 수정된 member의 id 를 가져옴 (log든 머든 방법을 가리지 않고)
        // open_chat_member 에서 member의 id 값에 해당하는 멤버 가져오기
    }

    private fun updateAllMembers() {
        database.rawQuery(
            "SELECT * FROM open_chat_member",
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val mem = cursor.getChatMember()

                val decryptMem = mem.copy(
                    nickName = KakaoDecrypt.decrypt(userId = 421448547, encType = mem.enc, b64Ciphertext = mem.nickName)
                )

                Logger.e("$decryptMem")
                // TODO
            }
        }
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
}