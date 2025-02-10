package com.messege.alarmbot.data.network.topic.dao

import androidx.room.*
import com.messege.alarmbot.data.network.topic.model.TopicData

@Dao
interface TopicDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicData)

    @Query("SELECT * FROM TopicData ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTopic(): TopicData?
}