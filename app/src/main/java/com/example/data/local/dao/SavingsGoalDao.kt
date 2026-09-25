package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoalById(id: Long, userId: Long)

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY targetDate ASC")
    fun getAllGoals(userId: Long): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGoalById(id: Long, userId: Long): SavingsGoalEntity?

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :addAmount WHERE id = :id AND userId = :userId")
    suspend fun addFundsToGoal(id: Long, userId: Long, addAmount: Double)
}
