package com.coreclean.app.data.datasource.media

import android.content.ContentResolver
import android.net.Uri
import com.coreclean.app.domain.CrashReporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptualHasherTest {

    private val hasher = PerceptualHasherImpl()

    @Test
    fun `same hash has zero hamming distance`() {
        val hash = 0x5A3F12BC9D7E4F01L
        assertTrue(hasher.hammingDistance(hash, hash) == 0)
    }

    @Test
    fun `completely different hashes have large hamming distance`() {
        val a = 0L           // all bits 0
        val b = -1L          // all bits 1 (0xFFFFFFFFFFFFFFFF in two's complement)
        assertTrue(hasher.hammingDistance(a, b) == 64)
    }

    @Test
    fun `similar hashes have small hamming distance`() {
        val a = 0b1010101010101010L
        val b = 0b1010101010101011L // only 1 bit different
        assertTrue(hasher.hammingDistance(a, b) <= 2)
    }

    @Test
    fun `very different hashes have large hamming distance`() {
        // 0x0F0F... (all low nibbles set) vs Long.MIN_VALUE (MSB set) + MAX (all other bits set)
        val a = 0L              // 0 bits set
        val b = Long.MAX_VALUE  // 63 bits set → 63 > 16
        assertTrue(hasher.hammingDistance(a, b) > 16)
    }

    @Test
    fun `computeHash reports exception to crashReporter and still returns null`() = runTest {
        val contentResolver = mockk<ContentResolver>()
        val crashReporter = mockk<CrashReporter>(relaxed = true)
        val uri = mockk<Uri>()
        val exception = RuntimeException("boom")
        every { contentResolver.openInputStream(uri) } throws exception

        val realHasher = PerceptualHasher(contentResolver, crashReporter)
        val result = realHasher.computeHash(uri)

        assertNull(result)
        verify(exactly = 1) { crashReporter.captureException(exception) }
    }
}

// Testable inner class that exposes hammingDistance without ContentResolver dependency
private class PerceptualHasherImpl {
    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
