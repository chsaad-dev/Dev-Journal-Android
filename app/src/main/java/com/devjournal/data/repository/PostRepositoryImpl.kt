package com.devjournal.data.repository

import com.devjournal.data.model.Post
import com.devjournal.domain.repository.PostRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PostRepository {

    override fun getPublishedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("published", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getPostById(postId: String): Flow<Post?> = callbackFlow {
        val listener = firestore.collection("posts")
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(Post::class.java)
                trySend(post)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun likePost(postId: String, uid: String) {
        val postRef = firestore.collection("posts").document(postId)
        val likeRef = firestore.collection("users").document(uid)
            .collection("likedPosts").document(postId)

        firestore.runTransaction { transaction ->
            val likedDoc = transaction.get(likeRef)
            if (!likedDoc.exists()) {
                transaction.set(likeRef, mapOf("likedAt" to FieldValue.serverTimestamp()))
                transaction.update(postRef, "likeCount", FieldValue.increment(1))
            }
        }.await()
    }

    override suspend fun unlikePost(postId: String, uid: String) {
        val postRef = firestore.collection("posts").document(postId)
        val likeRef = firestore.collection("users").document(uid)
            .collection("likedPosts").document(postId)

        firestore.runTransaction { transaction ->
            val likedDoc = transaction.get(likeRef)
            if (likedDoc.exists()) {
                transaction.delete(likeRef)
                transaction.update(postRef, "likeCount", FieldValue.increment(-1))
            }
        }.await()
    }
}
