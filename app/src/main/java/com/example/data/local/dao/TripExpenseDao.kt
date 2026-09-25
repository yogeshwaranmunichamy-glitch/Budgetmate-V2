package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TripExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripExpenseDao {

    @Query("SELECT * FROM trip_expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun getExpensesForTrip(tripId: Long): Flow<List<TripExpenseEntity>>

    @Query("SELECT * FROM trip_expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllTripExpensesForUser(userId: Long): Flow<List<TripExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM trip_expenses WHERE tripId = :tripId")
    fun getTotalSpentForTrip(tripId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: TripExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: TripExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: TripExpenseEntity)

    @Query("DELETE FROM trip_expenses WHERE tripId = :tripId")
    suspend fun deleteAllExpensesForTrip(tripId: Long)
}
