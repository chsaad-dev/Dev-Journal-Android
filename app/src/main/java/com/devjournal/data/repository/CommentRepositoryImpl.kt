package com.devjournal.data.repository

import com.devjournal.data.model.Comment
import com.devjournal.domain.repository.CommentRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.devjournal.util.NetworkMonitor
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : CommentRepository {

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(postId: String, comment: Comment) {
        if (!networkMonitor.isOnline()) {
            throw IOException("You are currently offline. Connect to the internet to post comments.")
        }

        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        val commentData = mutableMapOf<String, Any?>(
            "userId" to comment.userId,
            "text" to comment.text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!comment.parentCommentId.isNullOrBlank()) {
            commentData["parentCommentId"] = comment.parentCommentId
        }
        if (!comment.replyToUsername.isNullOrBlank()) {
            commentData["replyToUsername"] = comment.replyToUsername
        }

        withTimeout(10000L) {
            firestore.runTransaction { transaction ->
                transaction.set(commentRef, commentData)
                transaction.update(postRef, "commentCount", FieldValue.increment(1))
            }.await()
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        if (!networkMonitor.isOnline()) {
            return Result.failure(IOException("You are currently offline. Connect to the internet to delete comments."))
        }
        return try {
            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)
            
            withTimeout(10000L) {
                firestore.runTransaction { transaction ->
                    transaction.delete(commentRef)
                    transaction.update(postRef, "commentCount", FieldValue.increment(-1))
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
