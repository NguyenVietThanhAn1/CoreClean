package com.coreclean.app.data.datasource.storage

import android.os.Environment
import com.coreclean.app.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStatFs

/**
 * Verifies StorageDataSource structural contract using Robolectric.
 * Uses a plain Application to avoid CleanerApp.onCreate() initialising WorkManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class StorageDataSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: StorageDataSource

    @Before
    fun setUp() {
        // Register fake block counts so StatFs returns non-zero values
        ShadowStatFs.registerStats(
            Environment.getExternalStorageDirectory().absolutePath,
            /* blockCount    */ 256_000,
            /* freeBlocks    */ 128_000,
            /* availableBlocks */ 128_000
        )
        val context = RuntimeEnvironment.getApplication()
        dataSource = StorageDataSource(context, mainDispatcherRule.dispatcher)
    }

    @Test
    fun `breakdown contains exactly 5 categories`() = runTest(mainDispatcherRule.testScheduler) {
        val info = dataSource.getStorageInfo()
        assertEquals(5, info.breakdown.size)
    }

    @Test
    fun `usedBytes equals total minus free`() = runTest(mainDispatcherRule.testScheduler) {
        val info = dataSource.getStorageInfo()
        assertEquals(info.totalBytes - info.freeBytes, info.usedBytes)
    }

    @Test
    fun `all breakdown sizes are non-negative`() = runTest(mainDispatcherRule.testScheduler) {
        val info = dataSource.getStorageInfo()
        info.breakdown.forEach { cat ->
            assertTrue("${cat.name} size should be >= 0", cat.sizeBytes >= 0)
        }
    }
}
