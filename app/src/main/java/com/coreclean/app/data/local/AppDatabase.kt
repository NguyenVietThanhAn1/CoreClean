package com.coreclean.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.ScanResultEntity

@Database(
    entities = [ScanResultEntity::class],
    version = 1,
    exportSchema = true    // tạo schema JSON, quan trọng cho migration
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
}