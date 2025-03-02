package com.messege.alarmbot.data.database.topic.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TopicData")
@Keep
data class TopicData(
    val updateTime: Long,
    val userKey: Long,
    val userName: String,
    val topic: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "TopicReplyData")
@Keep
data class TopicReplyData(
    val topicKey: Long,
    val updateTime: Long,
    val userKey: Long,
    val userName: String,
    val reply: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}