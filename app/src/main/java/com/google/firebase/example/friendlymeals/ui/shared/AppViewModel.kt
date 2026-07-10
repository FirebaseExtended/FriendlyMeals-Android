package com.google.firebase.example.friendlymeals.ui.shared

import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : MainViewModel() {
    private val _isFirestoreAvailable = MutableStateFlow(true)
    val isFirestoreAvailable: StateFlow<Boolean>
        get() = _isFirestoreAvailable.asStateFlow()

    init {
        checkFirestoreAvailability()
    }

    private fun checkFirestoreAvailability() {
        launchCatchingIO {
            _isFirestoreAvailable.value = databaseRepository.isFirestoreAvailable()
        }
    }
}
