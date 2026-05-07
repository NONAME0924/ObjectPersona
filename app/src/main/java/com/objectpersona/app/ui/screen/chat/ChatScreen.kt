package com.objectpersona.app.ui.screen.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.objectpersona.app.data.model.ConversationState
import com.objectpersona.app.ui.component.FaceMascot
import com.objectpersona.app.ui.component.StatusRing
import com.objectpersona.app.ui.theme.*

/**
 * F-05：對話介面 — 常時聆聽語音對話 + 開發者模式。
 */
@Composable
fun ChatScreen(
    objectId: String,
    onSwitchObject: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 開發者模式狀態
    var devModeEnabled by remember { mutableStateOf(true) }
    var customInput by remember { mutableStateOf("") }

    // 預設測試句子
    val presetSentences = remember {
        listOf(
            "你好啊，今天過得怎麼樣？",
            "你能跟我說說你的故事嗎？",
            "你平常都在想什麼呢？",
            "哈哈，你真有趣！",
            "我今天心情不太好...",
            "你覺得人生的意義是什麼？",
            "你有什麼特別的回憶嗎？",
            "如果你可以去任何地方，你想去哪裡？"
        )
    }

    // 錄音權限（非開發者模式時使用）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadObject(objectId, useMic = true)
        }
    }

    LaunchedEffect(objectId, devModeEnabled) {
        if (!devModeEnabled) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.loadObject(objectId, useMic = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryPurple
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // 頂部：角色名稱 + 開發者模式開關
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.persona?.name ?: "",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    // 開發者模式開關
                    IconButton(
                        onClick = { devModeEnabled = !devModeEnabled }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "開發者模式",
                            tint = if (devModeEnabled) PrimaryPurple else TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 狀態環 + 狀態文字
                StatusRing(
                    emoji = "",
                    state = uiState.conversationState,
                    size = 24.dp
                )

                Spacer(Modifier.height(6.dp))

                val statusText = when (uiState.conversationState) {
                    ConversationState.LISTENING -> if (devModeEnabled) "等待輸入..." else "聆聽中..."
                    ConversationState.RECOGNIZING -> "辨識中..."
                    ConversationState.THINKING -> "思考中..."
                    ConversationState.SPEAKING -> "說話中..."
                    ConversationState.IDLE -> "準備中"
                }
                val statusColor = when (uiState.conversationState) {
                    ConversationState.LISTENING -> StateListening
                    ConversationState.RECOGNIZING -> StateRecognizing
                    ConversationState.THINKING -> StateThinking
                    ConversationState.SPEAKING -> StateSpeaking
                    ConversationState.IDLE -> TextSecondary
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // 填滿上方空間
                Spacer(Modifier.weight(1f))

                // 中間的方形角色（會眨眼、看左右）
                FaceMascot(state = uiState.conversationState)

                // 填滿下方空間
                Spacer(Modifier.weight(1f))

                // AI 回應文字區域（串流逐字顯示）
                val displayText = if (uiState.streamingText.isNotBlank()) {
                    uiState.streamingText
                } else {
                    uiState.latestAiMessage
                }

                if (displayText != null && displayText.isNotBlank()) {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(displayText) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceDark
                    ) {
                        Text(
                            text = displayText,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 使用者最新一句話
                if (uiState.latestUserMessage != null) {
                    Text(
                        text = "你：${uiState.latestUserMessage}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ─── 開發者模式面板 ───
                AnimatedVisibility(
                    visible = devModeEnabled,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 預設句子列表（橫向滾動）
                        Text(
                            text = "🧪 開發者模式 — 點擊句子發送",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetSentences) { sentence ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.clickable(
                                        enabled = uiState.conversationState != ConversationState.THINKING &&
                                            uiState.conversationState != ConversationState.SPEAKING
                                    ) {
                                        viewModel.onUserSpoke(sentence)
                                    }
                                ) {
                                    Text(
                                        text = sentence,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 自訂輸入欄
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customInput,
                                onValueChange = { customInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text("輸入自訂句子...", fontSize = 13.sp)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = SurfaceVariantDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = PrimaryPurple
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )

                            Spacer(Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (customInput.isNotBlank()) {
                                        viewModel.onUserSpoke(customInput)
                                        customInput = ""
                                    }
                                },
                                enabled = customInput.isNotBlank() &&
                                    uiState.conversationState != ConversationState.THINKING &&
                                    uiState.conversationState != ConversationState.SPEAKING
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "送出",
                                    tint = if (customInput.isNotBlank()) PrimaryPurple else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 「換個物體」按鈕
                OutlinedButton(
                    onClick = onSwitchObject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("← 換個物體", fontSize = 13.sp)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
