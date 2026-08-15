package com.devjournal.domain.usecase

import com.devjournal.data.model.Comment
import com.devjournal.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val repository: CommentRepository
) {
    operator fun invoke(postId: String): Flow<List<Comment>> = repository.getComments(postId)
}
