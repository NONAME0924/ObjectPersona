package com.objectpersona.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 物體記錄 Entity — 對應 spec F-06 的 objects 表。
 * 每個物體以 SHA-256 hash 前 8 碼作為唯一識別。
 */
@Entity(tableName = "objects")
data class ObjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "persona_name")
    val personaName: String,

    @ColumnInfo(name = "persona_gender")
    val personaGender: String, // "male" or "female"

    @ColumnInfo(name = "persona_personality")
    val personaPersonality: String, // JSON array string: ["固執", "惜字如金"]

    @ColumnInfo(name = "persona_style")
    val personaStyle: String,

    @ColumnInfo(name = "persona_weakness")
    val personaWeakness: String,

    @ColumnInfo(name = "persona_background")
    val personaBackground: String,

    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,

    @ColumnInfo(name = "emoji")
    val emoji: String = "🔮",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long = System.currentTimeMillis()
)
