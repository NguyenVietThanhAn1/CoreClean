package com.coreclean.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coreclean.app.data.local.entity.ScanResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScanResultEntity>)

    @Query("SELECT * FROM scan_results")
    fun getAll(): Flow<List<ScanResultEntity>>

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}