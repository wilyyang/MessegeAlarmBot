package com.messege.alarmbot.data.database.user.di

import android.content.Context
import com.messege.alarmbot.data.database.user.UserDatabaseHelper
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
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
class HiltUserDatabase {
    @Singleton
    @Provides
    fun provideUserDatabaseHelper(
        @ApplicationContext context : Context
    ) = UserDatabaseHelper.getDataBase(context, CoroutineScope(SupervisorJob()))

    @Singleton
    @Provides
    fun provideUserDatabaseDao(databaseHelper : UserDatabaseHelper) : UserDatabaseDao = databaseHelper.userDatabaseDao()
}