package com.google.firebase.example.friendlymeals.ui.groceryList

import com.google.firebase.example.friendlymeals.data.schema.StoreSchema

sealed interface StoreLocalizerUiState {
    object Idle : StoreLocalizerUiState
    object Loading : StoreLocalizerUiState
    data class Success(val stores: List<StoreSchema>) : StoreLocalizerUiState
    data class Error(val message: String) : StoreLocalizerUiState
}