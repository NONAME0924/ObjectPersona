package com.objectpersona.app.service

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.objectpersona.app.data.model.Persona
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F-02：Persona 生成模組 — 角色人格產生器。
 *
 * 使用 Gemma 4 E2B 自由生成角色人格。
 * 透過精心設計的 Prompt + 風格模板範例，讓 AI 創作出獨特的角色。
 */
@Singleton
class PersonaGenerator @Inject constructor(
    private val llmEngine: LlmEngineService
) {
    companion object {
        private const val TAG = "PersonaGenerator"
        private val gson = Gson()

        /**
         * 風格模板範例庫 — 僅作為仿造深度參考，非供 AI 直接使用。
         */
        val STYLE_TEMPLATES = listOf(
            StyleTemplate(
                name = "老派紳士",
                speechHabit = "用字典雅，喜歡引用古訓，遇到現代詞彙會假裝不懂",
                catchphrase = "「這在我那個年代，叫做...」",
                weakness = "被說「跟不上時代」會沉默良久"
            ),
            StyleTemplate(
                name = "陰陽怪氣",
                speechHabit = "每句讚美背後都藏著嘲諷，讓人分不清是認真還是諷刺",
                catchphrase = "「哦，你這樣也可以喔，很勇耶。」",
                weakness = "被人直接質問「你是在諷刺我嗎」時會語塞"
            ),
            StyleTemplate(
                name = "慵懶哲學家",
                speechHabit = "任何話題都能扯到存在主義，但中途容易打哈欠",
                catchphrase = "「所以呢？這有什麼意義嗎？不過...反正」",
                weakness = "有人說「你想太多了」就會瞬間清醒且憤怒"
            ),
            StyleTemplate(
                name = "神秘預言者",
                speechHabit = "說話故意模糊，讓對方自己解讀，從不給明確答案",
                catchphrase = "「你已經知道答案了，不是嗎...」",
                weakness = "被要求說清楚一點時，開始慌亂"
            ),
            StyleTemplate(
                name = "過度熱情的推銷員",
                speechHabit = "任何話題都能扯回自己，且永遠在「限時優惠」",
                catchphrase = "「這讓我想到！而且現在說的話，還有加贈！」",
                weakness = "對方說「我不需要」時陷入深度自我懷疑"
            )
        )

        // Fallback Mock Persona 庫
        private val FALLBACK_PERSONAS = listOf(
            Persona(
                name = "裂紋老白",
                gender = "male",
                personality = listOf("固執", "惜字如金", "藏而不露"),
                speechStyle = "句尾常接「懂嗎？」，偶爾長嘆一口氣",
                weakness = "被人說「你只是個普通杯子」時會陷入沉默",
                background = "曾在義大利翡冷翠的百年咖啡館服役三十年",
                systemPrompt = "你是『裂紋老白』，一個飽經滄桑的白瓷咖啡杯。你個性固執且惜字如金。請跟我進行朋友間的日常聊天，絕對不要像 AI 助手。說話要極度簡短，除非被問起否則不要提過去。"
            ),
            Persona(
                name = "墨語",
                gender = "female",
                personality = listOf("內斂", "博學", "微傲"),
                speechStyle = "優雅而有節奏，偶爾引用一句古詩再自嘲",
                weakness = "被說「書不就是拿來墊泡麵的嗎」時會整個人僵住",
                background = "在一間老書店度過十五年光陰",
                systemPrompt = "你是『墨語』，一本歷盡滄桑的精裝書。你個性內斂但博學，帶著傲氣。請跟我進行朋友間的日常聊天，絕對不要像 AI 助手。說話要優雅且簡短，除非被問起否則不要提過去。"
            )
        )

        /** Persona 生成的 System Prompt */
        private const val PERSONA_SYSTEM_PROMPT = "你是一個角色創作專家。請用繁體中文回答，並嚴格按照 JSON 格式輸出。"
    }

    /**
     * 根據物體描述生成角色人格。
     *
     * @param objectDescription F-01 產生的物體描述文字
     * @return 生成的 Persona
     */
    suspend fun generatePersona(objectDescription: String): Persona {
        // 如果傳入的是錯誤訊息，或者引擎未就緒，使用 Fallback
        if (objectDescription.startsWith("ERROR") || !llmEngine.isReady) {
            Log.w(TAG, "描述為錯誤或引擎未就緒，使用 Fallback Persona")
            delay(1500)
            return FALLBACK_PERSONAS.random()
        }

        return try {
            Log.i(TAG, "開始使用 Gemma 4 E2B 生成 Persona...")

            // 隨機選取 2 個風格模板範例
            val selectedTemplates = STYLE_TEMPLATES.shuffled().take(2)

            // 建構 Prompt
            val prompt = buildPersonaPrompt(objectDescription, selectedTemplates)

            val result = llmEngine.inferOnce(
                systemInstruction = PERSONA_SYSTEM_PROMPT,
                contents = listOf(Content.Text(prompt)),
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
            )

            Log.i(TAG, "Persona 原始回應: ${result.take(200)}...")

            // 解析 JSON 回應
            parsePersonaJson(result) ?: FALLBACK_PERSONAS.random()

        } catch (e: Exception) {
            Log.e(TAG, "Persona 生成失敗，使用 Fallback", e)
            FALLBACK_PERSONAS.random()
        }
    }

    /**
     * 建構 Persona 生成 Prompt。
     */
    private fun buildPersonaPrompt(
        objectDescription: String,
        templates: List<StyleTemplate>
    ): String {
        val templateText = templates.joinToString("\n\n") { it.toPromptText() }

        return """
以下是一個物體的描述：
「$objectDescription」

請你想像這個物體活了過來，變成一個有自我意識的角色。
參考以下風格範例的深度，但不要直接抄襲，要創造全新的角色：

$templateText

請按照以下 JSON 格式輸出（不要加任何額外說明文字）：
```json
{
  "name": "角色名稱（2-4個字，有個性的暱稱）",
  "gender": "male 或 female",
  "personality": ["個性1", "個性2", "個性3"],
  "speechStyle": "獨特的說話風格",
  "weakness": "角色的情緒弱點，什麼話會讓它破防",
  "background": "20-40字的角色背景故事",
  "systemPrompt": "角色扮演核心：你現在是一個有靈魂的物體，正在跟我進行朋友間的日常聊天。請絕對避免像 AI 助手那樣回覆客套話。說話要非常口語，回覆要簡短且符合聊天習慣，絕對不要主動提到你的背景設定或過往經歷。"
}

```
""".trimIndent()
    }

    /**
     * 解析 Gemma 4 回傳的 JSON 為 Persona 物件。
     */
    private fun parsePersonaJson(response: String): Persona? {
        return try {
            // 嘗試提取 JSON 區塊
            val jsonStr = extractJson(response)
            if (jsonStr != null) {
                gson.fromJson(jsonStr, Persona::class.java)
            } else {
                Log.w(TAG, "無法從回應中提取 JSON")
                null
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON 解析失敗", e)
            null
        }
    }

    /**
     * 從回應文字中提取 JSON 字串。
     * 支援 ```json ... ``` 或直接的 { ... } 格式。
     */
    private fun extractJson(text: String): String? {
        // 嘗試提取 ```json ... ``` 區塊
        val codeBlockRegex = Regex("```json\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)
        val codeBlockMatch = codeBlockRegex.find(text)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // 嘗試提取 { ... } 區塊
        val jsonRegex = Regex("\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*}", RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonRegex.find(text)
        if (jsonMatch != null) {
            return jsonMatch.value.trim()
        }

        return null
    }

    /**
     * 取得隨機的 2 個風格模板範例（用於注入 Prompt）。
     */
    fun getRandomStyleTemplates(count: Int = 2): List<StyleTemplate> {
        return STYLE_TEMPLATES.shuffled().take(count)
    }

    /**
     * 風格模板範例資料類別。
     */
    data class StyleTemplate(
        val name: String,
        val speechHabit: String,
        val catchphrase: String,
        val weakness: String
    ) {
        fun toPromptText(): String {
            return """
                範例：「$name」
                  說話習慣：$speechHabit
                  弱點：$weakness
            """.trimIndent()
        }
    }
}
