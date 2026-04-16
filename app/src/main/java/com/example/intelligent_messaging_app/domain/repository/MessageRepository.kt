package com.example.intelligent_messaging_app.domain.repository

import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import com.example.intelligent_messaging_app.data.local.entity.ConversationEntity
import com.example.intelligent_messaging_app.data.local.entity.MessageEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getConversations(): Flow<List<ConversationEntity>>
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>
    suspend fun sendMessage(conversationId: String, content: String)
    suspend fun retryMessage(clientMessageId: String)
    fun getPendingMessages(): Flow<List<OutboxEntity>>
    
    // Conflict Resolution
    fun getConflicts(): Flow<List<ConflictEntity>>
    suspend fun resolveConflict(clientMessageId: String, useLocal: Boolean)
}
