package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange

interface AppUsageRepository {
    suspend fun getUsageStats(range: UsageRange): List<AppUsageInfo>
    fun hasUsageAccessPermission(): Boolean
}
