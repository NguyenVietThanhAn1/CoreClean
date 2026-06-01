package com.coreclean.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.coreclean.app.R
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.domain.model.Frequency
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.ScheduleConfig
import com.coreclean.app.domain.usecase.junk.CleanJunkUseCase
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val CHANNEL_AUTO_CLEAN = "auto_clean_results"
private const val NOTIF_ID_AUTO_CLEAN = 1001

/** Categories safe to delete automatically — does NOT touch photos, contacts, or app data. */
private val SAFE_CATEGORIES = setOf(
    JunkCategory.TEMP_FILES,
    JunkCategory.EMPTY_FOLDERS,
    JunkCategory.RESIDUAL_APK
)

@HiltWorker
class AutoCleanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scanJunkUseCase: ScanJunkUseCase,
    private val cleanJunkUseCase: CleanJunkUseCase,
    private val dataStore: DataStore<Preferences>,
    private val workManager: WorkManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = loadConfig() ?: return Result.success()
        if (!config.enabled) return Result.success()

        return runCatching {
            // Intersect requested categories with safe-only set to prevent accidental data loss
            val allowedCategories = config.categories.intersect(SAFE_CATEGORIES)
            val allJunk = scanJunkUseCase()
            val toClean = allJunk.filter { it.category in allowedCategories }

            val result = cleanJunkUseCase(toClean)
            val freedMb = toClean.sumOf { it.sizeBytes } / (1024 * 1024)

            if (result.cleanedCount > 0) {
                showResultNotification(freedMb)
            }

            // Schedule next run
            scheduleNext(config)
            Result.success()
        }.getOrElse { Result.failure() }
    }

    private suspend fun loadConfig(): ScheduleConfig? {
        val prefs = dataStore.data.first()
        val json  = prefs[AppPreferenceKeys.SCHEDULE_CONFIG_JSON] ?: return null
        return runCatching { Json.decodeFromString<ScheduleConfig>(json) }.getOrNull()
    }

    private fun scheduleNext(config: ScheduleConfig) {
        val now  = LocalDateTime.now()
        val next = when (config.frequency) {
            Frequency.DAILY   -> now.plusDays(1).with(LocalTime.of(config.hour, config.minute))
            Frequency.WEEKLY  -> now.plusWeeks(1).with(LocalTime.of(config.hour, config.minute))
            Frequency.MONTHLY -> now.plusMonths(1).with(LocalTime.of(config.hour, config.minute))
        }
        val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<AutoCleanWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag("auto_clean")
            .build()
        workManager.enqueue(request)
    }

    private fun showResultNotification(freedMb: Long) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_AUTO_CLEAN,
                    applicationContext.getString(R.string.notif_channel_auto_clean),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_AUTO_CLEAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.notif_auto_clean_title))
            .setContentText(applicationContext.getString(R.string.notif_auto_clean_body, freedMb))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID_AUTO_CLEAN, notification) }
    }
}
