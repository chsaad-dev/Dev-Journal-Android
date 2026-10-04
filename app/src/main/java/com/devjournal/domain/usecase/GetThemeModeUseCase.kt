package com.devjournal.domain.usecase

import com.devjournal.data.local.ThemePreferences
import com.devjournal.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    operator fun invoke(): Flow<ThemeMode> {
        return themePreferences.themeMode.map { ThemeMode.fromString(it) }
    }
}
