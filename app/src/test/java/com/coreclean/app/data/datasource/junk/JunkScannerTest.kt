package com.coreclean.app.data.datasource.junk

import com.coreclean.app.domain.model.JunkCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class JunkScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `clean deletes existing file and returns 1`() = runTest {
        val tmpFile = tempFolder.newFile("test.tmp")
        assertTrue(tmpFile.exists())

        val context = mockk<android.content.Context>(relaxed = true)
        val scanner = JunkScanner(context)

        val item = com.coreclean.app.domain.model.JunkItem(
            path      = tmpFile.absolutePath,
            sizeBytes = 100L,
            category  = JunkCategory.TEMP_FILES
        )
        val count = scanner.clean(listOf(item))
        assertEquals(1, count)
        assertTrue(!tmpFile.exists())
    }

    @Test
    fun `clean returns 0 for non-existent file`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val scanner = JunkScanner(context)

        val item = com.coreclean.app.domain.model.JunkItem(
            path      = tempFolder.root.absolutePath + "/nonexistent.tmp",
            sizeBytes = 0L,
            category  = JunkCategory.TEMP_FILES
        )
        val count = scanner.clean(listOf(item))
        assertEquals(0, count)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected.toLong(), actual.toLong())
    }
}
