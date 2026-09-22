package com.google.firebase.example.friendlymeals.ui.recipe

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.google.firebase.example.friendlymeals.MainViewModel
import com.google.firebase.example.friendlymeals.data.model.Like
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.model.Review
import com.google.firebase.example.friendlymeals.data.repository.AIRepository
import com.google.firebase.example.friendlymeals.data.repository.AuthRepository
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import com.google.firebase.example.friendlymeals.ui.shared.AudioComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val databaseRepository: DatabaseRepository,
    private val aiRepository: AIRepository
) : MainViewModel() {
    private val recipeRoute = savedStateHandle.toRoute<RecipeRoute>()
    private val recipeId: String = recipeRoute.recipeId
    private val audioComponent = AudioComponent()
    private var cachedAudioData: ByteArray? = null
    private var cachedPairingText: String? = null
    private var pairingGenerationJob: Job? = null
    private var isUserWaitingForAudio: Boolean = false

    private val _recipeViewState = MutableStateFlow(RecipeViewState())
    val recipeViewState: StateFlow<RecipeViewState>
        get() = _recipeViewState.asStateFlow()

    val userId: String get() = authRepository.currentUser?.uid.orEmpty()

    init {
        loadRecipe()
    }

    fun loadRecipe() {
        launchCatching {
            val recipe = databaseRepository.getRecipe(recipeId)
            _recipeViewState.value = RecipeViewState(
                recipeId = recipeId,
                recipe = recipe,
                favorite = loadFavorite(),
                rating = loadRating(),
                audioState = RecipeAudioState.Idle
            )

            // First try to read from the field "pairing" in the "recipe" document
            if (!recipe.pairing.isNullOrBlank()) {
                // If the field "pairing" is not empty or null, and contains a text, stop the logic.
                cachedPairingText = recipe.pairing
            } else {
                // If the "pairing" field is null or empty, generate the pairing recommendation
                // and update the firestore document with it.
                preparePairingInBackground(recipe)
            }
        }
    }

    private fun preparePairingInBackground(recipe: Recipe) {
        pairingGenerationJob = launchCatching {
            try {
                val pairing = aiRepository.craftRecipePairing(recipe.title, recipe.ingredients)
                if (pairing.isNotBlank()) {
                    cachedPairingText = pairing
                    _recipeViewState.value = _recipeViewState.value.copy(
                        recipe = _recipeViewState.value.recipe.copy(pairing = pairing)
                    )

                    if (isUserWaitingForAudio) {
                        isUserWaitingForAudio = false
                        generateAndPlayAudio(pairing)
                    }
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Failed to generate recipe pairing in background", e)
                if (isUserWaitingForAudio) {
                    isUserWaitingForAudio = false
                    _recipeViewState.value = _recipeViewState.value.copy(
                        audioState = RecipeAudioState.Error(e.message ?: "Failed to load audio")
                    )
                }
            }
        }
    }

    fun onLearnMoreClick() {
        when (_recipeViewState.value.audioState) {
            is RecipeAudioState.Playing -> {
                audioComponent.pause()
                _recipeViewState.value = _recipeViewState.value.copy(
                    audioState = RecipeAudioState.Paused
                )
            }
            is RecipeAudioState.Paused -> {
                audioComponent.resume()
                _recipeViewState.value = _recipeViewState.value.copy(
                    audioState = RecipeAudioState.Playing
                )
            }
            is RecipeAudioState.LoadingAudio -> {
                // Already loading audio, do nothing
            }
            is RecipeAudioState.Idle, is RecipeAudioState.Error -> {
                if (cachedAudioData != null) {
                    playCachedAudio()
                } else if (!cachedPairingText.isNullOrBlank()) {
                    launchCatching {
                        generateAndPlayAudio(cachedPairingText!!)
                    }
                } else {
                    // Pairing text is still generating in the background
                    isUserWaitingForAudio = true
                    _recipeViewState.value = _recipeViewState.value.copy(
                        audioState = RecipeAudioState.LoadingAudio
                    )
                }
            }
        }
    }

    private fun playCachedAudio() {
        val audioData = cachedAudioData ?: return
        _recipeViewState.value = _recipeViewState.value.copy(audioState = RecipeAudioState.Playing)
        audioComponent.play(audioData) {
            _recipeViewState.value = _recipeViewState.value.copy(audioState = RecipeAudioState.Idle)
        }
    }

    private suspend fun generateAndPlayAudio(pairingText: String) {
        _recipeViewState.value = _recipeViewState.value.copy(audioState = RecipeAudioState.LoadingAudio)
        try {
            val audio = aiRepository.generateSpeech(pairingText)
            if (audio != null && audio.isNotEmpty()) {
                cachedAudioData = audio
                _recipeViewState.value = _recipeViewState.value.copy(audioState = RecipeAudioState.Playing)
                audioComponent.play(audio) {
                    _recipeViewState.value = _recipeViewState.value.copy(audioState = RecipeAudioState.Idle)
                }
            } else {
                _recipeViewState.value = _recipeViewState.value.copy(
                    audioState = RecipeAudioState.Error("Could not generate audio")
                )
            }
        } catch (e: Exception) {
            _recipeViewState.value = _recipeViewState.value.copy(
                audioState = RecipeAudioState.Error(e.message ?: "Failed to generate audio")
            )
        }
    }

    private suspend fun loadFavorite(): Boolean {
        return databaseRepository.getFavorite(userId, recipeId)
    }

    private suspend fun loadRating(): Int {
        return databaseRepository.getRating(userId, recipeId)
    }

    fun toggleFavorite() {
        val like = Like(
            recipeId = recipeId,
            userId = userId
        )

        launchCatching {
            if (_recipeViewState.value.favorite) {
                databaseRepository.removeFavorite(like)
            } else {
                databaseRepository.setFavorite(like)
            }

            _recipeViewState.value = _recipeViewState.value.copy(
                favorite = loadFavorite()
            )
        }
    }

    fun leaveReview(rating: Int) {
        launchCatching {
            databaseRepository.setReview(
                Review(
                    userId = userId,
                    recipeId = recipeId,
                    rating = rating
                )
            )

            _recipeViewState.value = _recipeViewState.value.copy(
                rating = loadRating()
            )
        }
    }

    fun addIngredientsToGroceryList(ingredients: List<String>, onSuccess: () -> Unit) {
        if (userId.isEmpty() || ingredients.isEmpty()) return

        launchCatching {
            databaseRepository.addIngredientsToGroceries(userId, ingredients)
            onSuccess()
        }
    }

    override fun onCleared() {
        audioComponent.release()
        super.onCleared()
    }
}