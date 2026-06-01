package com.coreclean.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Frequency { DAILY, WEEKLY, MONTHLY }

@Serializable
data class ScheduleConfig(
    val enabled: Boolean = false,
    val frequency: Frequency = Frequency.WEEKLY,
    val hour: Int = 2,
    val minute: Int = 0,
    val categories: Set<JunkCategory> = setOf(
        JunkCategory.TEMP_FILES,
        JunkCategory.EMPTY_FOLDERS
    )
)
