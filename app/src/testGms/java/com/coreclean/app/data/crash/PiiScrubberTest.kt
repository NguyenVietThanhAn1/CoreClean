package com.coreclean.app.data.crash

import com.coreclean.app.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import io.sentry.protocol.SentryStackFrame
import io.sentry.protocol.SentryStackTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class PiiScrubberTest {

    @Test
    fun `redactPii replaces storage file path`() {
        val input = "Failed to read /storage/emulated/0/DCIM/photo.jpg"
        assertEquals("Failed to read [REDACTED_PATH]", redactPii(input))
    }

    @Test
    fun `redactPii replaces data and sdcard paths`() {
        // The path match runs to end-of-line (not end-of-whitespace), so trailing prose on the
        // same line after the path is swallowed into the redaction too — see the FILE_PATH_REGEX
        // over-redaction-vs-leak trade-off comment in SentryCrashReporter.kt.
        assertEquals(
            "[REDACTED_PATH]",
            redactPii("/data/user/0/com.coreclean.app/cache denied")
        )
        assertEquals(
            "open [REDACTED_PATH]",
            redactPii("open /sdcard/Download/file.zip failed")
        )
    }

    @Test
    fun `redactPii replaces a bare path prefix with no trailing path segment`() {
        assertEquals("Path is [REDACTED_PATH]", redactPii("Path is /storage"))
        assertEquals("Failed: [REDACTED_PATH].", redactPii("Failed: /data."))
    }

    @Test
    fun `redactPii fully redacts a file path containing spaces instead of leaking the tail`() {
        // Android filenames routinely contain spaces (WhatsApp media, screenshots, human-typed
        // names). The path match must run past whitespace or it leaks everything after the first
        // space, e.g. only up to ".../Download/Jane" — see the FILE_PATH_REGEX comment.
        assertEquals(
            "Failed: [REDACTED_PATH]",
            redactPii("Failed: /storage/emulated/0/Download/Jane Doe Resume.pdf")
        )
        assertEquals(
            "Failed: [REDACTED_PATH]",
            redactPii("Failed: /storage/emulated/0/WhatsApp Images/IMG-20260101-WA0005.jpg")
        )
    }

    @Test
    fun `redactPii replaces content uri`() {
        val input = "Could not open content://com.android.providers.media.documents/document/image%3A123"
        assertEquals("Could not open [REDACTED_URI]", redactPii(input))
    }

    @Test
    fun `redactPii replaces installed package name`() {
        val input = "Unable to resolve package com.whatsapp.messenger"
        assertEquals("Unable to resolve package [REDACTED_PACKAGE]", redactPii(input))
    }

    @Test
    fun `redactPii does not redact a 2-segment dotted identifier below the package-shape threshold`() {
        // DOTTED_IDENTIFIER_REGEX matches "foo.bar", but the package-name pass only fires at
        // segmentCount >= 3, so a 2-segment token (file extension, version string, etc.) is left
        // untouched even though it matches the dotted-identifier shape.
        val input = "Unable to reach consulting-partner.com"
        assertEquals(input, redactPii(input))
    }

    @Test
    fun `redactPii replaces a 3+-segment domain whose leading label starts with a digit`() {
        // Only the first segment is allowed to start with a digit — without this, "1.bp..."
        // would only match starting at "bp", leaving "1." as an unredacted chopped prefix.
        assertEquals(
            "Could not reach [REDACTED_PACKAGE] today",
            redactPii("Could not reach 1.bp.blogspot.com today")
        )
        assertEquals(
            "Failed to load [REDACTED_PACKAGE] resource",
            redactPii("Failed to load 3rdpartyapi.example.com resource")
        )
    }

    @Test
    fun `redactPii replaces email address`() {
        val input = "Merge failed for contact john.doe+work@example.com"
        assertEquals("Merge failed for contact [REDACTED_EMAIL]", redactPii(input))
    }

    @Test
    fun `redactPii replaces email with a 3-segment domain without leaving a mangled package tail`() {
        val input = "Notify user@mail.example.com about the failure"
        assertEquals("Notify [REDACTED_EMAIL] about the failure", redactPii(input))
    }

    @Test
    fun `redactPii replaces international phone number with plus prefix`() {
        assertEquals("Call: [REDACTED_PHONE]", redactPii("Call: +14155552671"))
        assertEquals("Call: [REDACTED_PHONE]", redactPii("Call: +1 415 555 2671"))
    }

    @Test
    fun `redactPii replaces dash- and dot-separated phone numbers unconditionally, with no NANP validity check`() {
        // Dash/dot punctuation is already unambiguous phone-number formatting, so these are
        // redacted regardless of whether the area/exchange code happens to be NANP-valid (2-9).
        assertEquals("Call: [REDACTED_PHONE]", redactPii("Call: (415) 555-2671"))
        assertEquals("Call: [REDACTED_PHONE]", redactPii("Call: 415-555-2671"))
        assertEquals("Call: [REDACTED_PHONE]", redactPii("Call: 415.555.2671"))
        assertEquals(
            "Contact: [REDACTED_PHONE]",
            redactPii("Contact: 015-234-5678")
        )
    }

    @Test
    fun `redactPii does not redact any whitespace-separated 3-3-4 digit triple, phone-shaped or not`() {
        // Bare space/tab-separated triples are never redacted, on purpose: this app's
        // crash/breadcrumb text can legitimately contain unrelated whitespace-separated number
        // triples (frame dimensions, image-dedup coordinate triples, counters) that are
        // structurally identical to a phone number in this style, and there is no reliable
        // regex-only way to tell them apart. A real phone number written this way (even with a
        // "Call"/"phone"/"tel" label nearby) is an accepted residual, not redacted.
        assertEquals("codes 100 200 3000", redactPii("codes 100 200 3000"))
        assertEquals("Received frame 480 640 3840", redactPii("Received frame 480 640 3840"))
        assertEquals("Call: 415 555 2671", redactPii("Call: 415 555 2671"))
        assertEquals(
            "Received frame 480\t640\t3840",
            redactPii("Received frame 480\t640\t3840")
        )
    }

    @Test
    fun `redactPii does not redact space-separated digit runs that are not 3-3-4 shaped`() {
        assertEquals("userId 48291 049582", redactPii("userId 48291 049582"))
    }

    @Test
    fun `redactPii does not redact a bare unformatted digit run as a phone number`() {
        // A raw 10-15 digit run with no '+' and no phone-style grouping is far more likely to be
        // a database ID, epoch timestamp, or similar than an actual phone number.
        assertEquals(
            "userId=48291049582 failed",
            redactPii("userId=48291049582 failed")
        )
        assertEquals(
            "timestamp 1752556800000",
            redactPii("timestamp 1752556800000")
        )
        assertEquals(
            "Contact: 4155552671000",
            redactPii("Contact: 4155552671000")
        )
    }

    @Test
    fun `redactPii does not redact short digit runs, memory addresses, or error codes`() {
        assertEquals("lineno=42, retryCount=3", redactPii("lineno=42, retryCount=3"))
        assertEquals(
            "fault address 0x7f8a2c001000",
            redactPii("fault address 0x7f8a2c001000")
        )
        assertEquals("ERROR_1004: bad state", redactPii("ERROR_1004: bad state"))
        assertEquals("HTTP 404 Not Found", redactPii("HTTP 404 Not Found"))
    }

    @Test
    fun `redactPii does not redact an IPv4 address as a phone number or package name`() {
        assertEquals(
            "connected to 192.168.0.1 failed",
            redactPii("connected to 192.168.0.1 failed")
        )
        assertEquals(
            "connected to 192.168.1.100 failed",
            redactPii("connected to 192.168.1.100 failed")
        )
    }

    @Test
    fun `redactPii does not leave a path suffix unredacted when prefix is not followed by a slash`() {
        val input = "/storageX/secret/private-notes.txt unreadable"
        val result = redactPii(input)
        assertEquals("/storageX/secret/private-notes.txt unreadable", result)
    }

    @Test
    fun `redactPii does not redact framework or own-app namespaces`() {
        assertEquals(
            "at androidx.compose.foundation.Foo",
            redactPii("at androidx.compose.foundation.Foo")
        )
        assertEquals(
            "reported by com.coreclean.app.presentation.junk.JunkViewModel",
            redactPii("reported by com.coreclean.app.presentation.junk.JunkViewModel")
        )
        assertEquals(
            "caused by java.lang.NullPointerException",
            redactPii("caused by java.lang.NullPointerException")
        )
    }

    @Test
    fun `redactPii keeps a URL hostname intact while still redacting an email in the same URL`() {
        assertEquals(
            "https://api.example.com/x/[REDACTED_EMAIL]/y",
            redactPii("https://api.example.com/x/john@example.com/y")
        )
        assertEquals(
            "See http://sub.api.example.com for details",
            redactPii("See http://sub.api.example.com for details")
        )
    }

    @Test
    fun `redactPii keeps a protocol-relative URL hostname intact`() {
        assertEquals(
            "Load //api.example.com/resource failed",
            redactPii("Load //api.example.com/resource failed")
        )
        assertEquals(
            "//api.example.com/resource failed",
            redactPii("//api.example.com/resource failed")
        )
    }

    @Test
    fun `redactPii still redacts a package-shaped token after a mid-identifier double slash`() {
        // "foo//bar.baz.qux" is not a URL — the "//" is glued directly onto a preceding word
        // character, not preceded by whitespace/start-of-string/a scheme, so it must not be
        // treated as a protocol-relative hostname carve-out.
        assertEquals(
            "path foo//[REDACTED_PACKAGE] broken",
            redactPii("path foo//bar.baz.qux broken")
        )
    }

    @Test
    fun `redactPii still redacts a hostname after a non-http(s) scheme like ftp`() {
        // Only http(s) and bare protocol-relative "//" are exempt; other schemes (ftp, custom
        // deep-link schemes) are not URL-hostname carve-outs.
        assertEquals(
            "Fetch ftp://[REDACTED_PACKAGE]/export failed",
            redactPii("Fetch ftp://files.example.com/export failed")
        )
    }

    @Test
    fun `redactPii still redacts a hostname after a scheme that merely ends in http or https`() {
        // "shttp://" isn't the http(s) scheme — the "http" at its tail must not be mistaken for
        // a real scheme boundary just because it sits at the right fixed offset.
        assertEquals(
            "Deep link failed: shttp://[REDACTED_PACKAGE]/callback",
            redactPii("Deep link failed: shttp://com.attacker.malware.MainActivity/callback")
        )
    }

    @Test
    fun `redactPii still redacts a package name in a custom URI scheme authority (not http or https)`() {
        // A non-web scheme like "myapp://" is not a hostname carve-out case — the position right
        // after "://" here is an Android deep-link authority, which can legitimately be a real
        // installed-package/component name and must still be redacted.
        assertEquals(
            "Deep link failed: myapp://[REDACTED_PACKAGE]/callback",
            redactPii("Deep link failed: myapp://com.attacker.malware.MainActivity/callback")
        )
    }

    @Test
    fun `redactPii leaves clean text untouched`() {
        val input = "value was null, expected non-null Int"
        assertEquals(input, redactPii(input))
    }

    private fun exceptionEvent(
        type: String,
        value: String,
        filename: String,
        module: String,
        lineno: Int
    ): SentryEvent {
        val stackFrame = SentryStackFrame().apply {
            this.filename = filename
            this.module = module
            this.lineno = lineno
        }
        val exception = SentryException().apply {
            this.type = type
            this.value = value
            stacktrace = SentryStackTrace(mutableListOf(stackFrame))
        }
        return SentryEvent().apply {
            exceptions = mutableListOf(exception)
        }
    }

    @Test
    fun `scrubPii redacts exception message but keeps type and stack trace`() {
        val event = exceptionEvent(
            type = "java.io.FileNotFoundException",
            value = "/storage/emulated/0/DCIM/photo.jpg (No such file or directory)",
            filename = "JunkViewModel.kt",
            module = "com.coreclean.app.presentation.junk.JunkViewModel",
            lineno = 42
        )

        val result = scrubPii(event)

        val resultException = result.exceptions!!.first()
        assertEquals("java.io.FileNotFoundException", resultException.type)
        // Trailing context on the same line is swallowed into the redaction too (favoring
        // over-redaction over a leak) — see the FILE_PATH_REGEX comment in SentryCrashReporter.kt.
        assertEquals("[REDACTED_PATH]", resultException.value)
        val resultFrame = resultException.stacktrace!!.frames!!.first()
        assertEquals("JunkViewModel.kt", resultFrame.filename)
        assertEquals("com.coreclean.app.presentation.junk.JunkViewModel", resultFrame.module)
        assertEquals(42, resultFrame.lineno)
    }

    @Test
    fun `scrubPii redacts email and phone in exception message but keeps type, lineno, and class name`() {
        val event = exceptionEvent(
            type = "java.lang.IllegalStateException",
            value = "Merge failed for jane.doe@example.com, callback +14155552671, retryCount=3",
            filename = "ContactViewModel.kt",
            module = "com.coreclean.app.presentation.contact.ContactViewModel",
            lineno = 87
        )

        val result = scrubPii(event)

        val resultException = result.exceptions!!.first()
        assertEquals("java.lang.IllegalStateException", resultException.type)
        assertEquals(
            "Merge failed for [REDACTED_EMAIL], callback [REDACTED_PHONE], retryCount=3",
            resultException.value
        )
        val resultFrame = resultException.stacktrace!!.frames!!.first()
        assertEquals("ContactViewModel.kt", resultFrame.filename)
        assertEquals("com.coreclean.app.presentation.contact.ContactViewModel", resultFrame.module)
        assertEquals(87, resultFrame.lineno)
    }

    @Test
    fun `scrubPii redacts breadcrumb message and string data values only`() {
        val breadcrumb = Breadcrumb().apply {
            message = "Loaded content://media/external/images/media/456"
            setData("path", "/storage/emulated/0/Pictures/img.png")
            setData("count", 3)
        }
        val event = SentryEvent().apply {
            breadcrumbs = mutableListOf(breadcrumb)
        }

        val result = scrubPii(event)

        val resultBreadcrumb = result.breadcrumbs!!.first()
        assertEquals("Loaded [REDACTED_URI]", resultBreadcrumb.message)
        assertEquals("[REDACTED_PATH]", resultBreadcrumb.data["path"])
        assertEquals(3, resultBreadcrumb.data["count"])
    }

    @Test
    fun `scrubPii redacts extras string values only`() {
        val event = SentryEvent().apply {
            setExtra("lastPath", "/sdcard/Download/report.pdf")
            setExtra("retryCount", 2)
        }

        val result = scrubPii(event)

        assertEquals("[REDACTED_PATH]", result.extras!!["lastPath"])
        assertEquals(2, result.extras!!["retryCount"])
    }

    @Test
    fun `scrubPii leaves clean message untouched`() {
        val event = SentryEvent().apply {
            message = Message().apply {
                formatted = "App started successfully"
            }
        }

        val result = scrubPii(event)

        assertEquals("App started successfully", result.message!!.formatted)
    }

    @Test
    fun `scrubPii redacts PII in event message formatted and message fields`() {
        val event = SentryEvent().apply {
            message = Message().apply {
                formatted = "Notify jane.doe@example.com about /storage/emulated/0/report.pdf"
                message = "Notify jane.doe@example.com"
            }
        }

        val result = scrubPii(event)

        assertEquals(
            "Notify [REDACTED_EMAIL] about [REDACTED_PATH]",
            result.message!!.formatted
        )
        assertEquals("Notify [REDACTED_EMAIL]", result.message!!.message)
    }

    @Test
    fun `configureSentryOptions sets dsn, sample rate, and interaction tracing`() {
        val options = SentryAndroidOptions()

        configureSentryOptions(options)

        assertEquals(BuildConfig.SENTRY_DSN, options.dsn)
        assertEquals(0.1, options.tracesSampleRate!!, 0.0001)
        assertFalse(options.isEnableUserInteractionTracing)
    }

    @Test
    fun `configureSentryOptions wires beforeSend to scrubPii`() {
        val options = SentryAndroidOptions()
        configureSentryOptions(options)
        val beforeSend = options.beforeSend
        assertNotNull("beforeSend must be wired for PII to ever be scrubbed", beforeSend)

        val event = SentryEvent().apply {
            message = Message().apply { formatted = "Call: 415-555-2671" }
        }

        val result = beforeSend!!.execute(event, Hint())

        assertEquals("Call: [REDACTED_PHONE]", result!!.message!!.formatted)
    }
}
