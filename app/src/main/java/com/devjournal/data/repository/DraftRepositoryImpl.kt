package com.devjournal.data.repository

import com.devjournal.data.model.local.DraftDao
import com.devjournal.data.model.local.DraftEntity
import com.devjournal.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao
) : DraftRepository {
    override fun getAllDrafts(): Flow<List<DraftEntity>> {
        return draftDao.getAllDrafts()
    }

    override suspend fun getDraftById(id: String): DraftEntity? {
        return draftDao.getDraftById(id)
    }

    override suspend fun saveDraft(draft: DraftEntity) {
        draftDao.insertOrUpdateDraft(draft)
    }

    override suspend fun deleteDraft(id: String) {
        draftDao.deleteDraftById(id)
    }
}
