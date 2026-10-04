package com.devjournal.domain.usecase

import com.devjournal.data.model.BugReport
import com.devjournal.domain.repository.BugReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserBugReportsUseCase @Inject constructor(
    private val bugReportRepository: BugReportRepository
) {
    operator fun invoke(userId: String): Flow<List<BugReport>> {
        return bugReportRepository.getUserBugReports(userId)
    }
}
