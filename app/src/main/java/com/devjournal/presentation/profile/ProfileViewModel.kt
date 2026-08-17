package com.devjournal.presentation.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.model.UserProfile
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.domain.usecase.FollowUserUseCase
import com.devjournal.domain.usecase.GetBookmarkedPostsUseCase
import com.devjournal.domain.usecase.GetLikedPostsUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.IsFollowingUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.SignOutUseCase
import com.devjournal.domain.usecase.UnfollowUserUseCase
import com.devjournal.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val likedPosts: List<Post> = emptyList(),
    val bookmarkedPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val isUploadingPhoto: Boolean = false,
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null,
    
    // New fields
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val targetUid: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val getLikedPostsUseCase: GetLikedPostsUseCase,
    private val getBookmarkedPostsUseCase: GetBookmarkedPostsUseCase,
    private val cloudinaryUploader: CloudinaryUploader,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val navUid: String? = savedStateHandle.get<String>("uid")
    private var profileJob: Job? = null
    private var followingJob: Job? = null
    private var currentAuthUid: String? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user == null) {
                    profileJob?.cancel()
                    followingJob?.cancel()
                    currentAuthUid = null
                    _uiState.update {
                        it.copy(
                            isSignedOut = true,
                            isLoading = false,
                            likedPosts = emptyList(),
                            bookmarkedPosts = emptyList(),
                            profile = null
                        )
                    }
                } else {
                    currentAuthUid = user.uid
                    _uiState.update { it.copy(isSignedOut = false) }
                    
                    val uidToLoad = navUid ?: user.uid
                    val isOwnProfile = uidToLoad == user.uid
                    
                    _uiState.update { 
                        it.copy(
                            isOwnProfile = isOwnProfile,
                            targetUid = uidToLoad
                        ) 
                    }
                    
                    loadProfileAndPosts(
                        uid = uidToLoad,
                        email = if (isOwnProfile) user.email else null,
                        displayName = if (isOwnProfile) user.displayName else null,
                        photoUrl = if (isOwnProfile) user.photoUrl?.toString() else null
                    )
                    
                    if (!isOwnProfile) {
                        observeFollowingStatus(user.uid, uidToLoad)
                    }
                }
            }
        }
    }

    private fun observeFollowingStatus(currentUid: String, targetUid: String) {
        followingJob?.cancel()
        followingJob = viewModelScope.launch {
            isFollowingUseCase(currentUid, targetUid).collect { isFollowing ->
                _uiState.update { it.copy(isFollowing = isFollowing) }
            }
        }
    }

    private fun loadProfileAndPosts(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val existingProfile = getUserProfileUseCase(uid) ?: UserProfile(
                uid = uid,
                email = email ?: "",
                name = displayName ?: email?.substringBefore("@") ?: "Developer",
                photoUrl = photoUrl ?: "",
                role = "reader"
            )

            _uiState.update {
                it.copy(
                    profile = existingProfile,
                    editName = existingProfile.name,
                    editBio = existingProfile.bio,
                    followerCount = existingProfile.followerCount,
                    followingCount = existingProfile.followingCount,
                    isLoading = false
                )
            }

            launch {
                getLikedPostsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { liked ->
                        _uiState.update { it.copy(likedPosts = liked) }
                    }
            }

            launch {
                getBookmarkedPostsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { bookmarked ->
                        _uiState.update { it.copy(bookmarkedPosts = bookmarked) }
                    }
            }
        }
    }

    fun onFollowClick() {
        val currentUid = currentAuthUid ?: return
        val targetUid = _uiState.value.targetUid
        if (currentUid == targetUid || targetUid.isBlank()) return
        
        val currentlyFollowing = _uiState.value.isFollowing
        
        // Optimistic update
        _uiState.update { 
            it.copy(
                isFollowing = !currentlyFollowing,
                followerCount = it.followerCount + if (currentlyFollowing) -1 else 1
            ) 
        }
        
        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                unfollowUserUseCase(currentUid, targetUid)
            } else {
                followUserUseCase(currentUid, targetUid)
            }
            
            result.onFailure {
                // Revert on failure
                _uiState.update { 
                    it.copy(
                        isFollowing = currentlyFollowing,
                        followerCount = it.followerCount + if (currentlyFollowing) 1 else -1,
                        errorMessage = "Failed to update follow status"
                    ) 
                }
            }
        }
    }

    fun onEditClick() {
        val currentProfile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = !it.isEditing,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                errorMessage = null
            )
        }
    }

    fun onCancelEdit() {
        val currentProfile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                errorMessage = null
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(editBio = bio) }
    }

    fun onSaveClick() {
        val currentProfile = _uiState.value.profile ?: return
        val newName = _uiState.value.editName.trim()
        val newBio = _uiState.value.editBio.trim()

        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val updated = currentProfile.copy(name = newName, bio = newBio)
            try {
                updateUserProfileUseCase(updated)
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isEditing = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update profile: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        val currentProfile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            val result = cloudinaryUploader.uploadImage(uri)
            result.onSuccess { photoUrl ->
                val updated = currentProfile.copy(photoUrl = photoUrl)
                updateUserProfileUseCase(updated)
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isUploadingPhoto = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        errorMessage = "Photo upload failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }
}
