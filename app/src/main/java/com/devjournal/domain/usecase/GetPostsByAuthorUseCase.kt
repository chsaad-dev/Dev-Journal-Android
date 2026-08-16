package com.devjournal.domain.usecase

import com.devjournal.data.model.Post
import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostsByAuthorUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(authorId: String): Flow<List<Post>> {
        return postRepository.getPostsByAuthor(authorId)
    }
}
