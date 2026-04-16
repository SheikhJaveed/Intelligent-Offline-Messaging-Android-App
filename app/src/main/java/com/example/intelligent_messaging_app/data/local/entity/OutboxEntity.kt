package com.example.intelligent_messaging_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val clientMessageId: String,
    val priority: Int = 0,
    val retryCount: Int = 0,
    val lastAttemptTimestamp: Long = 0,
    val nextRetryTimestamp: Long = 0,
    val status: OutboxStatus = OutboxStatus.PENDING
)

enum class OutboxStatus {
    PENDING,
    SENDING,
    FAILED,
    PERMANENT_FAILURE
}
