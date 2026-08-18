package com.devjournal.domain.repository

import com.devjournal.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPublishedPosts(limit: Int = 10): Flow<List<Post>>
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    fun isPostLiked(postId: String, uid: String): Flow<Boolean>
    fun getLikedPosts(uid: String): Flow<List<Post>>
    fun observeLikedPostIds(uid: String): Flow<Set<String>>
    
    fun isPostBookmarked(postId: String, uid: String): Flow<Boolean>
    fun getBookmarkedPosts(uid: String): Flow<List<Post>>
    fun observeBookmarkedPostIds(uid: String): Flow<Set<String>>
    
    suspend fun likePost(postId: String, uid: String)
    suspend fun unlikePost(postId: String, uid: String)
    suspend fun bookmarkPost(postId: String, uid: String)
    suspend fun unbookmarkPost(postId: String, uid: String)
    
    suspend fun createPost(post: Post): Result<String>
    suspend fun updatePost(postId: String, post: Post): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun getDraftById(postId: String): Post?
}
