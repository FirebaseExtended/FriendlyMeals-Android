package com.google.firebase.example.friendlymeals.ui.groceryList

import com.google.firebase.example.friendlymeals.data.schema.StoreSchema

sealed interface StoreFinderUiState {
    object Idle : StoreFinderUiState
    object Loading : StoreFinderUiState
    data class Success(val stores: List<StoreSchema>) : StoreFinderUiState
    data class Error(val message: String) : StoreFinderUiState
}