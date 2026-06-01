package com.coreclean.app.domain.usecase.junk

import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.data.datasource.junk.JunkScanner
import javax.inject.Inject

class CleanJunkUseCase @Inject constructor(
    private val scanner: JunkScanner
) {
    suspend operator fun invoke(items: List<JunkItem>): CleanResult {
        val (cleanable, needsSettings) = items.partition { it.category != JunkCategory.APP_CACHE }
        val cleaned = scanner.clean(cleanable)
        return CleanResult(cleanedCount = cleaned, appCacheCount = needsSettings.size)
    }
}

data class CleanResult(val cleanedCount: Int, val appCacheCount: Int)
