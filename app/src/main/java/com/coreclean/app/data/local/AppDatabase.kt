package com.coreclean.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import com.coreclean.app.data.local.entity.PendingReviewEntity
import com.coreclean.app.data.local.entity.ScanResultEntity

@Database(
    entities   = [ScanResultEntity::class, PendingReviewEntity::class, BatteryHistoryEntity::class],
    version    = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun pendingReviewDao(): PendingReviewDao
    abstract fun batteryHistoryDao(): BatteryHistoryDao
}
