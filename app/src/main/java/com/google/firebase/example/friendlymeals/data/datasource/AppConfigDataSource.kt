package com.google.firebase.example.friendlymeals.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigDataSource @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    companion object {
        /**
         * TOGGLE SWITCH:
         * Set to [true] during offline workshops or local demos to bypass network fetches
         * and use hardcoded local defaults.
         * Set to [false] for standard production behavior fetching from Firebase Remote Config.
         */
        const val USE_LOCAL_CONFIG = true
    }

    fun getString(key: String): String {
        if (USE_LOCAL_CONFIG) {
            return getLocalDefault(key)
        }
        return remoteConfig.getString(key)
    }

    private fun getLocalDefault(key: String): String {
        return when (key) {
            "generate_ingredients" -> "generate-ingredients-template-v1-0-0"
            "generate_recipe" -> "generate-recipe-template-v1-0-0"
            "generate_recipe_photo_gemini" -> "generate-recipe-photo-gemini-template-v1-0-0"
            "generate_recipe_photo_imagen" -> "generate-recipe-photo-imagen-template-v1-0-0"
            "grounding_model" -> "gemini-3.1-flash-lite"
            "grounding_prompt" -> "What are the nearest grocery stores or markets near me that stock these ingredients: {{ingredients}}? For each place, tell me their business hours, if it's open right now on {{dayOfWeek}} at {{currentTime}}, and if it's closing in less than 30 minutes. Tell me what the parking situation is like at each place: is there a dedicated lot or should I look for street parking? Tell the Map URL so I can open it in Google Maps. Format your response strictly as a JSON object with a \"stores\" array containing store objects. Each store object must have these EXACT keys: \"name\": string, \"address\": string, \"distance\": string, \"openNow\": boolean, \"closingSoon\": boolean, \"hasParking\": boolean, \"parkingDetails\": string, \"mapUrl\": string. Do NOT include markdown code block formatting (like ```json). Output only the raw JSON string."
            "hybrid_cloud_model" -> "gemini-3.1-flash-lite"
            "hybrid_ingredients_prompt" -> "Please analyze this image and list all visible food ingredients. Output ONLY a comma-separated list of ingredients. Do not include any introductory text, headers, or concluding remarks. Provide the raw list only. Be specific with measurements where possible."
            "imagen_name" -> "imagen-4.0-fast-generate-001"
            "live_model_name" -> "gemini-2.5-flash-native-audio-preview-12-2025"
            "live_model_prompt" -> "You are a helpful live cooking assistant. The user is currently preparing the following recipe: Title: {{title}} Prep time: {{prepTime}}, Cook time: {{cookTime}}, Servings: {{servings}}  Ingredients: {{ingredients}}  Instructions: {{instructions}}  The user will stream real-time video of their cooking and ask questions like \"Is this the expected texture of the recipe?\". Confirm or deny accurately based on the recipe context and the video content. Be concise and helpful. If the user asks you to add an ingredient or item to their grocery list or shopping list, call the addIngredientToGroceryList function."
            "model_name" -> "gemini-2.5-flash-image"
            "scan_meal" -> "scan-meal-template-v1-0-0"
            "schema_model_name" -> "gemini-2.5-flash"
            else -> remoteConfig.getString(key)
        }
    }
}
