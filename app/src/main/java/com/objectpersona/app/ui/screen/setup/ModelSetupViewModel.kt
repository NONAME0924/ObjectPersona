package com.objectpersona.app.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.objectpersona.app.service.LlmEngineService
import com.objectpersona.app.service.LlmEngineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelSetupUiState(
    val engineState: LlmEngineState = LlmEngineState.NOT_INITIALIZED,
    val errorMessage: String? = null,
    val modelPath: String? = null,
    val statusText: String = "正在檢查模型..."
)

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val llmEngine: LlmEngineService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSetupUiState())
    val uiState: StateFlow<ModelSetupUiState> = _uiState.asStateFlow()

    init {
        // 監聽引擎狀態
        viewModelScope.launch {
            llmEngine.state.collect { state ->
                _uiState.value = _uiState.value.copy(
                    engineState = state,
                    statusText = when (state) {
                        LlmEngineState.NOT_INITIALIZED -> "正在檢查模型..."
                        LlmEngineState.LOADING -> "模型載入中，請稍候..."
                        LlmEngineState.READY -> "模型就緒！"
                        LlmEngineState.ERROR -> "模型載入失敗"
                    }
                )
            }
        }

        // 監聽錯誤訊息
        viewModelScope.launch {
            llmEngine.errorMessage.collect { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error)
            }
        }
    }

    /**
     * 嘗試尋找並初始化模型。
     */
    fun initializeModel() {
        viewModelScope.launch {
            llmEngine.initializeWithAutoDiscovery()
        }
    }

    /**
     * 跳過模型載入（使用 Mock 模式）。
     */
    fun skipModelSetup() {
        _uiState.value = _uiState.value.copy(
            engineState = LlmEngineState.READY,
            statusText = "使用離線模式"
        )
    }

    /**
     * 重試模型載入。
     */
    fun retryInitialization() {
        initializeModel()
    }
}
