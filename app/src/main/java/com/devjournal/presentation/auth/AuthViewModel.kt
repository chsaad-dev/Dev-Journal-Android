package com.devjournal.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.domain.repository.AuthRepository
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.RegisterFcmTokenUseCase
import com.devjournal.domain.usecase.ResendVerificationEmailUseCase
import com.devjournal.domain.usecase.SendPasswordResetEmailUseCase
import com.devjournal.domain.usecase.SignInUseCase
import com.devjournal.domain.usecase.SignInWithGoogleUseCase
import com.devjournal.domain.usecase.SignOutUseCase
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
    val isResettingPassword: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showResendButton: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isSignUpMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user != null) {
                    if (authRepository.isUserVerified(user)) {
                        // Check suspension before granting access
                        val profile = getUserProfileUseCase(user.uid)
                        if (profile?.suspended == true) {
                            signOutUseCase()
                            _uiState.update {
                                it.copy(
                                    isAuthenticated = false,
                                    isLoading = false,
                                    errorMessage = "Your account has been suspended. Contact support for more information."
                                )
                            }
                        } else {
                            registerFcmTokenForUser(user.uid)
                            _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                        }
                    } else {
                        _uiState.update { it.copy(isAuthenticated = false, isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(isAuthenticated = false, isLoading = false) }
                }
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _uiState.update { it.copy(errorMessage = null, successMessage = null, showResendButton = false) }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _uiState.update {
            it.copy(
                isSignUpMode = isSignUp,
                errorMessage = null,
                successMessage = null,
                showResendButton = false
            )
        }
        _password.value = ""
        _confirmPassword.value = ""
    }

    fun toggleMode() {
        setSignUpMode(!_uiState.value.isSignUpMode)
    }

    fun onSignInClick(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, showResendButton = false) }
            val result = signInUseCase(email.trim(), password)
            result.onSuccess { user ->
                // Check suspension before granting access
                val profile = getUserProfileUseCase(user.uid)
                if (profile?.suspended == true) {
                    signOutUseCase()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            errorMessage = "Your account has been suspended. Contact support for more information."
                        )
                    }
                } else {
                    registerFcmTokenForUser(user.uid)
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                }
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Authentication failed"
                val isUnverified = errorMsg.contains("verify", ignoreCase = true)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                        showResendButton = isUnverified
                    )
                }
            }
        }
    }

    fun onSignUpClick(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, showResendButton = false) }
            val result = signUpUseCase(email.trim(), password)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        isSignUpMode = false,
                        successMessage = "Account created! A verification link has been sent to ${email.trim()}. Please verify your email before logging in.",
                        showResendButton = true
                    )
                }
                _password.value = ""
                _confirmPassword.value = ""
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Sign up failed") }
            }
        }
    }

    fun onForgotPasswordClick(email: String, onSent: () -> Unit = {}) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address to reset password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isResettingPassword = true, errorMessage = null, successMessage = null) }
            val result = sendPasswordResetEmailUseCase(targetEmail)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isResettingPassword = false,
                        successMessage = "Password reset link sent to $targetEmail. Please check your inbox.",
                        errorMessage = null
                    )
                }
                onSent()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isResettingPassword = false,
                        errorMessage = error.localizedMessage ?: "Failed to send reset email"
                    )
                }
            }
        }
    }

    fun onResendVerificationClick() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value
        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email and password to resend the verification link.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = resendVerificationEmailUseCase(currentEmail, currentPassword)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Verification email sent to $currentEmail! Please check your inbox and spam folder.",
                        showResendButton = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to resend verification email"
                    )
                }
            }
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, showResendButton = false) }
            val result = signInWithGoogleUseCase(idToken)
            result.onSuccess { user ->
                // Check suspension before granting access
                val profile = getUserProfileUseCase(user.uid)
                if (profile?.suspended == true) {
                    signOutUseCase()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            errorMessage = "Your account has been suspended. Contact support for more information."
                        )
                    }
                } else {
                    registerFcmTokenForUser(user.uid)
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                }
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
