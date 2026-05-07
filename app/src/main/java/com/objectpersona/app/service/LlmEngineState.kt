package com.objectpersona.app.service

/**
 * LLM 引擎狀態。
 */
enum class LlmEngineState {
    /** 尚未初始化 */
    NOT_INITIALIZED,

    /** 模型載入中 */
    LOADING,

    /** 模型就緒，可進行推論 */
    READY,

    /** 載入失敗 */
    ERROR
}
