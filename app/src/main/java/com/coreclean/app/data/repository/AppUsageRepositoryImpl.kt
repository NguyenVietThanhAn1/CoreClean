package com.coreclean.app.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.coreclean.app.data.datasource.usage.AppUsageDataSource
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppUsageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: AppUsageDataSource
) : AppUsageRepository {

    override suspend fun getUsageStats(range: UsageRange): List<AppUsageInfo> =
        dataSource.getUsageStats(range)

    override fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
