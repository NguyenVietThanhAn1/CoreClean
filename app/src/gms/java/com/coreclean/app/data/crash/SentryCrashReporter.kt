package com.coreclean.app.data.crash

import android.content.Context
import com.coreclean.app.BuildConfig
import com.coreclean.app.domain.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import javax.inject.Inject

private val CONTENT_URI_REGEX = Regex("""content://\S+""")

// The trailing path-segment group consumes to end-of-line (`.*`, not `\S*`) rather than stopping
// at the first whitespace: real Android filenames routinely contain spaces (WhatsApp media,
// screenshots, human-typed names), and stopping at whitespace left the tail of such a filename
// unredacted, e.g. ".../Download/Jane Doe Resume.pdf" -> "[REDACTED_PATH] Doe Resume.pdf" — a
// direct leak of exactly the PII this regex exists to catch. The trade-off: any trailing prose on
// the same line after the path (e.g. "(No such file or directory)") is now swallowed into the
// redaction too, rather than preserved as debug context. That's an intentional choice — favoring
// over-redaction (safe, just loses some context) over under-redaction (a real leak).
private val FILE_PATH_REGEX = Regex("""(?:/data|/storage|/sdcard)(?=$|[^\w])(?:/.*)?""")
private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")

// Deliberately narrow: only matches phone-*shaped* text (a leading '+' international dialing
// prefix, a parenthesized NANP area code, or a dash/dot-grouped NANP triple), never a bare run of
// digits. A bare 10-15 digit run is far more likely to be a memory address, epoch timestamp,
// database ID, or error code than a phone number — those must NOT be redacted as one, so there is
// no branch here that matches digits without one of these formatting markers.
//
// The bare (unparenthesized) alternative deliberately requires a '-' or '.' separator, never a
// space or tab: a whitespace-separated 3-3-4 digit triple (e.g. "415 555 2671") is structurally
// identical to unrelated whitespace-separated number triples this app's crash/breadcrumb text can
// legitimately contain (frame dimensions, image-dedup coordinate triples, counters), and there is
// no reliable regex-only way to tell those apart — redacting them would destroy debugging context
// for no privacy benefit. A real phone number written in that bare whitespace-separated style is
// therefore not redacted; this is an accepted, deliberate residual (see TODOS.md), not an
// oversight. Dash/dot punctuation, by contrast, is already unambiguous phone-number formatting for
// this shape, so the bare alternative also has no NANP N-digit ([2-9]) validity check — it redacts
// any dash/dot 3-3-4 triple unconditionally, real NANP-valid or not, since narrowing by validity
// would just as easily discard a genuine (if malformed or non-US) phone number as a coincidental
// match.
private val PHONE_REGEX = Regex(
    """(?<!\d)\+\d{1,3}(?:[-.\s]?\d{2,4}){2,4}(?!\d)""" +
        """|(?<!\d)\(\d{3}\)[-.\s]?\d{3}[-.\s]?\d{4}(?!\d)""" +
        """|(?<!\d)\d{3}[-.]\d{3}[-.]\d{4}(?!\d)"""
)

