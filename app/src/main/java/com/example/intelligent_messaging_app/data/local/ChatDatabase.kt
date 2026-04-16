package com.example.intelligent_messaging_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.intelligent_messaging_app.data.local.dao.ConflictDao
import com.example.intelligent_messaging_app.data.local.dao.ConversationDao
import com.example.intelligent_messaging_app.data.local.dao.MessageDao
import com.example.intelligent_messaging_app.data.local.dao.OutboxDao
import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import com.example.intelligent_messaging_app.data.local.entity.ConversationEntity
import com.example.intelligent_messaging_app.data.local.entity.MessageEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxEntity
import com.example.intelligent_messaging_app.data.local.entity.SyncStateEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        OutboxEntity::class,
        SyncStateEntity::class,
        ConflictEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun outboxDao(): OutboxDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conflictDao(): ConflictDao
}
