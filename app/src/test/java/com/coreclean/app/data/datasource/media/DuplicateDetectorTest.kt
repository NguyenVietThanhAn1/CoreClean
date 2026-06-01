package com.coreclean.app.data.datasource.media

import android.content.ContentResolver
import android.net.Uri
import com.coreclean.app.domain.model.MediaImage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class DuplicateDetectorTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var detector: DuplicateDetector

    @Before
    fun setUp() {
        contentResolver = mockk()
        detector = DuplicateDetector(contentResolver)
    }

    // (a) Images with different sizes → 0 groups
    @Test
    fun `different size images produce no duplicate groups`() = runTest {
        val images = listOf(
            fakeImage(id = 1L, size = 100L),
            fakeImage(id = 2L, size = 200L),
            fakeImage(id = 3L, size = 300L)
        )
        val groups = detector.detect(images)
        assertEquals(0, groups.size)
    }

    // (b) Same size same name → 1 group (fast mode)
    @Test
    fun `same size same name produces 1 group in fast mode`() = runTest {
        val images = listOf(
            fakeImage(id = 1L, name = "photo.jpg", size = 512L),
            fakeImage(id = 2L, name = "photo.jpg", size = 512L)
        )
        val groups = detector.detect(images, fast = true)
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].images.size)
    }

    // (c) Same size, different name, same content hash → 1 group
    @Test
    fun `same size different name same hash produces 1 group`() = runTest {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val sharedContent = ByteArray(1024) { it.toByte() }

        every { contentResolver.openInputStream(uri1) } returns ByteArrayInputStream(sharedContent)
        every { contentResolver.openInputStream(uri2) } returns ByteArrayInputStream(sharedContent)

        val images = listOf(
            fakeImage(id = 1L, name = "alpha.jpg", size = 1024L, uri = uri1),
            fakeImage(id = 2L, name = "beta.jpg",  size = 1024L, uri = uri2)
        )
        val groups = detector.detect(images, fast = false)
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].images.size)
    }

    private fun fakeImage(
        id: Long = 0L,
        name: String = "image.jpg",
        size: Long = 100L,
        uri: Uri = mockk()
    ) = MediaImage(
        id        = id,
        uri       = uri,
        name      = name,
        path      = "/storage/$name",
        size      = size,
        dateAdded = 0L,
        mimeType  = "image/jpeg"
    )
}
