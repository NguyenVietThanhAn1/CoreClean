package com.coreclean.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coreclean.app.data.local.entity.BatteryHistoryEntity

@Dao
interface BatteryHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryHistoryEntity)

    /** Returns entries for the last 24 hours, excluding charging periods. */
    @Query("""
        SELECT * FROM battery_history
        WHERE isCharging = 0 AND timestamp >= :sinceMs
        ORDER BY timestamp ASC
    """)
    suspend fun getDischargingSince(sinceMs: Long): List<BatteryHistoryEntity>

    @Query("SELECT * FROM battery_history WHERE timestamp >= :sinceMs ORDER BY timestamp ASC")
    suspend fun getAllSince(sinceMs: Long): List<BatteryHistoryEntity>

    @Query("DELETE FROM battery_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM battery_history")
    suspend fun count(): Int
}
