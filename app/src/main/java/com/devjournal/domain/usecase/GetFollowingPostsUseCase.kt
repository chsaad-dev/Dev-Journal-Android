package com.devjournal.domain.usecase

import com.devjournal.data.model.Post
import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowingPostsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(uid: String, limit: Int = 10): Flow<List<Post>> =
        repository.getFollowingFeedPosts(uid, limit)
}
