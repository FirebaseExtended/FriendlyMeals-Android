package com.google.firebase.example.friendlymeals.data.repository

import android.graphics.Bitmap
import com.google.firebase.example.friendlymeals.data.datasource.AIRemoteDataSource
import com.google.firebase.example.friendlymeals.data.schema.MealSchema
import com.google.firebase.example.friendlymeals.data.schema.RecipeSchema
import com.google.firebase.example.friendlymeals.data.schema.LocalStore
import javax.inject.Inject

class AIRepository @Inject constructor(
    private val aiRemoteDataSource: AIRemoteDataSource
) {
    suspend fun generateIngredients(image: Bitmap): String {
        return aiRemoteDataSource.generateIngredients(image)
    }

    suspend fun localizeIngredients(
        ingredients: List<String>,
        latitude: Double,
        longitude: Double,
        currentTime: String,
        dayOfWeek: String
    ): List<LocalStore> {
        return aiRemoteDataSource.localizeIngredients(
            ingredients,
            latitude,
            longitude,
            currentTime,
            dayOfWeek
        )
    }

    suspend fun generateRecipe(ingredients: String, notes: String): RecipeSchema? {
        return aiRemoteDataSource.generateRecipe(ingredients, notes)
    }

    suspend fun generateRecipePhoto(recipeTitle: String): Bitmap? {
        return aiRemoteDataSource.generateRecipePhoto(recipeTitle)
    }

    suspend fun scanMeal(imageData: String): MealSchema? {
        return aiRemoteDataSource.scanMeal(imageData)
    }

    suspend fun loadOnDeviceModel() {
        aiRemoteDataSource.loadOnDeviceModel()
    }
}