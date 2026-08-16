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
                    trySend(emptyList())
                    close()
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
                    trySend(null)
                    close()
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(Post::class.java)
                trySend(post)
            }
        awaitClose { listener.remove() }
    }

    override fun isPostLiked(postId: String, uid: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("likedPosts")
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    close()
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override fun getLikedPosts(uid: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("likedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                firestore.collection("posts")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), postIds.take(30))
                    .get()
                    .addOnSuccessListener { postsSnapshot ->
                        val posts = postsSnapshot.toObjects(Post::class.java)
                        trySend(posts)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                        close()
                    }
            }
        awaitClose { listener.remove() }
    }

    override fun observeLikedPostIds(uid: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("likedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    close()
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    override fun isPostBookmarked(postId: String, uid: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    close()
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookmarkedPosts(uid: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                firestore.collection("posts")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), postIds.take(30))
                    .get()
                    .addOnSuccessListener { postsSnapshot ->
                        val posts = postsSnapshot.toObjects(Post::class.java)
                        trySend(posts)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                        close()
                    }
            }
        awaitClose { listener.remove() }
    }

    override fun observeBookmarkedPostIds(uid: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    close()
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
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

    override suspend fun bookmarkPost(postId: String, uid: String) {
        val bookmarkRef = firestore.collection("users").document(uid)
            .collection("bookmarks").document(postId)
        bookmarkRef.set(mapOf("bookmarkedAt" to FieldValue.serverTimestamp())).await()
    }

    override suspend fun unbookmarkPost(postId: String, uid: String) {
        val bookmarkRef = firestore.collection("users").document(uid)
            .collection("bookmarks").document(postId)
        bookmarkRef.delete().await()
    }

    override suspend fun createPost(post: Post): Result<String> = try {
        val newDoc = firestore.collection("posts").document()
        val slug = generateSlug(post.title)
        val words = post.content.split(Regex("\\s+")).count { it.isNotBlank() }
        val readTime = maxOf(1, words / 200)

        val postMap = hashMapOf(
            "title" to post.title,
            "slug" to slug,
            "content" to post.content,
            "excerpt" to post.excerpt,
            "coverImageUrl" to post.coverImageUrl,
            "coverImagePublicId" to post.coverImagePublicId,
            "authorId" to post.authorId,
            "tags" to post.tags,
            "published" to post.published,
            "readTimeMinutes" to readTime,
            "likeCount" to 0,
            "commentCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        newDoc.set(postMap).await()
        Result.success(newDoc.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePost(postId: String, post: Post): Result<Unit> = try {
        val docRef = firestore.collection("posts").document(postId)
        val slug = generateSlug(post.title)
        val words = post.content.split(Regex("\\s+")).count { it.isNotBlank() }
        val readTime = maxOf(1, words / 200)

        val updates = hashMapOf<String, Any>(
            "title" to post.title,
            "slug" to slug,
            "content" to post.content,
            "excerpt" to post.excerpt,
            "coverImageUrl" to post.coverImageUrl,
            "coverImagePublicId" to post.coverImagePublicId,
            "tags" to post.tags,
            "published" to post.published,
            "readTimeMinutes" to readTime,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = try {
        firestore.collection("posts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getDraftById(postId: String): Post? = try {
        val snapshot = firestore.collection("posts").document(postId).get().await()
        snapshot.toObject(Post::class.java)
    } catch (e: Exception) {
        null
    }

    private fun generateSlug(title: String): String {
        val normalized = title.lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
        return normalized.ifBlank { "post-${System.currentTimeMillis()}" }
    }
}
