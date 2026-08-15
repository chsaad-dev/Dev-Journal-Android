package com.devjournal.domain.usecase

import com.devjournal.data.model.Post
import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLikedPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(uid: String): Flow<List<Post>> =
        postRepository.getLikedPosts(uid)
}
