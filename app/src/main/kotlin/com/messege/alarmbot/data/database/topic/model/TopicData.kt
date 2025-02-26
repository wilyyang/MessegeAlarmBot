package com.messege.alarmbot.data.database.topic.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TopicData")
@Keep
data class TopicData(
    val updateTime: Long,
    val userKey: String,
    val userName: String,
    val topic: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}