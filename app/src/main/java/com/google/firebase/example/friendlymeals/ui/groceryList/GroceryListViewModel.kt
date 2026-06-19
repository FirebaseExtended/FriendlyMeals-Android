package com.google.firebase.example.friendlymeals.ui.groceryList

import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import com.google.firebase.example.friendlymeals.data.repository.AuthRepository
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import com.google.firebase.example.friendlymeals.data.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val databaseRepository: DatabaseRepository,
    private val aiRepository: AIRepository
) : MainViewModel() {
    private val _groceries = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceries: StateFlow<List<GroceryItem>> = _groceries.asStateFlow()

    private val _uiState = MutableStateFlow<StoreLocalizerUiState>(StoreLocalizerUiState.Idle)
    val uiState: StateFlow<StoreLocalizerUiState> = _uiState.asStateFlow()

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
        _uiState.value = StoreLocalizerUiState.Idle
    }

    fun localizeGroceryList(latitude: Double, longitude: Double) {
        val uncheckedIngredients = _groceries.value
            .filter { !it.checked }
            .map { it.name }

        if (uncheckedIngredients.isEmpty()) {
            _uiState.value = StoreLocalizerUiState.Error(EMPTY_ITEMS_ERROR)
            return
        }

        _uiState.value = StoreLocalizerUiState.Loading

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
                _uiState.value = StoreLocalizerUiState.Error(EMPTY_STORE_ERROR)
            } else {
                _uiState.value = StoreLocalizerUiState.Success(stores)
            }
        }
    }
    
    companion object {
        private const val EMPTY_ITEMS_ERROR = "Your grocery list is empty or all items are checked."
        private const val EMPTY_STORE_ERROR = "Failed to fetch local stores."
    }
}
