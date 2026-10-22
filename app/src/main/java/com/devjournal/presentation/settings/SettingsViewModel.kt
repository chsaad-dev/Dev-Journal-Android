package com.devjournal.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.UserProfile
import com.devjournal.domain.model.ThemeMode
import com.devjournal.domain.usecase.GetThemeModeUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.ObserveUserProfileUseCase
import com.devjournal.domain.usecase.SetThemeModeUseCase
import com.devjournal.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSignedOut: Boolean = false,
    val isAdmin: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val profile: UserProfile? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null

    init {
        observeCurrentUser()
        observeThemeMode()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                profileJob?.cancel()
                if (user != null) {
                    val initialProfile = getUserProfileUseCase(user.uid)
                    _uiState.update {
                        it.copy(
                            profile = initialProfile,
                            isAdmin = initialProfile?.role.equals("admin", ignoreCase = true)
                        )
                    }
                    profileJob = launch {
                        observeUserProfileUseCase(user.uid).collect { profile ->
                            if (profile != null) {
                                _uiState.update {
                                    it.copy(
                                        profile = profile,
                                        isAdmin = profile.role.equals("admin", ignoreCase = true)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            profile = null,
                            isAdmin = false
                        )
                    }
                }
            }
        }
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            getThemeModeUseCase().collect { mode ->
                _uiState.update {
                    it.copy(themeMode = mode)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.value = _uiState.value.copy(isSignedOut = true)
        }
    }
}

