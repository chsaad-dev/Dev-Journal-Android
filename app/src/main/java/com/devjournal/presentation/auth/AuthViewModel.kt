package com.devjournal.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.RegisterFcmTokenUseCase
import com.devjournal.domain.usecase.SignInUseCase
import com.devjournal.domain.usecase.SignInWithGoogleUseCase
import com.devjournal.domain.usecase.SignUpUseCase
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isSignUpMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user != null) {
                    registerFcmTokenForUser(user.uid)
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                }
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null
            )
        }
    }

    fun onSignInClick(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signInUseCase(email.trim(), password)
            result.onSuccess { user ->
                registerFcmTokenForUser(user.uid)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Authentication failed") }
            }
        }
    }

    fun onSignUpClick(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signUpUseCase(email.trim(), password)
            result.onSuccess { user ->
                registerFcmTokenForUser(user.uid)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Sign up failed") }
            }
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signInWithGoogleUseCase(idToken)
            result.onSuccess { user ->
                registerFcmTokenForUser(user.uid)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Google sign-in failed") }
            }
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    private suspend fun registerFcmTokenForUser(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                registerFcmTokenUseCase(uid, token)
            }
        } catch (_: Exception) {
            // Non-critical background registration
        }
    }
}
