package com.messege.alarmbot.data.database.message.di

import android.content.Context
import com.messege.alarmbot.data.database.message.MessageDatabaseHelper
import com.messege.alarmbot.data.database.message.dao.MessageDatabaseDao
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
class HiltMessageDatabase {
    @Singleton
    @Provides
    fun provideMessageDatabaseHelper(
        @ApplicationContext context : Context
    ) = MessageDatabaseHelper.getDataBase(context, CoroutineScope(SupervisorJob()))

    @Singleton
    @Provides
    fun provideMessageDatabaseDao(databaseHelper : MessageDatabaseHelper) : MessageDatabaseDao = databaseHelper.messageDatabaseDao()
}