package com.objectpersona.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.objectpersona.app.data.model.ConversationState
import com.objectpersona.app.ui.theme.*

/**
 * 角色頭像狀態環 — 對應 spec F-05 的 UI 狀態指示。
 * 🟢 聆聽中 / 🟡 辨識中 / 🔵 思考中 / 🟣 說話中
 */
@Composable
fun StatusRing(
    emoji: String,
    state: ConversationState,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ConversationState.LISTENING -> StateListening
        ConversationState.RECOGNIZING -> StateRecognizing
        ConversationState.THINKING -> StateThinking
        ConversationState.SPEAKING -> StateSpeaking
        ConversationState.IDLE -> TextSecondary
    }

    // 脈衝動畫（聆聽中）
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // 旋轉動畫（思考中 / 辨識中）
    val rotation by rememberInfiniteTransition(label = "rotate").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val alpha = when (state) {
        ConversationState.LISTENING -> pulseAlpha
        else -> 1f
    }

    val shouldRotate = state == ConversationState.THINKING || state == ConversationState.RECOGNIZING

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 4.dp.toPx()
            val ringColor = color.copy(alpha = alpha)

            if (shouldRotate) {
                rotate(rotation) {
                    // 旋轉弧線
                    drawArc(
                        color = ringColor,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            } else {
                // 完整圓環
                drawCircle(
                    color = ringColor,
                    style = Stroke(width = strokeWidth)
                )
            }

            // 內圈發光
            drawCircle(
                color = color.copy(alpha = alpha * 0.1f),
                radius = this.size.minDimension / 2 - strokeWidth
            )
        }

        // Emoji 頭像
        Text(
            text = emoji,
            fontSize = (size.value * 0.4f).sp
        )
    }
}
