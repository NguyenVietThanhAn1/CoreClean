package com.coreclean.app.data.worker

import android.content.Context
import org.robolectric.RuntimeEnvironment
import androidx.work.ListenableWorker.Result
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class MediaScanWorkerTest {

    private lateinit var context: Context
    private lateinit var mediaRepository: MediaRepository
    private lateinit var scanResultRepository: ScanResultRepository

    @Before
    fun setUp() {
        context               = RuntimeEnvironment.getApplication()
        mediaRepository       = mockk()
        scanResultRepository  = mockk(relaxed = true)  // relaxed: suspend funs return defaults
    }

    @Test
    fun `doWork returns success and replaces rows in repository`() = runBlocking {
        val fakeImages = listOf(
            fakeImage(1L, "img1.jpg", 1024L),
            fakeImage(2L, "img2.jpg", 2048L)
        )
        coEvery { mediaRepository.getAllImages() } returns flowOf(fakeImages)
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(
                FakeWorkerFactory(mediaRepository, scanResultRepository)
            )
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.success(), result)

        coVerify(exactly = 1) { scanResultRepository.replaceAll(match { it.size == 2 }) }
    }

    @Test
    fun `doWork returns failure when repository throws`() = runBlocking {
        val fakeImages = listOf(fakeImage(1L, "img1.jpg", 1024L))
        coEvery { mediaRepository.getAllImages() } returns flowOf(fakeImages)
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()
        coEvery { scanResultRepository.replaceAll(any()) } throws RuntimeException("db error")

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(
                FakeWorkerFactory(mediaRepository, scanResultRepository)
            )
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when mediaRepository throws`() = runBlocking {
        coEvery { mediaRepository.getAllImages() } throws RuntimeException("query failed")

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(
                FakeWorkerFactory(mediaRepository, scanResultRepository)
            )
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.failure(), result)
        coVerify(exactly = 0) { scanResultRepository.replaceAll(any()) }
    }

    private fun fakeImage(id: Long, name: String, size: Long) = MediaImage(
        id = id, uri = mockk(), name = name,
        path = "/storage/$name", size = size, dateAdded = 0L, mimeType = "image/jpeg"
    )
}

/** Minimal WorkerFactory that injects test doubles into [MediaScanWorker]. */
private class FakeWorkerFactory(
    private val repo: MediaRepository,
    private val scanResultRepository: ScanResultRepository
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters
    ): androidx.work.ListenableWorker = MediaScanWorker(appContext, workerParameters, repo, scanResultRepository)
}
