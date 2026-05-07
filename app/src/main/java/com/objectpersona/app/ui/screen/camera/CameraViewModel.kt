package com.objectpersona.app.ui.screen.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.objectpersona.app.service.VisionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val isAnalyzing: Boolean = false,
    val objectDescription: String? = null,
    val capturedImagePath: String? = null,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val visionService: VisionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun captureAndAnalyze(bitmap: Bitmap?) {
        if (bitmap == null) {
            _uiState.value = _uiState.value.copy(error = "無法擷取圖片")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                // 1. 先儲存圖片到暫存區
                val imagePath = visionService.saveImageToCache(bitmap)
                
                // 2. 進行 AI 辨識
                val description = visionService.analyzeImage(bitmap)
                
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    objectDescription = description,
                    capturedImagePath = imagePath
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = "辨識失敗：${e.message}"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = CameraUiState()
    }
}
