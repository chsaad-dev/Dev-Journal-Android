package com.devjournal.presentation.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.GetPostsUseCase
import com.devjournal.domain.usecase.GetFollowingPostsUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.BookmarkPostUseCase
import com.devjournal.domain.usecase.DeletePostUseCase
import com.devjournal.domain.usecase.ObserveLikedPostIdsUseCase
import com.devjournal.domain.usecase.ObserveBookmarkedPostIdsUseCase
import com.devjournal.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.google.firebase.firestore.DocumentSnapshot
import javax.inject.Inject

enum class FeedSortOption {
    LATEST,
    TRENDING
}

enum class FeedTab {
    FOR_YOU,
    TRENDING,
    FOLLOWING
}

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val allPosts: List<Post> = emptyList(),
    val followingPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowingLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val selectedTag: String = "All",
    val availableTags: List<String> = listOf("All", "Android", "Compose", "Kotlin", "Architecture", "Firebase"),
    val isAdmin: Boolean = false,
    val currentUserId: String? = null,
    val currentUserPhotoUrl: String? = null,
    val likedPostIds: Set<String> = emptySet(),
    val bookmarkedPostIds: Set<String> = emptySet(),
    val selectedTab: FeedTab = FeedTab.FOR_YOU,
    val selectedSortOption: FeedSortOption = FeedSortOption.LATEST,
    val authorNames: Map<String, String> = emptyMap(),
    val authorPhotoUrls: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val lastVisibleDocument: DocumentSnapshot? = null,
    val hasMorePosts: Boolean = true,
    val isLoadingNextPage: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getFollowingPostsUseCase: GetFollowingPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val observeLikedPostIdsUseCase: ObserveLikedPostIdsUseCase,
    private val observeBookmarkedPostIdsUseCase: ObserveBookmarkedPostIdsUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val networkMonitor: NetworkMonitor,
    private val notifyWorkerApi: NotifyWorkerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var interactionsJob: Job? = null
    private var postsJob: Job? = null
    private var followingPostsJob: Job? = null
    
    init {
        observeNetworkState()
        observeCurrentUser()
        fetchInitialPosts("All")
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user != null) {
                    val profile = getUserProfileUseCase(user.uid)
                    _uiState.update {
                        it.copy(
                            currentUserId = user.uid,
                            currentUserPhotoUrl = profile?.photoUrl ?: user.photoUrl?.toString(),
                            isAdmin = profile?.role.equals("admin", ignoreCase = true)
                        )
                    }
                    observeInteractions(user.uid)
                    observeFollowingPosts(user.uid, 20)
                } else {
                    interactionsJob?.cancel()
                    followingPostsJob?.cancel()
                    _uiState.update {
                        it.copy(
                            currentUserId = null,
                            currentUserPhotoUrl = null,
                            isAdmin = false,
                            likedPostIds = emptySet(),
                            bookmarkedPostIds = emptySet(),
                            followingPosts = emptyList(),
                            isFollowingLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun observeInteractions(uid: String) {
        interactionsJob?.cancel()
        interactionsJob = viewModelScope.launch {
            launch {
                observeLikedPostIdsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { likedIds ->
                        _uiState.update { it.copy(likedPostIds = likedIds) }
                    }
            }
            launch {
                observeBookmarkedPostIdsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { bookmarkedIds ->
                        _uiState.update { it.copy(bookmarkedPostIds = bookmarkedIds) }
                    }
            }
        }
    }

    fun fetchInitialPosts(tag: String = _uiState.value.selectedTag) {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedTag = tag,
                    lastVisibleDocument = null,
                    hasMorePosts = true
                )
            }
            val result = getPostsUseCase.getPage(
                pageSize = 10,
                startAfter = null,
                tag = tag
            )
            result.onSuccess { page ->
                // Resolve authors ONLY for new page
                resolveMissingAuthors(page.posts)

                _uiState.update { state ->
                    val displayPosts = if (state.selectedTab == FeedTab.TRENDING) {
                        page.posts.sortedByDescending { it.likeCount + it.commentCount }
                    } else {
                        page.posts
                    }
                    state.copy(
                        posts = if (state.selectedTab != FeedTab.FOLLOWING) displayPosts else state.posts,
                        allPosts = page.posts,
                        lastVisibleDocument = page.lastVisibleDocument,
                        hasMorePosts = page.hasMore,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("DevJournal", "Failed to fetch posts page", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Failed to load posts: ${error.message}"
                    )
                }
            }
        }
    }

    private fun observeFollowingPosts(uid: String, limit: Int) {
        followingPostsJob?.cancel()
        followingPostsJob = viewModelScope.launch {
            getFollowingPostsUseCase(uid, limit).collect { postsList ->
                resolveMissingAuthors(postsList)
                _uiState.update { state ->
                    state.copy(
                        followingPosts = postsList,
                        isFollowingLoading = false,
                        posts = if (state.selectedTab == FeedTab.FOLLOWING) postsList else state.posts,
                        isLoading = if (state.selectedTab == FeedTab.FOLLOWING) false else state.isLoading
                    )
                }
            }
        }
    }

    private suspend fun resolveMissingAuthors(newPosts: List<Post>) {
        val currentNames = _uiState.value.authorNames.toMutableMap()
        val currentPhotos = _uiState.value.authorPhotoUrls.toMutableMap()
        val missingIds = newPosts.map { it.authorId }.distinct().filter { !currentNames.containsKey(it) && it.isNotBlank() }
        
        missingIds.forEach { uid ->
            val profile = getUserProfileUseCase(uid)
            if (profile != null) {
                if (profile.name.isNotBlank()) {
                    currentNames[uid] = profile.name
                }
                if (profile.photoUrl.isNotBlank()) {
                    currentPhotos[uid] = profile.photoUrl
                }
            }
        }
        _uiState.update { it.copy(authorNames = currentNames, authorPhotoUrls = currentPhotos) }
    }

    fun loadMorePosts() {
        val state = _uiState.value
        if (state.selectedTab == FeedTab.FOLLOWING) {
            return
        }
        if (state.isLoading || state.isLoadingNextPage || !state.hasMorePosts) {
            return
        }
        val lastDoc = state.lastVisibleDocument ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNextPage = true) }
            val result = getPostsUseCase.getPage(
                pageSize = 10,
                startAfter = lastDoc,
                tag = state.selectedTag
            )
            result.onSuccess { page ->
                // Resolve missing authors ONLY for newly appended page
                resolveMissingAuthors(page.posts)

                _uiState.update { current ->
                    val combinedAll = (current.allPosts + page.posts).distinctBy { it.id }
                    val displayPosts = if (current.selectedTab == FeedTab.TRENDING) {
                        combinedAll.sortedByDescending { it.likeCount + it.commentCount }
                    } else {
                        (current.posts + page.posts).distinctBy { it.id }
                    }
                    current.copy(
                        posts = displayPosts,
                        allPosts = combinedAll,
                        lastVisibleDocument = page.lastVisibleDocument,
                        hasMorePosts = page.hasMore,
                        isLoadingNextPage = false
                    )
                }
            }.onFailure { error ->
                Log.e("DevJournal", "Failed to load more posts", error)
                _uiState.update {
                    it.copy(
                        isLoadingNextPage = false,
                        errorMessage = "Failed to load more posts"
                    )
                }
            }
        }
    }

    fun refreshFeed() {
        val uid = _uiState.value.currentUserId
        if (!networkMonitor.isOnline()) {
            _uiState.update { 
                it.copy(
                    isRefreshing = false, 
                    errorMessage = "You are currently offline. Connect to internet to refresh feeds."
                ) 
            }
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        if (uid != null) {
            observeFollowingPosts(uid, 20)
        }
        fetchInitialPosts(_uiState.value.selectedTag)
    }

    fun onTabSelected(tab: FeedTab) {
        if (_uiState.value.selectedTab == tab) return
        
        _uiState.update { state ->
            val sourcePosts = if (tab == FeedTab.FOLLOWING) state.followingPosts else state.allPosts
            val displayPosts = when (tab) {
                FeedTab.FOR_YOU -> sourcePosts.sortedByDescending { it.createdAt?.seconds ?: 0L }
                FeedTab.TRENDING -> sourcePosts.sortedByDescending { it.likeCount + it.commentCount }
                FeedTab.FOLLOWING -> sourcePosts
            }
            state.copy(
                selectedTab = tab,
                selectedSortOption = if (tab == FeedTab.TRENDING) FeedSortOption.TRENDING else FeedSortOption.LATEST,
                posts = displayPosts
            )
        }
    }

    fun onSortOptionSelected(option: FeedSortOption) {
        val tab = if (option == FeedSortOption.TRENDING) FeedTab.TRENDING else FeedTab.FOR_YOU
        onTabSelected(tab)
    }

    fun onTagSelected(tag: String) {
        if (_uiState.value.selectedTag.equals(tag, ignoreCase = true) && !_uiState.value.isLoading) return
        fetchInitialPosts(tag = tag)
    }

    fun onLikeClick(postId: String, alreadyLiked: Boolean) {
        val uid = _uiState.value.currentUserId ?: return
        if (!networkMonitor.isOnline()) {
            _uiState.update { it.copy(errorMessage = "You are currently offline. Connect to the internet to like articles.") }
            return
        }
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val newLikedIds = if (alreadyLiked) {
                    state.likedPostIds - postId
                } else {
                    state.likedPostIds + postId
                }
                state.copy(likedPostIds = newLikedIds)
            }
            try {
                likePostUseCase(postId, uid, alreadyLiked)
                // Fire like notification — only on the like action (not unlike), never to self
                if (!alreadyLiked) {
                    val post = _uiState.value.allPosts.find { it.id == postId }
                        ?: _uiState.value.followingPosts.find { it.id == postId }
                    val authorId = post?.authorId
                    if (post != null && !authorId.isNullOrBlank() && authorId != uid) {
                        val senderName = _uiState.value.authorNames[uid]
                            ?: getUserProfileUseCase(uid)?.name
                            ?: "Someone"
                        try {
                            notifyWorkerApi.sendNotification(
                                type = "new_like",
                                targetUid = authorId,
                                title = "$senderName liked your post",
                                body = post.title.take(80),
                                data = mapOf("postId" to post.id, "senderUid" to uid)
                            )
                        } catch (_: Exception) { /* best-effort */ }
                    }
                }
            } catch (e: Exception) {
                Log.e("DevJournal", "Like action failed", e)
                // Revert on error
                _uiState.update { state ->
                    val reverted = if (alreadyLiked) {
                        state.likedPostIds + postId
                    } else {
                        state.likedPostIds - postId
                    }
                    state.copy(likedPostIds = reverted, errorMessage = "Like failed: ${e.message}")
                }
            }
        }
    }

    fun onBookmarkClick(postId: String, alreadyBookmarked: Boolean) {
        val uid = _uiState.value.currentUserId ?: return
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val newBookmarks = if (alreadyBookmarked) {
                    state.bookmarkedPostIds - postId
                } else {
                    state.bookmarkedPostIds + postId
                }
                state.copy(bookmarkedPostIds = newBookmarks)
            }
            try {
                bookmarkPostUseCase(postId, uid, alreadyBookmarked)
            } catch (e: Exception) {
                Log.e("DevJournal", "Bookmark action failed", e)
                // Revert on error
                _uiState.update { state ->
                    val reverted = if (alreadyBookmarked) {
                        state.bookmarkedPostIds + postId
                    } else {
                        state.bookmarkedPostIds - postId
                    }
                    state.copy(bookmarkedPostIds = reverted, errorMessage = "Bookmark failed: ${e.message}")
                }
            }
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            try {
                deletePostUseCase(postId)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }


    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Fire a new_share notification when the share sheet is launched for a feed post. Best-effort. */
    fun notifyShare(post: com.devjournal.data.model.Post) {
        val uid = _uiState.value.currentUserId ?: return  // skip anonymous users
        val authorId = post.authorId
        if (authorId.isBlank() || authorId == uid) return  // no self-notify
        viewModelScope.launch {
            try {
                val senderName = _uiState.value.authorNames[uid]
                    ?: getUserProfileUseCase(uid)?.name
                    ?: "Someone"
                notifyWorkerApi.sendNotification(
                    type = "new_share",
                    targetUid = authorId,
                    title = "$senderName shared your post",
                    body = post.title.take(80),
                    data = mapOf("postId" to post.id, "senderUid" to uid)
                )
            } catch (_: Exception) { /* best-effort */ }
        }
    }
}
