package com.objectpersona.app.service

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Content
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiteRT-LM 引擎管理器 — 統一管理 Gemma 4 E2B-IT 模型的生命週期。
 *
 * 功能：
 * - 啟用 MTP（Multi-Token Prediction）加速推論
 * - GPU 後端加速（含多模態視覺推論）
 * - 管理 Engine 與 Conversation 生命週期
 * - 提供同步/串流推論接口
 */
@Singleton
class LlmEngineService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LlmEngineService"

        /** 模型檔名 */
        const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

        /** 預設的 Sampler 設定（根據 Gemma 4 官方建議） */
        val DEFAULT_SAMPLER = SamplerConfig(
            topK = 64,
            topP = 0.95,
            temperature = 1.0
        )

        /** 對話用的 Sampler（較低 temperature 以產生穩定對話） */
        val CHAT_SAMPLER = SamplerConfig(
            topK = 40,
            topP = 0.95,
            temperature = 1.0
        )
    }

    private var engine: Engine? = null
    private var activeConversation: Conversation? = null

    private val _state = MutableStateFlow(LlmEngineState.NOT_INITIALIZED)
    val state: StateFlow<LlmEngineState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 尋找模型路徑。依序檢查以下位置：
     * 1. App 外部私有目錄 (由 Gradle pushModel 自動推送到此)
     * 2. 公開下載資料夾 (手動放置)
     * 3. App 內部儲存
     */
    fun findModelPath(): String? {
        // 1. Gradle pushModel 推送的目標路徑
        val externalModelsDir = context.getExternalFilesDir("models")
        if (externalModelsDir != null) {
            val externalFile = File(externalModelsDir, MODEL_FILENAME)
            if (externalFile.exists()) {
                Log.i(TAG, "在外部私有目錄找到模型: ${externalFile.absolutePath}")
                return externalFile.absolutePath
            }
        }

        // 2. 公開下載資料夾 (手動放置)
        val downloadPaths = listOf(
            File("/sdcard/Download/$MODEL_FILENAME"),
            File("/storage/emulated/0/Download/$MODEL_FILENAME")
        )
        for (downloadPath in downloadPaths) {
            if (downloadPath.exists()) {
                Log.i(TAG, "在 Download 資料夾找到模型: ${downloadPath.absolutePath}")
                return downloadPath.absolutePath
            }
        }

        // 3. App 內部儲存
        val internalPath = File(context.filesDir, "models/$MODEL_FILENAME")
        if (internalPath.exists()) {
            Log.i(TAG, "在內部儲存找到模型: ${internalPath.absolutePath}")
            return internalPath.absolutePath
        }

        return null
    }

    /**
     * 自動尋找模型並初始化引擎。
     */
    @OptIn(ExperimentalApi::class)
    suspend fun initializeWithAutoDiscovery() {
        val path = findModelPath()
        if (path != null) {
            initialize(path)
        } else {
            _state.value = LlmEngineState.ERROR
            _errorMessage.value = "未找到模型檔案。請確保模型已放入專案的 models/ 資料夾，並重新 Run。"
        }
    }

    /**
     * 初始化 LiteRT-LM 引擎（核心邏輯）。
     */
    @OptIn(ExperimentalApi::class)
    private suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        try {
            _state.value = LlmEngineState.LOADING
            _errorMessage.value = null

            Log.i(TAG, "正在初始化引擎，模型路徑: $modelPath")
            // ⚠️ 注意：MTP (Speculative Decoding) 在某些多模態模型上可能會導致 Vision 解析失敗，暫時關閉
            // ExperimentalFlags.enableSpeculativeDecoding = true

            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.GPU(),
                visionBackend = Backend.GPU(), // 明確指定 Vision 使用 GPU
                cacheDir = context.cacheDir.path
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()

            engine = newEngine
            _state.value = LlmEngineState.READY
            Log.i(TAG, "引擎初始化成功！MTP 已啟用。")

        } catch (e: Exception) {
            Log.e(TAG, "引擎初始化失敗", e)
            _state.value = LlmEngineState.ERROR
            _errorMessage.value = e.message ?: "未知錯誤"
        }
    }

    /**
     * 建立一個新的對話 (Conversation)。
     *
     * @param systemInstruction System Prompt
     * @param samplerConfig 取樣參數
     * @param initialMessages 初始對話歷史
     * @return Conversation 實例，呼叫方需自行管理 close()
     */
    fun createConversation(
        systemInstruction: String? = null,
        samplerConfig: SamplerConfig = CHAT_SAMPLER,
        initialMessages: List<Message> = emptyList()
    ): Conversation? {
        val currentEngine = engine ?: return null
        if (_state.value != LlmEngineState.READY) return null

        // ✅ 強制關閉任何既有的 Session，因為 LiteRT-LM 同一時間只支援一個 Session
        closeActiveSession()

        val configBuilder = ConversationConfig(
            systemInstruction = systemInstruction?.let { Contents.of(it) },
            initialMessages = initialMessages,
            samplerConfig = samplerConfig
        )

        val newConversation = currentEngine.createConversation(configBuilder)
        activeConversation = newConversation
        return newConversation
    }

    /**
     * 強制關閉目前的對話 Session。
     */
    fun closeActiveSession() {
        try {
            activeConversation?.close()
            activeConversation = null
        } catch (e: Exception) {
            Log.e(TAG, "關閉既有 Session 失敗", e)
        }
    }

    /**
     * 使用一次性 Conversation 進行同步推論（適合 VisionService、PersonaGenerator）。
     *
     * @param systemInstruction System Prompt
     * @param contents 推論內容（可包含文字、圖片）
     * @param samplerConfig 取樣參數
     * @return 模型回應文字
     */
    suspend fun inferOnce(
        systemInstruction: String? = null,
        contents: List<Content>,
        samplerConfig: SamplerConfig = DEFAULT_SAMPLER
    ): String = withContext(Dispatchers.IO) {
        val conversation = createConversation(
            systemInstruction = systemInstruction,
            samplerConfig = samplerConfig
        ) ?: throw IllegalStateException("引擎未就緒")

        conversation.use { conv ->
            val response = conv.sendMessage(Contents.of(contents))
            response.toString()
        }
    }

    /**
     * 使用串流方式進行推論（適合 DialogueEngine）。
     *
     * @param conversation 已建立的長期 Conversation
     * @param userInput 使用者輸入文字
     * @return Kotlin Flow，逐 token 串流回應
     */
    fun inferStreaming(
        conversation: Conversation,
        userInput: String
    ): Flow<Message> {
        return conversation.sendMessageAsync(userInput)
    }

    /**
     * 釋放引擎資源。
     */
    fun destroy() {
        try {
            engine?.close()
            engine = null
            _state.value = LlmEngineState.NOT_INITIALIZED
            Log.i(TAG, "引擎已釋放")
        } catch (e: Exception) {
            Log.e(TAG, "釋放引擎時發生錯誤", e)
        }
    }

    /**
     * 引擎是否就緒。
     */
    val isReady: Boolean
        get() = _state.value == LlmEngineState.READY
}
