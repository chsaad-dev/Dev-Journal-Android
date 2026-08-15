package com.devjournal.domain.repository

import com.devjournal.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPublishedPosts(): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    fun isPostLiked(postId: String, uid: String): Flow<Boolean>
    suspend fun likePost(postId: String, uid: String)
    suspend fun unlikePost(postId: String, uid: String)
}
