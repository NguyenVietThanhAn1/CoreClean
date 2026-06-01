package com.coreclean.app.domain.usecase.usage

import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
import javax.inject.Inject

class GetAppUsageUseCase @Inject constructor(
    private val repository: AppUsageRepository
) {
    suspend operator fun invoke(range: UsageRange): List<AppUsageInfo> =
        repository.getUsageStats(range)
}
