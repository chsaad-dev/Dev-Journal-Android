package com.devjournal.domain.usecase

import com.devjournal.data.model.local.DraftEntity
import com.devjournal.domain.repository.DraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    suspend operator fun invoke(draft: DraftEntity) {
        repository.saveDraft(draft)
    }
}
