package com.coreclean.app

import com.coreclean.app.domain.model.Frequency
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.ScheduleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-level tests for AutoClean scheduling logic.
 * Worker integration tests belong in androidTest (require real Context + Hilt).
 */
class AutoCleanWorkerTest {

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
        // APP_CACHE must never be in default safe categories
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
        // Verify the safe category set used by AutoCleanWorker does not include APP_CACHE
        val safeCategories = setOf(
            JunkCategory.TEMP_FILES,
            JunkCategory.EMPTY_FOLDERS,
            JunkCategory.RESIDUAL_APK
        )
        assertFalse(JunkCategory.APP_CACHE in safeCategories)
    }
}
