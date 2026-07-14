package com.google.firebase.example.friendlymeals.data.repository

import com.google.firebase.example.friendlymeals.data.datasource.DatabaseRemoteDataSource
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.model.Review
import com.google.firebase.example.friendlymeals.data.model.Like
import com.google.firebase.example.friendlymeals.data.model.User
import com.google.firebase.example.friendlymeals.ui.recipeList.RecipeListItem
import com.google.firebase.example.friendlymeals.ui.recipeList.filter.FilterOptions
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject

class DatabaseRepository @Inject constructor(
    private val databaseRemoteDataSource: DatabaseRemoteDataSource
) {
    companion object {
        @Volatile
        private var isFirestoreSetupChecked: Boolean? = null

        private val localRecipes = java.util.concurrent.ConcurrentHashMap<String, Recipe>()
    }

    suspend fun isFirestoreSetup(): Boolean {
        isFirestoreSetupChecked?.let { return it }
        val available = isFirestoreAvailable()
        isFirestoreSetupChecked = available
        return available
    }

    suspend fun addUser(user: User) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.addUser(user)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun addRecipe(recipe: Recipe): String {
        return if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.addRecipe(recipe)
            } catch (e: Exception) {
                saveRecipeLocally(recipe)
            }
        } else {
            saveRecipeLocally(recipe)
        }
    }

    private fun saveRecipeLocally(recipe: Recipe): String {
        val localId = "local_" + UUID.randomUUID().toString()
        localRecipes[localId] = recipe
        return localId
    }

    suspend fun getRecipe(recipeId: String): Recipe {
        if (recipeId.startsWith("local_") || !isFirestoreSetup()) {
            localRecipes[recipeId]?.let { return it }
        }
        return databaseRemoteDataSource.getRecipe(recipeId)
    }

    suspend fun getAllRecipes(): List<RecipeListItem> {
        return if (isFirestoreSetup()) {
            try {
                val remote = databaseRemoteDataSource.getAllRecipes()
                val local = getLocalRecipeListItems()
                local + remote
            } catch (e: Exception) {
                getLocalRecipeListItems()
            }
        } else {
            getLocalRecipeListItems()
        }
    }

    private fun getLocalRecipeListItems(): List<RecipeListItem> {
        return localRecipes.map { (id, recipe) ->
            RecipeListItem(
                id = id,
                title = recipe.title,
                averageRating = 0.0,
                imageUri = recipe.imageUri
            )
        }
    }

    suspend fun getPopularTags(): List<String> {
        return if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.getPopularTags()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun setReview(review: Review) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.setReview(review)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun getRating(userId: String, recipeId: String): Int {
        return if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.getRating(userId, recipeId)
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }

    suspend fun setFavorite(like: Like) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.setFavorite(like)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun removeFavorite(like: Like) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.removeFavorite(like)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun getFavorite(userId: String, recipeId: String): Boolean {
        return if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.getFavorite(userId, recipeId)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    suspend fun getFilteredRecipes(
        filterOptions: FilterOptions,
        userId: String
    ): List<RecipeListItem> {
        return if (isFirestoreSetup()) {
            try {
                val remote = databaseRemoteDataSource.getFilteredRecipes(filterOptions, userId)
                val local = getLocalRecipeListItems()
                local + remote
            } catch (e: Exception) {
                getLocalRecipeListItems()
            }
        } else {
            getLocalRecipeListItems()
        }
    }

    fun getGroceriesFlow(userId: String): Flow<List<GroceryItem>> {
        return try {
            databaseRemoteDataSource.getGroceriesFlow(userId)
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun addGroceryItem(item: GroceryItem) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.addGroceryItem(item)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun updateGroceryItemChecked(itemId: String, checked: Boolean) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.updateGroceryItemChecked(itemId, checked)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun deleteGroceryItem(itemId: String) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.deleteGroceryItem(itemId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun addIngredientsToGroceries(userId: String, ingredients: List<String>) {
        if (isFirestoreSetup()) {
            try {
                databaseRemoteDataSource.addIngredientsToGroceries(userId, ingredients)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun isFirestoreAvailable(): Boolean {
        return databaseRemoteDataSource.isFirestoreAvailable()
    }
}