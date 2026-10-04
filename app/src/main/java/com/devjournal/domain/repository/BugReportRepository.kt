package com.devjournal.domain.repository

import android.net.Uri
import com.devjournal.data.model.BugReport
import kotlinx.coroutines.flow.Flow

interface BugReportRepository {
    suspend fun submitBugReport(report: BugReport, screenshotUri: Uri?): Result<Unit>
    fun getUserBugReports(userId: String): Flow<List<BugReport>>
}
