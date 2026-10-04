package com.devjournal.domain.usecase

import android.net.Uri
import com.devjournal.data.model.BugReport
import com.devjournal.domain.repository.BugReportRepository
import javax.inject.Inject

class SubmitBugReportUseCase @Inject constructor(
    private val bugReportRepository: BugReportRepository
) {
    suspend operator fun invoke(report: BugReport, screenshotUri: Uri? = null): Result<Unit> {
        return bugReportRepository.submitBugReport(report, screenshotUri)
    }
}
