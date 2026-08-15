package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckIfBookmarkedUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(postId: String, uid: String): Flow<Boolean> {
        return postRepository.isPostBookmarked(postId, uid)
    }
}
