package com.devjournal.data.repository

import com.devjournal.data.model.Post
import com.devjournal.data.model.PostPage
import com.devjournal.data.model.PostViewer
import com.devjournal.data.model.local.BookmarkedPostDao
import com.devjournal.data.model.local.toBookmarkedPostEntity
import com.devjournal.data.model.local.toPost
import com.devjournal.domain.repository.PostRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bookmarkedPostDao: BookmarkedPostDao
) : PostRepository {

    override suspend fun getPublishedPostsPage(
        pageSize: Int,
        startAfter: DocumentSnapshot?,
        tag: String?
    ): Result<PostPage> = runCatching {
        var baseQuery: Query = firestore.collection("posts")
            .whereEqualTo("published", true)

        val cleanTag = tag?.trim()?.removePrefix("#")
        if (!cleanTag.isNullOrBlank() && !cleanTag.equals("All", ignoreCase = true)) {
            baseQuery = baseQuery.whereArrayContains("tags", cleanTag)
        }

        var query = baseQuery
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit((pageSize + 1).toLong())

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val snapshot = query.get().await()
        val docs = snapshot.documents
        val hasMore = docs.size > pageSize
        val pageDocs = if (hasMore) docs.take(pageSize) else docs
        val posts = pageDocs.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
        val lastDoc = pageDocs.lastOrNull()

        PostPage(
            posts = posts,
            lastVisibleDocument = lastDoc,
            hasMore = hasMore
        )
    }

    override fun getPublishedPosts(limit: Int): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("published", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
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

    override fun getPostsByAuthor(authorId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java)
                    ?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getPostById(postId: String): Flow<Post?> = callbackFlow {
        // Emit cached post immediately if available for offline reading
        val offlineJob = launch {
            val cached = bookmarkedPostDao.getPostById(postId)?.toPost()
            if (cached != null) {
                trySend(cached)
            }
        }

        val listener = firestore.collection("posts")
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    launch {
                        val cached = bookmarkedPostDao.getPostById(postId)?.toPost()
                        trySend(cached)
                    }
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(Post::class.java)
                if (post != null) {
                    trySend(post)
                } else {
                    launch {
                        val cached = bookmarkedPostDao.getPostById(postId)?.toPost()
                        trySend(cached)
                    }
                }
            }
        awaitClose { 
            listener.remove()
            offlineJob.cancel()
        }
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
        // Emit offline cached bookmarks first so users can read instantly offline
        val offlineJob = launch {
            bookmarkedPostDao.getAllBookmarkedPosts().collect { entities ->
                if (entities.isNotEmpty()) {
                    trySend(entities.map { it.toPost() })
                }
            }
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    launch { bookmarkedPostDao.clearAll() }
                    return@addSnapshotListener
                }
                firestore.collection("posts")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), postIds.take(30))
                    .get()
                    .addOnSuccessListener { postsSnapshot ->
                        val posts = postsSnapshot.toObjects(Post::class.java)
                        trySend(posts)
                        // Sync to local Room database for offline reading
                        launch {
                            posts.forEach { post ->
                                bookmarkedPostDao.insert(post.toBookmarkedPostEntity())
                            }
                        }
                    }
            }
        awaitClose { 
            listener.remove()
            offlineJob.cancel()
        }
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
        try {
            val postDoc = firestore.collection("posts").document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
            if (post != null) {
                bookmarkedPostDao.insert(post.toBookmarkedPostEntity())
            }
        } catch (_: Exception) {
            // Best-effort offline caching
        }
    }

    override suspend fun unbookmarkPost(postId: String, uid: String) {
        val bookmarkRef = firestore.collection("users").document(uid)
            .collection("bookmarks").document(postId)
        bookmarkRef.delete().await()
        try {
            bookmarkedPostDao.deleteById(postId)
        } catch (_: Exception) {
            // Best-effort
        }
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
            "viewCount" to 0,
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

    override suspend fun recordView(postId: String, uid: String, userName: String?, userPhotoUrl: String?) {
        val postRef = firestore.collection("posts").document(postId)
        val viewRef = postRef.collection("views").document(uid)

        firestore.runTransaction { transaction ->
            val viewDoc = transaction.get(viewRef)
            if (!viewDoc.exists()) {
                val viewData = mutableMapOf<String, Any>(
                    "viewedAt" to FieldValue.serverTimestamp()
                )
                if (!userName.isNullOrBlank()) {
                    viewData["userName"] = userName
                }
                if (!userPhotoUrl.isNullOrBlank()) {
                    viewData["userPhotoUrl"] = userPhotoUrl
                }
                transaction.set(viewRef, viewData)
                transaction.update(postRef, "viewCount", FieldValue.increment(1))
            }
        }.await()
    }

    override fun getPostViewers(postId: String): Flow<List<PostViewer>> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .collection("views")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val viewers = snapshot?.documents?.mapNotNull { doc ->
                    val viewer = doc.toObject(PostViewer::class.java) ?: return@mapNotNull null
                    viewer.uid = doc.id
                    viewer
                }?.sortedByDescending { it.viewedAt?.toDate()?.time ?: 0L } ?: emptyList()
                trySend(viewers)
            }
        awaitClose { listener.remove() }
    }

    override fun getDraftsByAuthor(authorId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("published", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java)
                    ?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowingFeedPosts(uid: String, limit: Int): Flow<List<Post>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val postListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val chunkMap = java.util.concurrent.ConcurrentHashMap<Int, List<Post>>()

        val followingListener = firestore.collection("users").document(uid)
            .collection("following")
            .addSnapshotListener { followingSnapshot, followingError ->
                if (followingError != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                postListeners.forEach { it.remove() }
                postListeners.clear()
                chunkMap.clear()

                val followedUids = followingSnapshot?.documents?.mapNotNull { it.id } ?: emptyList()
                if (followedUids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chunks = followedUids.chunked(30)
                chunks.forEachIndexed { index, chunk ->
                    val listener = firestore.collection("posts")
                        .whereEqualTo("published", true)
                        .whereIn("authorId", chunk)
                        .limit(limit.toLong())
                        .addSnapshotListener { postSnapshot, postError ->
                            if (postError == null) {
                                val posts = postSnapshot?.toObjects(Post::class.java) ?: emptyList()
                                chunkMap[index] = posts

                                val allCombined = chunkMap.values.flatten()
                                    .distinctBy { it.id }
                                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                                    .take(limit)
                                trySend(allCombined)
                            }
                        }
                    postListeners.add(listener)
                }
            }

        awaitClose {
            followingListener.remove()
            postListeners.forEach { it.remove() }
        }
    }

    private fun generateSlug(title: String): String {
        val normalized = title.lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
        return normalized.ifBlank { "post-${System.currentTimeMillis()}" }
    }
}
