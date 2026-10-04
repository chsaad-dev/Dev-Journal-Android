package com.devjournal.domain.usecase

import com.devjournal.data.local.ThemePreferences
import com.devjournal.domain.model.ThemeMode
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    suspend operator fun invoke(mode: ThemeMode) {
        themePreferences.setThemeMode(mode.name.lowercase())
    }
}
