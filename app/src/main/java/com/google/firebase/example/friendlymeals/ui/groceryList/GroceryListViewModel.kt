package com.google.firebase.example.friendlymeals.ui.groceryList

import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import com.google.firebase.example.friendlymeals.data.repository.AuthRepository
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import com.google.firebase.example.friendlymeals.data.repository.AIRepository
import com.google.firebase.example.friendlymeals.data.schema.LocalStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

sealed interface LocalizerUiState {
    object Idle : LocalizerUiState
    object Loading : LocalizerUiState
    data class Success(val stores: List<LocalStore>) : LocalizerUiState
    data class Error(val message: String) : LocalizerUiState
}

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val databaseRepository: DatabaseRepository,
    private val aiRepository: AIRepository
) : MainViewModel() {
    private val _groceries = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceries: StateFlow<List<GroceryItem>> = _groceries.asStateFlow()

    private val _localizerState = MutableStateFlow<LocalizerUiState>(LocalizerUiState.Idle)
    val localizerState: StateFlow<LocalizerUiState> = _localizerState.asStateFlow()

    val userId: String get() = authRepository.currentUser?.uid.orEmpty()

    init {
        loadGroceries()
    }

    private fun loadGroceries() {
        if (userId.isEmpty()) return

        launchCatching {
            databaseRepository.getGroceriesFlow(userId)
                .catch { _ -> }
                .collect { items ->
                    _groceries.value = items
                }
        }
    }

    fun toggleItem(item: GroceryItem) {
        launchCatching {
            databaseRepository.updateGroceryItemChecked(item.id, !item.checked)
        }
    }

    fun addItem(name: String) {
        if (name.isBlank() || userId.isEmpty()) return

        launchCatching {
            val item = GroceryItem(
                userId = userId,
                name = name.trim(),
                checked = false
            )
            databaseRepository.addGroceryItem(item)
        }
    }

    fun deleteItem(item: GroceryItem) {
        launchCatching {
            databaseRepository.deleteGroceryItem(item.id)
        }
    }

    fun resetLocalizer() {
        _localizerState.value = LocalizerUiState.Idle
    }

    fun localizeGroceryList(latitude: Double, longitude: Double) {
        val uncheckedIngredients = _groceries.value
            .filter { !it.checked }
            .map { it.name }

        if (uncheckedIngredients.isEmpty()) {
            _localizerState.value = LocalizerUiState.Error(EMPTY_ITEMS_ERROR)
            return
        }

        _localizerState.value = LocalizerUiState.Loading

        launchCatching {
            val now = LocalDateTime.now()
            val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
            val currentTime = String.format(Locale.US, "%02d:%02d", now.hour, now.minute)

            val stores = aiRepository.localizeIngredients(
                ingredients = uncheckedIngredients,
                latitude = latitude,
                longitude = longitude,
                currentTime = currentTime,
                dayOfWeek = dayOfWeek
            )

            if (stores.isEmpty()) {
                _localizerState.value = LocalizerUiState.Error(EMPTY_STORE_ERROR)
            } else {
                _localizerState.value = LocalizerUiState.Success(stores)
            }
        }
    }
    
    companion object {
        private const val EMPTY_ITEMS_ERROR = "Your grocery list is empty or all items are checked."
        private const val EMPTY_STORE_ERROR = "Failed to fetch local stores."
    }
}
