package com.objectpersona.app.util

/**
 * Emoji 對應器 — 根據物體描述關鍵字匹配 Emoji 頭像。
 * 對應 spec 附錄 A 的物體類別 → 頭像對應表。
 */
object EmojiMapper {
    private val mappings = listOf(
        listOf("杯", "咖啡", "茶", "cup", "mug") to "☕",
        listOf("書", "筆記", "book", "note") to "📚",
        listOf("植物", "盆栽", "花", "多肉", "plant") to "🌿",
        listOf("手機", "電腦", "平板", "phone", "laptop") to "📱",
        listOf("食物", "飯", "麵", "蛋糕", "food") to "🍱",
        listOf("玩具", "玩偶", "熊", "toy", "doll") to "🧸",
        listOf("筆", "鉛筆", "原子筆", "pen") to "✏️",
        listOf("鑰匙", "key") to "🔑",
        listOf("時鐘", "手錶", "clock", "watch") to "⏰",
        listOf("眼鏡", "glasses") to "👓"
    )

    fun getEmoji(description: String): String {
        val lower = description.lowercase()
        for ((keywords, emoji) in mappings) {
            if (keywords.any { lower.contains(it) }) return emoji
        }
        return "🔮" // 預設：未知物體
    }
}
