package com.objectpersona.app.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F-07：TTS 語音合成模組 — Edge TTS 真實實作。
 */
@Singleton
class TtsService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TtsService"
        const val VOICE_FEMALE = "zh-TW-HsiaoChenNeural"
        const val VOICE_MALE = "zh-TW-YunJheNeural"

        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val CHROMIUM_MAJOR_VERSION = "143"
        private const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_FULL_VERSION"

        // Windows FILETIME epoch 與 Unix epoch 的差值（秒）
        private const val WIN_EPOCH = 11644473600L
        // 秒 → 100 奈秒 ticks 的換算係數
        private const val S_TO_NS = 10_000_000L

        private const val AUDIO_FORMAT = "audio-24khz-48kbitrate-mono-mp3"

        /**
         * 產生 Sec-MS-GEC 動態認證 token (2024年底微軟新要求)
         */
        fun generateSecMsGec(): String {
            var ticks = (System.currentTimeMillis() / 1000L) + WIN_EPOCH
            ticks -= ticks % 300
            ticks *= S_TO_NS
            val strToHash = "${ticks}${TRUSTED_CLIENT_TOKEN}"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(strToHash.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02X".format(it) }
        }
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getVoiceForGender(gender: String): String {
        return if (gender.lowercase() == "male") VOICE_MALE else VOICE_FEMALE
    }

    suspend fun speak(text: String, gender: String = "female", onComplete: () -> Unit = {}) {
        if (text.isBlank()) {
            onComplete()
            return
        }

        _isSpeaking.value = true
        try {
            val voice = getVoiceForGender(gender)
            Log.i(TAG, "==> 請求 Edge TTS: $text")
            
            val audioBytes = synthesize(text, voice)
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                Log.i(TAG, "==> 合成成功，位元組大小: ${audioBytes.size}，準備播放")
                playAudio(audioBytes)
            } else {
                Log.e(TAG, "==> Edge TTS 失敗 (回傳內容為空，可能是 403 或連線問題)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "==> TTS 異常", e)
        } finally {
            _isSpeaking.value = false
            onComplete()
        }
    }

    private suspend fun synthesize(text: String, voice: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val connectionId = UUID.randomUUID().toString().replace("-", "")
            val requestId = UUID.randomUUID().toString().replace("-", "")
            val audioBuffer = ByteArrayOutputStream()
            val latch = CountDownLatch(1)
            var success = false

            val secMsGec = generateSecMsGec()
            val wsUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                    "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                    "&Sec-MS-GEC=$secMsGec" +
                    "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
                    "&ConnectionId=$connectionId"

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Pragma", "no-cache")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 Edg/$CHROMIUM_MAJOR_VERSION.0.0.0")
                .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(buildConfigMessage(connectionId))
                    webSocket.send(buildSsmlMessage(requestId, text, voice))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("turn.end")) {
                        success = true
                        latch.countDown()
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    try {
                        val data = bytes.toByteArray()
                        if (data.size > 2) {
                            val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                            if (headerLen < data.size) {
                                audioBuffer.write(data, headerLen + 2, data.size - headerLen - 2)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析音訊失敗", e)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket 失敗: ${t.message} (HTTP ${response?.code})")
                    latch.countDown()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    latch.countDown()
                }
            }

            val webSocket = client.newWebSocket(request, listener)
            // 將逾時增加到 60 秒，確保長文本合成成功
            latch.await(60, TimeUnit.SECONDS)
            webSocket.close(1000, "done")
            if (success) audioBuffer.toByteArray() else null
        }

    private suspend fun playAudio(audioBytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "tts_temp.mp3")
            tempFile.writeBytes(audioBytes)

            val completionLatch = CountDownLatch(1)
            withContext(Dispatchers.Main) {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(tempFile.absolutePath)
                mediaPlayer.setOnCompletionListener {
                    it.release()
                    completionLatch.countDown()
                }
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer Error: what=$what, extra=$extra")
                    mp.release()
                    completionLatch.countDown()
                    true
                }
                mediaPlayer.setOnPreparedListener { mp ->
                    mp.start()
                }
                mediaPlayer.prepareAsync()
            }
            // 播放逾時增加到 120 秒，支援長語音
            completionLatch.await(120, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "播放失敗", e)
        }
    }

    private fun buildConfigMessage(connectionId: String): String {
        val timestamp = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US).format(Date())
        return "X-Timestamp:$timestamp\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
            """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"$AUDIO_FORMAT"}}}}"""
    }

    private fun buildSsmlMessage(requestId: String, text: String, voice: String): String {
        val timestamp = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US).format(Date())
        val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
        val ssml = """<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-TW'><voice name='$voice'><prosody pitch='+0Hz' rate='+0%' volume='+0%'>$escapedText</prosody></voice></speak>"""
        return "X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:$timestamp\r\nPath:ssml\r\n\r\n$ssml"
    }

    fun stop() {}
    fun release() {}
}
