package com.coreclean.app.domain.usecase.junk

import com.coreclean.app.data.datasource.junk.JunkScanner
import com.coreclean.app.domain.model.JunkItem
import javax.inject.Inject

class ScanJunkUseCase @Inject constructor(
    private val scanner: JunkScanner
) {
    suspend operator fun invoke(
        safFolderUriStrings: Set<String> = emptySet()
    ): List<JunkItem> = scanner.scan(safFolderUriStrings)
}
