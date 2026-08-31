package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import javax.inject.Inject

class RecordViewUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: String, uid: String) {
        repository.recordView(postId, uid)
    }
}
