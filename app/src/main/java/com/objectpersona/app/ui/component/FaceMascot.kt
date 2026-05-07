package com.objectpersona.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.objectpersona.app.data.model.ConversationState
import com.objectpersona.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FaceMascot(
    state: ConversationState,
    modifier: Modifier = Modifier
) {
    // 眼睛的縮放（用來眨眼或講話時的跳動）
    var eyeScaleY by remember { mutableFloatStateOf(1f) }
    // 眼睛的偏移（用來看左看右）
    var eyeOffsetX by remember { mutableFloatStateOf(0f) }
    var eyeOffsetY by remember { mutableFloatStateOf(0f) }

    // 根據狀態改變眼睛顏色
    val eyeColor by animateColorAsState(
        targetValue = when (state) {
            ConversationState.LISTENING -> StateListening
            ConversationState.RECOGNIZING -> StateRecognizing
            ConversationState.THINKING -> StateThinking
            ConversationState.SPEAKING -> StateSpeaking
            ConversationState.IDLE -> TextPrimary
        },
        label = "eyeColor"
    )

    // 控制眼睛的隨機動畫（眨眼、看左右）
    LaunchedEffect(Unit) {
        while (true) {
            val action = (0..10).random()
            when {
                action < 3 -> { // 眨眼
                    eyeScaleY = 0.1f
                    delay(150)
                    eyeScaleY = 1f
                }
                action < 7 -> { // 看左右
                    eyeOffsetX = (-12..12).random().toFloat()
                    eyeOffsetY = (-4..4).random().toFloat()
                    delay((600..1500).random().toLong())
                    eyeOffsetX = 0f
                    eyeOffsetY = 0f
                }
                else -> { // 發呆
                    delay(1000)
                }
            }
            delay((500..2000).random().toLong())
        }
    }

    // 說話時眼睛微微跳動
    LaunchedEffect(state) {
        if (state == ConversationState.SPEAKING) {
            while (true) {
                eyeScaleY = 0.8f
                delay(100)
                eyeScaleY = 1.2f
                delay(100)
                eyeScaleY = 1f
                delay((200..500).random().toLong())
            }
        } else {
            eyeScaleY = 1f
        }
    }

    val animatedScaleY by animateFloatAsState(
        targetValue = eyeScaleY,
        animationSpec = tween(if (eyeScaleY < 1f) 100 else 200),
        label = "eyeScaleY"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = eyeOffsetX,
        animationSpec = tween(400),
        label = "eyeOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = eyeOffsetY,
        animationSpec = tween(400),
        label = "eyeOffsetY"
    )

    // 方形身體
    Box(
        modifier = modifier
            .size(180.dp)
            .clip(RoundedCornerShape(40.dp)) // 圓角方形
            .background(SurfaceVariantDark)
            .border(3.dp, eyeColor.copy(alpha = 0.5f), RoundedCornerShape(40.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 眼睛容器，根據偏移量移動
        Row(
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.offset(x = animatedOffsetX.dp, y = animatedOffsetY.dp)
        ) {
            // 左眼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(scaleX = 1f, scaleY = animatedScaleY)
                    .clip(CircleShape)
                    .background(eyeColor)
            )
            // 右眼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(scaleX = 1f, scaleY = animatedScaleY)
                    .clip(CircleShape)
                    .background(eyeColor)
            )
        }
    }
}
