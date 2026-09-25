package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteTransactionById(id: Long, userId: Long)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(userId: Long, limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getTransactionsByType(userId: Long, type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRangeList(userId: Long, startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(userId: Long, category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category AND type = 'EXPENSE'")
    suspend fun getExpensesByCategoryList(userId: Long, category: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'EXPENSE'")
    suspend fun getAllExpensesList(userId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'INCOME'")
    suspend fun getAllIncomeList(userId: Long): List<TransactionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'INCOME'")
    fun getTotalIncome(userId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'EXPENSE'")
    fun getTotalExpenses(userId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'INCOME' AND date >= :startDate AND date <= :endDate")
    fun getPeriodIncome(userId: Long, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND date >= :startDate AND date <= :endDate")
    fun getPeriodExpenses(userId: Long, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND category = :category AND type = 'EXPENSE' AND date >= :startDate AND date <= :endDate")
    fun getCategoryExpenseForPeriod(userId: Long, category: String, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getTransactionById(id: Long, userId: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND (sourceOrMerchant LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY date DESC
    """)
    fun searchTransactions(userId: Long, query: String): Flow<List<TransactionEntity>>
}
