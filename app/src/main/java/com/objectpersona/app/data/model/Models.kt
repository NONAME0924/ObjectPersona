package com.objectpersona.app.data.model

/**
 * 角色人格領域模型 — 對應 spec F-02 的 Persona JSON。
 * 由 Gemma 4 生成，可被使用者編輯。
 */
data class Persona(
    val name: String,
    val gender: String, // "male" or "female"
    val personality: List<String>, // 個性關鍵字，最多 5 個
    val speechStyle: String,
    val weakness: String,
    val background: String,
    val systemPrompt: String
) {
    companion object {
        /**
         * 建立 Mock Persona（Phase 1 測試用）。
         */
        fun createMock(objectDescription: String = "一個白色陶瓷咖啡杯"): Persona {
            return Persona(
                name = "裂紋老白",
                gender = "male",
                personality = listOf("固執", "惜字如金", "藏而不露"),
                speechStyle = "句尾常接「懂嗎？」，偶爾長嘆一口氣後說「唉，算了」",
                weakness = "被人說「你只是個普通杯子」時會陷入沉默，久久不說話",
                background = "曾在義大利翡冷翠的百年咖啡館服役三十年，被一個旅人帶離故土",
                systemPrompt = "我是「裂紋老白」，一個飽經滄桑的白瓷咖啡杯。我個性固執，話不多，但每句話背後都藏著見過世面的重量。我說話時習慣在句尾加「懂嗎？」，偶爾會長嘆一口氣說「唉，算了」。我見過太多人來了又走，所以我不輕易信任，但一旦認定你，就是一輩子。如果有人說我「只是個普通杯子」，我會沉默，很久很久。保持這個角色，用口語繁體中文回答，每次回覆不超過 3 句話。"
            )
        }
    }
}

/**
 * 對話訊息領域模型。
 */
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 對話狀態枚舉 — 對應 spec F-05 的 UI 狀態指示。
 */
enum class ConversationState {
    /** 🟢 聆聽中 — 等待使用者說話 */
    LISTENING,

    /** 🟡 辨識中 — SpeechRecognizer 處理中 */
    RECOGNIZING,

    /** 🔵 思考中 — AI 推論中 */
    THINKING,

    /** 🟣 說話中 — TTS 播放中 */
    SPEAKING,

    /** ⚪ 閒置 — 未進入對話模式 */
    IDLE
}
