package com.coreclean.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Temporary store for selected image IDs when the list exceeds 200 entries. */
@Entity(tableName = "pending_review")
data class PendingReviewEntity(
    @PrimaryKey val imageId: Long
)
