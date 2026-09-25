package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteRecurringById(id: Long, userId: Long)

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextDueDate ASC")
    fun getAllRecurring(userId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 AND nextDueDate <= :timestamp")
    suspend fun getDueRecurring(userId: Long, timestamp: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurring(userId: Long): Flow<List<RecurringTransactionEntity>>
}
