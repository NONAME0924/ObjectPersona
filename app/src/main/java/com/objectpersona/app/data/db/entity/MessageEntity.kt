package com.objectpersona.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 對話訊息 Entity — 對應 spec F-06 的 messages 表。
 * 儲存每輪對話的使用者輸入與 AI 回應。
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["object_id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["object_id"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    val messageId: Long = 0,

    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "role")
    val role: String, // "user" or "assistant"

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
