package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id AND userId = :userId")
    suspend fun deleteBudgetById(id: Long, userId: Long)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    fun getBudgetsForMonth(userId: Long, monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    suspend fun getBudgetsForMonthList(userId: Long, monthYear: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(userId: Long, category: String, monthYear: String): BudgetEntity?
}
