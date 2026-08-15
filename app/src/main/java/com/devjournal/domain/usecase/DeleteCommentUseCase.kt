package com.devjournal.domain.usecase

import com.devjournal.domain.repository.CommentRepository
import javax.inject.Inject

class DeleteCommentUseCase @Inject constructor(
    private val repository: CommentRepository
) {
    suspend operator fun invoke(postId: String, commentId: String): Result<Unit> {
        return repository.deleteComment(postId, commentId)
    }
}
