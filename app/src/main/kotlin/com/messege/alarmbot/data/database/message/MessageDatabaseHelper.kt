package com.messege.alarmbot.data.database.message

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.data.database.message.dao.MessageDatabaseDao
import com.messege.alarmbot.data.database.message.model.MessageData
import kotlinx.coroutines.CoroutineScope

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        MessageData::class
    ]
)
abstract class MessageDatabaseHelper : RoomDatabase() {
    abstract fun messageDatabaseDao() : MessageDatabaseDao

    companion object {

        @Volatile
        private var instance : MessageDatabaseHelper? = null

        fun getDataBase(contextApplication : Context, scope : CoroutineScope) : MessageDatabaseHelper {
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(contextApplication, MessageDatabaseHelper::class.java, "message")
                    .addCallback(CallbackDatabaseMessage(scope))
                    .build()
                instance = database
                database
            }
        }

        private class CallbackDatabaseMessage(
            private val scope : CoroutineScope,
        ) : Callback() {
            override fun onCreate(db : SupportSQLiteDatabase) {
                super.onCreate(db)
            }
        }
    }
}