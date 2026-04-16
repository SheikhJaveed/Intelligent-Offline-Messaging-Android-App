package com.example.intelligent_messaging_app.data.local

import androidx.room.TypeConverter
import com.example.intelligent_messaging_app.data.local.entity.OutboxStatus
import com.example.intelligent_messaging_app.domain.model.MessageStatus

class Converters {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromOutboxStatus(status: OutboxStatus): String = status.name

    @TypeConverter
    fun toOutboxStatus(value: String): OutboxStatus = OutboxStatus.valueOf(value)
}
