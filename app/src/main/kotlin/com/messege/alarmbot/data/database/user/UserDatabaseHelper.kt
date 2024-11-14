package com.messege.alarmbot.data.database.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.data.database.user.model.UserData
import kotlinx.coroutines.CoroutineScope

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        UserData::class
    ]
)
abstract class UserDatabaseHelper : RoomDatabase() {
    abstract fun userDatabaseDao() : UserDatabaseDao

    companion object {

        @Volatile
        private var instance : UserDatabaseHelper? = null

        fun getDataBase(contextApplication : Context, scope : CoroutineScope) : UserDatabaseHelper {
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(contextApplication, UserDatabaseHelper::class.java, "user")
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