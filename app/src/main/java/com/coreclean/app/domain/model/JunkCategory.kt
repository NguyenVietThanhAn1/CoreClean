package com.coreclean.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class JunkCategory {
    APP_CACHE,
    EMPTY_FOLDERS,
    TEMP_FILES,
    RESIDUAL_APK
}
