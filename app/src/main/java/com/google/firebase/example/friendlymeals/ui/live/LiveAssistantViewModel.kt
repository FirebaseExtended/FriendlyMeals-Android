package com.google.firebase.example.friendlymeals.ui.live

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
@OptIn(PublicPreviewAPI::class)
class LiveAssistantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val databaseRepository: DatabaseRepository,
    private val firebaseAI: FirebaseAI
) : MainViewModel() {
    private val route = savedStateHandle.toRoute<LiveAssistantRoute>()
    val recipeId: String = route.recipeId

    private val _uiState = MutableStateFlow<LiveAssistantUiState>(LiveAssistantUiState.Loading)
    val uiState: StateFlow<LiveAssistantUiState> = _uiState.asStateFlow()

    private var liveSession: LiveSession? = null
    private var isConnected = false
    private var lastFrameTime = 0L

    init {
        loadRecipeAndConnect()
    }

    private fun loadRecipeAndConnect() {
        viewModelScope.launch {
            try {
                val recipe = databaseRepository.getRecipe(recipeId)
                _uiState.value = LiveAssistantUiState.Success(recipe)
                setupAndConnectLiveSession(recipe)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recipe for Live Assistant", e)
                _uiState.value = LiveAssistantUiState.Error(e.message ?: "Failed to load recipe")
            }
        }
    }

    private suspend fun setupAndConnectLiveSession(recipe: Recipe) {
        try {
            val liveGenerationConfig = liveGenerationConfig {
                speechConfig = SpeechConfig(voice = Voice("CHARON"))
                responseModality = ResponseModality.AUDIO
            }

            val instructionText = """
                You are a helpful live cooking assistant. The user is currently preparing the following recipe:
                Title: ${recipe.title}
                Prep time: ${recipe.prepTime}, Cook time: ${recipe.cookTime}, Servings: ${recipe.servings}
                
                Ingredients:
                ${recipe.ingredients.joinToString("\n")}
                
                Instructions:
                ${recipe.instructions}
                
                The user will stream real-time video of their cooking and ask questions like "Is this the expected texture of the recipe?".
                Confirm or deny accurately based on the recipe context and the video content. Be concise and helpful.
            """.trimIndent()

            val liveModel = firebaseAI.liveModel(
                modelName = "gemini-2.5-flash-native-audio-preview-09-2025",
                generationConfig = liveGenerationConfig,
                systemInstruction = content { text(instructionText) }
            )

            withContext(Dispatchers.IO) {
                liveSession = liveModel.connect()
            }
            isConnected = true
            startConversation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect LiveSession", e)
            _uiState.value = LiveAssistantUiState.Error("Failed to connect to live assistant: ${e.message}")
        }
    }

    private fun handler(functionCall: FunctionCallPart): FunctionResponsePart {
        return FunctionResponsePart(functionCall.name, JsonObject(emptyMap()), functionCall.id)
    }

    @SuppressLint("MissingPermission")
    fun startConversation() {
        viewModelScope.launch {
            try {
                liveSession?.startAudioConversation(::handler)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio conversation", e)
            }
        }
    }

    fun endConversation() {
        try {
            liveSession?.stopAudioConversation()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio conversation", e)
        }
    }

    fun sendVideoFrame(bitmap: Bitmap) {
        if (!isConnected || liveSession == null) return
        val currentTime = System.currentTimeMillis()
        // Limit sending frames to once per second to conserve bandwidth and processing
        if (currentTime - lastFrameTime < 1000) return
        lastFrameTime = currentTime

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val jpegBytes = outputStream.toByteArray()
                liveSession?.sendVideoRealtime(InlineData(jpegBytes, "image/jpeg"))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending video frame", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        endConversation()
    }

    companion object {
        private const val TAG = "LiveAssistantViewModel"
    }
}
