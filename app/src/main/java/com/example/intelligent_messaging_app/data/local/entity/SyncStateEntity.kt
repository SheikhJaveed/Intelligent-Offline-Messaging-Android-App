package com.example.intelligent_messaging_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: String = "global_sync_state",
    val lastSyncTimestamp: Long = 0,
    val isSyncing: Boolean = false,
    val lastError: String? = null
)
