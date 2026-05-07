package com.objectpersona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.objectpersona.app.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 訊息資料存取物件。
 * 提供對話訊息的插入與查詢，支援最近 N 輪歷史讀取。
 */
@Dao
interface MessageDao {

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    /**
     * 取得指定物體的最近 N 條訊息（用於 Context Window）。
     * 預設 limit = 20（10 輪 × 2 條/輪）。
     */
    @Query("""
        SELECT * FROM messages 
        WHERE object_id = :objectId 
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessages(objectId: String, limit: Int = 20): List<MessageEntity>

    /**
     * 取得指定物體的所有訊息（用於完整歷史顯示），以 Flow 響應式更新。
     */
    @Query("""
        SELECT * FROM messages 
        WHERE object_id = :objectId 
        ORDER BY created_at ASC
    """)
    fun getMessagesFlow(objectId: String): Flow<List<MessageEntity>>

    /**
     * 取得指定物體最後一條訊息（用於歷史列表預覽）。
     */
    @Query("""
        SELECT * FROM messages 
        WHERE object_id = :objectId 
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    suspend fun getLastMessage(objectId: String): MessageEntity?

    /**
     * 取得指定物體的訊息總數。
     */
    @Query("SELECT COUNT(*) FROM messages WHERE object_id = :objectId")
    suspend fun getMessageCount(objectId: String): Int

    /**
     * 刪除指定物體的所有訊息。
     */
    @Query("DELETE FROM messages WHERE object_id = :objectId")
    suspend fun deleteMessagesForObject(objectId: String)
}
