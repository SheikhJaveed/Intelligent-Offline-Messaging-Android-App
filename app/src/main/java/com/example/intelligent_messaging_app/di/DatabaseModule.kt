package com.example.intelligent_messaging_app.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.intelligent_messaging_app.data.local.ChatDatabase
import com.example.intelligent_messaging_app.data.local.dao.ConflictDao
import com.example.intelligent_messaging_app.data.local.dao.ConversationDao
import com.example.intelligent_messaging_app.data.local.dao.MessageDao
import com.example.intelligent_messaging_app.data.local.dao.OutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "chat_database"
        ).build()
    }

    @Provides
    fun provideMessageDao(database: ChatDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideOutboxDao(database: ChatDatabase): OutboxDao = database.outboxDao()

    @Provides
    fun provideConversationDao(database: ChatDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideConflictDao(database: ChatDatabase): ConflictDao = database.conflictDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
