package com.example.intelligent_messaging_app.di

import com.example.intelligent_messaging_app.data.repository.MessageRepositoryImpl
import com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository
import com.example.intelligent_messaging_app.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository = messageRepositoryImpl
}
