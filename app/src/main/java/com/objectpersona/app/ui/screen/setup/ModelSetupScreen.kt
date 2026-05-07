package com.objectpersona.app.ui.screen.setup

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.objectpersona.app.data.model.ConversationState
import com.objectpersona.app.service.LlmEngineState
import com.objectpersona.app.ui.component.FaceMascot
import com.objectpersona.app.ui.theme.*

/**
 * 模型設定畫面 — App 啟動時的 AI 模型初始化介面。
 *
 * 流程：
 * 1. 自動檢查模型檔案是否存在
 * 2. 存在 → 載入模型（顯示 FaceMascot 動畫）
 * 3. 不存在 → 顯示下載引導
 * 4. 載入完成 → 自動跳轉到相機畫面
 */
@Composable
fun ModelSetupScreen(
    onNavigateToCamera: () -> Unit,
    viewModel: ModelSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 自動開始初始化
    LaunchedEffect(Unit) {
        viewModel.initializeModel()
    }

    // 模型就緒後自動跳轉
    LaunchedEffect(uiState.engineState) {
        if (uiState.engineState == LlmEngineState.READY) {
            kotlinx.coroutines.delay(800) // 短暫展示「就緒」狀態
            onNavigateToCamera()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FaceMascot 角色（根據載入狀態顯示不同動畫）
            val mascotState = when (uiState.engineState) {
                LlmEngineState.NOT_INITIALIZED -> ConversationState.IDLE
                LlmEngineState.LOADING -> ConversationState.THINKING
                LlmEngineState.READY -> ConversationState.LISTENING
                LlmEngineState.ERROR -> ConversationState.IDLE
            }
            FaceMascot(state = mascotState)

            Spacer(Modifier.height(32.dp))

            // 狀態文字
            Text(
                text = uiState.statusText,
                color = when (uiState.engineState) {
                    LlmEngineState.READY -> StateListening
                    LlmEngineState.ERROR -> MaterialTheme.colorScheme.error
                    LlmEngineState.LOADING -> StateThinking
                    else -> TextPrimary
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // 載入中的進度指示
            if (uiState.engineState == LlmEngineState.LOADING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 8.dp),
                    color = PrimaryPurple,
                    trackColor = SurfaceVariantDark
                )

                Text(
                    text = "首次載入約需 5-10 秒",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 錯誤/模型未找到時的操作按鈕
            AnimatedVisibility(
                visible = uiState.engineState == LlmEngineState.ERROR,
                enter = fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // 錯誤訊息
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // 模型未找到時，顯示下載指引
                    if (uiState.modelPath == null) {
                        Text(
                            text = "請下載模型至手機的 Download 資料夾：",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // 下載按鈕
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm"
                                ))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Text("📥 前往 HuggingFace 下載", fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "檔名: gemma-4-E2B-it.litertlm (~2.58GB)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 重試按鈕
                    OutlinedButton(
                        onClick = { viewModel.retryInitialization() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("🔄 重新檢查", fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // 跳過按鈕（使用 Mock 模式）
                    TextButton(
                        onClick = {
                            viewModel.skipModelSetup()
                        }
                    ) {
                        Text(
                            "跳過，使用離線模式（Mock 資料）",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
