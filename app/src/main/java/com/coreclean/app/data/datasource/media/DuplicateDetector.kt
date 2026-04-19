package com.coreclean.app.data.datasource.media

import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor() {

    /**
     * Phát hiện ảnh trùng lặp theo 2 bước:
     * 1. Group theo file size — ảnh khác size chắc chắn không trùng
     * 2. Trong cùng nhóm size, so sánh tên file (normalize)
     *
     * Đây là mức cơ bản — Sprint 2 sẽ bổ sung hash nội dung file
     */
    fun detect(images: List<MediaImage>): List<DuplicateGroup> {
        // Bước 1: Group theo size (bỏ qua ảnh size = 0)
        val bySize = images
            .filter { it.size > 0 }
            .groupBy { it.size }
            .filter { (_, group) -> group.size > 1 }  // chỉ giữ nhóm có >1 ảnh

        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((_, sameSize) in bySize) {
            // Bước 2: Trong cùng nhóm size, group tiếp theo tên đã normalize
            val byName = sameSize.groupBy { normalizeName(it.name) }

            for ((_, candidates) in byName) {
                if (candidates.size > 1) {
                    // Ảnh đầu tiên coi là "gốc", phần còn lại là duplicate
                    val wastedSize = candidates.drop(1).sumOf { it.size }
                    duplicateGroups += DuplicateGroup(
                        images           = candidates,
                        totalWastedSize  = wastedSize
                    )
                }
            }

            // Nếu cùng size nhưng tên khác — vẫn là candidate đáng ngờ
            // Đánh dấu để xử lý ở sprint sau bằng content hash
        }

        return duplicateGroups.sortedByDescending { it.totalWastedSize }
    }

    /** Bỏ số suffix kiểu "IMG_001(1).jpg" → "IMG_001.jpg" */
    private fun normalizeName(name: String): String {
        val ext = name.substringAfterLast('.', "")
        val base = name.substringBeforeLast('.')
        val cleaned = base.replace(Regex("\\s*\\(\\d+\\)$"), "").trim()
        return if (ext.isNotEmpty()) "$cleaned.$ext" else cleaned
    }
}