package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> {
        return repository.deletePost(postId)
    }
}
