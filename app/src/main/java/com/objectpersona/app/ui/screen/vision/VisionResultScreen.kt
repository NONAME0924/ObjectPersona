package com.objectpersona.app.ui.screen.vision

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.objectpersona.app.ui.theme.*
import java.io.File

/**
 * F-01：辨識結果畫面 — 顯示拍攝的圖片與 AI 的初步描述。
 */
@Composable
fun VisionResultScreen(
    objectDescription: String,
    imagePath: String,
    onConfirm: (String) -> Unit,
    onRetake: () -> Unit
) {
    val bitmap = remember(imagePath) {
        val file = File(imagePath)
        if (file.exists()) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 標題
        Text(
            text = "辨識結果",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圖片預覽
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceVariantDark)
                    .border(2.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "擷取的物體",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("無法讀取圖片", color = TextSecondary)
                }
            }

            Spacer(Modifier.height(32.dp))

            // AI 描述區塊
            Text(
                text = "AI 的第一印象：",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                color = if (objectDescription.startsWith("ERROR")) Error.copy(alpha = 0.1f) else SurfaceVariantDark.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = if (objectDescription.startsWith("ERROR")) androidx.compose.foundation.BorderStroke(2.dp, Error) else BoxBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = objectDescription,
                    color = if (objectDescription.startsWith("ERROR")) Error else TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
        }

        // 底部按鈕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary)
            ) {
                Text("重新拍攝", color = TextSecondary, fontSize = 16.sp)
            }

            Button(
                onClick = { onConfirm(objectDescription) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("確定，建立角色", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
