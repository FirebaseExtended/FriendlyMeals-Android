# Firestore Pipeline Queries on Android



# Before you begin

In this codelab, you'll learn how to use the powerful new Pipeline Queries available in Firestore Enterprise edition. You will build a recipe management and discovery app on Android called **Friendly Meals**. With this app, you will learn how to perform advanced filtering, case-insensitive searches, complex sorting and more. These complex searches and transformations were previously not possible or required manual filtering in Firestore Standard edition.



## What you’ll learn



*   Standard **CRUD operations** in Firestore.
*   Using **atomic batches** to maintain data consistency.
*   Constructing **multi-stage Pipeline Queries** for advanced filtering.
*   Implementing **sorting and ordering** when filtering data.
*   Implementing **aggregations** when filtering data.
*   Implementing **case-insensitive searches**.


## Prerequisites



*   Latest version of [Android Studio](https://developer.android.com/studio).
*   An Android emulator with API 26 or higher.
*   Basic knowledge of Kotlin and [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html).



---



# Create a Firebase project

Duration: 3:00



1. Sign into the [Firebase console](https://console.firebase.google.com/) using your Google Account.
2. Click the **Create a new Firebase project **button, then enter a project name (for example, **FriendlyMeals**).

    ```
Note: This project name is used as a display name in Firebase interfaces, and Firebase auto-creates a unique project ID based on this project name. Note that you can optionally click the Edit icon to set your preferred project ID, but you cannot change this ID after project creation. If you forget your ID, you can always find it later in the Project Settings.
```


3. Click **Continue**.
4. If prompted, review and accept the Firebase terms, and then click **Continue**.
5. Follow the steps to finish setting up your project, then click **Create project**.
6. Wait for your project to provision, and then click **Continue**.



---



# Enable Firebase services

Duration: 3:00

In order to run all the steps in this codelab, you will need to enable a few services on the Firebase project you just created: [Firestore Enterprise edition](https://firebase.google.com/docs/firestore) (to store the recipes), [Authentication](https://firebase.google.com/docs/auth) (to sign the user in as a guest), and [Firebase AI Logic](https://firebase.google.com/docs/ai-logic) (since this app uses Gemini through Firebase AI Logic to generate recipes). At the end of this step, you will also need to register your Android app in this Firebase project.


## Enable Firestore Enterprise edition



1. In the Firebase console, [open the Firestore Database section](https://console.firebase.google.com/project/_/firestore/?useAutoProject=true).
2. Click **Create database** and select **Enterprise** for the database mode. Click **Next**.
3. Select **Firestore in Native Mode** for the operation mode.
4. You can use the **default database ID**. If you choose a custom ID, make sure to copy it exactly as written. You’ll need it soon.
5. Select a location for your database and click **Next**.
6. Select **Start in Test Mode**. Then click **Create**.

    ```
Warning: For real-world applications, you should always start Firestore in Production Mode. We are using Test Mode now to get started quickly, but make sure you understand how Security Rules work and implement them on your database before shipping your app to production.
```




## Enable Authentication



1. In the Firebase console, [open the Authentication section](https://console.firebase.google.com/project/_/authentication/?useAutoProject=true).
2. Click **Get started** and choose **Anonymous**.
3. Toggle the enable button and click **Save**.


## Enable Firebase AI Logic and register app



1. In the Firebase console, [open the AI Logic section](https://console.firebase.google.com/project/_/ailogic/?useAutoProject=true).
2. Click **Get started**, then **Enable API**.
    1. If prompted to select an API, proceed with the **Gemini Developer API**.
3. When prompted to add an app, **click the Android icon**.
4. When prompted for a package name, use `com.google.firebase.example.friendlymeals`
5. Leave other fields blank, **click Register App** and follow the instructions to download the `google-services.json` file. Keep this file handy as you’re going to need it soon, then click **Next**.
6. You can skip the instructions to add the Firebase SDK, as the code you will download in the next step already includes the Firebase SDK. At the end, click **Continue to console**.



---



# Set up the sample project

Duration: 8:00


## Download the code

Run the following command to clone the sample code for this codelab from [GitHub](https://github.com/FirebaseExtended/FriendlyMeals-Android). This will create a folder called `FriendlyMeals-Android` on your machine:


```
$ git clone https://github.com/FirebaseExtended/FriendlyMeals-Android.git
```


Navigate to the project folder and checkout the codelab branch:


```
$ cd FriendlyMeals-Android
$ git checkout codelab/firestore-enterprise-on-android
```



## Import the project

Open Android Studio. **Click File > New > Import Project** and select the `FriendlyMeals-Android` folder.

Move the `google-services.json` file you just downloaded into the **app/** folder.


## Run the app

Now that you have added the `google-services.json` file, the project should compile and you can safely run the app on your Android emulator, or on a physical device. Since this is the first time you’re running this app on your machine, it may take a while to sync the Gradle files and build the app.

Once the app is launched, you will automatically be signed in as a guest user through Firebase Anonymous Authentication, and you should land on the _New recipe screen_:

<b>>>>>>GDCALERT:inline image link here (to images/image2.png). Store image on your image server and adjust path/filename/extension if necessary.>>>>></b>
![alt_text](images/image2.png "image_tooltip")


You can use the tabs at the bottom to navigate to other screens.



---



# Write data to Firestore

Duration: 10:00

In this step you will write some data to Firestore so that you can populate the currently empty _Recipes screen_. Firestore Enterprise edition uses the same fundamental [data model](https://firebase.google.com/docs/firestore/data-model) as Firestore Standard edition:** Documents which are grouped into Collections and Subcollections.	**


## Initialize Firestore

Before you can make any calls to Firestore, you need to create a Cloud Firestore instance. Open `data/injection/FirebaseHiltModule.kt` and make sure the `firestore()` function is initializing Firestore with the correct database ID. If you created the database with the default database ID, it should look like this:


```
@Provides fun firestore(): FirebaseFirestore {
  return FirebaseFirestore.getInstance("default")
}
```


If you chose a different database ID, make sure you’re using that when calling `getInstance()`. Now you’re ready to make some calls to Firestore.


```
Note: Friendly Meals uses Hilt to inject dependencies. To learn more, check out the Hilt documentation.
```



## Add Recipes to the database

The main model object in this app is a `Recipe` (see `data/model/Recipe.kt`). You will store each recipe as a document in a top-level collection called `recipes`. Every time the user generates a new recipe using Gemini, and saves it, the app will create a new recipe document in this collection.

Open `data/datasource/DatabaseRemoteDataSource.kt` and replace the `addRecipe` function with:


```
suspend fun addRecipe(recipe: Recipe): String {
  val recipeRef = firestore.collection(RECIPES_COLLECTION).add(recipe).await()
  return recipeRef.id
}
```


Here you’re starting by getting a reference to the `recipes` collection. Collections are created implicitly when documents are added, so there is no need to create the collection before writing data. Documents can be created using Kotlin data classes, which you’re using here to create each `recipe` doc. Lastly, you’re returning the `id` of the document so it can be passed from the _Generate screen_ to the _Recipe screen_ once the recipe is finished being generated and stored.

---



# Update data in Firestore 

Duration: 10:00

In Firestore Standard edition, calculating the average rating of a recipe would typically require writing client-side transactions or cloud functions to re-calculate the average and persist that average back onto the recipe document on every review write.

With **Firestore Enterprise edition**, this pre-calculation is entirely unnecessary! We can leverage **subcollection aggregation** to calculate the average rating of a recipe dynamically on-the-fly during read and query operations.

As a result, we no longer need to persist a static average rating field or run client-side update logic when users write reviews. In the next section, you'll learn how to build a query pipeline that performs this subcollection aggregation dynamically.





---



# Read data from Firestore

Duration: 5:00

So far you have added and updated documents in your database. Now, let's retrieve these saved documents!

In the same file, replace the `getAllRecipes` function with:

```kotlin
suspend fun getAllRecipes(): List<RecipeListItem> {
  return firestore
    .pipeline()
    .collection(RECIPES_COLLECTION)
    .define(
      documentId(field(NAME_FIELD_PATH)).alias(CURRENT_RECIPE_ID_VAR)
    )
    .addFields(
      PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
        .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
        .toScalarExpression().alias(AVERAGE_RATING_FIELD)
    )
    .execute().await().results.toRecipeListItem()
}
```


This function retrieves all documents in the `recipe` collection. Notice the call to the `toRecipeListItem` extension (available at the bottom of the class). This extension maps each `PipelineResult` in the list to an object of the type `RecipeListItem`, which is a type that the application understands and uses in its business logic and to display data in the UI.

Similarly, when fetching a single recipe, you can call the `toRecipe` function to convert a single document to a `Recipe`. In the same file, replace the `getRecipe` function with:

```kotlin
suspend fun getRecipe(recipeId: String): Recipe {
  val recipePath = "${RECIPES_COLLECTION}/${recipeId}"

  return firestore
    .pipeline()
    .documents(recipePath)
    .define(
      documentId(field(NAME_FIELD_PATH)).alias(CURRENT_RECIPE_ID_VAR)
    )
    .addFields(
      PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
        .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
        .toScalarExpression().alias(AVERAGE_RATING_FIELD)
    )
    .execute().await().results.toRecipe()
}
```


Once again you are constructing a pipeline, but using the `documents()` function instead of `collection()`, because you want to retrieve one recipe only. You can make sure the pipeline will fetch only this specific recipe by providing the path to the document (`recipePath`).

The `getAllRecipes` and `getRecipe` functions are being called in the `RecipeListViewModel` and `RecipeViewModel` respectively. Once the view models receive the results, they assign these results to a `MutableStateFlow` object observed by the view layer. 

Once a new state is emitted via `MutableStateFlow`, the Jetpack Compose UI will collect this new state and recompose itself to display the changes to the user. You can check the code for these UIs in the `RecipeListScreen.kt` and `RecipeScreen.kt` files.



---



# Test recipe creation and listing

Duration: 8:00


## Enable Cloud Storage

All the recipe images generated by Gemini are stored in [Cloud Storage](https://firebase.google.com/docs/storage). In order to use Storage, you need to make sure that your Firebase project is on the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing). Don’t worry, you’re not going to be charged for anything at this stage!

To upgrade to Blaze and enable Cloud Storage, follow these steps:



1. Open the [Firebase console](https://console.firebase.google.com/project/_/?useAutoProject=true) and click **Upgrade** (bottom right corner).
2. Click **Select plan** and follow the steps to set up a billing account.
3. Finish the setup by **linking your project** with the selected billing account.
4. [Open the Storage section](https://console.firebase.google.com/project/_/storage/?useAutoProject=true).
5. Click **Get started**, select **No cost location** and click **Continue**.
6. Select **Start in Test Mode**. Then click **Create**.
    1. Once again: don’t forget that **for real-world applications, you should always start Firestore in Production Mode**!


## Time to run the app!

To test recipe creation, run the app and type a list of ingredients into the first input field in the _New recipe screen_. If you're feeling fancy, you can add special notes or cuisines on the second input field. Click **Generate Recipe** and wait for Gemini to generate a recipe for you. \
 \
Once the recipe is generated, the _Recipe screen _appears, where you can see the recipe image, ingredients, instructions and a few other details, like preparation time and number of servings. If you scroll down to the bottom of the recipe, you can add your rating to the recipe, which will trigger the `getAverageRatingForRecipe` function you wrote in a previous step. 

Scroll back to the top of the page and click the back arrow icon in the top left corner of the screen. It will take you back to the _Generate screen_, where you can generate as many recipes as you want. When you’re done generating recipes, navigate to the _Recipes screen_ using the navigation bar at the bottom, to see a beautiful list of all the recipes you generated:

<b>>>>>>GDCALERT:inline image link here (to images/image3.png). Store image on your image server and adjust path/filename/extension if necessary.>>>>></b>
![alt_text](images/image3.png "image_tooltip")
<b>>>>>>GDCALERT:inline image link here (to images/image4.png). Store image on your image server and adjust path/filename/extension if necessary.>>>>></b>
![alt_text](images/image4.png "image_tooltip")



```
Note: As you start generating recipes and updating the rating, you can open the Firestore section in the Firebase console and see all the updates happening in real time!
```




---



# Build a Pipeline Query

Duration: 8:00

Hopefully, by now, you have created lots of delicious recipes while testing the app! Which means now you need to build a good _Filter screen_ to allow users to find the recipes they want quickly and efficiently.

Go back to the `DatabaseRemoteDataSource.kt` file and locate the `getFilteredRecipes` function. You can see that this function receives two parameters: `filterOptions` and `userId`. You will use `filterOptions` to check which filters the user set up in the _Filter screen_. You will use `userId` to filter the recipes created by that user, should this filter be `true` in the `filterOptions `object. \
 \
Now open the `ui/recipeList/filter/FilterOptions.kt` file. It should look like this:


```kotlin
data class FilterOptions(
  val recipeTitle: String = "",
  val searchQuery: String = "",
  val filterByMine: Boolean = false,
  val rating: Int = 0,
  val selectedTags: List<String> = listOf(),
  val sortBy: SortByFilter = DEFAULT
)
```

The following steps will build on each other, all adding stages to a single pipeline query in the same function. 

Open `data/datasource/DatabaseRemoteDataSource.kt` and locate the `getFilteredRecipes` function. The starting state of the function in the codebase is a simple stub that initializes the pipeline on the `recipes` collection and returns all results without any filters:

```kotlin
@Suppress("UnstableApiUsage")
suspend fun getFilteredRecipes(
    filterOptions: FilterOptions,
    userId: String
): List<RecipeListItem> {
    var pipeline = firestore.pipeline().collection(RECIPES_COLLECTION)

    // Implement this function in the next codelab steps.

    return pipeline.execute().await().results.toRecipeListItem()
}
```

Let’s implement these filters and the sorting criteria using Pipelines.

## Text Search

Before we implement standard filtering, let's explore the new Full-Text Search functionality. We'll use full-text search to find content in recipe descriptions.

Inside the `getFilteredRecipes` function of `DatabaseRemoteDataSource.kt`, the text search is constructed using the `SearchStage.withQuery(...)` pipeline stage, which automatically ranks matching recipes by search score. Since average ratings and likes are dynamically calculated inline rather than stored statically on recipe documents, the query also defines dynamic pipeline aggregations. 

Add the following full-text search and dynamic aggregation stages directly under the pipeline initialization:

```kotlin
if (filterOptions.searchQuery.isNotBlank()) {
    val searchStage = SearchStage.withQuery(filterOptions.searchQuery)
        .withAddFields(Expression.score().alias(SCORE_ALIAS))

    pipeline = pipeline.search(searchStage).sort(field(SCORE_ALIAS).descending())
}

pipeline = pipeline
    .define(
        documentId(field(NAME_FIELD_PATH)).alias(CURRENT_RECIPE_ID_VAR)
    ).addFields(
        PipelineSource.subcollection(REVIEWS_SUBCOLLECTION)
            .aggregate(average(RATING_FIELD).alias(AVG_RATING_ALIAS))
            .toScalarExpression().alias(AVERAGE_RATING_FIELD),
        firestore.pipeline()
            .collectionGroup(LIKES_COLLECTION)
            .where(field(RECIPE_ID_FIELD)
                .equal(variable(CURRENT_RECIPE_ID_VAR)))
            .aggregate(countAll().alias(LIKES_COUNT_ALIAS))
            .toScalarExpression().alias(LIKES_FIELD)
    )
```


## Filter by title

Users can search for text in the description and filter by title at the same time. To filter by recipe title, add another `if` block right below the previous one:

```kotlin
if (filterOptions.recipeTitle.isNotBlank()) {
  pipeline = pipeline
    .where(field(TITLE_FIELD).toLower().stringContains(filterOptions.recipeTitle.lowercase()))
}
```


First, you need to check if the user is using this filter by checking if `recipeTitle` is not blank. Next, you need to add a `where` clause and check if the recipe’s title contains the words typed by the user, which you can do with `stringContains`. Don’t forget to use `toLower()` when specifying the `field`, and make sure the words typed by the user are _also_ in lower case. This is necessary because pipeline searches are case sensitive, so you need to transform these values to lowercase before comparing them.


## Filter by author

In the _Filter screen_, users can toggle the _Filter by mine_ option. To implement that, add another `if` statement right below the previous one:


```
if (filterOptions.filterByMine) {
  pipeline = pipeline
    .where(field(AUTHOR_ID_FIELD).equal(userId))
}
```


First, check if `filterByMine` is true - which means the user wants to use this filter. Then, add a `where` clause and check if the recipe’s author has the same id as the currently authenticated user, which you can do with the `equal` function. Here’s where you use that `userId` parameter!


## Filter by rating

Users can filter recipes based on a 1 to 5 stars average rating - here you can see how important it is to recalculate the average rating when someone submits a new review! To implement that, add another `if` statement below the previous one:


```
if (filterOptions.rating > 0) {
  pipeline = pipeline
    .where(field(AVERAGE_RATING_FIELD).greaterThanOrEqual(filterOptions.rating))
}
```


Start by checking if the user wants to filter by rating, by checking if `filterOptions.rating` is greater than zero. Next, you need to add another `where` clause and check if the recipe’s average rating is greater than, or equal to the minimum rating selected by the user.


## Filter by tags

Gemini generates a list of tags for each recipe (e.g.: _#dessert_, _#healthy_). Users can select a few of these tags in the _Filter screen _when searching for recipes. To implement that, add one last `if` statement to the function:


```
if (filterOptions.selectedTags.isNotEmpty()) {
  pipeline = pipeline
    .where(field(TAGS_FIELD).arrayContainsAny(filterOptions.selectedTags))
}
```


First you need to check if the list of selected tags is not empty. Then, add another `where` clause and check if the recipe’s tags list contains any of these tags selected by the user. You can do that with the `arrayContainsAny` function.


## Sorting

Users can also opt to sort recipes based on four different criteria: default (no sort), the average rating, the title (alphabetical order), or the popularity (how many users liked the recipe). To implement that, add the following code right below the last `if` statement:

```
when (filterOptions.sortBy) {
  SortByFilter.DEFAULT -> {}
  SortByFilter.RATING -> {
    pipeline = pipeline.sort(field(AVERAGE_RATING_FIELD).descending())
  }
  SortByFilter.ALPHABETICAL -> {
    pipeline = pipeline.sort(field(TITLE_FIELD).ascending())
  }
  SortByFilter.POPULARITY -> {
    pipeline = pipeline.sort(field(LIKES_FIELD).descending())
  }
}

return pipeline.execute().await().results.toRecipeListItem()
```


Each of these sorting criteria will have a logic to it - whether it is `ascending` or `descending` will depend on the sorting type selected using the radio buttons in the _Filter screen_.

At the end you need to execute the pipeline, transform the results in a list of `RecipeListItem`, and return the list so the UI can be updated to display only the recipes that match the criteria.

## Suggest Tags

In the Filter screen, the app can suggest popular category tags to the user. To support this, we need to write a pipeline query that unrolls the tags array on all recipes, groups and aggregates them to count occurrences, and sorts them to return the top 10 most popular tags.

In `DatabaseRemoteDataSource.kt`, replace the `getPopularTags` function with:

```kotlin
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
```

First, this code is getting a reference to the `recipes` collection and using the `unnest` operator to flatten the tags array on each recipe. It then groups them by tag name and aggregates them using `countAll()`. Finally, it sorts by popularity in descending order, limits the results to 10, executes the pipeline, and extracts the tag names from the resulting data maps.


## Test queries in the Firebase console

**You can test these queries straight in the Firebase console!** The Firestore section has a **Query Explorer** where you can run and test your queries in JavaScript. Although the implementation varies slightly from Kotlin, the logic is straightforward and user-friendly. The console also has a **Query Explainer** where you can see if your pipeline is using an index or performing a slow collection scan.

<b>>>>>>GDCALERT:inline image link here (to images/image5.png). Store image on your image server and adjust path/filename/extension if necessary.>>>>></b>
![alt_text](images/image5.png "image_tooltip")




---



# Test recipes filter

Duration: 8:00

**Time to run the app again!** To test the filters you built in the previous step, navigate to the _Recipes screen_, then click the filter icon in the top right corner to navigate to the** _Filter screen_**. Once in this screen, you can play around with all the filters, then **click the Apply Filters button**. You will be taken back to the _Recipes screen_, but you should now only see the recipes that match the filters you selected:

<b>>>>>>GDCALERT:inline image link here (to images/image6.png). Store image on your image server and adjust path/filename/extension if necessary.>>>>></b>
![alt_text](images/image6.png "image_tooltip")


To modify the filters, or go back to the unfiltered list, click the filter icon again, then click **Reset**.



---



# Conclusion

Congratulations! **You've successfully implemented Firestore Enterprise edition in an Android app and learned the basics about Pipeline Queries**. In this codelab, you learned how to perform complex searches and transformations with Pipeline Queries and convert it to an object that is recognizable in your Kotlin code, so you can show it in the UI.

**But there’s much, much more you can do with pipelines!** Firestore Enterprise edition also handles indexing and performance differently than the Standard edition. If you’re just starting out with Firestore or planning a migration from Standard edition, reviewing these **best practices** is essential. **They will help you optimize performance and ensure cost-efficiency, protecting you from unexpected billing spikes.**

To learn about Pipeline operations, indexes, pricing and performance, take a look at these resources:



*   [Pipeline operations documentation](https://firebase.google.com/docs/firestore/enterprise/pipelines-overview)
*   [Quick start documentation](https://firebase.google.com/docs/firestore/enterprise/quickstart)
*   [Optimize query execution](https://firebase.google.com/docs/firestore/enterprise/optimize-query-performance)
*   [Pricing documentation](https://firebase.google.com/docs/firestore/enterprise/pricing)

**Happy coding!**
