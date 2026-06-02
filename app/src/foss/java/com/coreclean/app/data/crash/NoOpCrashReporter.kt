package com.coreclean.app.data.crash

import com.coreclean.app.domain.CrashReporter
import javax.inject.Inject

class NoOpCrashReporter @Inject constructor() : CrashReporter {
    override fun setEnabled(enabled: Boolean) = Unit
    override fun captureException(throwable: Throwable) = Unit
    override fun addBreadcrumb(message: String) = Unit
}
