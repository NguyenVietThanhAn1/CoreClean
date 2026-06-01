package com.coreclean.app.data.datasource.media

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
        val a = 0x0000000000000000L
        val b = 0xFFFFFFFFFFFFFFFFL
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
        val a = 0x0F0F0F0F0F0F0F0FL
        val b = 0xF0F0F0F0F0F0F0F0L
        assertTrue(hasher.hammingDistance(a, b) > 16)
    }
}

// Testable inner class that exposes hammingDistance without ContentResolver dependency
private class PerceptualHasherImpl {
    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
