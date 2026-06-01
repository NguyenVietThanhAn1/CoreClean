package com.coreclean.app.data.datasource.junk

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class JunkScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scan(): List<JunkItem> = withContext(Dispatchers.IO) {
        buildList {
            addAll(scanAppCache())
            addAll(scanTempFiles())
            addAll(scanResidualApks())
            addAll(scanEmptyFolders())
        }
    }

    private fun scanAppCache(): List<JunkItem> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE)
            as? StorageStatsManager ?: return emptyList()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE)
            as? StorageManager ?: return emptyList()
        val uuid = try { storageManager.getUuidForPath(context.filesDir) } catch (_: Exception) {
            StorageManager.UUID_DEFAULT
        }
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { appInfo ->
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        uuid, appInfo.packageName, android.os.Process.myUserHandle()
                    )
                    if (stats.cacheBytes > 0) {
                        JunkItem(
                            path        = appInfo.packageName,
                            sizeBytes   = stats.cacheBytes,
                            category    = JunkCategory.APP_CACHE,
                            packageName = appInfo.packageName
                        )
                    } else null
                } catch (_: Exception) { null }
            }
    }

    private fun scanTempFiles(): List<JunkItem> {
        val dirs = listOf(
            context.cacheDir,
            context.externalCacheDir,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        ).filterNotNull()

        val patterns = listOf(".tmp", ".log", ".thumbdata")
        return dirs.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && patterns.any { p -> it.name.endsWith(p, ignoreCase = true) } }
                .map { JunkItem(it.absolutePath, it.length(), JunkCategory.TEMP_FILES) }
        }
    }

    private fun scanResidualApks(): List<JunkItem> {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) return emptyList()
        return downloads.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .map { JunkItem(it.absolutePath, it.length(), JunkCategory.RESIDUAL_APK) }
            .toList()
    }

    private fun scanEmptyFolders(): List<JunkItem> {
        val roots = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        ).filterNotNull().filter { it.exists() }

        return roots.flatMap { root ->
            root.walkBottomUp()
                .filter { it.isDirectory && it != root && (it.listFiles()?.isEmpty() == true) }
                .map { JunkItem(it.absolutePath, 0L, JunkCategory.EMPTY_FOLDERS) }
        }
    }

    suspend fun clean(items: List<JunkItem>): Int = withContext(Dispatchers.IO) {
        items.count { item ->
            try {
                val file = File(item.path)
                if (file.exists()) file.delete() else false
            } catch (_: Exception) { false }
        }
    }
}
