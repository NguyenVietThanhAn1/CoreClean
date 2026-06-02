package com.coreclean.app.data.crash

import android.content.Context
import com.coreclean.app.BuildConfig
import com.coreclean.app.domain.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

class SentryCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) : CrashReporter {
    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            SentryAndroid.init(context) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.tracesSampleRate = 0.1
                options.isEnableUserInteractionTracing = false
            }
        } else {
            Sentry.close()
        }
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    override fun addBreadcrumb(message: String) {
        Sentry.addBreadcrumb(message)
    }
}
