package com.messege.alarmbot.data.network.topic.di

import android.content.Context
import com.messege.alarmbot.data.network.topic.TopicDatabaseHelper
import com.messege.alarmbot.data.network.topic.dao.TopicDatabaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltTopicDatabase {
    @Singleton
    @Provides
    fun provideTopicDatabaseHelper(
        @ApplicationContext context : Context
    ) = TopicDatabaseHelper.getDataBase(context, CoroutineScope(SupervisorJob()))

    @Singleton
    @Provides
    fun provideTopicDatabaseDao(databaseHelper : TopicDatabaseHelper) : TopicDatabaseDao = databaseHelper.topicDatabaseDao()
}