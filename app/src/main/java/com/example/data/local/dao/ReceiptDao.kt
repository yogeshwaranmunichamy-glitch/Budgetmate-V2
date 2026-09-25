package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.ReceiptScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptScanEntity): Long

    @Query("SELECT * FROM receipt_scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllReceipts(userId: Long): Flow<List<ReceiptScanEntity>>
}
