package com.devjournal.domain.usecase

import com.devjournal.data.model.local.DraftEntity
import com.devjournal.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDraftsUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    operator fun invoke(): Flow<List<DraftEntity>> {
        return repository.getAllDrafts()
    }
}
