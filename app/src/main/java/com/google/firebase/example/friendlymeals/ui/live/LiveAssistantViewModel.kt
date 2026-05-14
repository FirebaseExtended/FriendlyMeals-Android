package com.google.firebase.example.friendlymeals.ui.live

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import com.google.firebase.example.friendlymeals.data.repository.LiveAIRepository
import com.google.firebase.example.friendlymeals.ui.live.LiveAssistantUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
@OptIn(PublicPreviewAPI::class)
class LiveAssistantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val databaseRepository: DatabaseRepository,
    private val liveAIRepository: LiveAIRepository
) : MainViewModel() {
    private val route = savedStateHandle.toRoute<LiveAssistantRoute>()
    val recipeId: String = route.recipeId

    private val _uiState = MutableStateFlow<LiveAssistantUiState>(Loading)
    val uiState: StateFlow<LiveAssistantUiState> = _uiState.asStateFlow()

    private var liveSession: LiveSession? = null
    private var isConnected = false
    private var lastFrameTime = 0L

    init {
        loadRecipeAndConnect()
    }

    private fun loadRecipeAndConnect() {
        launchCatching {
            val recipe = databaseRepository.getRecipe(recipeId)

            if (recipe.title.isBlank()) {
                _uiState.value = LiveAssistantUiState.Error(RECIPE_ERROR)
            } else {
                _uiState.value = LiveAssistantUiState.Success(recipe)
                setupLiveSession(recipe)
            }
        }
    }

    private suspend fun setupLiveSession(recipe: Recipe) {
        val session = liveAIRepository.setupLiveSession(recipe)

        if (session == null) {
            _uiState.value = LiveAssistantUiState.Error(CONNECTION_ERROR)
        } else {
            liveSession = session
            isConnected = true
            startConversation()
        }
    }

    private fun handler(functionCall: FunctionCallPart): FunctionResponsePart {
        return FunctionResponsePart(functionCall.name, JsonObject(emptyMap()), functionCall.id)
    }

    // Suppressing MissingPermission warning as we're
    // checking permissions before opening the screen
    @SuppressLint("MissingPermission")
    private fun startConversation() {
        launchCatching {
            liveSession?.startAudioConversation(::handler)
        }
    }

    private fun endConversation() {
        launchCatching {
            liveSession?.stopAudioConversation()
        }
    }

    fun sendVideoFrame(bitmap: Bitmap) {
        if (!isConnected || liveSession == null) return
        val currentTime = System.currentTimeMillis()

        // Limit sending frames to once per second to conserve bandwidth and processing
        if (currentTime - lastFrameTime < 1000) return
        lastFrameTime = currentTime

        launchCatching {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val jpegBytes = outputStream.toByteArray()
            liveSession?.sendVideoRealtime(InlineData(jpegBytes, MIME_TYPE))
        }
    }

    override fun onCleared() {
        super.onCleared()
        endConversation()
    }

    companion object {
        private const val MIME_TYPE = "image/jpeg"
        private const val RECIPE_ERROR = "Failed to load recipe"
        private const val CONNECTION_ERROR = "Failed to connect to live assistant"
    }
}
