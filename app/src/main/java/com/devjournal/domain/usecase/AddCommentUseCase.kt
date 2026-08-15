package com.devjournal.domain.usecase

import com.devjournal.data.model.Comment
import com.devjournal.domain.repository.CommentRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val repository: CommentRepository
) {
    suspend operator fun invoke(postId: String, comment: Comment) {
        repository.addComment(postId, comment)
    }
}
