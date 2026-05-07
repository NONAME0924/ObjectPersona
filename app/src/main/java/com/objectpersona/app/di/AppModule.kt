package com.objectpersona.app.di

import android.content.Context
import com.objectpersona.app.service.DialogueEngine
import com.objectpersona.app.service.LlmEngineService
import com.objectpersona.app.service.PersonaGenerator
import com.objectpersona.app.service.TtsService
import com.objectpersona.app.service.VisionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI Module — 提供 Service 層實例。
 * Phase 2：所有 Service 注入 LlmEngineService 進行真實推論。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLlmEngineService(@ApplicationContext context: Context): LlmEngineService {
        return LlmEngineService(context)
    }

    @Provides
    @Singleton
    fun provideVisionService(
        @ApplicationContext context: Context,
        llmEngine: LlmEngineService
    ): VisionService {
        return VisionService(context, llmEngine)
    }

    @Provides
    @Singleton
    fun providePersonaGenerator(llmEngine: LlmEngineService): PersonaGenerator {
        return PersonaGenerator(llmEngine)
    }

    @Provides
    @Singleton
    fun provideDialogueEngine(llmEngine: LlmEngineService): DialogueEngine {
        return DialogueEngine(llmEngine)
    }

    @Provides
    @Singleton
    fun provideTtsService(@ApplicationContext context: Context): TtsService {
        return TtsService(context)
    }
}
