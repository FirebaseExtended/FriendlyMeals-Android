package com.google.firebase.example.friendlymeals.data.injection

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.example.friendlymeals.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FriendlyMealsHiltApp : Application() {
  override fun onCreate() {
    super.onCreate()

    // Cryptographically protect AI endpoints from unauthorized clients
    Firebase.appCheck.installAppCheckProviderFactory(
      if (BuildConfig.DEBUG) {
        DebugAppCheckProviderFactory.getInstance()
      } else {
        PlayIntegrityAppCheckProviderFactory.getInstance()
      }
    )
  }
}
