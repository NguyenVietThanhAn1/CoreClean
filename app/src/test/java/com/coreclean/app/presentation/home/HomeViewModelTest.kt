package com.coreclean.app.presentation.home

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppListRepository
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.StorageRepository
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import com.coreclean.app.domain.usecase.suggest.GenerateSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>

    private val mediaRepository    = mockk<MediaRepository>()
    private val storageRepository  = mockk<StorageRepository>()
    private val appListRepository  = mockk<AppListRepository>()
    private val appUsageRepository = mockk<AppUsageRepository>()
    private val scanJunkUseCase    = mockk<ScanJunkUseCase>()

    // Real use case — verify rule-firing end-to-end.
    private val generateSuggestions = GenerateSuggestionsUseCase()

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope       = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.dispatcher),
            produceFile = { tmpFolder.newFile("home_test_prefs.preferences_pb") }
        )
        // Default stubs — override per test where needed.
        every  { mediaRepository.getAllImages() }                  returns flowOf(emptyList())
        coEvery { storageRepository.getStorageInfo() }             returns emptyStorage()
        coEvery { mediaRepository.findDuplicates(any()) }          returns emptyList()
        coEvery { appListRepository.getInstalledApps() }           returns emptyList()
        every  { appUsageRepository.hasUsageAccessPermission() }   returns false
        coEvery { appUsageRepository.getUsageStats(any()) }        returns emptyList()
        coEvery { scanJunkUseCase(any()) }                         returns emptyList()
    }

    private fun createViewModel() = HomeViewModel(
        generateSuggestions = generateSuggestions,
        mediaRepository     = mediaRepository,
        storageRepository   = storageRepository,
        appListRepository   = appListRepository,
        appUsageRepository  = appUsageRepository,
        scanJunkUseCase     = scanJunkUseCase,
        dataStore           = dataStore
    )

    // ── Rule 1: LargeDuplicateGroup (>100 MB wasted) ──────────────────────

    @Test
    fun `rule1 fires when duplicate groups waste more than 100 MB`() =
        runTest(mainDispatcherRule.testScheduler) {
            val wastedBytes = 110L * 1024 * 1024
            val group = DuplicateGroup(
                images          = emptyList(),
                totalWastedSize = wastedBytes
            )
            coEvery { mediaRepository.findDuplicates(any()) } returns listOf(group)

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.suggestions.value.any { it is CleaningSuggestion.LargeDuplicateGroup })
        }

    // ── Rule 2: UnusedApp (>60 days unused AND >50 MB) ────────────────────

    @Test
    fun `rule2 fires when an app is unused for more than 60 days and large`() =
        runTest(mainDispatcherRule.testScheduler) {
            val seventyDaysAgoMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(70)
            val app = InstalledApp(
                packageName  = "com.example.old",
                appName      = "OldApp",
                versionName  = "1.0",
                versionCode  = 1L,
                apkSizeBytes = 60L * 1024 * 1024,
                cacheBytes   = 0L,
                dataBytes    = 0L,
                installTime  = seventyDaysAgoMs - TimeUnit.DAYS.toMillis(365),
                updateTime   = seventyDaysAgoMs,
                isSystem     = false
            )
            coEvery { appListRepository.getInstalledApps() } returns listOf(app)
            every  { appUsageRepository.hasUsageAccessPermission() } returns true
            coEvery { appUsageRepository.getUsageStats(UsageRange.LAST_30) } returns listOf(
                AppUsageInfo(
                    packageName           = "com.example.old",
                    appName               = "OldApp",
                    totalTimeForegroundMs = 0L,
                    lastTimeUsed          = seventyDaysAgoMs
                )
            )

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.suggestions.value.any { it is CleaningSuggestion.UnusedApp })
        }

    // ── Rule 3: OversizedDownload (residual APK >200 MB) ─────────────────

    @Test
    fun `rule3 fires when a residual APK is larger than 200 MB`() =
        runTest(mainDispatcherRule.testScheduler) {
            val bigApk = JunkItem(
                path      = "/storage/emulated/0/Download/old.apk",
                sizeBytes = 210L * 1024 * 1024,
                category  = JunkCategory.RESIDUAL_APK
            )
            coEvery { scanJunkUseCase(any()) } returns listOf(bigApk)

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.suggestions.value.any { it is CleaningSuggestion.OversizedDownload })
        }

    // ── Rule 4: StaleScreenshot (screenshot >90 days old) ────────────────

    @Test
    fun `rule4 fires when screenshots are older than 90 days`() =
        runTest(mainDispatcherRule.testScheduler) {
            val ninetyOneDaysAgoSec =
                System.currentTimeMillis() / 1000L - TimeUnit.DAYS.toSeconds(91)
            val screenshot = MediaImage(
                id        = 1L,
                uri       = Uri.parse("content://media/external/images/media/1"),
                name      = "screenshot_001.png",
                path      = "/storage/emulated/0/DCIM/Screenshots/screenshot_001.png",
                size      = 500_000L,
                dateAdded = ninetyOneDaysAgoSec,
                mimeType  = "image/png"
            )
            every { mediaRepository.getAllImages() } returns flowOf(listOf(screenshot))

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.suggestions.value.any { it is CleaningSuggestion.StaleScreenshot })
        }

    // ── Rule 5: StorageFull (<10% free) ──────────────────────────────────

    @Test
    fun `rule5 fires when free storage is below 10 percent`() =
        runTest(mainDispatcherRule.testScheduler) {
            val total = 32L * 1024 * 1024 * 1024
            coEvery { storageRepository.getStorageInfo() } returns StorageInfo(
                totalBytes = total,
                freeBytes  = (total * 0.05).toLong(),
                usedBytes  = (total * 0.95).toLong(),
                breakdown  = emptyList()
            )

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.suggestions.value.any { it is CleaningSuggestion.StorageFull })
        }

    // ── Cache: heavy repos skipped when cache is fresh ───────────────────

    @Test
    fun `heavy repos not called on second loadSuggestions within 6 hours`() =
        runTest(mainDispatcherRule.testScheduler) {
            val vm = createViewModel()
            advanceUntilIdle()          // first load — writes cache timestamp

            var appListCallCount = 0
            coEvery { appListRepository.getInstalledApps() } answers {
                appListCallCount++
                emptyList()
            }

            vm.loadSuggestions()
            advanceUntilIdle()

            assertEquals(
                "appListRepository should not be called when cache is fresh",
                0, appListCallCount
            )
        }

    // ── Cache JSON: cold load emits cached suggestions before IO ─────────

    @Test
    fun `cold load emits cached suggestions from DataStore before heavy IO runs`() =
        runTest(mainDispatcherRule.testScheduler) {
            // Pre-populate DataStore with a fresh timestamp + serialized suggestion.
            val cachedSuggestion = CleaningSuggestion.StorageFull(freePercent = 0.04f)
            val cachedJson = Json.encodeToString<List<CleaningSuggestion>>(listOf(cachedSuggestion))
            val freshTs = System.currentTimeMillis()   // not stale

            dataStore.edit { prefs ->
                prefs[AppPreferenceKeys.HOME_SUGGESTIONS_CACHE_TS]   = freshTs
                prefs[AppPreferenceKeys.HOME_SUGGESTIONS_CACHE_JSON] = cachedJson
            }

            // Heavy IO stubs return empty — if they run, suggestions would be cleared.
            coEvery { storageRepository.getStorageInfo() } returns emptyStorage()

            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(
                "Expected cached StorageFull suggestion after cold load",
                vm.suggestions.value.any { it is CleaningSuggestion.StorageFull }
            )
        }

    // ── Error resilience ─────────────────────────────────────────────────

    @Test
    fun `loadSuggestions does not crash on repository error`() =
        runTest(mainDispatcherRule.testScheduler) {
            coEvery { storageRepository.getStorageInfo() } throws RuntimeException("Simulated error")

            val vm = createViewModel()
            advanceUntilIdle()

            assertEquals(emptyList<CleaningSuggestion>(), vm.suggestions.value)
        }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun emptyStorage() = StorageInfo(
        totalBytes = 0L, freeBytes = 0L, usedBytes = 0L, breakdown = emptyList()
    )
}
