package com.devjournal.presentation.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Comment
import com.devjournal.data.model.Post
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.AddCommentUseCase
import com.devjournal.domain.usecase.BookmarkPostUseCase
import com.devjournal.domain.usecase.CheckIfBookmarkedUseCase
import com.devjournal.domain.usecase.CheckIfLikedUseCase
import com.devjournal.domain.usecase.DeleteCommentUseCase
import com.devjournal.domain.usecase.DeletePostUseCase
import com.devjournal.domain.usecase.GetCommentsUseCase
import com.devjournal.domain.usecase.GetPostDetailUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val currentUserId: String? = null,
    val commentInput: String = "",
    val isSubmittingComment: Boolean = false,
    val isAdmin: Boolean = false,
    val isDeletingPost: Boolean = false,
    val postDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val checkIfBookmarkedUseCase: CheckIfBookmarkedUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val checkIfLikedUseCase: CheckIfLikedUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val notifyWorkerApi: NotifyWorkerApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        observePost()
        observeComments()
        observeAuthAndLikeState()
    }

    private fun observePost() {
        viewModelScope.launch {
            getPostDetailUseCase(postId).collect { post ->
                _uiState.update { it.copy(post = post, isLoading = false) }
            }
        }
    }

    private fun observeComments() {
        viewModelScope.launch {
            getCommentsUseCase(postId).collect { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }
    }

    private fun observeAuthAndLikeState() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.uid) }
                if (user != null) {
                    launch {
                        val profile = getUserProfileUseCase(user.uid)
                        _uiState.update { it.copy(isAdmin = profile?.role.equals("admin", ignoreCase = true)) }
                    }
                    launch {
                        checkIfLikedUseCase(postId, user.uid)
                            .catch { /* ignore on signout */ }
                            .collect { liked ->
                                _uiState.update { it.copy(isLiked = liked) }
                            }
                    }
                    launch {
                        checkIfBookmarkedUseCase(postId, user.uid)
                            .catch { /* ignore on signout */ }
                            .collect { bookmarked ->
                                _uiState.update { it.copy(isBookmarked = bookmarked) }
                            }
                    }
                } else {
                    _uiState.update { it.copy(isLiked = false, isBookmarked = false, isAdmin = false) }
                }
            }
        }
    }

    fun onLikeClick() {
        val uid = _uiState.value.currentUserId ?: return
        val currentPost = _uiState.value.post ?: return
        val alreadyLiked = _uiState.value.isLiked

        viewModelScope.launch {
            try {
                likePostUseCase(postId, uid, alreadyLiked)
                if (!alreadyLiked && currentPost.authorId.isNotBlank() && currentPost.authorId != uid) {
                    notifyWorkerApi.sendNotification(
                        type = "new_like",
                        targetUid = currentPost.authorId,
                        title = "New like on \"${currentPost.title}\"",
                        body = "Someone liked your post"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update like: ${e.message}") }
            }
        }
    }

    fun onBookmarkClick() {
        val uid = _uiState.value.currentUserId ?: return
        val alreadyBookmarked = _uiState.value.isBookmarked

        viewModelScope.launch {
            try {
                bookmarkPostUseCase(postId, uid, alreadyBookmarked)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update bookmark: ${e.message}") }
            }
        }
    }

    fun onCommentInputChange(text: String) {
        _uiState.update { it.copy(commentInput = text, errorMessage = null) }
    }

    fun onSubmitComment() {
        val text = _uiState.value.commentInput.trim()
        val uid = _uiState.value.currentUserId ?: return
        val currentPost = _uiState.value.post ?: return

        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true, errorMessage = null) }
            try {
                val newComment = Comment(
                    userId = uid,
                    text = text
                )
                addCommentUseCase(postId, newComment)

                if (currentPost.authorId.isNotBlank() && currentPost.authorId != uid) {
                    notifyWorkerApi.sendNotification(
                        type = "new_comment",
                        targetUid = currentPost.authorId,
                        title = "New comment on \"${currentPost.title}\"",
                        body = text.take(100)
                    )
                }
                _uiState.update { it.copy(commentInput = "", isSubmittingComment = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingComment = false, errorMessage = "Failed to post comment: ${e.message}") }
            }
        }
    }

    fun onDeletePost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingPost = true, errorMessage = null) }
            val result = deletePostUseCase(postId)
            result.onSuccess {
                _uiState.update { it.copy(isDeletingPost = false, postDeleted = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isDeletingPost = false, errorMessage = "Failed to delete post: ${e.message}") }
            }
        }
    }

    fun onDeleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                deleteCommentUseCase(postId, commentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete comment: ${e.message}") }
            }
        }
    }
}
