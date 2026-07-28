package com.dopalabs.vybrik.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun enabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Update suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET tabId = :destinationTabId WHERE tabId = :sourceTabId")
    suspend fun moveToTab(sourceTabId: String, destinationTabId: String)

    @Delete suspend fun delete(reminder: ReminderEntity)
}
