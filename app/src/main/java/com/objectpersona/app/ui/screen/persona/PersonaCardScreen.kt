package com.objectpersona.app.ui.screen.persona

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.objectpersona.app.ui.component.LoadingOverlay
import com.objectpersona.app.ui.theme.*

/**
 * F-02：角色卡片畫面 — 顯示 AI 生成的角色預覽，支援編輯。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaCardScreen(
    objectDescription: String,
    onStartChat: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PersonaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(objectDescription) {
        viewModel.generatePersona(objectDescription)
    }

    LaunchedEffect(uiState.savedObjectId) {
        uiState.savedObjectId?.let { onStartChat(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (uiState.isGenerating) {
            LoadingOverlay(message = "角色生成中...")
        } else if (uiState.persona != null) {
            PersonaContent(
                uiState = uiState,
                onBack = onBack,
                onNameChange = viewModel::updateName,
                onGenderChange = viewModel::updateGender,
                onAddTag = viewModel::addPersonalityTag,
                onRemoveTag = viewModel::removePersonalityTag,
                onStyleChange = viewModel::updateSpeechStyle,
                onFreeDescChange = viewModel::updateFreeDescription,
                onStartChat = viewModel::saveAndStartChat
            )
        }
    }
}

@Composable
private fun PersonaContent(
    uiState: PersonaUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onStyleChange: (String) -> Unit,
    onFreeDescChange: (String) -> Unit,
    onStartChat: () -> Unit
) {
    val persona = uiState.persona ?: return
    var newTag by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 頂部返回列
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.ArrowBack, "返回", tint = TextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Emoji 頭像（發光效果）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    PrimaryPurple.copy(alpha = 0.3f),
                                    BackgroundDark
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.emoji, fontSize = 48.sp)
                }
            }

            // 歡迎回來提示
            if (uiState.isExistingObject) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "歡迎回來！上次我們聊過...",
                    color = Accent,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // 角色名稱（可編輯）
            SectionLabel("角色名稱")
            OutlinedTextField(
                value = uiState.editedName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                colors = personaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 性別選擇（影響 TTS 音色）
            SectionLabel("語音性別")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderChip("male", "♂ 男聲", uiState.editedGender == "male", onGenderChange)
                GenderChip("female", "♀ 女聲", uiState.editedGender == "female", onGenderChange)
            }

            Spacer(Modifier.height(16.dp))

            // 個性標籤（最多5個）
            SectionLabel("個性關鍵字（最多 5 個）")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.editedPersonality.forEach { tag ->
                    AssistChip(
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, "移除", Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PrimaryPurple.copy(alpha = 0.2f),
                            labelColor = PrimaryPurpleLight
                        )
                    )
                }
            }
            if (uiState.editedPersonality.size < 5) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("新增標籤", color = TextSecondary) },
                        colors = personaFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (newTag.isNotBlank()) { onAddTag(newTag); newTag = "" }
                    }) {
                        Text("新增", color = Accent)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 說話風格
            SectionLabel("說話風格")
            OutlinedTextField(
                value = uiState.editedSpeechStyle,
                onValueChange = onStyleChange,
                modifier = Modifier.fillMaxWidth(),
                colors = personaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(Modifier.height(16.dp))

            // 弱點（唯讀）
            SectionLabel("弱點")
            Text(
                text = persona.weakness,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark)
                    .padding(16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // 背景故事（唯讀）
            SectionLabel("背景故事")
            Text(
                text = persona.background,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark)
                    .padding(16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // 自由描述（可選填）
            SectionLabel("自由描述（選填，追加到角色設定）")
            OutlinedTextField(
                value = uiState.editedFreeDescription,
                onValueChange = onFreeDescChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：喜歡在深夜特別話多...", color = TextSecondary) },
                colors = personaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(Modifier.height(32.dp))
        }

        // 底部「開始對話」按鈕
        Button(
            onClick = onStartChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = TextOnPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = if (uiState.isExistingObject) "繼續對話" else "開始對話",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun GenderChip(
    value: String, label: String, selected: Boolean, onSelect: (String) -> Unit
) {
    val bgColor = if (selected) PrimaryPurple.copy(alpha = 0.3f) else SurfaceVariantDark
    val borderColor = if (selected) PrimaryPurple else Divider

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect(value) }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(label, color = if (selected) PrimaryPurpleLight else TextSecondary)
    }
}

@Composable
private fun personaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = Divider,
    cursorColor = PrimaryPurple,
    focusedContainerColor = SurfaceVariantDark.copy(alpha = 0.5f),
    unfocusedContainerColor = SurfaceVariantDark.copy(alpha = 0.3f)
)
