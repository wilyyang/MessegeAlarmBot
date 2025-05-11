package com.messege.alarmbot.data.database.topic.dao

import androidx.room.*
import com.messege.alarmbot.data.database.topic.model.TopicData
import com.messege.alarmbot.data.database.topic.model.TopicReplyData

@Dao
interface TopicDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicData): Long

    @Query("SELECT * FROM TopicData ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTopic(): TopicData?

    @Query("SELECT * FROM TopicData WHERE idx = :idx")
    suspend fun getSelectTopic(idx: Long): TopicData?

    @Query("SELECT * FROM TopicData WHERE idx BETWEEN :start AND :end")
    suspend fun getSelectTopics(start: Long, end: Long): List<TopicData>

    @Query("DELETE FROM TopicData WHERE idx = :idx")
    suspend fun deleteTopic(idx: Long): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplyTopic(reply: TopicReplyData): Long

    @Query("SELECT * FROM TopicReplyData WHERE topicKey = :topicKey")
    suspend fun getSelectReplyList(topicKey: Long): List<TopicReplyData>

    @Query("DELETE FROM TopicReplyData WHERE topicKey = :topicKey")
    suspend fun deleteReplyLists(topicKey: Long): Int

    @Query("DELETE FROM TopicReplyData WHERE idx = :idx")
    suspend fun deleteReplyTopic(idx: Long): Int

    @Query("""
    DELETE FROM TopicReplyData 
    WHERE idx = (
        SELECT idx FROM TopicReplyData 
        WHERE topicKey = :topicKey 
        ORDER BY updateTime ASC 
        LIMIT 1 OFFSET :orderNumber
    )
    """)
    suspend fun deleteReplyTopicOrderNumber(topicKey: Long, orderNumber: Int): Int
}