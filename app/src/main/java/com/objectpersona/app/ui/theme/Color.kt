package com.objectpersona.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Primary 漸層系 ──
val PrimaryPurple = Color(0xFF7C3AED)
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryPurpleLight = Color(0xFFA78BFA)

// ── Surface / Background ──
val SurfaceDark = Color(0xFF1A1A2E)
val BackgroundDark = Color(0xFF0F0F1A)
val SurfaceVariantDark = Color(0xFF242440)
val CardDark = Color(0xFF1E1E36)

// ── 狀態環顏色（對應 spec F-05）──
val StateListening = Color(0xFF10B981)    // 🟢 聆聽中
val StateRecognizing = Color(0xFFF59E0B)  // 🟡 辨識中
val StateThinking = Color(0xFF3B82F6)     // 🔵 思考中
val StateSpeaking = Color(0xFF8B5CF6)     // 🟣 說話中

// ── 文字 ──
val TextPrimary = Color(0xFFE2E8F0)
val TextSecondary = Color(0xFF94A3B8)
val TextOnPrimary = Color(0xFFFFFFFF)

// ── 對話氣泡 ──
val BubbleAI = Color(0xFF242440)
val BubbleUser = Color(0xFF7C3AED)

// ── 其他 ──
val Accent = Color(0xFF10B981)
val Error = Color(0xFFEF4444)
val Divider = Color(0xFF334155)

// ── UI 常量 ──
val BoxBorder = androidx.compose.foundation.BorderStroke(1.dp, Divider)
