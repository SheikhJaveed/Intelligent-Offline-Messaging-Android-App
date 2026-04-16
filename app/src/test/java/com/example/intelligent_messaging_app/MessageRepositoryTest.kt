package com.example.intelligent_messaging_app

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.intelligent_messaging_app.data.local.dao.ConflictDao
import com.example.intelligent_messaging_app.data.local.dao.ConversationDao
import com.example.intelligent_messaging_app.data.local.dao.MessageDao
import com.example.intelligent_messaging_app.data.local.dao.OutboxDao
import com.example.intelligent_messaging_app.data.repository.MessageRepositoryImpl
import com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MessageRepositoryTest {

    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val outboxDao = mockk<OutboxDao>(relaxed = true)
    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val conflictDao = mockk<ConflictDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val userPrefs = mockk<UserPreferencesRepository>(relaxed = true)

    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setup() {
        every { userPrefs.userName } returns flowOf("test_user")
        repository = MessageRepositoryImpl(
            messageDao, outboxDao, conversationDao, conflictDao, workManager, userPrefs
        )
    }

    @Test
    fun `sendMessage should insert message and outbox entry`() = runTest {
        // Arrange
        val content = "Hello World"
        val conversationId = "conv1"

        // Act
        repository.sendMessage(conversationId, content)

        // Assert
        coVerify { messageDao.insertMessage(any()) }
        coVerify { outboxDao.insertOutboxEntry(any()) }
        coVerify { workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `resolveConflict with useLocal true should retry message`() = runTest {
        // Arrange
        val messageId = "msg1"
        val mockConflict = com.example.intelligent_messaging_app.data.local.entity.ConflictEntity(
            clientMessageId = messageId,
            localContent = "Local",
            remoteContent = "Remote",
            localVersion = 1L,
            remoteVersion = 2L
        )
        val mockMessage = com.example.intelligent_messaging_app.data.local.entity.MessageEntity(
            clientMessageId = messageId,
            conversationId = "conv1",
            senderId = "me",
            content = "Local",
            timestamp = 1L,
            status = com.example.intelligent_messaging_app.domain.model.MessageStatus.FAILED
        )

        coEvery { conflictDao.getConflictById(messageId) } returns mockConflict
        coEvery { messageDao.getMessageById(messageId) } returns mockMessage

        // Act
        repository.resolveConflict(messageId, useLocal = true)

        // Assert
        coVerify { messageDao.updateMessage(match { it.status == com.example.intelligent_messaging_app.domain.model.MessageStatus.PENDING }) }
        coVerify { outboxDao.insertOutboxEntry(match { it.clientMessageId == messageId }) }
        coVerify { conflictDao.deleteConflict(messageId) }
    }
}
