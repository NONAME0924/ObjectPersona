package com.objectpersona.app.data.repository

import com.objectpersona.app.data.db.dao.ObjectDao
import com.objectpersona.app.data.db.entity.ObjectEntity
import com.objectpersona.app.data.model.Persona
import com.objectpersona.app.util.HashUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 物體 Repository — 管理物體 Persona 的 CRUD 操作。
 * 使用 SHA-256 hash 前 8 碼作為 object_id。
 */
@Singleton
class ObjectRepository @Inject constructor(
    private val objectDao: ObjectDao
) {
    /**
     * 儲存新物體與其 Persona 設定。
     * 如果相同描述的物體已存在，使用 REPLACE 策略（建立新分支）。
     */
    suspend fun saveObject(
        description: String,
        persona: Persona,
        emoji: String
    ): String {
        val objectId = HashUtils.generateObjectId(description)
        val entity = ObjectEntity(
            objectId = objectId,
            description = description,
            personaName = persona.name,
            personaGender = persona.gender,
            personaPersonality = persona.personality.joinToString(","),
            personaStyle = persona.speechStyle,
            personaWeakness = persona.weakness,
            personaBackground = persona.background,
            systemPrompt = persona.systemPrompt,
            emoji = emoji
        )
        objectDao.insertObject(entity)
        return objectId
    }

    /**
     * 取得物體資訊（用於載入歷史記憶）。
     */
    suspend fun getObject(objectId: String): ObjectEntity? {
        return objectDao.getObjectById(objectId)
    }

    /**
     * 取得所有歷史物體列表（響應式 Flow）。
     */
    fun getAllObjectsFlow(): Flow<List<ObjectEntity>> {
        return objectDao.getAllObjectsFlow()
    }

    /**
     * 更新最後互動時間。
     */
    suspend fun updateLastActiveTime(objectId: String) {
        objectDao.updateLastActiveTime(objectId)
    }

    /**
     * 更新物體的 Persona 設定（使用者編輯後）。
     * 按照設計，修改 Persona 會建立新分支（新的 object_id）。
     */
    suspend fun updatePersona(
        objectId: String,
        persona: Persona,
        emoji: String
    ) {
        val existing = objectDao.getObjectById(objectId) ?: return
        val updated = existing.copy(
            personaName = persona.name,
            personaGender = persona.gender,
            personaPersonality = persona.personality.joinToString(","),
            personaStyle = persona.speechStyle,
            personaWeakness = persona.weakness,
            personaBackground = persona.background,
            systemPrompt = persona.systemPrompt,
            emoji = emoji,
            lastActiveAt = System.currentTimeMillis()
        )
        objectDao.updateObject(updated)
    }

    /**
     * 刪除物體及其所有對話記錄（CASCADE）。
     */
    suspend fun deleteObject(objectId: String) {
        objectDao.deleteObject(objectId)
    }

    /**
     * 將 ObjectEntity 轉換為 Persona 領域模型。
     */
    fun entityToPersona(entity: ObjectEntity): Persona {
        return Persona(
            name = entity.personaName,
            gender = entity.personaGender,
            personality = entity.personaPersonality.split(",").filter { it.isNotBlank() },
            speechStyle = entity.personaStyle,
            weakness = entity.personaWeakness,
            background = entity.personaBackground,
            systemPrompt = entity.systemPrompt
        )
    }
}
