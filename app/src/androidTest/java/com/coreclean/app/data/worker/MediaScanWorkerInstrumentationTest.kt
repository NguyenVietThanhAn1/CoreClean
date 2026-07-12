package com.coreclean.app.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.ScanResultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for [MediaScanWorker].
 * Uses [TestListenableWorkerBuilder] with a custom [WorkerFactory] to inject test doubles —
 * no Hilt setup required for a unit-style instrumentation test.
 */
@RunWith(AndroidJUnit4::class)
class MediaScanWorkerInstrumentationTest {

    private lateinit var context: Context
    private lateinit var mediaRepository: MediaRepository
    private lateinit var scanResultRepository: ScanResultRepository

    @Before
    fun setUp() {
        context               = ApplicationProvider.getApplicationContext()
        mediaRepository       = mockk()
        scanResultRepository  = mockk(relaxed = true)
    }

    @Test
    fun doWork_withImages_returnsSuccessAndReplacesRows() = runBlocking {
        val fakeImages = listOf(
            fakeImage(1L, "photo1.jpg", 512_000L),
            fakeImage(2L, "photo2.jpg", 1_024_000L)
        )
        coEvery { mediaRepository.getAllImages() } returns flowOf(fakeImages)
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(FakeWorkerFactory(mediaRepository, scanResultRepository))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { scanResultRepository.replaceAll(match { it.size == 2 }) }
    }

    @Test
    fun doWork_withEmptyMedia_returnsSuccessAndReplacesWithEmpty() = runBlocking {
        coEvery { mediaRepository.getAllImages() } returns flowOf(emptyList())
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(FakeWorkerFactory(mediaRepository, scanResultRepository))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { scanResultRepository.replaceAll(emptyList()) }
    }

    @Test
    fun doWork_whenRepositoryThrows_returnsFailure() = runBlocking {
        coEvery { mediaRepository.getAllImages() } returns flowOf(listOf(fakeImage(1L, "photo1.jpg", 512_000L)))
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()
        coEvery { scanResultRepository.replaceAll(any()) } throws RuntimeException("db error")

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(FakeWorkerFactory(mediaRepository, scanResultRepository))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    private fun fakeImage(id: Long, name: String, size: Long) = MediaImage(
        id        = id,
        uri       = mockk(),
        name      = name,
        path      = "/storage/emulated/0/$name",
        size      = size,
        dateAdded = 0L,
        mimeType  = "image/jpeg"
    )

    private class FakeWorkerFactory(
        private val repo: MediaRepository,
        private val scanResultRepository: ScanResultRepository
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker = MediaScanWorker(appContext, workerParameters, repo, scanResultRepository)
    }
}
