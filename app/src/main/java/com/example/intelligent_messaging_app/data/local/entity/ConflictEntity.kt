package com.example.intelligent_messaging_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conflicts")
data class ConflictEntity(
    @PrimaryKey val clientMessageId: String,
    val localContent: String,
    val remoteContent: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val resolved: Boolean = false
)
