package com.messege.alarmbot.data.database

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.*
import com.messege.alarmbot.data.database.party.model.PartyData
import com.messege.alarmbot.data.database.party.model.PartyLog
import com.messege.alarmbot.data.database.party.model.PartyRule
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.data.database.topic.model.TopicData
import com.messege.alarmbot.data.database.topic.model.TopicReplyData
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Database(
    version = 5,
    exportSchema = true,
    entities = [
        MemberData::class,
        NicknameData::class,
        AdminLogData::class,
        ChatProfileData::class,
        DeleteTalkData::class,
        EnterData::class,
        KickData::class,
        SanctionData::class,
        LikeData::class,
        DislikeData::class,
        PartyChangeData::class,

        TopicData::class,
        TopicReplyData::class,

        PartyData::class,
        PartyRule::class,
        PartyLog::class,
    ]
)
abstract class ChatBotDatabaseHelper : RoomDatabase() {
    abstract fun memberDatabaseDao() : MemberDatabaseDao
    abstract fun topicDatabaseDao() : TopicDatabaseDao

    companion object {

        @Volatile
        private var instance : ChatBotDatabaseHelper? = null

        fun getDataBaseHelper(contextApplication : Context, scope : CoroutineScope) : ChatBotDatabaseHelper {
            return instance ?: synchronized(this) {
                val externalPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ChatBot/chatbot.db")
                val databaseHelper = Room.databaseBuilder(
                    contextApplication,
                    ChatBotDatabaseHelper::class.java,
                    externalPath.absolutePath
                )
                .addCallback(CallbackDatabaseChatbot(scope))
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .build()

                instance = databaseHelper
                databaseHelper
            }
        }

        private class CallbackDatabaseChatbot(
            private val scope : CoroutineScope,
        ) : Callback() {
            override fun onCreate(db : SupportSQLiteDatabase) {
                super.onCreate(db)
            }
        }
    }
}