package com.coreclean.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

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
        val prefs  = dataStore.data.first()
        val config = loadConfigFromPrefs(prefs) ?: return Result.success()
        if (!config.enabled) return Result.success()

        val recommendationsEnabled = prefs[AppPreferenceKeys.RECOMMENDATIONS_ENABLED] ?: true

        return runCatching {
            val safUris = prefs[AppPreferenceKeys.SAF_FOLDER_URIS] ?: emptySet()
            val allowedCategories = config.categories.intersect(SAFE_CATEGORIES)
            val allJunk = scanJunkUseCase(safUris)
            val toClean = allJunk.filter { it.category in allowedCategories }

            val result = cleanJunkUseCase(toClean)
            val freedMb = toClean.sumOf { it.sizeBytes } / (1024 * 1024)

            if (result.cleanedCount > 0 && recommendationsEnabled) {
                showResultNotification(freedMb)
            }

            scheduleNext(config)
            Result.success()
        }.getOrElse { scheduleNext(config); Result.failure() }
    }

    private fun loadConfigFromPrefs(prefs: Preferences): ScheduleConfig? {
        val json = prefs[AppPreferenceKeys.SCHEDULE_CONFIG_JSON] ?: return null
        return runCatching { Json.decodeFromString<ScheduleConfig>(json) }.getOrNull()
    }

    private suspend fun scheduleNext(config: ScheduleConfig) {
        val now  = LocalDateTime.now()
        val next = when (config.frequency) {
            Frequency.DAILY   -> now.plusDays(1).with(LocalTime.of(config.hour, config.minute))
            Frequency.WEEKLY  -> now.plusWeeks(1).with(LocalTime.of(config.hour, config.minute))
            Frequency.MONTHLY -> now.plusMonths(1).with(LocalTime.of(config.hour, config.minute))
        }
        val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()
        val request = OneTimeWorkRequestBuilder<AutoCleanWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("auto_clean")
            .build()
        workManager.enqueueUniqueWork("auto_clean", ExistingWorkPolicy.REPLACE, request)
    }

    private suspend fun showResultNotification(freedMb: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext, "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            dataStore.edit { it[AppPreferenceKeys.NOTIF_PERMISSION_NEEDED] = true }
            return
        }

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
