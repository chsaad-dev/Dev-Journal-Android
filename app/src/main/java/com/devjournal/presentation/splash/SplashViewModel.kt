package com.devjournal.presentation.splash

import androidx.lifecycle.ViewModel
import com.devjournal.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun isUserAuthenticated(): Boolean {
        return authRepository.currentUser != null
    }
}
