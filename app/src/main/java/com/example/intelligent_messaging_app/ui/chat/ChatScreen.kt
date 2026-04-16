package com.example.intelligent_messaging_app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.intelligent_messaging_app.data.local.entity.MessageEntity
import com.example.intelligent_messaging_app.domain.model.MessageStatus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Offline Chat")
                            if (uiState.isSyncing) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::logout) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Connectivity Banner
                AnimatedVisibility(
                    visible = !uiState.isOnline,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Waiting for network...",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatInput(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                EmptyChatPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        items = uiState.messages.reversed(),
                        key = { it.clientMessageId },
                        contentType = { "message" }
                    ) { message ->
                        MessageItem(
                            message = message,
                            currentUserId = uiState.currentUserId,
                            onRetry = { viewModel.retryMessage(message.clientMessageId) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp),
                    userScrollEnabled = false
                ) {
                    items(10) {
                        SkeletonMessageItem()
                    }
                }
            }

            // Show Conflict Dialog if any
            uiState.conflicts.firstOrNull()?.let { conflict ->
                ConflictDialog(
                    conflict = conflict,
                    onResolve = { useLocal ->
                        viewModel.resolveConflict(conflict.clientMessageId, useLocal)
                    }
                )
            }
        }
    }
}

@Composable
fun ConflictDialog(
    conflict: com.example.intelligent_messaging_app.data.local.entity.ConflictEntity,
    onResolve: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { }, // Force resolution
        title = { Text("Sync Conflict") },
        text = {
            Column {
                Text("A version of this message already exists on the server.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Local: ${conflict.localContent}", style = MaterialTheme.typography.bodySmall)
                Text("Server: ${conflict.remoteContent}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = { onResolve(true) }) { Text("Keep Mine") }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(false) }) { Text("Use Server's") }
        }
    )
}

@Composable
fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No messages yet. Say hello!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun MessageItem(
    message: MessageEntity,
    currentUserId: String,
    onRetry: () -> Unit
) {
    val isFromMe = message.senderId == currentUserId
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isFromMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isFromMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeString = remember(message.timestamp) {
        timeFormatter.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        if (!isFromMe) {
            Text(
                text = message.senderId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusIndicator(message.status, onRetry, isFromMe)
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: MessageStatus, onRetry: () -> Unit, isFromMe: Boolean) {
    if (!isFromMe) return

    val statusText = when (status) {
        MessageStatus.PENDING -> "⏳"
        MessageStatus.SENDING -> "..."
        MessageStatus.SENT -> "✓"
        MessageStatus.DELIVERED -> "✓✓"
        MessageStatus.READ -> "✓✓"
        MessageStatus.FAILED -> "Retry"
    }
    
    val color = when (status) {
        MessageStatus.FAILED -> Color.Red
        MessageStatus.READ -> Color.Cyan // Or a blue color typical for read receipts
        else -> Color.Unspecified
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = if (status == MessageStatus.FAILED) {
            Modifier.clickable(onClick = onRetry)
        } else {
            Modifier
        }
    )
}

@Composable
fun SkeletonMessageItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (Math.random() > 0.5) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(width = (100..200).random().dp, height = 40.dp)
                .background(
                    color = Color.LightGray.copy(alpha = alpha),
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}
