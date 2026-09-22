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
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.example.friendlymeals.data.schema.MealSchema
import com.google.firebase.example.friendlymeals.data.schema.RecipeSchema
import com.google.firebase.perf.performance
import com.google.firebase.perf.trace
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.TemplateTool
import com.google.firebase.ai.type.TemplateToolConfig
import com.google.firebase.ai.type.retrievalConfig
import com.google.firebase.example.friendlymeals.data.schema.StoreFinderResult
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

    suspend fun findStores(
        ingredients: List<String>,
        latitude: Double,
        longitude: Double,
        currentTime: String,
        dayOfWeek: String
    ): List<StoreSchema> {
        val groundingModel = aiModel.templateGenerativeModel(
            tools = listOf(TemplateTool.googleMaps()),
            toolConfig = TemplateToolConfig(
                retrievalConfig = retrievalConfig {
                    latLng = LatLng(latitude = latitude, longitude = longitude)
                    languageCode = LANGUAGE
                }
            )
        )

        return try {
            val response = groundingModel.generateContent(
                templateId = remoteConfig.getString(FIND_STORES_KEY),
                inputs = mapOf(
                    INGREDIENTS_FIELD to ingredients.joinToString(),
                    DAY_FIELD to dayOfWeek,
                    TIME_FIELD to currentTime
                )
            )

            val rawText = response.text ?: return emptyList()
            
            val cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            json.decodeFromString<StoreFinderResult>(cleanJson).stores
        } catch (e: Exception) {
            Log.e(TAG, "Error finding stores with these ingredients", e)
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

    suspend fun craftRecipePairing(dishTitle: String, ingredients: List<String>): String {
        val model = aiModel.generativeModel(
            modelName = remoteConfig.getString(HYBRID_CLOUD_MODEL_KEY)
        )
        val prompt = """
            Craft a concise, expert wine pairing recommendation (strictly 2 or 3 sentences) for the dish "$dishTitle" (key ingredients: ${ingredients.joinToString()}).
            Recommend the ideal French wine that pairs nicely with this dish, and explain why their flavor profiles and characteristics complement each other. It should ALWAYS be paired with a FRENCH WINE.
            Do not use markdown formatting or bullet points; write in clear conversational prose suitable for being read aloud by a French chef. Keep it strictly to 2 or 3 sentences.
        """.trimIndent()

        val response = model.generateContent(prompt)
        return response.text.orEmpty().trim()
    }

    suspend fun generateSpeech(text: String?): ByteArray? {
        if (text.isNullOrBlank()) return null

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO)
            speechConfig = SpeechConfig(
                voice = Voice(TTS_VOICE),
                languageCode = LANGUAGE_CODE
            )
        }

        val model = aiModel.generativeModel(
            modelName = TTS_MODEL_NAME,
            generationConfig = config
        )

        val prompt = """
            [Audio Profile: A French chef who is an expert in pairing with wines, with a charming French accent]
            [Scene: An elegant Parisian restaurant during a masterclass on food and wine pairing]
            [Director's Notes: Speak as if you're a French chef who is an expert in pairing with wines. It should ALWAYS be paired with a FRENCH WINE. The speaker should have a charming French accent, with warm enthusiasm, culinary sophistication, and flair.]
            $text
        """.trimIndent()

        val response = model.generateContent(prompt)
        val part = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
        return (part as? InlineDataPart)?.inlineData
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
        private const val FIND_STORES_KEY = "find_stores"
        private const val HYBRID_CLOUD_MODEL_KEY = "hybrid_cloud_model"
        private const val HYBRID_INGREDIENTS_PROMPT_KEY = "hybrid_ingredients_prompt"

        //Template input fields
        private const val IMAGE_DATA_FIELD = "imageData"
        private const val MIME_TYPE_FIELD = "mimeType"
        private const val INGREDIENTS_FIELD = "ingredients"
        private const val NOTES_FIELD = "notes"
        private const val RECIPE_TITLE_FIELD = "recipeTitle"
        private const val DAY_FIELD = "dayOfWeek"
        private const val TIME_FIELD = "currentTime"

        //Template input values
        private const val MIME_TYPE_VALUE = "image/jpeg"

        //Grounding with Maps config
        private const val LANGUAGE = "en_US"

        //TTS Config
        private const val TTS_MODEL_NAME = "gemini-3.1-flash-tts-preview"
        private const val TTS_VOICE = "Charon"
        private const val LANGUAGE_CODE = "en-US"

        //Class TAG
        private const val TAG = "AIRemoteDataSource"
    }
}