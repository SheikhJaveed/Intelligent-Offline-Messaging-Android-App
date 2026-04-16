package com.example.intelligent_messaging_app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import com.example.intelligent_messaging_app.data.local.entity.MessageEntity
import com.example.intelligent_messaging_app.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.intelligent_messaging_app.util.NetworkMonitor
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val conflicts: List<ConflictEntity> = emptyList(),
    val inputText: String = "",
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val userPreferencesRepository: com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
    workManager: WorkManager
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val isSyncing = workManager.getWorkInfosForUniqueWorkFlow("sync_messages")
        .map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }

    private val syncStatusFlow = combine(networkMonitor.isOnline, isSyncing) { online, syncing ->
        online to syncing
    }

    val uiState: StateFlow<ChatUiState> = combine(
        repository.getMessages("default_conv"),
        repository.getConflicts(),
        _inputText,
        userPreferencesRepository.userName,
        syncStatusFlow
    ) { messages, conflicts, text, userName, syncStatus ->
        ChatUiState(
            messages = messages,
            conflicts = conflicts,
            inputText = text,
            currentUserId = userName ?: "Anonymous",
            isLoading = false,
            isOnline = syncStatus.first,
            isSyncing = syncStatus.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState(isLoading = true)
    )

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            repository.sendMessage("default_conv", content)
            _inputText.value = ""
        }
    }
    
    fun retryMessage(clientMessageId: String) {
        viewModelScope.launch {
            repository.retryMessage(clientMessageId)
        }
    }

    fun resolveConflict(clientMessageId: String, useLocal: Boolean) {
        viewModelScope.launch {
            repository.resolveConflict(clientMessageId, useLocal)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferencesRepository.clearUserData()
        }
    }
}
