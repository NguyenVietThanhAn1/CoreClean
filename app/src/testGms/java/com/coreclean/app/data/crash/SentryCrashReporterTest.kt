package com.coreclean.app.data.crash

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class SentryCrashReporterTest {

    private val context: Context = mockk(relaxed = true)

    private fun stubSentryInit() {
        every {
            SentryAndroid.init(
                any<Context>(),
                any<io.sentry.Sentry.OptionsConfiguration<SentryAndroidOptions>>()
            )
        } just runs
    }

    @Test
    fun `setEnabled false closes Sentry`() {
        mockkStatic(Sentry::class)
        every { Sentry.close() } just runs

        val reporter = SentryCrashReporter(context)
        reporter.setEnabled(false)

        verify { Sentry.close() }
    }

    @Test
    fun `setEnabled true does not close Sentry`() {
        mockkStatic(Sentry::class)
        mockkStatic(SentryAndroid::class)
        stubSentryInit()

        val reporter = SentryCrashReporter(context)
        reporter.setEnabled(true)

        verify(exactly = 0) { Sentry.close() }
    }

    @Test
    fun `setEnabled true after false re-inits Sentry`() {
        mockkStatic(Sentry::class)
        mockkStatic(SentryAndroid::class)
        every { Sentry.close() } just runs
        stubSentryInit()

        val reporter = SentryCrashReporter(context)
        reporter.setEnabled(false)
        reporter.setEnabled(true)

        verify(ordering = io.mockk.Ordering.SEQUENCE) {
            Sentry.close()
            SentryAndroid.init(
                any<Context>(),
                any<io.sentry.Sentry.OptionsConfiguration<SentryAndroidOptions>>()
            )
        }
    }
}
