package com.devjournal.domain.usecase

import com.devjournal.data.model.Post
import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostDetailUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postId: String): Flow<Post?> = repository.getPostById(postId)
}
