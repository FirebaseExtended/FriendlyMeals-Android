package com.google.firebase.example.friendlymeals.data.datasource

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject

@OptIn(PublicPreviewAPI::class)
class LiveAIRemoteDataSource @Inject constructor(
    private val aiModel: FirebaseAI,
    private val remoteConfig: FirebaseRemoteConfig
) {
    private val groceryListTool = Tool.functionDeclarations(listOf(
        FunctionDeclaration(
            name = ADD_INGREDIENTS_TOOL_NAME,
            description = ADD_INGREDIENTS_TOOL_DESCRIPTION,
            parameters = mapOf(
                INGREDIENT_FIELD_NAME to Schema.string(INGREDIENT_FIELD_DESCRIPTION)
            )
        )
    ))

    @OptIn(PublicPreviewAPI::class)
    suspend fun setupLiveSession(recipe: Recipe): LiveSession? {
        val liveGenerationConfig = liveGenerationConfig {
            speechConfig = SpeechConfig(voice = Voice(LIVE_MODEL_VOICE))
            responseModality = ResponseModality.AUDIO
        }

        // This is temporary, prompt will be moved to Remote Config soon
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
                If the user asks you to add an ingredient or item to their grocery list or shopping list, call the addIngredientToGroceryList function.
            """.trimIndent()

        val liveModel = aiModel.liveModel(
            modelName = remoteConfig.getString(LIVE_MODEL_NAME_KEY),
            generationConfig = liveGenerationConfig,
            systemInstruction = content { text(instructionText) },
            tools = listOf(groceryListTool)
        )

        return try {
            liveModel.connect()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        //Live Model Config
        private const val LIVE_MODEL_VOICE = "CHARON"

        //Tools config
        private const val ADD_INGREDIENTS_TOOL_NAME = "addIngredientToGroceryList"
        private const val ADD_INGREDIENTS_TOOL_DESCRIPTION = "Adds a specified ingredient to the " +
                "user's grocery list in the database."
        private const val INGREDIENT_FIELD_NAME = "ingredient"
        private const val INGREDIENT_FIELD_DESCRIPTION = "The name of the ingredient to add."

        //Remote Config Keys
        private const val LIVE_MODEL_NAME_KEY = "live_model_name"
        private const val LIVE_MODEL_PROMPT_KEY = "live_model_prompt"
    }
}