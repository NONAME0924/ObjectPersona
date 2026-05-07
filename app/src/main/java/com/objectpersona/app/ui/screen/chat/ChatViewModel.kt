package com.objectpersona.app.ui.screen.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.objectpersona.app.data.model.ChatMessage
import com.objectpersona.app.data.model.ConversationState
import com.objectpersona.app.data.model.Persona
import com.objectpersona.app.data.repository.MessageRepository
import com.objectpersona.app.data.repository.ObjectRepository
import com.objectpersona.app.service.DialogueEngine
import com.objectpersona.app.service.SpeechService
import com.objectpersona.app.service.TtsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val persona: Persona? = null,
    val emoji: String = "🔮",
    val gender: String = "male",
    val conversationState: ConversationState = ConversationState.IDLE,
    val latestAiMessage: String? = null,
    val latestUserMessage: String? = null,
    val isLoading: Boolean = true,
    /** 串流推論中逐字累積的文字 */
    val streamingText: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val objectRepository: ObjectRepository,
    private val messageRepository: MessageRepository,
    private val dialogueEngine: DialogueEngine,
    private val ttsService: TtsService
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val SESSION_REFRESH_INTERVAL = 5
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentObjectId: String = ""
    private var chatHistory = mutableListOf<ChatMessage>()

    /** 語音辨識服務 */
    private val speechService = SpeechService(application)

    /** 當前推論 Job（可取消） */
    private var inferenceJob: Job? = null

    private var useMicrophone = false

    fun loadObject(objectId: String, useMic: Boolean = false) {
        // 如果正在載入中，不要重複觸發
        if (_uiState.value.isLoading && currentObjectId == objectId) return
        
        currentObjectId = objectId
        useMicrophone = useMic

        viewModelScope.launch {
            val entity = objectRepository.getObject(objectId) ?: return@launch
            val persona = objectRepository.entityToPersona(entity)
            val history = messageRepository.getRecentHistory(objectId)
            chatHistory = history.toMutableList()

            // 為此角色建立 AI 對話 Session
            dialogueEngine.startSession(persona, history)

            objectRepository.updateLastActiveTime(objectId)

            _uiState.value = ChatUiState(
                persona = persona,
                emoji = entity.emoji,
                gender = entity.personaGender,
                conversationState = if (useMic) ConversationState.IDLE else ConversationState.LISTENING,
                latestAiMessage = history.lastOrNull { it.role == "assistant" }?.content,
                latestUserMessage = history.lastOrNull { it.role == "user" }?.content,
                isLoading = false
            )

            // 只有使用麥克風時才初始化語音辨識
            if (useMic) {
                initializeSpeechRecognition()
                startListening()
            }
        }
    }

    /**
     * 初始化語音辨識。
     */
    private fun initializeSpeechRecognition() {
        speechService.initialize { recognizedText ->
            // 語音辨識結果回調（在主執行緒）
            Log.i(TAG, "語音辨識結果: $recognizedText")
            onUserSpoke(recognizedText)
        }

        // 監聽 SpeechService 的狀態（LISTENING, RECOGNIZING 等）
        viewModelScope.launch {
            speechService.state.collect { sttState ->
                val currentState = _uiState.value.conversationState
                // 只有在聆聽/辨識階段才同步 STT 狀態
                if (currentState != ConversationState.THINKING &&
                    currentState != ConversationState.SPEAKING
                ) {
                    _uiState.value = _uiState.value.copy(conversationState = sttState)
                }
            }
        }
    }

    /**
     * 開始聆聽。
     */
    fun startListening() {
        _uiState.value = _uiState.value.copy(conversationState = ConversationState.LISTENING)
        speechService.startListening()
    }

    /**
     * 使用者說話後的完整處理流程：
     * 辨識 → 思考（串流推論）→ 說話（TTS）→ 再聆聽
     */
    fun onUserSpoke(text: String) {
        inferenceJob?.cancel()
        inferenceJob = viewModelScope.launch {
            // 1. 停止聆聽
            speechService.stopListening()

            // 2. 更新 UI：顯示使用者訊息
            _uiState.value = _uiState.value.copy(
                latestUserMessage = text,
                conversationState = ConversationState.THINKING,
                streamingText = ""
            )

            // 3. 儲存使用者訊息
            messageRepository.saveMessage(currentObjectId, "user", text)
            chatHistory.add(ChatMessage("user", text))

            // 4. AI 串流推論
            val persona = _uiState.value.persona ?: return@launch
            val fullResponse = StringBuilder()

            dialogueEngine.generateResponseStream(text).collect { chunk ->
                fullResponse.append(chunk)
                _uiState.value = _uiState.value.copy(
                    streamingText = fullResponse.toString()
                )
            }

            val response = fullResponse.toString().ifBlank {
                // Fallback
                "嗯...讓我想想。"
            }

            // 5. 推論完成，儲存 AI 回應
            messageRepository.saveMessage(currentObjectId, "assistant", response)
            chatHistory.add(ChatMessage("assistant", response))

            // 6. 更新 UI + TTS 播放
            _uiState.value = _uiState.value.copy(
                latestAiMessage = response,
                streamingText = "",
                conversationState = ConversationState.SPEAKING
            )

            ttsService.speak(response, _uiState.value.gender) {
                // 7. TTS 完成 → 重新聆聽（或等待開發者輸入）
                if (useMicrophone) {
                    startListening()
                } else {
                    _uiState.value = _uiState.value.copy(
                        conversationState = ConversationState.LISTENING
                    )
                }
            }
        }
    }

    /**
     * 手動觸發模擬輸入（開發測試用）。
     */
    fun simulateUserInput() {
        val mockInputs = listOf(
            "你好啊，今天過得怎麼樣？",
            "你能跟我說說你的故事嗎？",
            "你平常都在想什麼呢？",
            "哈哈，你真有趣！",
            "我今天心情不太好..."
        )
        onUserSpoke(mockInputs.random())
    }

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        speechService.destroy()
        ttsService.stop()
        dialogueEngine.closeSession()
    }
}
