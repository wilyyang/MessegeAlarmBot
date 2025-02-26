package com.messege.alarmbot.data.database

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.data.database.topic.model.TopicData
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.data.database.user.model.UserNameData
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        UserNameData::class,
        TopicData::class
    ]
)
abstract class ChatBotDatabaseHelper : RoomDatabase() {
    abstract fun userDatabaseDao() : UserDatabaseDao
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