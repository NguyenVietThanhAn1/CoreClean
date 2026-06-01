package com.coreclean.app.data.datasource.battery

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.coreclean.app.data.datasource.battery.BatteryDataSource
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class BatteryHistoryRecorder @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val batteryDataSource: BatteryDataSource,
    private val batteryHistoryDao: BatteryHistoryDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val info = batteryDataSource.getBatteryInfo()
            batteryHistoryDao.insert(
                BatteryHistoryEntity(
                    timestamp    = System.currentTimeMillis(),
                    levelPercent = info.levelPercent,
                    isCharging   = info.isCharging
                )
            )
            Result.success()
        }.getOrElse { Result.failure() }
    }

    companion object {
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<BatteryHistoryRecorder>(15, TimeUnit.MINUTES)
                .addTag("battery_history")
                .build()
            workManager.enqueueUniquePeriodicWork(
                "battery_history_record",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