// Matches the *maximal* run of dot-separated segments (1+), so a namespace check can be applied
// to the whole identifier in one place. A lookahead-based exclusion embedded in the pattern
// itself doesn't work here: rejecting a match starting at "androidx" just makes the regex engine
// retry at the next offset and match "compose.foundation.Foo" as its own 3-segment package name.
// Matching greedily first and then classifying the full match avoids that. The same reasoning is
// why the URL-hostname carve-out below is done as a post-match string check (isUrlHostname)
// rather than a lookbehind baked into this pattern: a lookbehind only blocks a match that starts
// *exactly* at "://", so for a multi-label host like "sub.api.example.com" the engine would just
// retry one segment later and still mangle "api.example.com" into [REDACTED_PACKAGE].
//
// Only the FIRST segment's leading character is allowed to be a digit (e.g. the "1" in
// "1.bp.blogspot.com" or the "3" in "3rdpartyapi.example.com") — every segment after the first
// dot still requires a letter-start. Widening every segment to allow digit-start would make an
// IPv4 address like "192.168.0.1" match too (four digit-led segments), silently overriding the
// dedicated IPv4 exclusion below; requiring subsequent segments to start with a letter keeps that
// exclusion intact (no IPv4 octet after the first is letter-led) while still catching a
// domain-shaped identifier whose leading label happens to start with a digit — Java/Kotlin package
// segments can never start with a digit, so this only ever affects domain-shaped tokens, not real
// package names.
private val DOTTED_IDENTIFIER_REGEX = Regex("""\b[a-zA-Z0-9][a-zA-Z0-9_]*(?:\.[a-zA-Z][a-zA-Z0-9_]*)+\b""")

// Framework/own-package namespaces are debug-relevant (e.g. "androidx.compose.foundation.Foo" or
// "com.coreclean.app.JunkViewModel") and never identify a user-installed app, so they're exempt.
private val SAFE_NAMESPACE_PREFIXES =
    listOf("androidx", "android", "kotlinx", "kotlin", "javax", "java", "com.coreclean")

private fun isSafeNamespace(identifier: String): Boolean =
    SAFE_NAMESPACE_PREFIXES.any { prefix -> identifier == prefix || identifier.startsWith("$prefix.") }

// True when the dotted identifier at [matchStart] in [text] is a web URL hostname, i.e.
// immediately preceded by "http://", "https://", or a bare protocol-relative "//". Because
// DOTTED_IDENTIFIER_REGEX matches the *maximal* run of segments greedily, a hostname (even a
// multi-label one like
// "sub.api.example.com") is always captured as a single match starting right after the scheme —
// so this simple fixed-offset check is sufficient and doesn't need to walk back through the
// hostname label by label. A bare hostname isn't PII, and redacting it would destroy debugging
// context (e.g. which API host a network error came from) for no privacy benefit; any actual PII
// elsewhere in the same URL (an email in the path, say) is still redacted normally by the earlier
// passes in redactPii, since this check only exempts the package-name pass, not the other
// regexes.
//
// Deliberately checks for the literal "http://"/"https://" schemes, plus bare "//" (a
// protocol-relative URL, e.g. "//api.example.com/resource"), NOT a generic "any scheme://"
// pattern — an earlier version checked only for a trailing "://", which incorrectly exempted
// Android custom-URI-scheme authorities too (e.g. "myapp://com.attacker.malware.MainActivity",
// where the position right after "://" is a real installed-package name, not a web host).
// Restricting to http(s) and bare "//" keeps that PII caught while still protecting real
// hostnames.
//
// Matching "http://"/"https://" isn't enough on its own: a scheme whose name merely *ends* in
// those letters (e.g. "shttp://") would otherwise also match at the same fixed offset.
// isSchemeBoundary additionally requires the scheme to start at a real token boundary — the
// beginning of the text or right after a non-letter character — so "shttp://" (preceded by the
// letter 's') is correctly rejected while ordinary usage (start of string, after a
// space/punctuation/digit) still matches.
//
// The bare "//" case needs two extra guards the scheme-prefixed cases don't:
//   - not preceded by ':' — otherwise this would re-exempt exactly the custom-scheme case above,
//     since "myapp://host" also has "//" right before the hostname.
//   - not preceded by a word character — otherwise a "//" glued mid-identifier (e.g.
//     "foo//bar.baz.qux", not a URL at all) would be wrongly treated as protocol-relative.
private fun isSchemeBoundary(text: String, schemeStart: Int): Boolean {
    val charBeforeScheme = schemeStart - 1
    return charBeforeScheme < 0 || !text[charBeforeScheme].isLetter()
}

