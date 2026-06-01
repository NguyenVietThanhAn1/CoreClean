package com.coreclean.app.data.datasource.storage

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.coreclean.app.core.di.IoDispatcher
import com.coreclean.app.domain.model.StorageCategory
import com.coreclean.app.domain.model.StorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getStorageInfo(): StorageInfo = withContext(ioDispatcher) {
        val statFs     = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val totalBytes = statFs.totalBytes
        val freeBytes  = statFs.availableBytes
        val usedBytes  = totalBytes - freeBytes

        val imageBytes = queryMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videoBytes = queryMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val audioBytes = queryMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val appBytes   = getAppsStorageBytes()
        val otherBytes = maxOf(0L, usedBytes - imageBytes - videoBytes - audioBytes - appBytes)

        StorageInfo(
            totalBytes = totalBytes,
            freeBytes  = freeBytes,
            usedBytes  = usedBytes,
            breakdown  = listOf(
                StorageCategory("Anh",      imageBytes, 0xFF4285F4.toInt()),
                StorageCategory("Video",    videoBytes, 0xFF34A853.toInt()),
                StorageCategory("Am thanh", audioBytes, 0xFFFBBC04.toInt()),
                StorageCategory("Ung dung", appBytes,   0xFFEA4335.toInt()),
                StorageCategory("Khac",     otherBytes, 0xFF9E9E9E.toInt()),
            )
        )
    }

    private fun queryMediaSize(collectionUri: android.net.Uri): Long {
        var total = 0L
        context.contentResolver.query(
            collectionUri,
            arrayOf(MediaStore.MediaColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) total += cursor.getLong(col)
        }
        return total
    }

    /**
     * Estimates total app installation size using StorageStatsManager (API 26+).
     * Falls back to 0 if PACKAGE_USAGE_STATS permission is not granted.
     */
    private fun getAppsStorageBytes(): Long {
        return try {
            val storageStatsManager = context.getSystemService(
                android.app.usage.StorageStatsManager::class.java
            ) ?: return 0L
            val storageUuid = StorageManager.UUID_DEFAULT
            val userHandle  = android.os.Process.myUserHandle()
            context.packageManager
                .getInstalledPackages(PackageManager.GET_META_DATA)
                .sumOf { pkg ->
                    runCatching {
                        storageStatsManager
                            .queryStatsForPackage(storageUuid, pkg.packageName, userHandle)
                            .appBytes
                    }.getOrDefault(0L)
                }
        } catch (_: Exception) { 0L }
    }
}
