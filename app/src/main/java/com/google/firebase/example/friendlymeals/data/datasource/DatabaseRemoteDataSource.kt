package com.google.firebase.example.friendlymeals.data.datasource

import android.util.Log
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.data.model.Review
import com.google.firebase.example.friendlymeals.data.model.Like
import com.google.firebase.example.friendlymeals.data.model.User
import com.google.firebase.example.friendlymeals.ui.recipeList.RecipeListItem
import com.google.firebase.example.friendlymeals.ui.recipeList.filter.FilterOptions
import com.google.firebase.example.friendlymeals.ui.recipeList.filter.SortByFilter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PipelineResult
import com.google.firebase.firestore.PipelineSource
import com.google.firebase.firestore.pipeline.AggregateFunction.Companion.average
import com.google.firebase.firestore.pipeline.AggregateFunction.Companion.countAll
import com.google.firebase.firestore.pipeline.AggregateStage
import com.google.firebase.firestore.pipeline.Expression
import com.google.firebase.firestore.pipeline.Expression.Companion.documentId
import com.google.firebase.firestore.pipeline.Expression.Companion.field
import com.google.firebase.firestore.pipeline.Expression.Companion.variable
import com.google.firebase.firestore.pipeline.SearchStage
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.collections.first
import kotlin.collections.mapNotNull

class DatabaseRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addUser(user: User) {
        firestore.collection(USERS_COLLECTION).add(user).await()
    }

    suspend fun addRecipe(recipe: Recipe): String {
        return ""
    }

    suspend fun getRecipe(recipeId: String): Recipe {
        return Recipe()
    }

    suspend fun getAllRecipes(): List<RecipeListItem> {
        return listOf()
    }

    suspend fun getPopularTags(): List<String> {
        return listOf()
    }

    /*
    NOTE: The right way to do this is in a transaction via Cloud Function,
    this function is here just for demonstrating aggregate

    To enforce a "one review per user" constraint, Review documents use a deterministic ID based on
    the following pattern: "${recipeId}_${userId}". With this approach, you can retrieve a specific
    review instantly without searching the entire collection.
     */
    suspend fun setReview(review: Review) {
        val recipeRef = firestore
            .collection(RECIPES_COLLECTION)
            .document(review.recipeId)

        val reviewRef = recipeRef
            .collection(REVIEWS_SUBCOLLECTION)
            .document("${review.recipeId}_${review.userId}")

        reviewRef.set(review).await()
    }

    suspend fun getRating(userId: String, recipeId: String): Int {
        val reviewId = "${recipeId}_${userId}"
        val reviewPath = "${RECIPES_COLLECTION}/${recipeId}/${REVIEWS_SUBCOLLECTION}/${reviewId}"

        val results = firestore
            .pipeline()
            .documents(reviewPath)
            .execute().await().results

        if (results.isEmpty()) return 0

        val reviewData = results.first().getData()
        return (reviewData[RATING_FIELD] as? Number)?.toInt() ?: 0
    }

    /*
    To enforce a "one favorite per user" constraint, Like documents use a deterministic ID based on
    the following pattern: "${recipeId}_${userId}". With this approach, you can retrieve a specific
    Like document instantly without searching the entire collection.
     */
    suspend fun setFavorite(like: Like) {
        val likeRef = firestore
            .collection(LIKES_COLLECTION)
            .document("${like.recipeId}_${like.userId}")

        likeRef.set(like).await()
    }

    suspend fun removeFavorite(like: Like) {
        firestore
            .collection(LIKES_COLLECTION)
            .document("${like.recipeId}_${like.userId}")
            .delete()
            .await()
    }

    suspend fun getFavorite(userId: String, recipeId: String): Boolean {
        val favoriteId = "${recipeId}_${userId}"
        val favoritePath = "${LIKES_COLLECTION}/${favoriteId}"

        return firestore
            .pipeline()
            .documents(favoritePath)
            .execute().await().results.isNotEmpty()
    }

    @Suppress("UnstableApiUsage")
    suspend fun getFilteredRecipes(
        filterOptions: FilterOptions,
        userId: String
    ): List<RecipeListItem> {
        return listOf()
    }

    private fun List<PipelineResult>.toRecipe(): Recipe {
        val itemData = this.first().getData()

        return Recipe(
            title = itemData[TITLE_FIELD] as? String ?: "",
            instructions = itemData[INSTRUCTIONS_FIELD] as? String ?: "",
            ingredients = (itemData[INGREDIENTS_FIELD] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
            authorId = itemData[AUTHOR_ID_FIELD] as? String ?: "",
            tags = (itemData[TAGS_FIELD] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
            averageRating = (itemData[AVERAGE_RATING_FIELD] as? Number)?.toDouble() ?: 0.0,
            prepTime = itemData[PREP_TIME_FIELD] as? String ?: "",
            cookTime = itemData[COOK_TIME_FIELD] as? String ?: "",
            servings = itemData[SERVINGS_FIELD] as? String ?: "",
            imageUri = itemData[IMAGE_URI_FIELD] as? String
        )
    }

    private fun List<PipelineResult>.toRecipeListItem(): List<RecipeListItem> {
        return this.mapNotNull { result ->
            val id = result.getId()

            if (id.isNullOrEmpty()) {
                Log.w(this::class.java.simpleName, "Empty recipe ID")
                return@mapNotNull null
            }

            val itemData = result.getData()

            RecipeListItem(
                id = id,
                title = itemData[TITLE_FIELD] as? String ?: "",
                averageRating = (itemData[AVERAGE_RATING_FIELD] as? Number)?.toDouble() ?: 0.0,
                imageUri = itemData[IMAGE_URI_FIELD] as? String
            )
        }
    }

    fun getGroceriesFlow(userId: String): Flow<List<GroceryItem>> {
        if (userId.isEmpty()) {
            return flowOf(emptyList())
        }

        return firestore.collection(GROCERIES_COLLECTION)
            .whereEqualTo(USER_ID_FIELD, userId)
            .snapshots()
            .mapNotNull { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GroceryItem::class.java)?.copy(id = doc.id)
                }
            }
    }

    suspend fun addGroceryItem(item: GroceryItem) {
        val docRef = firestore.collection(GROCERIES_COLLECTION).document()
        val itemWithId = item.copy(id = docRef.id)
        docRef.set(itemWithId).await()
    }

    suspend fun updateGroceryItemChecked(itemId: String, checked: Boolean) {
        firestore.collection(GROCERIES_COLLECTION).document(itemId)
            .update(CHECKED_FIELD, checked).await()
    }

    suspend fun deleteGroceryItem(itemId: String) {
        firestore.collection(GROCERIES_COLLECTION).document(itemId)
            .delete().await()
    }

    suspend fun addIngredientsToGroceries(userId: String, ingredients: List<String>) {
        if (userId.isEmpty() || ingredients.isEmpty()) return

        val batch = firestore.batch()
        val collection = firestore.collection(GROCERIES_COLLECTION)

        for (ingredient in ingredients) {
            val docRef = collection.document()
            val item = GroceryItem(
                id = docRef.id,
                userId = userId,
                name = ingredient,
                checked = false
            )
            batch.set(docRef, item)
        }
        batch.commit().await()
    }

    companion object {
        //Collections
        private const val USERS_COLLECTION = "users"
        private const val RECIPES_COLLECTION = "recipes"
        private const val LIKES_COLLECTION = "likes"
        private const val REVIEWS_SUBCOLLECTION = "reviews"
        private const val GROCERIES_COLLECTION = "groceries"

        //Fields
        private const val RATING_FIELD = "rating"
        private const val AVERAGE_RATING_FIELD = "averageRating"
        private const val AUTHOR_ID_FIELD = "authorId"
        private const val TITLE_FIELD = "title"
        private const val TAGS_FIELD = "tags"
        private const val LIKES_FIELD = "likes"
        private const val IMAGE_URI_FIELD = "imageUri"
        private const val PREP_TIME_FIELD = "prepTime"
        private const val COOK_TIME_FIELD = "cookTime"
        private const val SERVINGS_FIELD = "servings"
        private const val INSTRUCTIONS_FIELD = "instructions"
        private const val INGREDIENTS_FIELD = "ingredients"
        private const val RECIPE_ID_FIELD = "recipeId"
        private const val USER_ID_FIELD = "userId"
        private const val CHECKED_FIELD = "checked"

        //Field aliases
        private const val AVG_RATING_ALIAS = "avg_rating"
        private const val TAG_NAME_ALIAS = "tagName"
        private const val TAG_COUNT_ALIAS = "tagCount"
        private const val LIKES_COUNT_ALIAS = "likesCount"
        private const val SCORE_ALIAS = "score"

        //Variables
        private const val CURRENT_RECIPE_ID_VAR = "current_recipe_id"

        //Field paths
        private const val NAME_FIELD_PATH = "__name__"
    }
}