package com.messege.alarmbot.data.database.user.dao

import androidx.room.*
import com.messege.alarmbot.data.database.user.model.UserData

@Dao
interface UserDatabaseDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserData)

    @Query("SELECT name FROM UserData WHERE userKey = :userKey ORDER BY updateTime DESC")
    suspend fun getUserNames(userKey: String) : List<String>
}