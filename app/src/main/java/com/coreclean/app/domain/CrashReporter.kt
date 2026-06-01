package com.coreclean.app.domain

interface CrashReporter {
    fun setEnabled(enabled: Boolean)
    fun captureException(throwable: Throwable)
    fun addBreadcrumb(message: String)
}
