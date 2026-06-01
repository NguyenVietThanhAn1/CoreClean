package com.coreclean.app.core.di

import android.content.Context
import androidx.room.Room
import com.coreclean.app.data.local.AppDatabase
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.migration.MIGRATION_1_2
import com.coreclean.app.data.local.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "coreclean.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides @Singleton
    fun provideScanResultDao(db: AppDatabase): ScanResultDao = db.scanResultDao()

    @Provides @Singleton
    fun providePendingReviewDao(db: AppDatabase): PendingReviewDao = db.pendingReviewDao()

    @Provides @Singleton
    fun provideBatteryHistoryDao(db: AppDatabase): BatteryHistoryDao = db.batteryHistoryDao()
}
