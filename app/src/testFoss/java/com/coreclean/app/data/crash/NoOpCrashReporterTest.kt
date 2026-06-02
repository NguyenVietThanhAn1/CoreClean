package com.coreclean.app.data.crash

import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class NoOpCrashReporterTest {

    private val reporter = NoOpCrashReporter()

    @Test
    fun `setEnabled does not throw`() {
        reporter.setEnabled(true)
        reporter.setEnabled(false)
    }

    @Test
    fun `captureException does not throw`() {
        reporter.captureException(RuntimeException("test"))
    }

    @Test
    fun `addBreadcrumb does not throw`() {
        reporter.addBreadcrumb("test")
    }

    @Test
    fun `no method calls propagate to any external system`() {
        val spy = spyk(reporter)
        spy.setEnabled(false)
        spy.captureException(RuntimeException())
        spy.addBreadcrumb("msg")
        verify(exactly = 1) { spy.setEnabled(false) }
        verify(exactly = 1) { spy.captureException(any()) }
        verify(exactly = 1) { spy.addBreadcrumb(any()) }
    }
}
