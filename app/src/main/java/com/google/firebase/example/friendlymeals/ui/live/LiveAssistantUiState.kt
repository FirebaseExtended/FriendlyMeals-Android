package com.google.firebase.example.friendlymeals.ui.live

import com.google.firebase.example.friendlymeals.data.model.Recipe

sealed interface LiveAssistantUiState {
    data object Loading : LiveAssistantUiState
    data class Success(val recipe: Recipe) : LiveAssistantUiState
    data class Error(val message: String) : LiveAssistantUiState
}
