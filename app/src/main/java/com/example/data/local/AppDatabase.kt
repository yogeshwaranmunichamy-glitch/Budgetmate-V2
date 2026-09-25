package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.MLFeedbackDao
import com.example.data.local.dao.ReceiptDao
import com.example.data.local.dao.RecurringDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.TripDao
import com.example.data.local.dao.TripExpenseDao
import com.example.data.local.dao.TripPlanItemDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.MLFeedbackEntity
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.RecurringTransactionEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        RecurringTransactionEntity::class,
        MLFeedbackEntity::class,
        ReceiptScanEntity::class,
        TripEntity::class,
        TripExpenseEntity::class,
        TripPlanItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringDao(): RecurringDao
    abstract fun mlFeedbackDao(): MLFeedbackDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun tripDao(): TripDao
    abstract fun tripExpenseDao(): TripExpenseDao
    abstract fun tripPlanItemDao(): TripPlanItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetmate_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
