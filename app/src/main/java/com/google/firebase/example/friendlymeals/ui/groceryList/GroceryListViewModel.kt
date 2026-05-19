package com.google.firebase.example.friendlymeals.ui.groceryList

import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import com.google.firebase.example.friendlymeals.data.repository.AuthRepository
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val databaseRepository: DatabaseRepository
) : MainViewModel() {
    private val _groceries = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceries: StateFlow<List<GroceryItem>> = _groceries.asStateFlow()

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
}
