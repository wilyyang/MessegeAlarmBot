package com.messege.alarmbot.data.database.message.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MessageData")
@Keep
data class MessageData(
    val postTime: Long,
    val message: String,
    val userName: String,
    val userKey: String,
    val roomName: String,
    val roomKey: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}