private fun isUrlHostname(text: String, matchStart: Int): Boolean {
    val httpsStart = matchStart - 8
    val precededByHttps = httpsStart >= 0 &&
        text.regionMatches(httpsStart, "https://", 0, 8, ignoreCase = true) &&
        isSchemeBoundary(text, httpsStart)
    val httpStart = matchStart - 7
    val precededByHttp = httpStart >= 0 &&
        text.regionMatches(httpStart, "http://", 0, 7, ignoreCase = true) &&
        isSchemeBoundary(text, httpStart)
    val precededBySlashes = matchStart >= 2 && text.regionMatches(matchStart - 2, "//", 0, 2)
    val charBeforeSlashes = matchStart - 3
    val precededByScheme = charBeforeSlashes >= 0 && text[charBeforeSlashes] == ':'
    val precededByWordChar = charBeforeSlashes >= 0 &&
        (text[charBeforeSlashes].isLetterOrDigit() || text[charBeforeSlashes] == '_')
    val precededByProtocolRelative = precededBySlashes && !precededByScheme && !precededByWordChar
    return precededByHttps || precededByHttp || precededByProtocolRelative
}

/**
 * Order matters: content:// URIs, file paths, emails, and phone numbers are all redacted
 * before the package-name pass runs. Otherwise a 3+-segment email domain (e.g.
 * "mail.example.com" in "user@mail.example.com") would get caught by the package-name pass
 * first, leaving a dangling "user@[REDACTED_PACKAGE]" instead of a clean [REDACTED_EMAIL].
 */
internal fun redactPii(text: String): String {
    val withoutStructuredPii = text
        .replace(CONTENT_URI_REGEX, "[REDACTED_URI]")
        .replace(FILE_PATH_REGEX, "[REDACTED_PATH]")
        .replace(EMAIL_REGEX, "[REDACTED_EMAIL]")
        .replace(PHONE_REGEX, "[REDACTED_PHONE]")
    return DOTTED_IDENTIFIER_REGEX.replace(withoutStructuredPii) { match ->
        val identifier = match.value
        val segmentCount = identifier.count { it == '.' } + 1
        val isPackageShaped = segmentCount >= 3 && !isSafeNamespace(identifier)
        if (isPackageShaped && !isUrlHostname(withoutStructuredPii, match.range.first)) {
            "[REDACTED_PACKAGE]"
        } else {
            identifier
        }
    }
}

private fun redactPiiOrNull(text: String?): String? = text?.let(::redactPii)

private fun redactStringValues(map: MutableMap<String, Any>) {
    for (entry in map.entries) {
        (entry.value as? String)?.let { entry.setValue(redactPii(it)) }
    }
}

/** Strips file paths / content URIs / package names / emails / phone numbers from a Sentry event before upload. Stack traces (class/file/line) are left untouched — they're needed for debugging and don't contain user data. */
internal fun scrubPii(event: SentryEvent): SentryEvent {
    event.message?.apply {
        formatted = redactPiiOrNull(formatted)
        message = redactPiiOrNull(message)
    }
    event.exceptions?.forEach { exception ->
        exception.value = redactPiiOrNull(exception.value)
    }
    event.breadcrumbs?.forEach { breadcrumb ->
        breadcrumb.message = redactPiiOrNull(breadcrumb.message)
        redactStringValues(breadcrumb.data)
    }
    event.extras?.let(::redactStringValues)
    return event
}

/** Single shared config applied at every Sentry.init call site so beforeSend scrubbing can never be wired up at one but forgotten at another. */
internal fun configureSentryOptions(options: SentryAndroidOptions) {
    options.dsn = BuildConfig.SENTRY_DSN
    options.tracesSampleRate = 0.1
    options.isEnableUserInteractionTracing = false
    options.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> scrubPii(event) }
}

class SentryCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) : CrashReporter {
    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            SentryAndroid.init(context) { options -> configureSentryOptions(options) }
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
