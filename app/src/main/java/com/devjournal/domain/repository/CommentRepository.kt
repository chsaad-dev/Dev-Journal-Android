package com.devjournal.domain.repository

import com.devjournal.data.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getComments(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, comment: Comment)
}
