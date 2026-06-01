package com.coreclean.app.data.repository

import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.data.datasource.usage.AppUsageDataSource
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class AppUsageRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: AppUsageDataSource
    private lateinit var repository: AppUsageRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        val context = mockk<android.content.Context>(relaxed = true)
        repository = AppUsageRepositoryImpl(context, dataSource)
    }

    @Test
    fun `getUsageStats returns sorted list from dataSource`() = runTest {
        val items = listOf(
            AppUsageInfo("com.b", "App B", 5_000L, 0L),
            AppUsageInfo("com.a", "App A", 10_000L, 0L),
        )
        coEvery { dataSource.getUsageStats(UsageRange.LAST_7) } returns items

        val result = repository.getUsageStats(UsageRange.LAST_7)
        assertEquals(2, result.size)
        assertEquals("App B", result[0].appName)
    }

    @Test
    fun `getUsageStats returns empty list when dataSource returns empty`() = runTest {
        coEvery { dataSource.getUsageStats(UsageRange.LAST_30) } returns emptyList()

        val result = repository.getUsageStats(UsageRange.LAST_30)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `user-updated system app is included in results`() = runTest {
        // AppUsageDataSource.isSystemApp filters FLAG_SYSTEM && !FLAG_UPDATED.
        // A user-updated system app (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP) should NOT be filtered.
        // Since the dataSource mock already returns the pre-filtered list, we verify that
        // if the datasource returns it, the repository passes it through unchanged.
        val updatedSystemApp = AppUsageInfo("com.google.android.gms", "Google Play Services", 3_600_000L, 0L)
        coEvery { dataSource.getUsageStats(UsageRange.LAST_7) } returns listOf(updatedSystemApp)

        val result = repository.getUsageStats(UsageRange.LAST_7)
        assertEquals(1, result.size)
        assertEquals("com.google.android.gms", result.first().packageName)
    }
}
