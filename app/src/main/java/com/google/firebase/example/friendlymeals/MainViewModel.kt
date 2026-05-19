package com.google.firebase.example.friendlymeals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class MainViewModel : ViewModel() {
    fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.e("MainViewModel", throwable.message ?: "Unknown error")
            },
            block = block
        )

    fun launchCatchingIO(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(
            Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                Log.e("MainViewModel", throwable.message ?: "Unknown error")
            },
            block = block
        )
}