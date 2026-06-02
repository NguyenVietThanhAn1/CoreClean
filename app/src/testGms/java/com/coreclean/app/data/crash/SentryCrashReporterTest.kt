package com.coreclean.app.data.crash

import io.mockk.mockkStatic
import io.mockk.verify
import io.sentry.Sentry
import org.junit.Test

class SentryCrashReporterTest {

    @Test
    fun `disableAfterEnable closes Sentry`() {
        mockkStatic(Sentry::class)
        val reporter = SentryCrashReporter()
        reporter.setEnabled(false)
        verify { Sentry.close() }
    }

    @Test
    fun `enable true does not close Sentry`() {
        mockkStatic(Sentry::class)
        val reporter = SentryCrashReporter()
        reporter.setEnabled(true)
        verify(exactly = 0) { Sentry.close() }
    }
}
