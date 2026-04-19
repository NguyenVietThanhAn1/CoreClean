package com.coreclean.app.domain.model

data class MediaImage(
    val id: Long,
    val uri: android.net.Uri,
    val name: String,
    val path: String,
    val size: Long,          // bytes
    val dateAdded: Long,     // epoch seconds
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0
)

data class DuplicateGroup(
    val images: List<MediaImage>,  // nhóm ảnh trùng nhau
    val totalWastedSize: Long      // dung lượng có thể giải phóng
)