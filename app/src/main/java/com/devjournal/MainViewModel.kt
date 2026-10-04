package com.devjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.domain.model.ThemeMode
import com.devjournal.domain.usecase.GetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )
}
