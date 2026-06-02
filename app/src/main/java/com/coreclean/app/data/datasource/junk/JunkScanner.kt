package com.coreclean.app.data.datasource.junk

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import androidx.documentfile.provider.DocumentFile
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class JunkScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * @param safFolderUriStrings SAF tree URIs granted by the user via OpenDocumentTree.
     *   Used for EMPTY_FOLDERS and RESIDUAL_APK scanning. Pass an empty set to skip those.
     */
    suspend fun scan(safFolderUriStrings: Set<String> = emptySet()): List<JunkItem> =
        withContext(Dispatchers.IO) {
            buildList {
                addAll(scanAppCache())
                addAll(scanTempFiles())
                addAll(scanResidualApks(safFolderUriStrings))
                addAll(scanEmptyFolders(safFolderUriStrings))
            }
        }

    private fun scanAppCache(): List<JunkItem> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE)
            as? StorageStatsManager ?: return emptyList()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE)
            as? StorageManager ?: return emptyList()
        val uuid = try { storageManager.getUuidForPath(context.filesDir) } catch (_: Exception) {
            StorageManager.UUID_DEFAULT
        }
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { appInfo ->
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        uuid, appInfo.packageName, android.os.Process.myUserHandle()
                    )
                    if (stats.cacheBytes > 0) {
                        JunkItem(
                            path        = appInfo.packageName,
                            sizeBytes   = stats.cacheBytes,
                            category    = JunkCategory.APP_CACHE,
                            packageName = appInfo.packageName
                        )
                    } else null
                } catch (_: Exception) { null }
            }
    }

    private fun scanTempFiles(): List<JunkItem> {
        val dirs = listOf(
            context.cacheDir,
            context.externalCacheDir
        ).filterNotNull()

        val patterns = listOf(".tmp", ".log", ".thumbdata")
        return dirs.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && patterns.any { p -> it.name.endsWith(p, ignoreCase = true) } }
                .map { JunkItem(it.absolutePath, it.length(), JunkCategory.TEMP_FILES) }
        }
    }

    private fun scanResidualApks(safFolderUriStrings: Set<String>): List<JunkItem> {
        val result = mutableListOf<JunkItem>()

        // Scan app's own external cache — no broad storage permission needed.
        listOf(context.externalCacheDir).filterNotNull().filter { it.exists() }.forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
                .mapTo(result) { JunkItem(it.absolutePath, it.length(), JunkCategory.RESIDUAL_APK) }
        }

        // Also walk user-granted SAF trees for leftover APKs.
        safFolderUriStrings.forEach { uriString ->
            runCatching {
                val root = DocumentFile.fromTreeUri(context, Uri.parse(uriString)) ?: return@runCatching
                collectApkFiles(root, result)
            }
        }

        return result
    }

    private fun collectApkFiles(dir: DocumentFile, result: MutableList<JunkItem>) {
        for (child in dir.listFiles()) {
            when {
                child.isFile && child.name?.endsWith(".apk", ignoreCase = true) == true ->
                    result += JunkItem(child.uri.toString(), child.length(), JunkCategory.RESIDUAL_APK)
                child.isDirectory -> collectApkFiles(child, result)
            }
        }
    }

    /**
     * Scan SAF tree URIs for empty directories.
     * Returns an empty list when no SAF folders have been granted.
     */
    private fun scanEmptyFolders(safUriStrings: Set<String>): List<JunkItem> {
        return safUriStrings.flatMap { uriString ->
            runCatching {
                val uri  = Uri.parse(uriString)
                val root = DocumentFile.fromTreeUri(context, uri) ?: return@flatMap emptyList<JunkItem>()
                collectEmptyDirs(root)
            }.getOrElse { emptyList() }
        }
    }

    private fun collectEmptyDirs(dir: DocumentFile): List<JunkItem> {
        val children = dir.listFiles()
        if (children.isEmpty()) {
            return listOf(JunkItem(dir.uri.toString(), 0L, JunkCategory.EMPTY_FOLDERS))
        }
        return children.filter { it.isDirectory }.flatMap { collectEmptyDirs(it) }
    }

    suspend fun clean(items: List<JunkItem>): Int = withContext(Dispatchers.IO) {
        items.count { item ->
            try {
                if (item.category == JunkCategory.EMPTY_FOLDERS) {
                    DocumentFile.fromSingleUri(context, Uri.parse(item.path))?.delete() ?: false
                } else {
                    val file = File(item.path)
                    if (file.exists()) file.delete() else false
                }
            } catch (_: Exception) { false }
        }
    }
}
