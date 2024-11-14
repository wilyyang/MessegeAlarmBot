package com.messege.alarmbot.data.database.user.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserData")
@Keep
data class UserData(
    val updateTime: Long,
    val userKey: String,
    val name: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}