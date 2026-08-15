package com.devjournal.domain.repository

import com.devjournal.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPublishedPosts(): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    suspend fun likePost(postId: String, uid: String)
    suspend fun unlikePost(postId: String, uid: String)
}
