package com.objectpersona.app.service

import android.util.Log
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Message
import com.objectpersona.app.data.model.ChatMessage
import com.objectpersona.app.data.model.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F-04：對話推論模組 — Dialogue Engine。
 *
 * 使用 Gemma 4 E2B LiteRT-LM 進行端側對話推論。
 * 維護一個長期 Conversation 實例，支援串流回應。
 *
 * 推論參數（Gemma 4 官方建議）：
 * - topK: 40
 * - topP: 0.95
 * - temperature: 0.8
 */
@Singleton
class DialogueEngine @Inject constructor(
    private val llmEngine: LlmEngineService
) {
    companion object {
        private const val TAG = "DialogueEngine"

        /** Context Window 最大輪數 (設為 5 輪以提升穩定性) */
        const val MAX_HISTORY_ROUNDS = 5

        // Fallback Mock AI 回應庫
        private val FALLBACK_RESPONSES = listOf(
            "嗯...你說的我懂。不過我見過的比這多多了，懂嗎？唉，算了。",
            "哼，你以為我沒經歷過嗎？我在翡冷翠的時候，這種事天天上演。",
            "你這話讓我想起以前的主人。他也總喜歡說些有的沒的。懂嗎？",
            "唉...又來了。每個人都喜歡跟我聊這些。好吧，我聽著就是了。",
            "這問題啊...我想了想，還是算了。有些事不說比較好，懂嗎？",
            "呼～你一口氣說好多！等等讓我消化一下嘛～不過你說的有道理啦！",
            "欸欸欸，你有沒有好好吃飯啊？看你這樣我好擔心耶～呼～",
            "你這樣也可以喔，很勇耶。不是啦，我是認真的...大概吧。",
            "所以呢？這有什麼意義嗎？不過...反正，你開心就好。"
        )
    }

    /** 當前活躍的 Conversation（跟隨角色生命週期） */
    private var activeConversation: Conversation? = null

    /**
     * 為指定 Persona 建立新的對話 Session。
     * 呼叫此方法會關閉之前的 Conversation。
     *
     * @param persona 角色人格設定
     * @param history 先前的對話歷史（用於恢復上下文）
     */
    fun startSession(persona: Persona, history: List<ChatMessage> = emptyList()) {
        // 關閉舊的 Conversation
        closeSession()

        if (!llmEngine.isReady) {
            Log.w(TAG, "引擎未就緒，跳過 Conversation 建立")
            return
        }

        try {
            // 將歷史對話轉為 LiteRT-LM Message 格式
            val initialMessages = history.takeLast(MAX_HISTORY_ROUNDS * 2).map { msg ->
                if (msg.role == "user") {
                    Message.user(msg.content)
                } else {
                    Message.model(msg.content)
                }
            }

            activeConversation = llmEngine.createConversation(
                systemInstruction = persona.systemPrompt + "\n" +
                        "【日常聊天模式：請像朋友一樣自然地跟我聊天。嚴格遵守你的角色設定，用該角色的口氣說話，絕對不要表現得像 AI 助手或回覆客套話。說話要口語化且簡短，像在通訊軟體上互動。除非我主動問起，否則不要主動提你的背景故事。只輸出對話文字，不要加括號或額外說明。】",
                initialMessages = initialMessages
            )

            Log.i(TAG, "對話 Session 已建立，角色：${persona.name}")
        } catch (e: Exception) {
            Log.e(TAG, "建立 Session 失敗", e)
        }
    }

    /**
     * 進行對話推論（同步模式）。
     *
     * @param persona 當前角色的 Persona 設定
     * @param history 對話歷史（最近 N 輪）
     * @param userInput 使用者當前輸入
     * @return AI 角色回應文字
     */
    suspend fun generateResponse(
        persona: Persona,
        history: List<ChatMessage>,
        userInput: String
    ): String {
        // 如果引擎未就緒或 Conversation 未建立，使用 Fallback
        val conversation = activeConversation
        if (!llmEngine.isReady || conversation == null) {
            Log.w(TAG, "引擎未就緒，使用 Fallback 回應")
            delay((1000L..3000L).random())
            return FALLBACK_RESPONSES.random()
        }

        return try {
            Log.i(TAG, "開始推論，使用者輸入: ${userInput.take(30)}...")

            val response = withContext(Dispatchers.IO) {
                conversation.sendMessage(userInput)
            }

            val text = response.toString()
            Log.i(TAG, "推論完成: ${text.take(50)}...")
            text.ifBlank { FALLBACK_RESPONSES.random() }

        } catch (e: Exception) {
            Log.e(TAG, "推論失敗，使用 Fallback", e)
            FALLBACK_RESPONSES.random()
        }
    }

    /**
     * 進行對話推論（串流模式 — 逐 token 產生）。
     *
     * @param userInput 使用者輸入
     * @return Kotlin Flow，逐步產生回應文字
     */
    fun generateResponseStream(userInput: String): Flow<String> {
        val conversation = activeConversation
        if (!llmEngine.isReady || conversation == null) {
            Log.w(TAG, "引擎未就緒，使用 Fallback 串流回應")
            return flowOf(FALLBACK_RESPONSES.random())
        }

        return llmEngine.inferStreaming(conversation, userInput)
            .map { message -> message.toString() }
            .catch { e ->
                Log.e(TAG, "串流推論錯誤", e)
                emit(FALLBACK_RESPONSES.random())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * 關閉當前對話 Session。
     */
    fun closeSession() {
        try {
            activeConversation?.close()
            activeConversation = null
            Log.i(TAG, "對話 Session 已關閉")
        } catch (e: Exception) {
            Log.e(TAG, "關閉 Session 時發生錯誤", e)
        }
    }

    /**
     * 建構完整推論 Prompt（保留供除錯用）。
     */
    fun buildPrompt(
        persona: Persona,
        history: List<ChatMessage>,
        userInput: String
    ): String {
        val sb = StringBuilder()

        sb.appendLine("<system>")
        sb.appendLine(persona.systemPrompt)
        sb.appendLine("</system>")
        sb.appendLine()

        if (history.isNotEmpty()) {
            sb.appendLine("<conversation_history>")
            val recentHistory = history.takeLast(MAX_HISTORY_ROUNDS * 2)
            for (msg in recentHistory) {
                val roleLabel = if (msg.role == "user") "User" else "Assistant"
                sb.appendLine("$roleLabel: ${msg.content}")
            }
            sb.appendLine("</conversation_history>")
            sb.appendLine()
        }

        sb.appendLine("<user>")
        sb.appendLine(userInput)
        sb.appendLine("</user>")
        sb.appendLine()

        sb.appendLine("<assistant>")

        return sb.toString()
    }
}
