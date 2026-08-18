package com.devjournal.domain.usecase

import com.devjournal.domain.repository.DraftRepository
import javax.inject.Inject

class DeleteDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteDraft(id)
    }
}
