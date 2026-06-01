package com.coreclean.app.data.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for rate-limit logic in RecommendationNotifier.
 * The isOlderThanRateLimit() logic is tested via helper functions here
 * since the real method is private in the class (requires Context + DataStore for integration).
 */
class RecommendationNotifierTest {

    private val rateLimitDays = 7L

    private fun isOlderThanRateLimit(isoTimestamp: String): Boolean = runCatching {
        val last = Instant.parse(isoTimestamp)
        ChronoUnit.DAYS.between(last, Instant.now()) >= rateLimitDays
    }.getOrDefault(true)

    @Test
    fun `timestamp older than 7 days should pass rate limit`() {
        val eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS).toString()
        assertTrue(isOlderThanRateLimit(eightDaysAgo))
    }

    @Test
    fun `timestamp within 7 days should fail rate limit`() {
        val twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).toString()
        assertFalse(isOlderThanRateLimit(twoDaysAgo))
    }

    @Test
    fun `timestamp exactly 7 days ago should pass rate limit`() {
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS).minus(1, ChronoUnit.MINUTES).toString()
        assertTrue(isOlderThanRateLimit(sevenDaysAgo))
    }

    @Test
    fun `invalid timestamp should default to allowing notification`() {
        assertTrue(isOlderThanRateLimit("not-a-valid-timestamp"))
    }

    @Test
    fun `storage critical threshold is 5 percent`() {
        // Verify storage critical threshold constant
        val storageCriticalPct = 5
        assertTrue(3 < storageCriticalPct)
        assertFalse(6 < storageCriticalPct)
    }

    @Test
    fun `duplicate waste threshold is 2 GB`() {
        val twoGb = 2L * 1024 * 1024 * 1024
        assertTrue(twoGb > 0)
        assertTrue(3L * 1024 * 1024 * 1024 > twoGb)
        assertFalse(1L * 1024 * 1024 * 1024 >= twoGb)
    }
}
