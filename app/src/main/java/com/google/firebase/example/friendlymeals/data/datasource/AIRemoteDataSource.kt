package com.google.firebase.example.friendlymeals.data.datasource

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.DownloadStatus.DownloadCompleted
import com.google.firebase.ai.DownloadStatus.DownloadFailed
import com.google.firebase.ai.DownloadStatus.DownloadInProgress
import com.google.firebase.ai.DownloadStatus.DownloadStarted
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.OnDeviceModelStatus.Companion.AVAILABLE
import com.google.firebase.ai.OnDeviceModelStatus.Companion.DOWNLOADABLE
import com.google.firebase.ai.OnDeviceModelStatus.Companion.DOWNLOADING
import com.google.firebase.ai.OnDeviceModelStatus.Companion.UNAVAILABLE
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.content
import com.google.firebase.example.friendlymeals.data.schema.MealSchema
import com.google.firebase.example.friendlymeals.data.schema.RecipeSchema
import com.google.firebase.example.friendlymeals.data.schema.StoreLocalizerResult
import com.google.firebase.perf.performance
import com.google.firebase.perf.trace
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.retrievalConfig
import com.google.firebase.example.friendlymeals.data.schema.StoreSchema

@OptIn(PublicPreviewAPI::class)
class AIRemoteDataSource @Inject constructor(
    private val aiModel: FirebaseAI,
    private val remoteConfig: FirebaseRemoteConfig
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val hybridGenerativeModel = aiModel.generativeModel(
        modelName = remoteConfig.getString(HYBRID_CLOUD_MODEL_KEY),
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_IN_CLOUD)
    )

    private val templateGenerativeModel = aiModel.templateGenerativeModel()

    suspend fun localizeIngredients(
        ingredients: List<String>,
        latitude: Double,
        longitude: Double,
        currentTime: String,
        dayOfWeek: String
    ): List<StoreSchema> {
        val groundingModel = aiModel.generativeModel(
            modelName = remoteConfig.getString(GROUNDING_MODEL_KEY),
            tools = listOf(Tool.googleMaps()),
            toolConfig = ToolConfig(
                retrievalConfig = retrievalConfig {
                    latLng = LatLng(latitude = latitude, longitude = longitude)
                    languageCode = LANGUAGE
                }
            )
        )

        val groundingPrompt = remoteConfig.getString(GROUNDING_PROMPT_KEY)
            .replace("{{ingredients}}", ingredients.joinToString(", "))
            .replace("{{dayOfWeek}}", dayOfWeek)
            .replace("{{currentTime}}", currentTime)

        return try {
            val response = groundingModel.generateContent(groundingPrompt)
            val rawText = response.text ?: return emptyList()
            
            val cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = json.decodeFromString<StoreLocalizerResult>(cleanJson)
            val groundingChunks = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks

            result.stores.map { store ->
                val matchingChunk = groundingChunks?.find { chunk ->
                    val chunkTitle = chunk.maps?.title.orEmpty().lowercase()
                    val storeName = store.name.lowercase()
                    chunkTitle.contains(storeName) || storeName.contains(chunkTitle)
                }
                store.copy(mapUrl = matchingChunk?.maps?.uri.orEmpty())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error localizing ingredients", e)
            emptyList()
        }
    }

    suspend fun generateIngredients(image: Bitmap): String {
        // Adding a Performance Monitoring trace is completely optional. Traces can help you
        // measure how long it takes to generate ingredients on device and in cloud.
        Firebase.performance.newTrace("hybrid-inference").trace {
            val prompt = content {
                image(image)
                text(remoteConfig.getString(HYBRID_INGREDIENTS_PROMPT_KEY))
            }

            val response = hybridGenerativeModel.generateContent(prompt)

            // This is an optional function that adds an attribute to the Performance Monitoring
            // trace. It helps you identify the source of the inference.
            putAttribute(
                "inferenceSource",
                when (response.inferenceSource) {
                    InferenceSource.ON_DEVICE -> "On device"
                    else -> "In cloud"
                }
            )

            return response.text.orEmpty()
        }
    }

    suspend fun generateRecipe(ingredients: String, notes: String): RecipeSchema? {
        val response = templateGenerativeModel.generateContent(
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
        val response = templateGenerativeModel.generateContent(
            templateId = remoteConfig.getString(GENERATE_RECIPE_PHOTO_GEMINI_KEY),
            inputs = mapOf(RECIPE_TITLE_FIELD to recipeTitle)
        )

        return response.candidates.firstOrNull()?.content?.parts
            ?.filterIsInstance<ImagePart>()?.firstOrNull()?.image
    }

    suspend fun scanMeal(imageData: String): MealSchema? {
        val response = templateGenerativeModel.generateContent(
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

    suspend fun loadOnDeviceModel() {
        when (hybridGenerativeModel.onDeviceExtension?.checkStatus()) {
            UNAVAILABLE -> {
                Log.i(TAG, "On-device model is unavailable")
            }
            DOWNLOADABLE -> {
                hybridGenerativeModel.onDeviceExtension?.download()?.collect { status ->
                    when (status) {
                        is DownloadStarted ->
                            Log.i(TAG, "Starting download - ${status.bytesToDownload}")

                        is DownloadInProgress ->
                            Log.i(TAG, "Download in progress ${status.totalBytesDownloaded} bytes downloaded")

                        is DownloadCompleted ->
                            Log.i(TAG, "On-device model download complete")

                        is DownloadFailed ->
                            Log.e(TAG, "Download failed $status")
                    }
                }
            }
            DOWNLOADING -> {
                Log.i(TAG, "On-device model is being downloaded")
            }
            AVAILABLE -> {
                Log.i(TAG, "On-device model is available")
            }
        }
    }

    companion object {
        //Remote Config Keys
        private const val GENERATE_RECIPE_KEY = "generate_recipe"
        private const val GENERATE_RECIPE_PHOTO_GEMINI_KEY = "generate_recipe_photo_gemini"
        private const val SCAN_MEAL_KEY = "scan_meal"
        private const val HYBRID_CLOUD_MODEL_KEY = "hybrid_cloud_model"
        private const val HYBRID_INGREDIENTS_PROMPT_KEY = "hybrid_ingredients_prompt"
        private const val GROUNDING_MODEL_KEY = "grounding_model"
        private const val GROUNDING_PROMPT_KEY = "grounding_prompt"

        //Template input fields
        private const val IMAGE_DATA_FIELD = "imageData"
        private const val MIME_TYPE_FIELD = "mimeType"
        private const val INGREDIENTS_FIELD = "ingredients"
        private const val NOTES_FIELD = "notes"
        private const val RECIPE_TITLE_FIELD = "recipeTitle"

        //Template input values
        private const val MIME_TYPE_VALUE = "image/jpeg"

        //Grounding with Maps config
        private const val LANGUAGE = "en_US"

        //Class TAG
        private const val TAG = "AIRemoteDataSource"
    }
}