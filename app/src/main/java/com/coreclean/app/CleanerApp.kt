package com.coreclean.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.coreclean.app.data.datasource.battery.BatteryHistoryRecorder
import com.coreclean.app.data.worker.MediaScanWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CleanerApp : Application() {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var dataStore: DataStore<Preferences>

    override fun onCreate() {
        super.onCreate()

        initializeSentry(dataStore)

        // Default WorkManagerInitializer is removed in manifest; initialize manually with Hilt factory.
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
        schedulePeriodicMediaScan()
        BatteryHistoryRecorder.schedule(WorkManager.getInstance(this))
    }

    private fun schedulePeriodicMediaScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<MediaScanWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "media_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
