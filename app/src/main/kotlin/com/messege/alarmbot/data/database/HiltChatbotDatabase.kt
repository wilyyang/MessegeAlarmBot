package com.messege.alarmbot.data.database

import android.content.Context
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
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
class HiltChatbotDatabase {

    @Singleton
    @Provides
    fun provideChatbotDatabaseHelper(
        @ApplicationContext context : Context
    ) = ChatBotDatabaseHelper.getDataBaseHelper(context, CoroutineScope(SupervisorJob()))

    @Singleton
    @Provides
    fun provideMemberDatabaseDao(databaseHelper : ChatBotDatabaseHelper) : MemberDatabaseDao = databaseHelper.memberDatabaseDao()

    @Singleton
    @Provides
    fun provideTopicDatabaseDao(databaseHelper : ChatBotDatabaseHelper) : TopicDatabaseDao = databaseHelper.topicDatabaseDao()
}