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
import javax.inject.Inject

class DatabaseRepository @Inject constructor(
    private val databaseRemoteDataSource: DatabaseRemoteDataSource
) {
    suspend fun addUser(user: User) {
        databaseRemoteDataSource.addUser(user)
    }

    suspend fun addRecipe(recipe: Recipe): String {
        return databaseRemoteDataSource.addRecipe(recipe)
    }

    suspend fun getRecipe(recipeId: String): Recipe {
        return databaseRemoteDataSource.getRecipe(recipeId)
    }

    suspend fun getAllRecipes(): List<RecipeListItem> {
        return databaseRemoteDataSource.getAllRecipes()
    }

    suspend fun getPopularTags(): List<String> {
        return databaseRemoteDataSource.getPopularTags()
    }

    suspend fun setReview(review: Review) {
        databaseRemoteDataSource.setReview(review)
    }

    suspend fun getRating(userId: String, recipeId: String): Int {
        return databaseRemoteDataSource.getRating(userId, recipeId)
    }

    suspend fun setFavorite(like: Like) {
        databaseRemoteDataSource.setFavorite(like)
    }

    suspend fun removeFavorite(like: Like) {
        databaseRemoteDataSource.removeFavorite(like)
    }

    suspend fun getFavorite(userId: String, recipeId: String): Boolean {
        return databaseRemoteDataSource.getFavorite(userId, recipeId)
    }

    suspend fun getFilteredRecipes(
        filterOptions: FilterOptions,
        userId: String
    ): List<RecipeListItem> {
        return databaseRemoteDataSource.getFilteredRecipes(filterOptions, userId)
    }

    fun getGroceriesFlow(userId: String): Flow<List<GroceryItem>> {
        return databaseRemoteDataSource.getGroceriesFlow(userId)
    }

    suspend fun addGroceryItem(item: GroceryItem) {
        databaseRemoteDataSource.addGroceryItem(item)
    }

    suspend fun updateGroceryItemChecked(itemId: String, checked: Boolean) {
        databaseRemoteDataSource.updateGroceryItemChecked(itemId, checked)
    }

    suspend fun deleteGroceryItem(itemId: String) {
        databaseRemoteDataSource.deleteGroceryItem(itemId)
    }

    suspend fun addIngredientsToGroceries(userId: String, ingredients: List<String>) {
        databaseRemoteDataSource.addIngredientsToGroceries(userId, ingredients)
    }
}