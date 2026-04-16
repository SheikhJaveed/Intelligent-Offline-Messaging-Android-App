package com.example.intelligent_messaging_app.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.intelligent_messaging_app.data.local.dao.ConflictDao
import com.example.intelligent_messaging_app.data.local.dao.ConversationDao
import com.example.intelligent_messaging_app.data.local.dao.MessageDao
import com.example.intelligent_messaging_app.data.local.dao.OutboxDao
import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import com.example.intelligent_messaging_app.data.local.entity.MessageEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxStatus
import com.example.intelligent_messaging_app.domain.model.MessageStatus
import com.example.intelligent_messaging_app.domain.repository.MessageRepository
import com.example.intelligent_messaging_app.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val outboxDao: OutboxDao,
    private val conversationDao: ConversationDao,
    private val conflictDao: ConflictDao,
    private val workManager: WorkManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : MessageRepository {

    override fun getConversations() = conversationDao.getAllConversations()

    override fun getMessages(conversationId: String) = messageDao.getMessagesForConversation(conversationId)

    override suspend fun sendMessage(conversationId: String, content: String) {
        val clientMessageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val currentUserName = userPreferencesRepository.userName.first() ?: "Anonymous"
        
        // 1. Bounded Growth: Evict oldest if queue is too large
        val maxQueueSize = 100
        val currentCount = outboxDao.getOutboxCount()
        if (currentCount >= maxQueueSize) {
            outboxDao.evictOldest(1)
        }

        val message = MessageEntity(
            clientMessageId = clientMessageId,
            conversationId = conversationId,
            senderId = currentUserName,
            content = content,
            timestamp = timestamp,
            status = MessageStatus.PENDING
        )

        val outboxEntry = OutboxEntity(
            clientMessageId = clientMessageId,
            priority = 0,
            status = OutboxStatus.PENDING,
            lastAttemptTimestamp = timestamp
        )

        messageDao.insertMessage(message)
        outboxDao.insertOutboxEntry(outboxEntry)
        
        triggerSync()
    }

    override suspend fun retryMessage(clientMessageId: String) {
        val message = messageDao.getMessageById(clientMessageId)
        if (message != null) {
            messageDao.updateMessage(message.copy(status = MessageStatus.PENDING))
            outboxDao.insertOutboxEntry(OutboxEntity(
                clientMessageId = clientMessageId,
                status = OutboxStatus.PENDING
            ))
            triggerSync()
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "sync_messages",
            ExistingWorkPolicy.REPLACE, // Replace current sync to start immediately with new items
            syncRequest
        )
    }

    override fun getPendingMessages(): Flow<List<OutboxEntity>> = outboxDao.getPendingMessages()

    override fun getConflicts(): Flow<List<ConflictEntity>> = conflictDao.getUnresolvedConflicts()

    override suspend fun resolveConflict(clientMessageId: String, useLocal: Boolean) {
        val conflict = conflictDao.getConflictById(clientMessageId) ?: return
        val message = messageDao.getMessageById(clientMessageId) ?: return

        // Last Write Wins (LWW) rule for simple automatic resolution (example logic)
        // If conflict.remoteVersion > conflict.localVersion + 1000ms (1 sec buffer), 
        // we might prefer remote unless user explicitly says otherwise.
        // For now, we follow the user's explicit choice as requested.

        if (useLocal) {
            // Keep local version, retry sync
            messageDao.updateMessage(message.copy(
                status = MessageStatus.PENDING,
                timestamp = System.currentTimeMillis() // Update timestamp for LWW on next clash
            ))
            outboxDao.insertOutboxEntry(OutboxEntity(
                clientMessageId = clientMessageId,
                status = OutboxStatus.PENDING,
                retryCount = 0 // Reset retry count for manual retry
            ))
        } else {
            // Use remote version
            messageDao.updateMessage(message.copy(
                content = conflict.remoteContent,
                status = MessageStatus.SENT,
                serverId = "server_$clientMessageId",
                timestamp = conflict.remoteVersion
            ))
            outboxDao.deleteOutboxEntry(clientMessageId)
        }
        conflictDao.deleteConflict(clientMessageId)
        triggerSync()
    }
}
