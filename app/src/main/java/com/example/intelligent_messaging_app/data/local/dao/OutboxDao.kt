package com.example.intelligent_messaging_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.intelligent_messaging_app.data.local.entity.OutboxEntity
import com.example.intelligent_messaging_app.data.local.entity.OutboxStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE status IN ('PENDING', 'FAILED', 'SENDING') ORDER BY priority DESC, nextRetryTimestamp ASC")
    fun getPendingMessages(): Flow<List<OutboxEntity>>

    @Update
    suspend fun updateOutboxEntry(entry: OutboxEntity)

    @Query("DELETE FROM outbox WHERE clientMessageId = :clientMessageId")
    suspend fun deleteOutboxEntry(clientMessageId: String)

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun getOutboxCount(): Int

    @Query("DELETE FROM outbox WHERE clientMessageId IN (SELECT clientMessageId FROM outbox ORDER BY lastAttemptTimestamp ASC LIMIT :count)")
    suspend fun evictOldest(count: Int)
}
