package com.coreclean.app.data.worker

import android.content.Context
import org.robolectric.RuntimeEnvironment
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.ScanResultEntity
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
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
    private lateinit var scanResultDao: ScanResultDao

    @Before
    fun setUp() {
        context          = RuntimeEnvironment.getApplication()
        mediaRepository  = mockk()
        scanResultDao    = mockk(relaxed = true)  // relaxed: suspend funs return defaults
    }

    @Test
    fun `doWork returns success and inserts rows into DB`() = runBlocking {
        val fakeImages = listOf(
            fakeImage(1L, "img1.jpg", 1024L),
            fakeImage(2L, "img2.jpg", 2048L)
        )
        coEvery { mediaRepository.getAllImages() } returns flowOf(fakeImages)
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList<DuplicateGroup>()

        val worker = TestListenableWorkerBuilder<MediaScanWorker>(context)
            .setWorkerFactory(
                FakeWorkerFactory(mediaRepository, scanResultDao)
            )
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.success(), result)

        // DB should have been cleared then populated with 2 entities
        coVerify(exactly = 1) { scanResultDao.clearAll() }
        coVerify(exactly = 1) { scanResultDao.insertAll(match { it.size == 2 }) }
    }

    private fun fakeImage(id: Long, name: String, size: Long) = MediaImage(
        id = id, uri = mockk(), name = name,
        path = "/storage/$name", size = size, dateAdded = 0L, mimeType = "image/jpeg"
    )
}

/** Minimal WorkerFactory that injects test doubles into [MediaScanWorker]. */
private class FakeWorkerFactory(
    private val repo: MediaRepository,
    private val dao:  ScanResultDao
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters
    ): androidx.work.ListenableWorker = MediaScanWorker(appContext, workerParameters, repo, dao)
}
