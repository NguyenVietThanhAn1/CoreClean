package com.coreclean.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coreclean.app.data.local.entity.PendingReviewEntity

@Dao
interface PendingReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PendingReviewEntity>)

    @Query("SELECT imageId FROM pending_review")
    suspend fun getAllIds(): List<Long>

    @Query("DELETE FROM pending_review")
    suspend fun clearAll()
}
