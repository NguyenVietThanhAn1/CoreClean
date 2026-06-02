package com.coreclean.app.data.datasource.junk

import androidx.documentfile.provider.DocumentFile
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
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

    @Test
    fun `clean deletes EMPTY_FOLDERS item via DocumentFile SAF`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val docFile = mockk<DocumentFile>()
        every { docFile.delete() } returns true

        mockkStatic(DocumentFile::class)
        every { DocumentFile.fromSingleUri(context, any()) } returns docFile

        val scanner = JunkScanner(context)
        val item = JunkItem(
            path      = "content://com.android.externalstorage.documents/tree/primary%3AEmptyDir",
            sizeBytes = 0L,
            category  = JunkCategory.EMPTY_FOLDERS
        )
        val count = scanner.clean(listOf(item))
        assertEquals(1, count)
    }

    @Test
    fun `clean counts 0 when DocumentFile delete returns false for EMPTY_FOLDERS`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val docFile = mockk<DocumentFile>()
        every { docFile.delete() } returns false

        mockkStatic(DocumentFile::class)
        every { DocumentFile.fromSingleUri(context, any()) } returns docFile

        val scanner = JunkScanner(context)
        val item = JunkItem(
            path      = "content://com.android.externalstorage.documents/tree/primary%3AEmptyDir",
            sizeBytes = 0L,
            category  = JunkCategory.EMPTY_FOLDERS
        )
        val count = scanner.clean(listOf(item))
        assertEquals(0, count)
    }

    @Test
    fun `scan SAF tree containing apk files picks them up as RESIDUAL_APK`() = runTest {
        val treeUriStr = "content://com.android.externalstorage.documents/tree/primary%3ADownloads"

        val apkFile = mockk<DocumentFile>()
        every { apkFile.isFile }      returns true
        every { apkFile.isDirectory } returns false
        every { apkFile.name }        returns "app.apk"
        every { apkFile.length() }    returns 50_000L
        every { apkFile.uri }         returns android.net.Uri.parse("$treeUriStr/app.apk")

        val rootDir = mockk<DocumentFile>()
        every { rootDir.isDirectory } returns true
        every { rootDir.listFiles() } returns arrayOf(apkFile)

        val context = mockk<android.content.Context>(relaxed = true)
        mockkStatic(DocumentFile::class)
        every { DocumentFile.fromTreeUri(context, any()) } returns rootDir

        val scanner = JunkScanner(context)
        val result = scanner.scan(safFolderUriStrings = setOf(treeUriStr))

        assertTrue("Expected RESIDUAL_APK item from SAF tree",
            result.any { it.category == JunkCategory.RESIDUAL_APK && it.sizeBytes == 50_000L })
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected.toLong(), actual.toLong())
    }
}
