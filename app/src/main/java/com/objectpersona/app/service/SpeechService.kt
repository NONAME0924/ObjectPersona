package com.objectpersona.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.objectpersona.app.data.model.ConversationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * F-03：語音輸入模組（STT）— 常時聆聽封裝。
 * 使用 Android SpeechRecognizer，靜音 1.5 秒自動截止。
 */
class SpeechService(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isActive = false

    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private var onResult: ((String) -> Unit)? = null

    private var silenceJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    fun initialize(onSpeechResult: (String) -> Unit) {
        onResult = onSpeechResult
        android.util.Log.i("SpeechService", "初始化 SpeechRecognizer")
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                android.util.Log.i("SpeechService", "準備好了，請說話...")
                _state.value = ConversationState.LISTENING
            }
            override fun onBeginningOfSpeech() {
                android.util.Log.i("SpeechService", "偵測到說話開始")
                _state.value = ConversationState.RECOGNIZING
                resetSilenceTimer()
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                android.util.Log.i("SpeechService", "說話結束")
                _state.value = ConversationState.THINKING
                silenceJob?.cancel()
            }
            override fun onError(error: Int) {
                android.util.Log.e("SpeechService", "錯誤發生: $error")
                silenceJob?.cancel()
                if (isActive) restartListening()
            }
            override fun onResults(results: Bundle?) {
                silenceJob?.cancel()
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                android.util.Log.i("SpeechService", "辨識結果: $text")
                if (!text.isNullOrBlank()) {
                    onResult?.invoke(text)
                } else if (isActive) {
                    restartListening()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                android.util.Log.d("SpeechService", "部分結果: $partialText")
                resetSilenceTimer()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun resetSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = scope.launch {
            kotlinx.coroutines.delay(2000L) // 2秒沒有新的 partial result 就強制結束
            if (isActive && _state.value == ConversationState.RECOGNIZING) {
                stopListening()
            }
        }
    }

    fun startListening() {
        isActive = true
        silenceJob?.cancel()
        recognizer?.startListening(buildIntent())
    }

    fun stopListening() {
        isActive = false
        silenceJob?.cancel()
        recognizer?.stopListening()
        _state.value = ConversationState.IDLE
    }

    fun restartListening() {
        if (isActive) {
            silenceJob?.cancel()
            recognizer?.cancel()
            recognizer?.startListening(buildIntent())
        }
    }

    fun updateState(state: ConversationState) {
        _state.value = state
    }

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-TW")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
    }

    fun destroy() {
        isActive = false
        silenceJob?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}
