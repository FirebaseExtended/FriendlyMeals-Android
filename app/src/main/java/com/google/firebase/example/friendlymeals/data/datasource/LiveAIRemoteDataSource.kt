package com.google.firebase.example.friendlymeals.data.datasource

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.AutoFunctionDeclaration
import com.google.firebase.ai.type.FirebaseAutoFunctionException
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.repository.AuthRepository
import com.google.firebase.example.friendlymeals.data.repository.DatabaseRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.collections.mapOf

@OptIn(PublicPreviewAPI::class)
class LiveAIRemoteDataSource @Inject constructor(
    private val aiModel: FirebaseAI,
    private val remoteConfig: FirebaseRemoteConfig,
    private val databaseRepository: DatabaseRepository,
    private val authRepository: AuthRepository
) {
    private val groceryListAutoTool = Tool.functionDeclarations(
        autoFunctionDeclarations = listOf(
            AutoFunctionDeclaration.create(
                functionName = ADD_INGREDIENTS_TOOL_NAME,
                description = ADD_INGREDIENTS_TOOL_DESCRIPTION,
                inputSchema = JsonSchema.obj(
                    properties = mapOf(
                        INGREDIENT_FIELD_NAME to JsonSchema.string(INGREDIENT_FIELD_DESCRIPTION)
                    )
                ),
                functionReference = ::addToGroceryList
            )
        )
    )

    private suspend fun addToGroceryList(input: JsonObject): FunctionResponsePart {
        val ingredients = input[INGREDIENT_FIELD_NAME]?.jsonPrimitive?.content
        val userId = authRepository.currentUser?.uid.orEmpty()

        if (ingredients.isNullOrBlank() || userId.isEmpty()) {
            throw FirebaseAutoFunctionException(LIVE_MODEL_ERROR)
        }

        val ingredientsList = ingredients.split(",").map { it.trim() }
        databaseRepository.addIngredientsToGroceries(userId, ingredientsList)

        return FunctionResponsePart(
            ADD_INGREDIENTS_TOOL_NAME,
            JsonObject(mapOf(
                ADD_INGREDIENTS_TOOL_RESULT to JsonPrimitive(ADD_INGREDIENTS_TOOL_RESULT_DESCRIPTION)
            ))
        )
    }

    @OptIn(PublicPreviewAPI::class)
    suspend fun setupLiveSession(recipe: Recipe): LiveSession? {
        val liveGenerationConfig = liveGenerationConfig {
            speechConfig = SpeechConfig(voice = Voice(LIVE_MODEL_VOICE))
            responseModality = ResponseModality.AUDIO
        }

        val promptTemplate = remoteConfig.getString(LIVE_MODEL_PROMPT_KEY)
        val instructionText = formatInstructionPrompt(promptTemplate, recipe)

        val liveModel = aiModel.liveModel(
            modelName = remoteConfig.getString(LIVE_MODEL_NAME_KEY),
            generationConfig = liveGenerationConfig,
            systemInstruction = content { text(instructionText) },
            tools = listOf(groceryListAutoTool)
        )

        return try {
            liveModel.connect()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatInstructionPrompt(template: String, recipe: Recipe): String {
        return template
            .replace("{{title}}", recipe.title)
            .replace("{{prepTime}}", recipe.prepTime)
            .replace("{{cookTime}}", recipe.cookTime)
            .replace("{{servings}}", recipe.servings)
            .replace("{{ingredients}}", recipe.ingredients.joinToString("\n"))
            .replace("{{instructions}}", recipe.instructions)
    }

    companion object {
        //Live Model Config
        private const val LIVE_MODEL_VOICE = "CHARON"
        private const val LIVE_MODEL_ERROR = "Unable to add ingredients to the list"

        //Tools config
        private const val ADD_INGREDIENTS_TOOL_NAME = "addIngredientToGroceryList"
        private const val ADD_INGREDIENTS_TOOL_DESCRIPTION = "Adds a list of ingredients to the " +
                "user's grocery list in the database."
        private const val ADD_INGREDIENTS_TOOL_RESULT = "result"
        private const val ADD_INGREDIENTS_TOOL_RESULT_DESCRIPTION = "Successfully added " +
                "ingredients to grocery list"
        private const val INGREDIENT_FIELD_NAME = "ingredient"
        private const val INGREDIENT_FIELD_DESCRIPTION = "The name of the ingredient to add."

        //Remote Config Keys
        private const val LIVE_MODEL_NAME_KEY = "live_model_name"
        private const val LIVE_MODEL_PROMPT_KEY = "live_model_prompt"
    }
}