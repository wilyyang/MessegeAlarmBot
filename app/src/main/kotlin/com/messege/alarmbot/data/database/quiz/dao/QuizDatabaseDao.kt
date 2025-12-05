package com.messege.alarmbot.data.database.quiz.dao

import androidx.room.*
import com.messege.alarmbot.data.database.quiz.model.QuizData

@Dao
interface QuizDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizData): Long

    @Query("SELECT * FROM QuizData ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuiz(): QuizData?

    @Query("SELECT * FROM QuizData WHERE idx = :idx")
    suspend fun getSelectQuiz(idx: Long): QuizData?

    @Query("DELETE FROM QuizData WHERE idx = :idx")
    suspend fun deleteQuiz(idx: Long): Int
}