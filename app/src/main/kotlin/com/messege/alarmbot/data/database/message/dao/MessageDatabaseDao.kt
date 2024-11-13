package com.messege.alarmbot.data.database.message.dao

import androidx.room.*
import com.messege.alarmbot.data.database.message.model.MessageData

@Dao
interface MessageDatabaseDao {

    @Query("SELECT * FROM MessageData")
    suspend fun getMessageAll() : List<MessageData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageData)

    @Query("DELETE FROM MessageData WHERE idx < (SELECT idx FROM MessageData ORDER BY idx DESC LIMIT 1 OFFSET 300000);")
    suspend fun deleteMessage()

    @Query("DELETE FROM MessageData")
    suspend fun deleteMessageAll()
}