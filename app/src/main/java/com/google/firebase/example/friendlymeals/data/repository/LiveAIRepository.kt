package com.google.firebase.example.friendlymeals.data.repository

import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.example.friendlymeals.data.datasource.LiveAIRemoteDataSource
import com.google.firebase.example.friendlymeals.data.model.Recipe
import javax.inject.Inject

class LiveAIRepository @Inject constructor(
    private val liveAiRemoteDataSource: LiveAIRemoteDataSource
) {
    @OptIn(PublicPreviewAPI::class)
    suspend fun setupLiveSession(recipe: Recipe): LiveSession? {
        return liveAiRemoteDataSource.setupLiveSession(recipe)
    }
}