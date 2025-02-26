package com.messege.alarmbot.data.database.topic.dao

import androidx.room.*
import com.messege.alarmbot.data.database.topic.model.TopicData

@Dao
interface TopicDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicData): Long

    @Query("SELECT * FROM TopicData ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTopic(): TopicData?

    @Query("SELECT * FROM TopicData WHERE idx = :idx")
    suspend fun getSelectTopic(idx: Long): TopicData?

    @Query("DELETE FROM TopicData WHERE idx = :idx")
    suspend fun deleteTopic(idx: Long): Int
}