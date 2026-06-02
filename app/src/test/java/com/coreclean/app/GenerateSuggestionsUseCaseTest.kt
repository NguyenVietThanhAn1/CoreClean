package com.coreclean.app

import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.usecase.suggest.GenerateSuggestionsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateSuggestionsUseCaseTest {

    private lateinit var useCase: GenerateSuggestionsUseCase

    @Before fun setUp() { useCase = GenerateSuggestionsUseCase() }

    @Test fun `empty inputs produce no suggestions`() {
        val result = useCase.invoke()
        assertTrue(result.isEmpty())
    }

    @Test fun `duplicate groups below threshold produce no suggestion`() {
        val groups = listOf(
            DuplicateGroup(images = emptyList(), totalWastedSize = 50 * 1024 * 1024L) // 50 MB
        )
        val result = useCase.invoke(duplicateGroups = groups)
        assertTrue(result.none { it is CleaningSuggestion.LargeDuplicateGroup })
    }

    @Test fun `duplicate groups above 100 MB threshold produce LargeDuplicateGroup suggestion`() {
        val groups = listOf(
            DuplicateGroup(images = emptyList(), totalWastedSize = 200 * 1024 * 1024L)
        )
        val result = useCase.invoke(duplicateGroups = groups)
        val suggestion = result.filterIsInstance<CleaningSuggestion.LargeDuplicateGroup>().firstOrNull()
        assertTrue(suggestion != null)
        assertEquals(200 * 1024 * 1024L, suggestion!!.wastedBytes)
    }

    @Test fun `unused app below size threshold produces no suggestion`() {
        val app = makeApp(apkSizeBytes = 10 * 1024 * 1024L) // 10 MB
        val result = useCase.invoke(
            installedApps = listOf(app),
            appLastUsedDays = mapOf(app.packageName to 90)
        )
        assertTrue(result.none { it is CleaningSuggestion.UnusedApp })
    }

    @Test fun `unused app above thresholds produces UnusedApp suggestion`() {
        val app = makeApp(apkSizeBytes = 100 * 1024 * 1024L)
        val result = useCase.invoke(
            installedApps   = listOf(app),
            appLastUsedDays = mapOf(app.packageName to 90)
        )
        val suggestion = result.filterIsInstance<CleaningSuggestion.UnusedApp>().firstOrNull()
        assertTrue(suggestion != null)
        assertEquals(app.packageName, suggestion!!.packageName)
    }

    @Test fun `recently used app not suggested even if large`() {
        val app = makeApp(apkSizeBytes = 200 * 1024 * 1024L)
        val result = useCase.invoke(
            installedApps   = listOf(app),
            appLastUsedDays = mapOf(app.packageName to 5) // only 5 days
        )
        assertTrue(result.none { it is CleaningSuggestion.UnusedApp })
    }

    @Test fun `system app not suggested for uninstall`() {
        val app = makeApp(apkSizeBytes = 500 * 1024 * 1024L, isSystem = true)
        val result = useCase.invoke(
            installedApps   = listOf(app),
            appLastUsedDays = mapOf(app.packageName to 180)
        )
        assertTrue(result.none { it is CleaningSuggestion.UnusedApp })
    }

    @Test fun `storage below 10 percent produces StorageFull suggestion`() {
        val storage = StorageInfo(
            totalBytes = 100 * 1024 * 1024 * 1024L,
            freeBytes  = 5 * 1024 * 1024 * 1024L,  // 5%
            usedBytes  = 95 * 1024 * 1024 * 1024L,
            breakdown  = emptyList()
        )
        val result = useCase.invoke(storageInfo = storage)
        val suggestion = result.filterIsInstance<CleaningSuggestion.StorageFull>().firstOrNull()
        assertTrue(suggestion != null)
    }

    @Test fun `storage above 10 percent not suggested`() {
        val storage = StorageInfo(
            totalBytes = 100 * 1024 * 1024 * 1024L,
            freeBytes  = 20 * 1024 * 1024 * 1024L,  // 20%
            usedBytes  = 80 * 1024 * 1024 * 1024L,
            breakdown  = emptyList()
        )
        val result = useCase.invoke(storageInfo = storage)
        assertTrue(result.none { it is CleaningSuggestion.StorageFull })
    }

    @Test fun `usageStatsAvailable false suppresses UnusedApp even for large long-installed apps`() {
        val app = makeApp(apkSizeBytes = 200 * 1024 * 1024L)
        val result = useCase.invoke(
            installedApps       = listOf(app),
            appLastUsedDays     = mapOf(app.packageName to 90),
            usageStatsAvailable = false
        )
        assertTrue(result.none { it is CleaningSuggestion.UnusedApp })
    }

    @Test fun `results sorted by estimatedWastedBytes descending`() {
        val groups  = listOf(DuplicateGroup(emptyList(), 150 * 1024 * 1024L))
        val app     = makeApp(apkSizeBytes = 200 * 1024 * 1024L)
        val result  = useCase.invoke(duplicateGroups = groups, installedApps = listOf(app), appLastUsedDays = mapOf(app.packageName to 90))
        if (result.size >= 2) {
            assertTrue(result[0].estimatedWastedBytes >= result[1].estimatedWastedBytes)
        }
    }

    private fun makeApp(
        packageName: String = "com.test.app",
        apkSizeBytes: Long = 60 * 1024 * 1024L,
        isSystem: Boolean = false
    ) = InstalledApp(
        packageName  = packageName,
        appName      = "Test App",
        versionName  = "1.0",
        versionCode  = 1L,
        apkSizeBytes = apkSizeBytes,
        cacheBytes   = 0L,
        dataBytes    = 0L,
        installTime  = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000,
        updateTime   = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000,
        isSystem     = isSystem
    )
}
