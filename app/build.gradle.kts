plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.objectpersona.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.objectpersona.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

/**
 * 自動推送模型到手機。
 * 模型檔案放在專案的 models/ 資料夾，每次 Run 時自動 adb push 到手機。
 * 只有在模型檔案存在時才會執行，不會影響正常編譯。
 */
tasks.register("pushModel") {
    group = "install"
    description = "自動將 Gemma 4 模型推送到手機"

    doLast {
        val modelFile = file("../models/gemma-4-E2B-it.litertlm")
        if (modelFile.exists()) {
            // 先確保手機上的目標資料夾存在
            ProcessBuilder("adb", "shell", "mkdir", "-p",
                "/sdcard/Android/data/com.objectpersona.app/files/models/")
                .inheritIO().start().waitFor()

            // 推送模型
            val result = ProcessBuilder("adb", "push", modelFile.absolutePath,
                "/sdcard/Android/data/com.objectpersona.app/files/models/")
                .inheritIO().start().waitFor()

            if (result == 0) {
                println("✅ 模型已推送到手機！")
            } else {
                println("⚠️ 模型推送失敗，請確認手機已連線並開啟 USB 偵錯。")
            }
        } else {
            println("⚠️ 模型檔不存在於 models/ 資料夾，跳過推送。")
            println("   請將 gemma-4-E2B-it.litertlm 放入專案的 models/ 資料夾。")
        }
    }
}

// 每次 installDebug 完成後自動推送模型
tasks.whenTaskAdded {
    if (name == "installDebug") {
        finalizedBy("pushModel")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose (BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking (for Edge TTS)
    implementation(libs.okhttp)

    // AI / LiteRT-LM (Gemma 4 E2B)
    implementation(libs.litertlm.android)

    // JSON parsing
    implementation(libs.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
