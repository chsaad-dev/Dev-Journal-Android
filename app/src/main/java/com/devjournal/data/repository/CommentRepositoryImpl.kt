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
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommentRepository {

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(postId: String, comment: Comment) {
        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        val commentData = mapOf(
            "userId" to comment.userId,
            "text" to comment.text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.runTransaction { transaction ->
            transaction.set(commentRef, commentData)
            transaction.update(postRef, "commentCount", FieldValue.increment(1))
        }.await()
    }
}
