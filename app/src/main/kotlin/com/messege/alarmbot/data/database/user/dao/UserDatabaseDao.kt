package com.messege.alarmbot.data.database.user.dao

import androidx.room.*
import com.messege.alarmbot.data.database.user.model.UserNameData

@Dao
interface UserDatabaseDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserNameData)

    @Query("SELECT name FROM UserNameData WHERE userKey = :userKey ORDER BY updateTime DESC")
    suspend fun getUserNames(userKey: String) : List<String>
}