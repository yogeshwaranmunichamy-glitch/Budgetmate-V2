package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PaymentReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentReminderDao {

    @Query("SELECT * FROM payment_reminders WHERE userId = :userId ORDER BY dueDate ASC")
    fun getAllReminders(userId: Long): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE userId = :userId AND status != 'COMPLETED' ORDER BY dueDate ASC")
    fun getPendingReminders(userId: Long): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE userId = :userId AND reminderType = :type AND status != 'COMPLETED' ORDER BY dueDate ASC")
    fun getRemindersByType(userId: Long, type: String): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): PaymentReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PaymentReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: PaymentReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: PaymentReminderEntity)

    @Query("UPDATE payment_reminders SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE payment_reminders SET dueDate = :newDueDate, status = 'SNOOZED' WHERE id = :id")
    suspend fun snoozeReminder(id: Long, newDueDate: Long)

    @Query("UPDATE payment_reminders SET isAlarmEnabled = :enabled WHERE id = :id")
    suspend fun toggleAlarm(id: Long, enabled: Boolean)
}
