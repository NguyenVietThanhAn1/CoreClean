package com.coreclean.app.data.datasource.media

import android.content.ContentResolver
import android.net.Uri
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor(
    private val contentResolver: ContentResolver,
    private val perceptualHasher: PerceptualHasher
) {
    /**
     * Detects duplicate images.
     *
     * @param fast When true, uses only size + normalized name comparison (fast but less accurate).
     *             When false (default), uses MD5 of first 256 KB for confirmation (slower but detects
     *             same-content files with different names).
     * @param perceptual When true, uses perceptual hashing (DCT-based pHash) to detect visually
     *                   similar images regardless of file size or name. Slower but more accurate for
     *                   near-duplicate photos (e.g., burst shots, slight edits).
     */
    suspend fun detect(images: List<MediaImage>, fast: Boolean = false, perceptual: Boolean = false): List<DuplicateGroup> {
        if (perceptual) return detectByPerceptualHash(images)

        val bySize = images
            .filter { it.size > 0 }
            .groupBy { it.size }
            .filter { (_, group) -> group.size > 1 }

        return if (fast) detectByName(bySize) else detectByHash(bySize)
    }

    private fun detectByName(bySize: Map<Long, List<MediaImage>>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        for ((_, sameSize) in bySize) {
            sameSize.groupBy { normalizeName(it.name) }.values
                .filter { it.size > 1 }
                .mapTo(groups) { candidates ->
                    DuplicateGroup(candidates, candidates.drop(1).sumOf { it.size })
                }
        }
        return groups.sortedByDescending { it.totalWastedSize }
    }

    private suspend fun detectByHash(bySize: Map<Long, List<MediaImage>>): List<DuplicateGroup> {
        val parallelism = Runtime.getRuntime().availableProcessors()
        val semaphore = Semaphore(parallelism)

        return coroutineScope {
            val groups = mutableListOf<DuplicateGroup>()
            for ((_, sameSize) in bySize) {
                val withHash = sameSize.map { image ->
                    async(Dispatchers.Default) {
                        semaphore.withPermit { image to computePartialHash(image.uri) }
                    }
                }.awaitAll()

                withHash.groupBy { (_, hash) -> hash }
                    .values
                    .filter { pairs -> pairs.size > 1 && pairs.first().second.isNotEmpty() }
                    .mapTo(groups) { pairs ->
                        val candidates = pairs.map { it.first }
                        DuplicateGroup(candidates, candidates.drop(1).sumOf { it.size })
                    }
            }
            groups.sortedByDescending { it.totalWastedSize }
        }
    }

    private fun computePartialHash(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(256 * 1024)
                val bytesRead = stream.read(buffer)
                if (bytesRead <= 0) return ""
                val digest = MessageDigest.getInstance("MD5")
                digest.update(buffer, 0, bytesRead)
                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun detectByPerceptualHash(images: List<MediaImage>): List<DuplicateGroup> {
        // Compute pHash for each image
        val hashMap = mutableMapOf<Long, Long>() // imageId -> hash
        for (image in images) {
            val hash = perceptualHasher.computeHash(image.uri) ?: continue
            hashMap[image.id] = hash
        }

        // Union-Find style grouping: group images with hammingDistance <= 8
        val groups = mutableListOf<DuplicateGroup>()
        val assigned = mutableSetOf<Long>()

        for (image in images) {
            val hash = hashMap[image.id] ?: continue
            if (image.id in assigned) continue

            val group = mutableListOf(image)
            assigned.add(image.id)

            for (other in images) {
                if (other.id == image.id || other.id in assigned) continue
                val otherHash = hashMap[other.id] ?: continue
                if (perceptualHasher.hammingDistance(hash, otherHash) <= 8) {
                    group.add(other)
                    assigned.add(other.id)
                }
            }

            if (group.size >= 2) {
                groups.add(DuplicateGroup(group, group.drop(1).sumOf { it.size }))
            }
        }

        return groups.sortedByDescending { it.totalWastedSize }
    }

    private fun normalizeName(name: String): String {
        val ext = name.substringAfterLast('.', "")
        val base = name.substringBeforeLast('.')
        val cleaned = base.replace(Regex("\\s*\\(\\d+\\)$"), "").trim()
        return if (ext.isNotEmpty()) "$cleaned.$ext" else cleaned
    }
}
