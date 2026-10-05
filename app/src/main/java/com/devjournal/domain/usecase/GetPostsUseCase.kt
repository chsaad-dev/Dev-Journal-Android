package com.devjournal.domain.usecase

import com.devjournal.data.model.Post
import com.devjournal.data.model.PostPage
import com.devjournal.domain.repository.PostRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Post>> = repository.getPublishedPosts(limit)

    suspend fun getPage(
        pageSize: Int = 10,
        startAfter: DocumentSnapshot? = null,
        tag: String? = null
    ): Result<PostPage> = repository.getPublishedPostsPage(pageSize, startAfter, tag)
}
