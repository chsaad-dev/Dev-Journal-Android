package com.devjournal.presentation.author

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.model.UserProfile
import com.devjournal.domain.usecase.BookmarkPostUseCase
import com.devjournal.domain.usecase.DeletePostUseCase
import com.devjournal.domain.usecase.GetPostsByAuthorUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.ObserveBookmarkedPostIdsUseCase
import com.devjournal.domain.usecase.ObserveLikedPostIdsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthorProfileUiState(
    val authorProfile: UserProfile? = null,
    val posts: List<Post> = emptyList(),
    val totalLikes: Int = 0,
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val likedPostIds: Set<String> = emptySet(),
    val bookmarkedPostIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class AuthorProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getPostsByAuthorUseCase: GetPostsByAuthorUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeLikedPostIdsUseCase: ObserveLikedPostIdsUseCase,
    private val observeBookmarkedPostIdsUseCase: ObserveBookmarkedPostIdsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val deletePostUseCase: DeletePostUseCase
) : ViewModel() {

    val authorId: String = checkNotNull(savedStateHandle["authorId"])

    private val _uiState = MutableStateFlow(AuthorProfileUiState())
    val uiState: StateFlow<AuthorProfileUiState> = _uiState.asStateFlow()

    private var interactionsJob: Job? = null

    init {
        loadAuthorData()
        observeCurrentUser()
    }

    private fun loadAuthorData() {
        viewModelScope.launch {
            // Load Author Profile details
            val profile = getUserProfileUseCase(authorId) ?: UserProfile(
                uid = authorId,
                email = "",
                name = "Author ${authorId.take(6)}",
                role = "author"
            )
            _uiState.update { it.copy(authorProfile = profile) }

            // Observe Author's published posts
            getPostsByAuthorUseCase(authorId)
                .catch { /* ignore errors */ }
                .collect { authorPosts ->
                    val totalLikes = authorPosts.sumOf { it.likeCount }
                    _uiState.update {
                        it.copy(
                            posts = authorPosts,
                            totalLikes = totalLikes,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.uid) }
                if (user != null) {
                    val currentProfile = getUserProfileUseCase(user.uid)
                    _uiState.update { it.copy(isAdmin = currentProfile?.role.equals("admin", ignoreCase = true)) }
                    observeInteractions(user.uid)
                } else {
                    interactionsJob?.cancel()
                    _uiState.update {
                        it.copy(
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
                    .catch { /* ignore */ }
                    .collect { likedIds ->
                        _uiState.update { it.copy(likedPostIds = likedIds) }
                    }
            }
            launch {
                observeBookmarkedPostIdsUseCase(uid)
                    .catch { /* ignore */ }
                    .collect { bookmarkedIds ->
                        _uiState.update { it.copy(bookmarkedPostIds = bookmarkedIds) }
                    }
            }
        }
    }

    fun onLikeClick(postId: String) {
        val uid = _uiState.value.currentUserId ?: return
        val isLiked = _uiState.value.likedPostIds.contains(postId)
        viewModelScope.launch {
            likePostUseCase(postId, uid, isLiked)
        }
    }

    fun onBookmarkClick(postId: String) {
        val uid = _uiState.value.currentUserId ?: return
        val isBookmarked = _uiState.value.bookmarkedPostIds.contains(postId)
        viewModelScope.launch {
            bookmarkPostUseCase(postId, uid, isBookmarked)
        }
    }

    fun onDeletePostClick(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId)
        }
    }
}
