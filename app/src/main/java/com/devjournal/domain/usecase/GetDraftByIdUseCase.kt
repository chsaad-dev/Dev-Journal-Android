package com.devjournal.domain.usecase

import com.devjournal.data.model.local.DraftEntity
import com.devjournal.domain.repository.DraftRepository
import javax.inject.Inject

class GetDraftByIdUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    suspend operator fun invoke(id: String): DraftEntity? {
        return repository.getDraftById(id)
    }
}
