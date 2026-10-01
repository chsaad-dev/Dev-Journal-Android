package com.devjournal.presentation.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.domain.usecase.GetPostsUseCase
import com.devjournal.domain.usecase.GetFollowingPostsUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.BookmarkPostUseCase
import com.devjournal.domain.usecase.DeletePostUseCase
import com.devjournal.domain.usecase.ObserveLikedPostIdsUseCase
import com.devjournal.domain.usecase.ObserveBookmarkedPostIdsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
    val errorMessage: String? = null
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
    private val deletePostUseCase: DeletePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var interactionsJob: Job? = null
    private var postsJob: Job? = null
    private var followingPostsJob: Job? = null
    
    private val _postLimit = MutableStateFlow(10)

    init {
        observeCurrentUser()
        observePosts()
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
                    observeFollowingPosts(user.uid, _postLimit.value)
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

    private fun observePosts() {
        viewModelScope.launch {
            _postLimit.collect { limit ->
                postsJob?.cancel()
                val currentUid = _uiState.value.currentUserId
                if (currentUid != null) {
                    observeFollowingPosts(currentUid, limit)
                }
                postsJob = launch {
                    getPostsUseCase(limit).collect { postsList ->
                        resolveMissingAuthors(postsList)

                        _uiState.update { state ->
                            val filteredAndSorted = filterAndSortPosts(postsList, state.selectedTag, state.selectedTab)
                            state.copy(
                                allPosts = postsList,
                                posts = if (state.selectedTab != FeedTab.FOLLOWING) filteredAndSorted else state.posts,
                                isLoading = if (state.selectedTab != FeedTab.FOLLOWING) false else state.isLoading
                            )
                        }
                    }
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
                    val filteredAndSorted = filterAndSortPosts(postsList, state.selectedTag, state.selectedTab)
                    state.copy(
                        followingPosts = postsList,
                        isFollowingLoading = false,
                        posts = if (state.selectedTab == FeedTab.FOLLOWING) filteredAndSorted else state.posts,
                        isLoading = if (state.selectedTab == FeedTab.FOLLOWING) false else state.isLoading
                    )
                }
            }
        }
    }

    private suspend fun resolveMissingAuthors(postsList: List<Post>) {
        val currentNames = _uiState.value.authorNames.toMutableMap()
        val currentPhotos = _uiState.value.authorPhotoUrls.toMutableMap()
        val missingIds = postsList.map { it.authorId }.distinct().filter { !currentNames.containsKey(it) && it.isNotBlank() }
        
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
        _postLimit.update { it + 10 }
    }

    fun onTabSelected(tab: FeedTab) {
        if (_uiState.value.selectedTab == tab) return
        
        _uiState.update { state ->
            val sourcePosts = if (tab == FeedTab.FOLLOWING) state.followingPosts else state.allPosts
            val filteredAndSorted = filterAndSortPosts(sourcePosts, state.selectedTag, tab)
            state.copy(
                selectedTab = tab,
                selectedSortOption = if (tab == FeedTab.TRENDING) FeedSortOption.TRENDING else FeedSortOption.LATEST,
                posts = filteredAndSorted
            )
        }
    }

    fun onSortOptionSelected(option: FeedSortOption) {
        val tab = if (option == FeedSortOption.TRENDING) FeedTab.TRENDING else FeedTab.FOR_YOU
        onTabSelected(tab)
    }

    fun onTagSelected(tag: String) {
        _uiState.update { state ->
            val sourcePosts = if (state.selectedTab == FeedTab.FOLLOWING) state.followingPosts else state.allPosts
            val filteredAndSorted = filterAndSortPosts(sourcePosts, tag, state.selectedTab)
            state.copy(
                selectedTag = tag,
                posts = filteredAndSorted
            )
        }
    }

    fun onLikeClick(postId: String, alreadyLiked: Boolean) {
        val uid = _uiState.value.currentUserId ?: return
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

    private fun filterAndSortPosts(posts: List<Post>, tag: String, tab: FeedTab): List<Post> {
        val filtered = if (tag.equals("All", ignoreCase = true)) {
            posts
        } else {
            posts.filter { post ->
                post.tags.any { it.equals(tag, ignoreCase = true) || it.equals("#$tag", ignoreCase = true) }
            }
        }
        
        return when (tab) {
            FeedTab.FOR_YOU -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
            FeedTab.TRENDING -> filtered.sortedByDescending { it.likeCount + it.commentCount }
            FeedTab.FOLLOWING -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }
}
