package com.example.intelligent_messaging_app.data.local.dao

import androidx.room.*
import com.example.intelligent_messaging_app.data.local.entity.ConflictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConflictDao {
    @Query("SELECT * FROM conflicts WHERE clientMessageId = :clientMessageId")
    suspend fun getConflictById(clientMessageId: String): ConflictEntity?

    @Query("SELECT * FROM conflicts WHERE resolved = 0")
    fun getUnresolvedConflicts(): Flow<List<ConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflict(conflict: ConflictEntity)

    @Update
    suspend fun updateConflict(conflict: ConflictEntity)

    @Query("DELETE FROM conflicts WHERE clientMessageId = :clientMessageId")
    suspend fun deleteConflict(clientMessageId: String)
}
