package com.objectpersona.app.ui.screen.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.objectpersona.app.ui.component.LoadingOverlay
import com.objectpersona.app.ui.theme.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.objectpersona.app.ui.component.LoadingOverlay
import com.objectpersona.app.ui.theme.*

/**
 * F-01：相機畫面 — 開啟後置鏡頭，使用者拍照後辨識物體。
 */
@Composable
fun CameraScreen(
    onNavigateToResult: (String, String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    // 相機相關物件
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 導航到 Vision Result 畫面
    LaunchedEffect(uiState.objectDescription, uiState.capturedImagePath) {
        if (uiState.objectDescription != null && uiState.capturedImagePath != null) {
            onNavigateToResult(uiState.objectDescription!!, uiState.capturedImagePath!!)
            viewModel.clearResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (hasCameraPermission) {
            // 相機預覽
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture
            )
        } else {
            // 無權限提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "需要相機權限才能辨識物體",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        }

        // 頂部漸層遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BackgroundDark.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        // 頂部標題列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ObjectPersona",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "歷史物體",
                    tint = TextPrimary
                )
            }
        }

        // 底部漸層遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.9f))
                    )
                )
        )

        // 底部拍照區域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "對準物體，按下拍照",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            CaptureButton(
                onClick = {
                    takePhoto(context, imageCapture, cameraExecutor) { bitmap ->
                        viewModel.captureAndAnalyze(bitmap)
                    }
                },
                enabled = !uiState.isAnalyzing
            )
        }

        // 辨識中覆蓋層
        if (uiState.isAnalyzing) {
            LoadingOverlay(message = "辨識中...")
        }

        // 錯誤提示
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Error
            ) {
                Text(error, color = TextOnPrimary)
            }
        }
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit, enabled: Boolean) {
    val scale by rememberInfiniteTransition(label = "capture").animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(if (enabled) scale else 1f)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(PrimaryPurple, PrimaryBlue))
            )
            .border(3.dp, TextPrimary.copy(alpha = 0.3f), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(TextOnPrimary.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) { /* 相機綁定失敗 */ }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}

private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (android.graphics.Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                onImageCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // 處理錯誤
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): android.graphics.Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    // 處理旋轉
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return android.graphics.Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
}
