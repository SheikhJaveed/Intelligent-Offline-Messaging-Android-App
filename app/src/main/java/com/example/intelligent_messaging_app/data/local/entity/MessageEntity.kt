package com.example.intelligent_messaging_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.intelligent_messaging_app.domain.model.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val clientMessageId: String,
    val serverId: String? = null,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val version: Long = 0
)
