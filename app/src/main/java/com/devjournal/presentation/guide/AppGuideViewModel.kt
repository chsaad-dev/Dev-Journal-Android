package com.devjournal.presentation.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppGuideViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    fun markGuideAsSeen() {
        viewModelScope.launch {
            appPreferences.setHasSeenAppGuide(true)
        }
    }
}
