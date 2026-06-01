package com.coreclean.app.domain.usecase.junk

import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.data.datasource.junk.JunkScanner
import javax.inject.Inject

class ScanJunkUseCase @Inject constructor(
    private val scanner: JunkScanner
) {
    suspend operator fun invoke(): List<JunkItem> = scanner.scan()
}
