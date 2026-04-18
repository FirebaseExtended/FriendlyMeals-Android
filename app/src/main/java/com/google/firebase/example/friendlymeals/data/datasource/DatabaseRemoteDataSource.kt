package com.google.firebase.example.friendlymeals.data.datasource

import android.util.Log
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
import com.google.firebase.firestore.pipeline.Expression.Companion.equal
import com.google.firebase.firestore.pipeline.Expression.Companion.field
import com.google.firebase.firestore.pipeline.Expression.Companion.variable
import com.google.firebase.firestore.pipeline.SearchStage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.collections.first
import kotlin.collections.mapNotNull

@Suppress("UnstableApiUsage")
class DatabaseRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addUser(user: User) {
        firestore.collection(USERS_COLLECTION).add(user).await()
    }

    suspend fun addRecipe(recipe: Recipe): String {
        val recipeRef = firestore.collection(RECIPES_COLLECTION).add(recipe).await()
        return recipeRef.id
    }

    suspend fun getRecipe(recipeId: String): Recipe {
        val recipePath = "${RECIPES_COLLECTION}/${recipeId}"

        return firestore
            .pipeline()
            .documents(recipePath)
            .define(field("id").alias(CURRENT_RECIPE_ID_VAR))
            .addFields(
                PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
                    .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
                    .toScalarExpression().alias(AVERAGE_RATING_FIELD),
                firestore.pipeline().collection(LIKES_COLLECTION)
                    .where(equal(RECIPE_ID_FIELD, variable(CURRENT_RECIPE_ID_VAR)))
                    .aggregate(countAll().alias(COUNT_ALIAS))
                    .toScalarExpression().alias(LIKES_FIELD)
            )
            .execute().await().results.toRecipe()
    }

    suspend fun getAllRecipes(): List<RecipeListItem> {
        return firestore
            .pipeline()
            .collection(RECIPES_COLLECTION)
            .define(field("id").alias(CURRENT_RECIPE_ID_VAR))
            .addFields(
                PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
                    .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
                    .toScalarExpression().alias(AVERAGE_RATING_FIELD),
                firestore.pipeline().collection(LIKES_COLLECTION)
                    .where(equal(RECIPE_ID_FIELD, variable(CURRENT_RECIPE_ID_VAR)))
                    .aggregate(countAll().alias(COUNT_ALIAS))
                    .toScalarExpression().alias(LIKES_FIELD)
            )
            .execute().await().results.toRecipeListItem()
    }

    suspend fun getPopularTags(): List<String> {
        val results = firestore.pipeline()
            .collection(RECIPES_COLLECTION)
            .unnest(field(TAGS_FIELD).alias(TAG_NAME_ALIAS))
            .aggregate(
                AggregateStage.withAccumulators(countAll().alias(TAG_COUNT_ALIAS))
                    .withGroups(TAG_NAME_ALIAS)
            )
            .sort(field(TAG_COUNT_ALIAS).descending())
            .limit(10)
            .execute().await().results

        return results.mapNotNull { it.getData()[TAG_NAME_ALIAS] as? String }
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

    private suspend fun getAverageRatingForRecipe(recipeId: String): Double {
        val results = firestore
            .pipeline()
            .documents("${RECIPES_COLLECTION}/${recipeId}")
            .addFields(
                PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
                    .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
                    .toScalarExpression().alias(AVERAGE_RATING_FIELD)
            )
            .execute().await().results

        val itemData = results.first().getData()
        return (itemData[AVERAGE_RATING_FIELD] as? Number)?.toDouble() ?: 0.0
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

    suspend fun getFilteredRecipes(
        filterOptions: FilterOptions,
        userId: String
    ): List<RecipeListItem> {
        var pipeline = firestore.pipeline().collection(RECIPES_COLLECTION)
        Log.v("DEBUG", "Applying filters")

        if (filterOptions.searchQuery.isNotBlank()) {
            val searchStage = SearchStage.withQuery(filterOptions.searchQuery)
                .withAddFields(Expression.score().alias(SCORE_ALIAS))

            pipeline = pipeline.search(searchStage)
                .sort(field(SCORE_ALIAS).descending())
            Log.v("DEBUG","Search stage")
        } else if (filterOptions.recipeTitle.isNotBlank()) {
            pipeline = pipeline
                .where(
                    field(TITLE_FIELD).toLower()
                        .stringContains(filterOptions.recipeTitle.lowercase())
                )
        }

        if (filterOptions.filterByMine) {
            pipeline = pipeline
                .where(field(AUTHOR_ID_FIELD)
                    .equal(userId))
        }

        if (filterOptions.rating > 0) {
            pipeline = pipeline
                .where(field(AVERAGE_RATING_FIELD)
                    .greaterThanOrEqual(filterOptions.rating))
        }

        if (filterOptions.selectedTags.isNotEmpty()) {
            pipeline = pipeline
                .where(field(TAGS_FIELD)
                    .arrayContainsAny(filterOptions.selectedTags))
        }

        when (filterOptions.sortBy) {
            SortByFilter.RATING -> {
                pipeline = pipeline
                    .define(field("id").alias(CURRENT_RECIPE_ID_VAR))
                    .addFields(
                        PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
                            .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
                            .toScalarExpression().alias(AVERAGE_RATING_FIELD)
                    )
                    .sort(field(AVERAGE_RATING_FIELD)
                        .descending())
            }
            SortByFilter.ALPHABETICAL -> {
                pipeline = pipeline
                    .sort(field(TITLE_FIELD)
                        .ascending())
            }
            SortByFilter.POPULARITY -> {
                pipeline = pipeline
                    .sort(field(LIKES_FIELD)
                        .descending())
            }
        }

        return pipeline.execute().await().results.toRecipeListItem()
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
            likes = (itemData[LIKES_FIELD] as? Number)?.toInt() ?: 0,
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
                likes = (itemData[LIKES_FIELD] as? Number)?.toInt() ?: 0,
                imageUri = itemData[IMAGE_URI_FIELD] as? String
            )
        }
    }

    companion object {
        //Collections
        private const val USERS_COLLECTION = "users"
        private const val RECIPES_COLLECTION = "recipes"
        private const val LIKES_COLLECTION = "likes"
        private const val REVIEWS_SUBCOLLECTION = "reviews"

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

        //Field aliases
        private const val AVG_RATING_ALIAS = "avg_rating"
        private const val TAG_NAME_ALIAS = "tagName"
        private const val TAG_COUNT_ALIAS = "tagCount"
        private const val COUNT_ALIAS = "count"
        private const val SCORE_ALIAS = "score"

        //Variables
        private const val CURRENT_RECIPE_ID_VAR = "current_recipe_id"
    }
}