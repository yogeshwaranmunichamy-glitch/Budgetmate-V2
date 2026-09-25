package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.MLFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MLFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: MLFeedbackEntity): Long

    @Query("SELECT * FROM ml_feedback WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllFeedback(userId: Long): Flow<List<MLFeedbackEntity>>

    @Query("SELECT * FROM ml_feedback WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllFeedbackList(userId: Long): List<MLFeedbackEntity>

    @Query("SELECT COUNT(*) FROM ml_feedback WHERE userId = :userId")
    fun getFeedbackCount(userId: Long): Flow<Int>

    @Query("DELETE FROM ml_feedback WHERE userId = :userId")
    suspend fun clearFeedback(userId: Long)
}
