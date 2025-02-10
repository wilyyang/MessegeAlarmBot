package com.messege.alarmbot.data.network.topic

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.data.network.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.data.network.topic.model.TopicData
import kotlinx.coroutines.CoroutineScope

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        TopicData::class
    ]
)
abstract class TopicDatabaseHelper : RoomDatabase() {
    abstract fun topicDatabaseDao() : TopicDatabaseDao

    companion object {

        @Volatile
        private var instance : TopicDatabaseHelper? = null

        fun getDataBase(contextApplication : Context, scope : CoroutineScope) : TopicDatabaseHelper {
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(contextApplication, TopicDatabaseHelper::class.java, "topic")
                    .addCallback(CallbackDatabaseUser(scope))
                    .build()
                instance = database
                database
            }
        }

        private class CallbackDatabaseUser(
            private val scope : CoroutineScope,
        ) : Callback() {
            override fun onCreate(db : SupportSQLiteDatabase) {
                super.onCreate(db)
            }
        }
    }
}