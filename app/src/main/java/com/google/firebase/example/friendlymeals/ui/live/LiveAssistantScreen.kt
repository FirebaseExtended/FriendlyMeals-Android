package com.google.firebase.example.friendlymeals.ui.live

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.example.friendlymeals.R
import com.google.firebase.example.friendlymeals.data.model.Recipe
import com.google.firebase.example.friendlymeals.ui.theme.FriendlyMealsTheme
import com.google.firebase.example.friendlymeals.ui.theme.Teal
import kotlinx.serialization.Serializable

@Serializable
data class LiveAssistantRoute(val recipeId: String)

@Composable
fun LiveAssistantScreen(
    viewModel: LiveAssistantViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    showError: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LiveAssistantScreenContent(
        uiState = uiState.value,
        navigateBack = navigateBack,
        sendVideoFrame = viewModel::sendVideoFrame,
        showError = showError
    )
}

@Composable
fun LiveAssistantScreenContent(
    uiState: LiveAssistantUiState,
    navigateBack: () -> Unit = {},
    sendVideoFrame: (Bitmap) -> Unit = {},
    showError: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = navigateBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.back_button_content_description),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = stringResource(id = R.string.live_assistant_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            when (uiState) {
                is LiveAssistantUiState.Loading -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Teal)
                    }
                }
                is LiveAssistantUiState.Error -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is LiveAssistantUiState.Success -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        AndroidView(
                            factory = { context ->
                                val previewView = PreviewView(context)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(Dispatchers.IO.asExecutor()) { imageProxy ->
                                                val bitmap = imageProxy.toBitmap()
                                                sendVideoFrame(bitmap)
                                                imageProxy.close()
                                            }
                                        }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalyzer
                                        )
                                    } catch (_: Exception) {
                                        showError()
                                    }
                                }, getMainExecutor(context))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.live_assistant_hint),
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LiveAssistantScreenPreview() {
    FriendlyMealsTheme {
        LiveAssistantScreenContent(
            uiState = LiveAssistantUiState.Success(Recipe())
        )
    }
}
