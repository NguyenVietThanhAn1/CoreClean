package com.coreclean.app.data.datasource.applist

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.storage.StorageManager
import com.coreclean.app.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AppListDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm: PackageManager get() = context.packageManager

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val storageStats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val uuid = runCatching {
            storageManager?.getUuidForPath(context.filesDir) ?: StorageManager.UUID_DEFAULT
        }.getOrDefault(StorageManager.UUID_DEFAULT)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        else PackageManager.GET_META_DATA

        @Suppress("DEPRECATION")
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        else pm.getInstalledPackages(PackageManager.GET_META_DATA)

        packages.mapNotNull { pkg ->
            runCatching {
                val appInfo  = pkg.applicationInfo ?: return@mapNotNull null
                val appName  = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val apkSize  = runCatching { File(appInfo.sourceDir).length() }.getOrDefault(0L)

                var cacheBytes = 0L
                var dataBytes  = 0L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStats != null) {
                    runCatching {
                        val stats = storageStats.queryStatsForPackage(
                            uuid, pkg.packageName, android.os.Process.myUserHandle()
                        )
                        cacheBytes = stats.cacheBytes
                        dataBytes  = stats.dataBytes
                    }
                }

                InstalledApp(
                    packageName  = pkg.packageName,
                    appName      = appName,
                    versionName  = pkg.versionName ?: "",
                    versionCode  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else pkg.versionCode.toLong(),
                    apkSizeBytes = apkSize,
                    cacheBytes   = cacheBytes,
                    dataBytes    = dataBytes,
                    installTime  = pkg.firstInstallTime,
                    updateTime   = pkg.lastUpdateTime,
                    isSystem     = isSystem
                )
            }.getOrNull()
        }
    }
}
