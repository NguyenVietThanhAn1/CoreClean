package com.coreclean.app.data.crash

import com.coreclean.app.domain.CrashReporter
import io.sentry.Sentry
import io.sentry.SentryLevel
import javax.inject.Inject

class SentryCrashReporter @Inject constructor() : CrashReporter {
    override fun setEnabled(enabled: Boolean) {
        // Sentry.setEnabled() is not available in sentry-android; we close the SDK instead.
        if (!enabled) Sentry.close()
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    override fun addBreadcrumb(message: String) {
        Sentry.addBreadcrumb(message)
    }
}
