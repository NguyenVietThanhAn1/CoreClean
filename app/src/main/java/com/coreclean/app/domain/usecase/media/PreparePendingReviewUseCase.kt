package com.coreclean.app.domain.usecase.media

import com.coreclean.app.domain.repository.MediaRepository
import javax.inject.Inject

private const val ROUTE_ID_LIMIT = 200

/**
 * Decides how a set of selected image IDs should be carried into the review route.
 * If [ids] fits within [limit] it's returned as-is to embed inline in the route.
 * Otherwise the IDs are persisted via [MediaRepository] and an empty list is returned,
 * signaling the caller to read the selection back from the DB instead.
 */
class PreparePendingReviewUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(ids: List<Long>, limit: Int = ROUTE_ID_LIMIT): List<Long> {
        if (ids.size <= limit) return ids
        mediaRepository.savePendingReviewIds(ids)
        return emptyList()
    }
}
