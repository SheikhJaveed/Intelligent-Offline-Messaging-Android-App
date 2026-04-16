package com.example.intelligent_messaging_app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.intelligent_messaging_app.data.local.dao.MessageDao
import com.example.intelligent_messaging_app.data.local.dao.OutboxDao
import com.example.intelligent_messaging_app.data.local.dao.ConflictDao
import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxStatus
import com.example.intelligent_messaging_app.domain.model.MessageStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageDao: MessageDao,
    private val outboxDao: OutboxDao,
    private val conflictDao: ConflictDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingEntries = outboxDao.getPendingMessages().first()
        
        if (pendingEntries.isEmpty()) return Result.success()

        var hasError = false
        val MAX_RETRIES = 3

        for (entry in pendingEntries) {
            // Check for worker cancellation (e.g., network switch, app kill)
            if (isStopped) return Result.retry()

            if (entry.retryCount >= MAX_RETRIES) {
                outboxDao.updateOutboxEntry(entry.copy(status = OutboxStatus.PERMANENT_FAILURE))
                val message = messageDao.getMessageById(entry.clientMessageId)
                if (message != null) {
                    messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
                }
                continue
            }

            try {
                val message = messageDao.getMessageById(entry.clientMessageId) ?: continue
                
                // Update status to SENDING and increment retry count
                val updatedEntry = entry.copy(
                    status = OutboxStatus.SENDING,
                    retryCount = entry.retryCount + 1,
                    lastAttemptTimestamp = System.currentTimeMillis()
                )
                outboxDao.updateOutboxEntry(updatedEntry)
                messageDao.updateMessage(message.copy(status = MessageStatus.SENDING))
                
                // SIMULATE NETWORK CALL
                delay(1500) 
                if (isStopped) return Result.retry()

                // SIMULATE A CONFLICT (e.g., if message content is "conflict")
                if (message.content.contains("conflict", ignoreCase = true)) {
                    val conflict = ConflictEntity(
                        clientMessageId = message.clientMessageId,
                        localContent = message.content,
                        remoteContent = "[SERVER] " + message.content,
                        localVersion = System.currentTimeMillis(),
                        remoteVersion = System.currentTimeMillis() + 100
                    )
                    conflictDao.insertConflict(conflict)
                    messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
                    outboxDao.updateOutboxEntry(entry.copy(status = OutboxStatus.FAILED))
                    continue
                }
                
                // SUCCESS path
                messageDao.updateMessage(message.copy(
                    status = MessageStatus.SENT,
                    serverId = "server_${message.clientMessageId}"
                ))
                outboxDao.deleteOutboxEntry(entry.clientMessageId)

                // SIMULATE DELIVERED status after 2 seconds
                delay(2000)
                messageDao.updateMessage(message.copy(status = MessageStatus.DELIVERED))

                // SIMULATE READ status after 3 more seconds
                delay(3000)
                messageDao.updateMessage(message.copy(status = MessageStatus.READ))
                
            } catch (e: Exception) {
                hasError = true
                val message = messageDao.getMessageById(entry.clientMessageId)
                if (message != null) {
                    messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
                }
                outboxDao.updateOutboxEntry(entry.copy(status = OutboxStatus.FAILED))
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }
}
