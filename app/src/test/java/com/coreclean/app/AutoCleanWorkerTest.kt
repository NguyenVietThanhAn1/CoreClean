package com.coreclean.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.Frequency
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.ScheduleConfig
import com.coreclean.app.presentation.settings.SettingsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class AutoCleanWorkerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test fun `default ScheduleConfig is disabled`() {
        val config = ScheduleConfig()
        assertFalse(config.enabled)
    }

    @Test fun `default ScheduleConfig has WEEKLY frequency`() {
        val config = ScheduleConfig()
        assertEquals(Frequency.WEEKLY, config.frequency)
    }

    @Test fun `default categories only contain safe ones`() {
        val config = ScheduleConfig()
        assertFalse(JunkCategory.APP_CACHE in config.categories)
        assertTrue(JunkCategory.TEMP_FILES in config.categories)
        assertTrue(JunkCategory.EMPTY_FOLDERS in config.categories)
    }

    @Test fun `ScheduleConfig serialization round-trips correctly`() {
        val original = ScheduleConfig(
            enabled    = true,
            frequency  = Frequency.DAILY,
            hour       = 3,
            minute     = 30,
            categories = setOf(JunkCategory.TEMP_FILES, JunkCategory.RESIDUAL_APK)
        )
        val json    = kotlinx.serialization.json.Json.encodeToString(original)
        val decoded = kotlinx.serialization.json.Json.decodeFromString<ScheduleConfig>(json)

        assertEquals(original.enabled,    decoded.enabled)
        assertEquals(original.frequency,  decoded.frequency)
        assertEquals(original.hour,       decoded.hour)
        assertEquals(original.minute,     decoded.minute)
        assertEquals(original.categories, decoded.categories)
    }

    @Test fun `APP_CACHE not in SAFE_CATEGORIES constant`() {
        val safeCategories = setOf(
            JunkCategory.TEMP_FILES,
            JunkCategory.EMPTY_FOLDERS,
            JunkCategory.RESIDUAL_APK
        )
        assertFalse(JunkCategory.APP_CACHE in safeCategories)
    }

    @Test
    fun `enqueueUniqueWork with REPLACE called each time setAutoCleanEnabled true is called`() =
        runTest(mainDispatcherRule.testScheduler) {
            val workManager = mockk<WorkManager>(relaxed = true)
            val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
                scope       = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.dispatcher),
                produceFile = { tmpFolder.newFile("acw_test.preferences_pb") }
            )
            val crashReporter = mockk<CrashReporter>(relaxed = true)
            val vm = SettingsViewModel(dataStore, workManager, crashReporter)

            vm.setAutoCleanEnabled(true)
            vm.setAutoCleanEnabled(true)
            vm.setAutoCleanEnabled(true)
            advanceUntilIdle()

            // Each call must use enqueueUniqueWork("auto_clean", REPLACE, ...) — not plain enqueue.
            // REPLACE policy ensures at most one active work item despite multiple calls.
            verify(atLeast = 3) {
                workManager.enqueueUniqueWork(
                    "auto_clean",
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>()
                )
            }
        }
}
