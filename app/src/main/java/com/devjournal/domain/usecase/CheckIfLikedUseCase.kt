package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckIfLikedUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postId: String, uid: String): Flow<Boolean> =
        repository.isPostLiked(postId, uid)
}
