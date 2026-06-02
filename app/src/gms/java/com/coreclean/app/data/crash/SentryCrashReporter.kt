package com.coreclean.app.data.crash

import com.coreclean.app.domain.CrashReporter
import io.sentry.Sentry
import javax.inject.Inject

class SentryCrashReporter @Inject constructor() : CrashReporter {
    override fun setEnabled(enabled: Boolean) {
        if (!enabled) Sentry.close()
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    override fun addBreadcrumb(message: String) {
        Sentry.addBreadcrumb(message)
    }
}
