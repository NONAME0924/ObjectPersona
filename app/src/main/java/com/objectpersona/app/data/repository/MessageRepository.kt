package com.objectpersona.app.data.repository

import com.objectpersona.app.data.db.dao.MessageDao
import com.objectpersona.app.data.db.entity.MessageEntity
import com.objectpersona.app.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 訊息 Repository — 管理對話歷史的存取。
 * 提供最近 N 輪歷史讀取（用於 Context Window）與完整歷史查詢。
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    /**
     * 儲存一條對話訊息。
     */
    suspend fun saveMessage(objectId: String, role: String, content: String) {
        val entity = MessageEntity(
            objectId = objectId,
            role = role,
            content = content
        )
        messageDao.insertMessage(entity)
    }

    /**
     * 取得最近 N 輪對話歷史（用於 F-04 Context Window）。
     * @param rounds 輪數（預設 10 輪 = 20 條訊息）
     * @return 按時間正序排列的訊息列表
     */
    suspend fun getRecentHistory(objectId: String, rounds: Int = 10): List<ChatMessage> {
        val messages = messageDao.getRecentMessages(objectId, limit = rounds * 2)
        return messages
            .sortedBy { it.createdAt }
            .map { it.toChatMessage() }
    }

    /**
     * 取得所有訊息（響應式 Flow，用於 UI 顯示）。
     */
    fun getMessagesFlow(objectId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesFlow(objectId).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    /**
     * 取得最後一條訊息（用於歷史列表預覽）。
     */
    suspend fun getLastMessage(objectId: String): ChatMessage? {
        return messageDao.getLastMessage(objectId)?.toChatMessage()
    }

    /**
     * 取得訊息總數。
     */
    suspend fun getMessageCount(objectId: String): Int {
        return messageDao.getMessageCount(objectId)
    }

    private fun MessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            timestamp = createdAt
        )
    }
}
