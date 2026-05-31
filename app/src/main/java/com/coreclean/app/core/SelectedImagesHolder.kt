package com.coreclean.app.core

import com.coreclean.app.domain.model.MediaImage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedImagesHolder @Inject constructor() {
    var images: List<MediaImage> = emptyList()
}
