package com.objectpersona.app.service

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Content
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * F-01：Vision 模組 — 物體辨識與描述。
 *
 * 使用 Gemma 4 E2B 多模態能力直接解讀圖像，不需要額外的物體辨識模型。
 * 透過 LiteRT-LM 的 Content.ImageBytes 傳入圖片，搭配文字 Prompt 進行推論。
 */
@Singleton
class VisionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmEngine: LlmEngineService
) {
    companion object {
        private const val TAG = "VisionService"

        /**
         * Vision System Prompt — 指示 Gemma 4 如何描述物體。
         */
        const val VISION_SYSTEM_PROMPT = """你是一個物體描述專家。請用繁體中文回答。"""

        /**
         * Vision Prompt（傳入 Gemma 4 的提示詞）。
         */
        const val VISION_PROMPT = """請描述這張圖片中最主要的物體。
描述格式：物體名稱、外觀特徵、材質、給你的感覺（2～3句話）。
不要描述背景，只描述主體物體。"""

        // Fallback Mock 描述（引擎未就緒時使用）
        private val FALLBACK_DESCRIPTIONS = listOf(
            "一個白色陶瓷咖啡杯，杯身有細裂紋，略帶古舊感。杯口微微泛黃，像是裝過無數杯深色咖啡留下的痕跡。",
            "一本藍色封面的精裝書，書脊已經有些彎曲，書頁泛黃。封面上隱約可見燙金字樣，散發著歲月的沉穩感。",
            "一盆小型多肉植物，深綠色的葉片厚實飽滿，盆器是灰色水泥材質。整體給人寧靜而堅韌的感覺。",
            "一隻棕色毛絨玩偶熊，耳朵微微歪斜，肚子上有一顆紅色扣子。看起來被主人抱過很多次，帶著溫暖的舊感。",
            "一支銀色的金屬原子筆，筆身有些許刮痕，筆夾略微彎曲。它像是經歷過無數會議和筆記的老兵。"
        )
    }

    /**
     * 分析圖像並回傳物體描述。
     *
     * @param bitmap 從相機擷取的圖像
     * @return 物體描述文字
     */
    suspend fun analyzeImage(bitmap: Bitmap?): String {
        // 如果引擎未就緒，回傳明確錯誤
        if (!llmEngine.isReady) {
            return "ERROR: AI 引擎尚未就緒，請檢查模型載入狀態。"
        }
        if (bitmap == null) {
            return "ERROR: 無法讀取相機圖片。"
        }

        return try {
            Log.i(TAG, "開始縮放圖片...")
            // ✅ 將圖片縮放到最大 896px (LiteRT 最穩定尺寸)
            val resizedBitmap = resizeBitmap(bitmap, 896)

            Log.i(TAG, "開始使用 Gemma 4 E2B 分析圖像 (Resized)...")
            val imageBytes = bitmapToByteArray(resizedBitmap)

            // 使用多模態推論（文字 + 圖片）
            val result = llmEngine.inferOnce(
                systemInstruction = VISION_SYSTEM_PROMPT,
                contents = listOf(
                    Content.Text(VISION_PROMPT),
                    Content.ImageBytes(imageBytes)
                )
            )

            Log.i(TAG, "圖像分析完成: ${result.take(50)}...")
            if (result.isBlank()) {
                "ERROR: AI 回傳了空內容，可能無法辨識此物體。"
            } else {
                result
            }

        } catch (e: Exception) {
            Log.e(TAG, "圖像分析失敗: ${e.message}", e)
            "ERROR: 圖像分析過程發生錯誤: ${e.message}"
        }
    }

    private fun resizeBitmap(source: Bitmap, maxLength: Int = 896): Bitmap {
        if (source.width <= maxLength && source.height <= maxLength) return source
        val aspectRatio = source.width.toFloat() / source.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (source.width > source.height) {
            targetWidth = maxLength
            targetHeight = (maxLength / aspectRatio).toInt()
        } else {
            targetHeight = maxLength
            targetWidth = (maxLength * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * 將 Bitmap 壓縮為 JPEG ByteArray (90% 品質)。
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    /**
     * 將圖片儲存到暫存目錄，供後續畫面顯示。
     */
    fun saveImageToCache(bitmap: Bitmap): String {
        val file = File(context.cacheDir, "last_captured_object.jpg")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "儲存暫存圖片失敗", e)
            return ""
        }
    }
}
