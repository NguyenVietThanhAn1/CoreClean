package com.coreclean.app.data.datasource.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerceptualHasher @Inject constructor(private val contentResolver: ContentResolver) {

    suspend fun computeHash(uri: Uri): Long? = withContext(Dispatchers.Default) {
        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val resized = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
            val gray = toGrayscaleArray(resized)
            resized.recycle()
            bitmap.recycle()

            val dct = computeDct8x8(gray)
            val median = computeMedian(dct)

            var hash = 0L
            for (i in 0 until 64) {
                if (dct[i] > median) hash = hash or (1L shl i)
            }
            hash
        } catch (e: Exception) {
            null
        }
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun toGrayscaleArray(bitmap: Bitmap): DoubleArray {
        val array = DoubleArray(32 * 32)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                array[y * 32 + x] = 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        return array
    }

    private fun computeDct8x8(gray: DoubleArray): DoubleArray {
        // Extract top-left 8x8 from 32x32 grayscale via simplified DCT
        val result = DoubleArray(64)
        val n = 8
        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (x in 0 until 32) {
                    for (y in 0 until 32) {
                        sum += gray[y * 32 + x] *
                            Math.cos((2 * x + 1) * u * Math.PI / (2.0 * 32)) *
                            Math.cos((2 * y + 1) * v * Math.PI / (2.0 * 32))
                    }
                }
                val cu = if (u == 0) 1.0 / Math.sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / Math.sqrt(2.0) else 1.0
                result[u * n + v] = (2.0 / 32) * cu * cv * sum
            }
        }
        return result
    }

    private fun computeMedian(values: DoubleArray): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }
}
