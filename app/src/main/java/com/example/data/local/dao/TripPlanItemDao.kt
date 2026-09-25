package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TripPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPlanItemDao {

    @Query("SELECT * FROM trip_plan_items WHERE tripId = :tripId ORDER BY dayNumber ASC, id ASC")
    fun getPlanItemsForTrip(tripId: Long): Flow<List<TripPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(item: TripPlanItemEntity): Long

    @Update
    suspend fun updatePlanItem(item: TripPlanItemEntity)

    @Delete
    suspend fun deletePlanItem(item: TripPlanItemEntity)

    @Query("UPDATE trip_plan_items SET isDone = :isDone WHERE id = :id")
    suspend fun toggleDone(id: Long, isDone: Boolean)

    @Query("DELETE FROM trip_plan_items WHERE tripId = :tripId")
    suspend fun deleteAllPlanItemsForTrip(tripId: Long)
}
