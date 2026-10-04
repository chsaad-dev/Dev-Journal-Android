package com.devjournal.presentation.postdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Comment
import com.devjournal.data.model.Post
import com.devjournal.data.model.PostViewer
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.AddCommentUseCase
import com.devjournal.domain.usecase.BookmarkPostUseCase
import com.devjournal.domain.usecase.CheckIfBookmarkedUseCase
import com.devjournal.domain.usecase.CheckIfLikedUseCase
import com.devjournal.domain.usecase.DeleteCommentUseCase
import com.devjournal.domain.usecase.DeletePostUseCase
import com.devjournal.domain.usecase.GetCommentsUseCase
import com.devjournal.domain.usecase.GetPostDetailUseCase
import com.devjournal.domain.usecase.GetPostViewersUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.LikePostUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.RecordViewUseCase
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
    val errorMessage: String? = null,
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val commenterNames: Map<String, String> = emptyMap(),
    val commenterUsernames: Map<String, String> = emptyMap(),
    val commenterPhotoUrls: Map<String, String> = emptyMap(),
    val replyingToComment: Comment? = null,
    val replyingToAuthorName: String? = null,
    val viewers: List<PostViewer> = emptyList(),
    val isViewersSheetOpen: Boolean = false,
    val isLoadingViewers: Boolean = false
) {
    val canViewReadersList: Boolean
        get() = isAdmin || (post != null && currentUserId != null && post.authorId == currentUserId)
}

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
    private val recordViewUseCase: RecordViewUseCase,
    private val getPostViewersUseCase: GetPostViewersUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var viewRecorded = false

    init {
        observePost()
        observeComments()
        observeAuthAndLikeState()
    }

    private fun observePost() {
        viewModelScope.launch {
            getPostDetailUseCase(postId).collect { post ->
                if (post != null && post.authorId.isNotBlank()) {
                    val profile = getUserProfileUseCase(post.authorId)
                    val authorName = if (profile != null && profile.name.isNotBlank()) profile.name else ""
                    val authorPhotoUrl = if (profile != null && profile.photoUrl.isNotBlank()) profile.photoUrl else ""
                    _uiState.update { it.copy(post = post, authorName = authorName, authorPhotoUrl = authorPhotoUrl, isLoading = false) }
                } else {
                    _uiState.update { it.copy(post = post, isLoading = false) }
                }
            }
        }
    }

    private fun observeComments() {
        viewModelScope.launch {
            getCommentsUseCase(postId).collect { comments ->
                val currentNames = _uiState.value.commenterNames.toMutableMap()
                val currentUsernames = _uiState.value.commenterUsernames.toMutableMap()
                val currentPhotos = _uiState.value.commenterPhotoUrls.toMutableMap()
                val missingIds = comments.map { it.userId }.distinct().filter { !currentNames.containsKey(it) && it.isNotBlank() }
                
                missingIds.forEach { uid ->
                    val profile = getUserProfileUseCase(uid)
                    if (profile != null) {
                        if (profile.name.isNotBlank()) currentNames[uid] = profile.name
                        if (profile.username.isNotBlank()) currentUsernames[uid] = profile.username
                        if (profile.photoUrl.isNotBlank()) currentPhotos[uid] = profile.photoUrl
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        comments = comments, 
                        commenterNames = currentNames, 
                        commenterUsernames = currentUsernames,
                        commenterPhotoUrls = currentPhotos
                    ) 
                }
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

                        // Record view once per screen visit with user profile details
                        if (!viewRecorded) {
                            viewRecorded = true
                            try {
                                recordViewUseCase(
                                    postId = postId,
                                    uid = user.uid,
                                    userName = profile?.name,
                                    userPhotoUrl = profile?.photoUrl
                                )
                            } catch (_: Exception) { /* non-critical */ }
                        }
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

    fun onOpenViewersSheet() {
        if (!_uiState.value.canViewReadersList) return
        _uiState.update { it.copy(isViewersSheetOpen = true, isLoadingViewers = true) }
        viewModelScope.launch {
            getPostViewersUseCase(postId).collect { viewersList ->
                _uiState.update { it.copy(viewers = viewersList, isLoadingViewers = false) }
            }
        }
    }

    fun onCloseViewersSheet() {
        _uiState.update { it.copy(isViewersSheetOpen = false) }
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
                Log.e("DevJournal", "Bookmark action failed in PostDetail", e)
                _uiState.update { it.copy(errorMessage = "Failed to update bookmark: ${e.message}") }
            }
        }
    }

    fun onCommentInputChange(text: String) {
        _uiState.update { it.copy(commentInput = text, errorMessage = null) }
    }

    fun onReplyToComment(comment: Comment, authorName: String) {
        val displayName = authorName.ifBlank { "developer" }
        _uiState.update {
            it.copy(
                replyingToComment = comment,
                replyingToAuthorName = displayName,
                commentInput = if (it.commentInput.isBlank()) "@$displayName " else it.commentInput
            )
        }
    }

    fun onCancelReply() {
        _uiState.update {
            it.copy(
                replyingToComment = null,
                replyingToAuthorName = null
            )
        }
    }

    fun onSubmitComment() {
        val text = _uiState.value.commentInput.trim()
        val uid = _uiState.value.currentUserId ?: return
        val currentPost = _uiState.value.post ?: return
        val replyingComment = _uiState.value.replyingToComment
        val replyingName = _uiState.value.replyingToAuthorName

        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true, errorMessage = null) }
            try {
                val newComment = Comment(
                    userId = uid,
                    text = text,
                    parentCommentId = replyingComment?.id,
                    replyToUsername = replyingName
                )
                addCommentUseCase(postId, newComment)

                // Send notification to author or replied commenter
                if (replyingComment != null && replyingComment.userId != uid) {
                    notifyWorkerApi.sendNotification(
                        type = "comment_reply",
                        targetUid = replyingComment.userId,
                        title = "Reply to your comment",
                        body = text.take(100)
                    )
                } else if (currentPost.authorId.isNotBlank() && currentPost.authorId != uid) {
                    notifyWorkerApi.sendNotification(
                        type = "new_comment",
                        targetUid = currentPost.authorId,
                        title = "New comment on \"${currentPost.title}\"",
                        body = text.take(100)
                    )
                }
                _uiState.update { 
                    it.copy(
                        commentInput = "", 
                        replyingToComment = null,
                        replyingToAuthorName = null,
                        isSubmittingComment = false
                    ) 
                }
            } catch (e: Exception) {
                Log.e("DevJournal", "Comment action failed", e)
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
                Log.e("DevJournal", "Delete post failed", e)
                _uiState.update { it.copy(isDeletingPost = false, errorMessage = "Failed to delete post: ${e.message}") }
            }
        }
    }

    fun onDeleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                deleteCommentUseCase(postId, commentId)
            } catch (e: Exception) {
                Log.e("DevJournal", "Delete comment failed", e)
                _uiState.update { it.copy(errorMessage = "Failed to delete comment: ${e.message}") }
            }
        }
    }
}
