package com.coreclean.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.coreclean.app.data.worker.MediaScanWorker
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CleanerApp : Application() {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Sentry: init with DSN from BuildConfig; no-op when DSN is empty
        if (BuildConfig.SENTRY_DSN.isNotEmpty()) {
            SentryAndroid.init(this) { options ->
                options.dsn               = BuildConfig.SENTRY_DSN
                options.tracesSampleRate  = 0.1
                options.isEnableUserInteractionTracing = false
            }
        }

        // Default WorkManagerInitializer is removed in manifest; initialize manually with Hilt factory.
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
        schedulePeriodicMediaScan()
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
