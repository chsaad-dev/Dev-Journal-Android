package com.devjournal.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val HAS_SEEN_APP_GUIDE = booleanPreferencesKey("has_seen_app_guide")
    }

    val hasSeenAppGuide: Flow<Boolean> = context.appPreferencesDataStore.data.map { preferences ->
        preferences[HAS_SEEN_APP_GUIDE] ?: false
    }

    suspend fun getHasSeenAppGuide(): Boolean {
        return context.appPreferencesDataStore.data.first()[HAS_SEEN_APP_GUIDE] ?: false
    }

    suspend fun setHasSeenAppGuide(hasSeen: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[HAS_SEEN_APP_GUIDE] = hasSeen
        }
    }
}
