package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import javax.inject.Inject

class LikePostUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: String, uid: String, alreadyLiked: Boolean) {
        if (alreadyLiked) {
            repository.unlikePost(postId, uid)
        } else {
            repository.likePost(postId, uid)
        }
    }
}
