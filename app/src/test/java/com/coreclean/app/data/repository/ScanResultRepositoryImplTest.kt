package com.coreclean.app.data.repository

import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.domain.model.ScanResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScanResultRepositoryImplTest {

    private lateinit var dao: ScanResultDao
    private lateinit var repository: ScanResultRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk()
        repository = ScanResultRepositoryImpl(dao)
    }

    @Test
    fun `replaceAll clears then inserts mapped entities in order`() = runTest {
        coEvery { dao.clearAll() } returns Unit
        coEvery { dao.insertAll(any()) } returns Unit

        val results = listOf(
            ScanResult(filePath = "/a.jpg", fileSize = 100L, fileType = "image/jpeg", lastModified = 10L),
            ScanResult(filePath = "/b.jpg", fileSize = 200L, fileType = "image/jpeg", lastModified = 20L)
        )
        repository.replaceAll(results)

        coVerifyOrder {
            dao.clearAll()
            dao.insertAll(match { entities ->
                entities.size == 2 &&
                    entities[0].filePath == "/a.jpg" && entities[0].fileSize == 100L &&
                    entities[1].filePath == "/b.jpg" && entities[1].fileSize == 200L
            })
        }
    }

    @Test
    fun `replaceAll with empty list still clears and inserts empty`() = runTest {
        coEvery { dao.clearAll() } returns Unit
        coEvery { dao.insertAll(any()) } returns Unit

        repository.replaceAll(emptyList())

        coVerify(exactly = 1) { dao.clearAll() }
        coVerify(exactly = 1) { dao.insertAll(match { it.isEmpty() }) }
    }

    @Test
    fun `getCount delegates to dao`() = runTest {
        coEvery { dao.count() } returns 5

        assertEquals(5, repository.getCount())
    }

    @Test
    fun `clearAll delegates to dao`() = runTest {
        coEvery { dao.clearAll() } returns Unit

        repository.clearAll()

        coVerify(exactly = 1) { dao.clearAll() }
    }
}
