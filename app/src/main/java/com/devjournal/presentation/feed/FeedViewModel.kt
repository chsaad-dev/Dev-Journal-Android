package com.devjournal.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.domain.usecase.GetPostsUseCase
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

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val allPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTag: String = "All",
    val availableTags: List<String> = listOf("All", "Android", "Compose", "Kotlin", "Architecture", "Firebase"),
    val isAdmin: Boolean = false,
    val currentUserId: String? = null,
    val currentUserPhotoUrl: String? = null,
    val likedPostIds: Set<String> = emptySet(),
    val bookmarkedPostIds: Set<String> = emptySet(),
    val selectedSortOption: FeedSortOption = FeedSortOption.LATEST,
    val authorNames: Map<String, String> = emptyMap(),
    val authorPhotoUrls: Map<String, String> = emptyMap()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
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
                } else {
                    interactionsJob?.cancel()
                    _uiState.update {
                        it.copy(
                            currentUserId = null,
                            currentUserPhotoUrl = null,
                            isAdmin = false,
                            likedPostIds = emptySet(),
                            bookmarkedPostIds = emptySet()
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
                postsJob = launch {
                    getPostsUseCase(limit).collect { postsList ->
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

                        _uiState.update { state ->
                            val filteredAndSorted = filterAndSortPosts(postsList, state.selectedTag, state.selectedSortOption)
                            state.copy(
                                allPosts = postsList,
                                posts = filteredAndSorted,
                                authorNames = currentNames,
                                authorPhotoUrls = currentPhotos,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMorePosts() {
        _postLimit.update { it + 10 }
    }

    fun onTagSelected(tag: String) {
        _uiState.update { state ->
            val filteredAndSorted = filterAndSortPosts(state.allPosts, tag, state.selectedSortOption)
            state.copy(
                selectedTag = tag,
                posts = filteredAndSorted
            )
        }
    }
    
    fun onSortOptionSelected(option: FeedSortOption) {
        if (_uiState.value.selectedSortOption == option) return
        
        _uiState.update { state ->
            val filteredAndSorted = filterAndSortPosts(state.allPosts, state.selectedTag, option)
            state.copy(
                selectedSortOption = option,
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
            } catch (_: Exception) {
                // Revert on error
                _uiState.update { state ->
                    val reverted = if (alreadyLiked) {
                        state.likedPostIds + postId
                    } else {
                        state.likedPostIds - postId
                    }
                    state.copy(likedPostIds = reverted)
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
            } catch (_: Exception) {
                // Revert on error
                _uiState.update { state ->
                    val reverted = if (alreadyBookmarked) {
                        state.bookmarkedPostIds + postId
                    } else {
                        state.bookmarkedPostIds - postId
                    }
                    state.copy(bookmarkedPostIds = reverted)
                }
            }
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            try {
                deletePostUseCase(postId)
            } catch (e: Exception) {
                // Should show error to user in a real app
            }
        }
    }

    private fun filterAndSortPosts(posts: List<Post>, tag: String, sortOption: FeedSortOption): List<Post> {
        val filtered = if (tag.equals("All", ignoreCase = true)) {
            posts
        } else {
            posts.filter { post ->
                post.tags.any { it.equals(tag, ignoreCase = true) || it.equals("#$tag", ignoreCase = true) }
            }
        }
        
        return when (sortOption) {
            FeedSortOption.LATEST -> filtered.sortedByDescending { it.createdAt }
            FeedSortOption.TRENDING -> filtered.sortedByDescending { it.likeCount + it.commentCount }
        }
    }
}
