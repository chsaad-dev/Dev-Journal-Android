package com.devjournal.presentation.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.model.UserProfile
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.CheckUsernameAvailabilityUseCase
import com.devjournal.domain.usecase.FollowUserUseCase
import com.devjournal.domain.usecase.GetBookmarkedPostsUseCase
import com.devjournal.domain.usecase.GetLikedPostsUseCase
import com.devjournal.domain.usecase.GetPostsByAuthorUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.GetRemoteDraftsUseCase
import com.devjournal.domain.usecase.IsFollowingUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.ObserveUserProfileUseCase
import com.devjournal.domain.usecase.SignOutUseCase
import com.devjournal.domain.usecase.UnfollowUserUseCase
import com.devjournal.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    TOO_SHORT
}

data class ProfileUiState(
    val profile: UserProfile? = null,
    val authorPosts: List<Post> = emptyList(),
    val likedPosts: List<Post> = emptyList(),
    val bookmarkedPosts: List<Post> = emptyList(),
    val remoteDrafts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val editUsername: String = "",
    val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
    val isCheckingUsername: Boolean = false,
    val usernameError: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null,
    
    // Followers & Following
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val targetUid: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val getPostsByAuthorUseCase: GetPostsByAuthorUseCase,
    private val getLikedPostsUseCase: GetLikedPostsUseCase,
    private val getBookmarkedPostsUseCase: GetBookmarkedPostsUseCase,
    private val cloudinaryUploader: CloudinaryUploader,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    private val getRemoteDraftsUseCase: GetRemoteDraftsUseCase,
    private val notifyWorkerApi: NotifyWorkerApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val navUid: String? = savedStateHandle.get<String>("uid")
    private var profileJob: Job? = null
    private var followingJob: Job? = null
    private var usernameCheckJob: Job? = null
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
                            remoteDrafts = emptyList(),
                            authorPosts = emptyList(),
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
            // Real-time listener for user profile and follower/following counts
            launch {
                observeUserProfileUseCase(uid).collect { observedProfile ->
                    if (observedProfile != null) {
                        _uiState.update { current ->
                            current.copy(
                                profile = observedProfile,
                                followerCount = observedProfile.followerCount,
                                followingCount = observedProfile.followingCount,
                                editName = if (current.isEditing) current.editName else observedProfile.name,
                                editBio = if (current.isEditing) current.editBio else observedProfile.bio,
                                editUsername = if (current.isEditing) current.editUsername else observedProfile.displayUsername,
                                isLoading = false
                            )
                        }
                    } else if (_uiState.value.profile == null) {
                        // User document not created yet, supply fallback
                        val fallback = UserProfile(
                            uid = uid,
                            email = email ?: "",
                            name = displayName ?: email?.substringBefore("@") ?: "Developer",
                            photoUrl = photoUrl ?: "",
                            role = "reader"
                        )
                        _uiState.update { current ->
                            current.copy(
                                profile = fallback,
                                followerCount = 0,
                                followingCount = 0,
                                editName = fallback.name,
                                editBio = fallback.bio,
                                editUsername = fallback.displayUsername,
                                isLoading = false
                            )
                        }
                    }
                }
            }

            launch {
                getPostsByAuthorUseCase(uid)
                    .catch { /* ignore */ }
                    .collect { posts ->
                        _uiState.update { it.copy(authorPosts = posts) }
                    }
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

            if (_uiState.value.isOwnProfile) {
                launch {
                    getRemoteDraftsUseCase(uid)
                        .catch { /* ignore */ }
                        .collect { drafts ->
                            _uiState.update { it.copy(remoteDrafts = drafts) }
                        }
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
                followerCount = (it.followerCount + if (currentlyFollowing) -1 else 1).coerceAtLeast(0)
            ) 
        }
        
        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                unfollowUserUseCase(currentUid, targetUid)
            } else {
                followUserUseCase(currentUid, targetUid)
            }
            
            result.onSuccess {
                // Send push notification on follow (not unfollow)
                if (!currentlyFollowing) {
                    val currentUserName = _uiState.value.profile?.name
                        ?: observeAuthStateUseCase().firstOrNull()?.displayName
                        ?: "Someone"
                    launch {
                        notifyWorkerApi.sendNotification(
                            type = "follow",
                            targetUid = targetUid,
                            title = "$currentUserName started following you",
                            body = "Tap to view their profile",
                            data = mapOf("senderUid" to currentUid)
                        )
                    }
                }
            }
            
            result.onFailure { error ->
                Log.e("DevJournalFollow", "Follow action failed: ${error.message}", error)
                // Revert on failure
                _uiState.update { 
                    it.copy(
                        isFollowing = currentlyFollowing,
                        followerCount = (it.followerCount + if (currentlyFollowing) 1 else -1).coerceAtLeast(0),
                        errorMessage = "Failed to update follow: ${error.localizedMessage ?: error.message}"
                    ) 
                }
            }
        }
    }

    fun onEditClick() {
        val currentProfile = _uiState.value.profile
        val username = currentProfile?.displayUsername ?: ""
        _uiState.update {
            it.copy(
                isEditing = !it.isEditing,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                editUsername = username,
                usernameAvailability = if (username.isNotBlank()) UsernameAvailability.AVAILABLE else UsernameAvailability.IDLE,
                usernameError = null,
                errorMessage = null
            )
        }
    }

    fun onCancelEdit() {
        usernameCheckJob?.cancel()
        val currentProfile = _uiState.value.profile
        val username = currentProfile?.displayUsername ?: ""
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                editUsername = username,
                usernameAvailability = if (username.isNotBlank()) UsernameAvailability.AVAILABLE else UsernameAvailability.IDLE,
                usernameError = null,
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

    fun onUsernameChange(username: String) {
        val cleaned = username.lowercase().replace(Regex("[^a-z0-9_]"), "").take(20)
        val currentProfileUsername = _uiState.value.profile?.username?.lowercase() ?: ""

        usernameCheckJob?.cancel()

        if (cleaned.isBlank()) {
            _uiState.update { 
                it.copy(
                    editUsername = cleaned,
                    usernameAvailability = UsernameAvailability.IDLE,
                    usernameError = null,
                    isCheckingUsername = false
                ) 
            }
            return
        }

        if (cleaned.length < 3) {
            _uiState.update { 
                it.copy(
                    editUsername = cleaned,
                    usernameAvailability = UsernameAvailability.TOO_SHORT,
                    usernameError = "Username must be at least 3 characters",
                    isCheckingUsername = false
                ) 
            }
            return
        }

        // If identical to current user's username, it's immediately available to them
        if (cleaned == currentProfileUsername) {
            _uiState.update { 
                it.copy(
                    editUsername = cleaned,
                    usernameAvailability = UsernameAvailability.AVAILABLE,
                    usernameError = null,
                    isCheckingUsername = false
                ) 
            }
            return
        }

        _uiState.update { 
            it.copy(
                editUsername = cleaned,
                usernameAvailability = UsernameAvailability.CHECKING,
                usernameError = null,
                isCheckingUsername = true
            ) 
        }

        usernameCheckJob = viewModelScope.launch {
            delay(350)
            val currentUid = currentAuthUid ?: ""
            val isAvailable = checkUsernameAvailabilityUseCase(cleaned, currentUid)
            _uiState.update { current ->
                if (current.editUsername == cleaned) {
                    current.copy(
                        usernameAvailability = if (isAvailable) UsernameAvailability.AVAILABLE else UsernameAvailability.TAKEN,
                        usernameError = if (isAvailable) null else "@$cleaned is already taken",
                        isCheckingUsername = false
                    )
                } else {
                    current
                }
            }
        }
    }

    fun onSaveClick(onSuccess: (() -> Unit)? = null) {
        val currentProfile = _uiState.value.profile ?: return
        val newName = _uiState.value.editName.trim()
        val newBio = _uiState.value.editBio.trim()
        val newUsername = _uiState.value.editUsername.trim().lowercase()

        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        if (newUsername.length < 3 || newUsername.length > 20) {
            _uiState.update { it.copy(usernameError = "Username must be 3-20 characters long") }
            return
        }

        if (!newUsername.matches(Regex("^[a-z0-9_]+$"))) {
            _uiState.update { it.copy(usernameError = "Username can only contain letters, numbers, and underscores") }
            return
        }

        if (_uiState.value.usernameAvailability == UsernameAvailability.TAKEN) {
            _uiState.update { it.copy(usernameError = "Username @$newUsername is already taken") }
            return
        }

        val currentUid = currentAuthUid ?: return

        viewModelScope.launch {
            // Check availability if username was modified and not already confirmed available
            if (newUsername != currentProfile.username.lowercase() && _uiState.value.usernameAvailability != UsernameAvailability.AVAILABLE) {
                _uiState.update { it.copy(isCheckingUsername = true) }
                val isAvailable = checkUsernameAvailabilityUseCase(newUsername, currentUid)
                _uiState.update { it.copy(isCheckingUsername = false) }
                if (!isAvailable) {
                    _uiState.update { 
                        it.copy(
                            usernameAvailability = UsernameAvailability.TAKEN,
                            usernameError = "Username @$newUsername is already taken"
                        ) 
                    }
                    return@launch
                }
            }

            val updated = currentProfile.copy(
                name = newName,
                bio = newBio,
                username = newUsername
            )
            try {
                updateUserProfileUseCase(updated)
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isEditing = false,
                        usernameError = null,
                        errorMessage = null
                    )
                }
                onSuccess?.invoke()
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
