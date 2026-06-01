package com.coreclean.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.coreclean.app.R
import com.coreclean.app.core.preferences.AppPreferenceKeys
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_RECOMMENDATIONS = "recommendations"
private const val NOTIF_ID_STORAGE_FULL   = 2001
private const val NOTIF_ID_DUPLICATE      = 2002
private const val RATE_LIMIT_DAYS         = 7L
private const val STORAGE_CRITICAL_PCT    = 5

@Singleton
class RecommendationNotifier @Inject constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RECOMMENDATIONS,
                    context.getString(R.string.notif_channel_recommendations),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = context.getString(R.string.notif_channel_recommendations_desc) }
            )
        }
    }

    /** Notify if free storage < 5% and rate-limit not hit. */
    suspend fun maybeNotifyStorageFull(freePercent: Int) {
        if (freePercent >= STORAGE_CRITICAL_PCT) return
        val prefs = dataStore.data.first()
        if (!prefs[AppPreferenceKeys.RECOMMENDATIONS_ENABLED].let { it ?: true }) return
        val lastTs = prefs[AppPreferenceKeys.NOTIF_LAST_STORAGE_FULL]
        if (lastTs != null && !isOlderThanRateLimit(lastTs)) return

        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_RECOMMENDATIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_storage_full_title))
            .setContentText(context.getString(R.string.notif_storage_full_body, freePercent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID_STORAGE_FULL, notification) }
        dataStore.edit { it[AppPreferenceKeys.NOTIF_LAST_STORAGE_FULL] = Instant.now().toString() }
    }

    /** Notify if newly detected duplicate waste > 2 GB and rate-limit not hit. */
    suspend fun maybeNotifyDuplicates(duplicateWasteBytes: Long) {
        val twoGb = 2L * 1024 * 1024 * 1024
        if (duplicateWasteBytes < twoGb) return
        val prefs = dataStore.data.first()
        if (!prefs[AppPreferenceKeys.RECOMMENDATIONS_ENABLED].let { it ?: true }) return
        val lastTs = prefs[AppPreferenceKeys.NOTIF_LAST_DUPLICATE]
        if (lastTs != null && !isOlderThanRateLimit(lastTs)) return

        ensureChannel()
        val wasteMb = duplicateWasteBytes / (1024 * 1024)
        val notification = NotificationCompat.Builder(context, CHANNEL_RECOMMENDATIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_duplicate_title))
            .setContentText(context.getString(R.string.notif_duplicate_body, wasteMb))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID_DUPLICATE, notification) }
        dataStore.edit { it[AppPreferenceKeys.NOTIF_LAST_DUPLICATE] = Instant.now().toString() }
    }

    private fun isOlderThanRateLimit(isoTimestamp: String): Boolean = runCatching {
        val last = Instant.parse(isoTimestamp)
        ChronoUnit.DAYS.between(last, Instant.now()) >= RATE_LIMIT_DAYS
    }.getOrDefault(true)
}
