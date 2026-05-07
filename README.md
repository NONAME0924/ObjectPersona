# ObjectPersona AI

[繁體中文](#繁體中文) | [English](#english)

---

<a name="繁體中文"></a>
## 🇹🇼 繁體中文說明

ObjectPersona 是一個基於 Android 的創新應用，它利用 Google LiteRT-LM (Gemma 4) 技術，讓現實世界中的任何物體都能「活」過來。透過手機鏡頭拍攝物體，AI 會自動為其生成獨特的人格（Persona），並讓您能以語音與該物體進行富有個性的即時對話。

### ✨ 核心功能
*   **物體視覺辨識 (Vision)**：拍下物體，AI 自動解讀其外觀特徵與材質。
*   **動態人格生成 (Persona)**：根據物體特性，隨機生成具備名字、個性、弱點與背景故事的角色。
*   **自然語音對話 (Voice Chat)**：支援常時聆聽 (Always-on Listening)，無需按鈕即可與角色聊天。
*   **持久化記憶 (Memory)**：同一物體會保留對話歷史，隨著互動次數增加，角色會越來越了解您。
*   **高品質語音 (Edge TTS)**：整合 Microsoft Edge TTS，提供極其自然的語音回應。

### 🛠️ 技術架構
*   **LLM 引擎**: Google LiteRT (前稱 TensorFlow Lite Runtime) 運算 Gemma 4 E2B-IT 模型。
*   **開發語言**: Kotlin 1.9+
*   **UI 框架**: Jetpack Compose (Modern Android UI)
*   **資料庫**: Room Database (儲存對話歷史與角色卡片)
*   **依賴注入**: Hilt
*   **非同步**: Coroutines & Flow

### 🚀 快速開始 (開發者指南)
如果您想修改或執行此專案，請遵循以下步驟：

1.  **準備模型檔案**:
    *   由於模型檔案體積巨大 (約 2.5GB)，本儲存庫已將其忽略。
    *   檔案名稱：`gemma-4-E2B-it.litertlm`
    *   放置路徑：`models/gemma-4-E2B-it.litertlm`
2.  **開啟專案**:
    *   使用 Android Studio (Ladybug 2024.2.1+) 開啟 `ObjectPersona` 資料夾。
3.  **環境需求**:
    *   Android 12 (API 31) 或以上。
    *   建議 8GB RAM 以獲得最佳推論速度。

---

<a name="english"></a>
## 🇺🇸 English Description

ObjectPersona is an innovative Android application that leverages Google LiteRT-LM (Gemma 4) technology to bring everyday objects to life. By capturing an object through the camera, AI automatically generates a unique Persona for it, enabling you to have real-time, voice-based conversations with the object.

### ✨ Key Features
*   **Visual Recognition (Vision)**: Capture an object and the AI interprets its appearance, materials, and "vibe."
*   **Dynamic Persona Generation**: Automatically creates a character with a name, personality, weaknesses, and a background story based on the object's traits.
*   **Natural Voice Interaction**: Features always-on listening for a seamless, button-free chatting experience.
*   **Persistent Memory**: Retains conversation history for the same object across different sessions.
*   **Premium Speech (Edge TTS)**: Integrates Microsoft Edge TTS for highly natural and expressive voice responses.

### 🛠️ Technical Stack
*   **LLM Engine**: Google LiteRT (formerly TensorFlow Lite Runtime) running the Gemma 4 E2B-IT model.
*   **Language**: Kotlin 1.9+
*   **UI Framework**: Jetpack Compose
*   **Database**: Room Database (for chat history and character cards)
*   **Dependency Injection**: Hilt
*   **Asynchrony**: Coroutines & Flow

### 🚀 Quick Start (For Developers)
To run or modify this project, please follow these steps:

1.  **Prepare the Model File**:
    *   The core model file is excluded due to its size (~2.5GB).
    *   Filename: `gemma-4-E2B-it.litertlm`
    *   Location: Place it in `models/gemma-4-E2B-it.litertlm`.
2.  **Open the Project**:
    *   Open the `ObjectPersona` folder in Android Studio (Ladybug 2024.2.1+).
3.  **Requirements**:
    *   Android 12 (API 31) or higher.
    *   8GB RAM recommended for optimal inference performance.

---

## 📂 專案結構 / Project Structure

```text
ObjectPersona/              # 專案根目錄 (Repository Root)
├── app/                    # 主要應用程式模組 (Main App Module)
├── models/                 # 存放 Gemma 模型 (Place models here - git ignored)
├── InternalDocs/           # 技術文件與規格 (Internal Docs - git ignored)
├── README.md               # 您現在的位置 (You are here!)
└── .gitignore              # Git 忽略設定
```

## ⚖️ 授權與免責聲明 / License & Disclaimer
本專案僅供學習與研究使用。語音合成技術使用 Edge TTS，需遵守其相關使用規範。模型推論完全於本地執行，保障使用者隱私。
This project is for educational and research purposes only. Voice synthesis uses Edge TTS; please adhere to their terms of use. All AI inference is performed locally to ensure user privacy.
