package com.devjournal.domain.repository

import com.devjournal.data.model.local.DraftEntity
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    fun getAllDrafts(): Flow<List<DraftEntity>>
    suspend fun getDraftById(id: String): DraftEntity?
    suspend fun saveDraft(draft: DraftEntity)
    suspend fun deleteDraft(id: String)
}
