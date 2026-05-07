package com.objectpersona.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.objectpersona.app.ui.theme.*

/**
 * 載入覆蓋層 — 用於辨識中、角色生成中等載入狀態。
 */
@Composable
fun LoadingOverlay(
    message: String = "處理中...",
    modifier: Modifier = Modifier
) {
    val alpha by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800), RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = PrimaryPurple,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = TextPrimary.copy(alpha = alpha),
                fontSize = 16.sp
            )
        }
    }
}
