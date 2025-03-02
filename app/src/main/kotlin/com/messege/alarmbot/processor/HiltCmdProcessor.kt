package com.messege.alarmbot.processor

import android.content.Context
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CmdProcessorEntryPoint {
    fun getCmdProcessor(): CmdProcessor
}

@Module
@InstallIn(SingletonComponent::class)
object CmdProcessorModule {
    @Singleton
    @Provides
    fun provideCmdProcessor(
        @ApplicationContext context : Context,
        memberDatabaseDao : MemberDatabaseDao,
        userDatabaseDao : UserDatabaseDao,
        topicDatabaseDao : TopicDatabaseDao
    ) : CmdProcessor {
        return CmdProcessor(context, memberDatabaseDao, userDatabaseDao, topicDatabaseDao)
    }
}