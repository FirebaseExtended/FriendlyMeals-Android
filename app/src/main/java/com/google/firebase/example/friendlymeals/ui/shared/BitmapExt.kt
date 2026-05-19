package com.google.firebase.example.friendlymeals.ui.shared

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

fun Bitmap.toBase64(): String {
    val byteArray = ByteArrayOutputStream().use { outputStream ->
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    }
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}