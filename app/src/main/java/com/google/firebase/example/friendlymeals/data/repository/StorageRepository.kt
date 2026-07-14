package com.google.firebase.example.friendlymeals.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.example.friendlymeals.data.datasource.StorageRemoteDataSource
import com.google.firebase.example.friendlymeals.data.injection.FirebaseHiltModule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject

class StorageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storageRemoteDataSource: StorageRemoteDataSource
) {
    fun isStorageAvailable(): Boolean {
        return FirebaseHiltModule.isStorageSetup() && storageRemoteDataSource.isStorageAvailable()
    }

    suspend fun addImage(image: Bitmap): String? {
        if (isStorageAvailable()) {
            val bucket = Firebase.app.options.storageBucket
            if (!bucket.isNullOrEmpty()) {
                val exists = verifyBucketExists(bucket)
                if (!exists) {
                    FirebaseHiltModule.isStorageSetupRuntime = false
                }
            }

            if (isStorageAvailable()) {
                try {
                    return withTimeout(2000) {
                        storageRemoteDataSource.addImage(image)
                    }
                } catch (e: Exception) {
                    FirebaseHiltModule.isStorageSetupRuntime = false
                }
            }
        }
        return saveImageLocally(image)
    }

    private suspend fun verifyBucketExists(bucketName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://firebasestorage.googleapis.com/v0/b/$bucketName")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                val responseCode = connection.responseCode
                responseCode != 404 && responseCode != 400
            } catch (e: Exception) {
                true
            }
        }
    }

    private fun saveImageLocally(image: Bitmap): String? {
        return try {
            val randomId = UUID.randomUUID().toString()
            val file = File(context.cacheDir, "recipe_$randomId.webp")
            FileOutputStream(file).use { out ->
                image.compress(Bitmap.CompressFormat.WEBP, 70, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }
}