package com.coreclean.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.PendingReviewEntity
import com.coreclean.app.data.local.entity.ScanResultEntity

@Database(
    entities   = [ScanResultEntity::class, PendingReviewEntity::class],
    version    = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun pendingReviewDao(): PendingReviewDao
}
