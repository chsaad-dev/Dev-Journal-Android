package com.devjournal.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.domain.usecase.GetPostsUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val allPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTag: String = "All",
    val availableTags: List<String> = listOf("All", "Android", "Compose", "Kotlin", "Architecture", "Firebase"),
    val isAdmin: Boolean = false,
    val currentUserId: String? = null,
    val currentUserPhotoUrl: String? = null,
    val likedPostIds: Set<String> = emptySet()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

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
                } else {
                    _uiState.update {
                        it.copy(
                            currentUserId = null,
                            currentUserPhotoUrl = null,
                            isAdmin = false
                        )
                    }
                }
            }
        }
    }

    private fun observePosts() {
        viewModelScope.launch {
            getPostsUseCase().collect { postsList ->
                _uiState.update { state ->
                    val filtered = filterPostsByTag(postsList, state.selectedTag)
                    state.copy(
                        allPosts = postsList,
                        posts = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onTagSelected(tag: String) {
        _uiState.update { state ->
            val filtered = filterPostsByTag(state.allPosts, tag)
            state.copy(
                selectedTag = tag,
                posts = filtered
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

    private fun filterPostsByTag(posts: List<Post>, tag: String): List<Post> {
        if (tag.equals("All", ignoreCase = true)) {
            return posts
        }
        return posts.filter { post ->
            post.tags.any { it.equals(tag, ignoreCase = true) || it.equals("#$tag", ignoreCase = true) }
        }
    }
}
