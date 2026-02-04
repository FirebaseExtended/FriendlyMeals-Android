package com.google.firebase.example.friendlymeals.data.datasource

import android.graphics.Bitmap
import com.google.firebase.ai.TemplateGenerativeModel
import com.google.firebase.ai.TemplateImagenModel
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.example.friendlymeals.data.schema.MealSchema
import com.google.firebase.example.friendlymeals.data.schema.RecipeSchema
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(PublicPreviewAPI::class)
class AIRemoteDataSource @Inject constructor(
    private val generativeModel: TemplateGenerativeModel,
    private val imagenModel: TemplateImagenModel,
    private val remoteConfig: FirebaseRemoteConfig
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateIngredients(imageData: String): String {
        val response = generativeModel.generateContent(
            templateId = remoteConfig.getString(GENERATE_INGREDIENTS_KEY),
            inputs = mapOf(
                MIME_TYPE_FIELD to MIME_TYPE_VALUE,
                IMAGE_DATA_FIELD to imageData
            )
        )

        return response.text.orEmpty()
    }

    suspend fun generateRecipe(ingredients: String, notes: String): RecipeSchema? {
        val response = generativeModel.generateContent(
            templateId = remoteConfig.getString(GENERATE_RECIPE_KEY),
            inputs = buildMap {
                put(INGREDIENTS_FIELD, ingredients)
                if (notes.isNotBlank()) {
                    put(NOTES_FIELD, notes)
                }
            }
        )

        return response.text?.let {
            json.decodeFromString<RecipeSchema>(it)
        }
    }

    suspend fun generateRecipePhoto(recipeTitle: String): Bitmap? {
        val response = generativeModel.generateContent(
            templateId = remoteConfig.getString(GENERATE_RECIPE_PHOTO_GEMINI_KEY),
            inputs = mapOf(RECIPE_TITLE_FIELD to recipeTitle)
        )

        return response.candidates.firstOrNull()?.content?.parts
            ?.filterIsInstance<ImagePart>()?.firstOrNull()?.image
    }

    suspend fun generateRecipePhotoImagen(recipeTitle: String): Bitmap? {
        val response = imagenModel.generateImages(
            templateId = remoteConfig.getString(GENERATE_RECIPE_PHOTO_IMAGEN_KEY),
            inputs = mapOf(RECIPE_TITLE_FIELD to recipeTitle)
        )

        return response.images.firstOrNull()?.asBitmap()
    }

    suspend fun scanMeal(imageData: String): MealSchema? {
        val response = generativeModel.generateContent(
            templateId = remoteConfig.getString(SCAN_MEAL_KEY),
            inputs = mapOf(
                MIME_TYPE_FIELD to MIME_TYPE_VALUE,
                IMAGE_DATA_FIELD to imageData
            )
        )

        return response.text?.let {
            json.decodeFromString<MealSchema>(it)
        }
    }

    companion object {
        //Remote Config Keys
        private const val GENERATE_INGREDIENTS_KEY = "generate_ingredients"
        private const val GENERATE_RECIPE_KEY = "generate_recipe"
        private const val GENERATE_RECIPE_PHOTO_GEMINI_KEY = "generate_recipe_photo_gemini"
        private const val GENERATE_RECIPE_PHOTO_IMAGEN_KEY = "generate_recipe_photo_imagen"
        private const val SCAN_MEAL_KEY = "scan_meal"

        //Template input fields
        private const val IMAGE_DATA_FIELD = "imageData"
        private const val MIME_TYPE_FIELD = "mimeType"
        private const val INGREDIENTS_FIELD = "ingredients"
        private const val NOTES_FIELD = "notes"
        private const val RECIPE_TITLE_FIELD = "recipeTitle"

        //Template input values
        private const val MIME_TYPE_VALUE = "image/jpeg"
    }
}