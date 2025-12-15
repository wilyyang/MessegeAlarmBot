package com.messege.alarmbot.data.database.quiz.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QuizData")
@Keep
data class QuizData(
    val updateTime: Long,
    val userKey: Long,
    val category: String,
    val difficulty: Int,
    val quiz: String,
    val answer: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}