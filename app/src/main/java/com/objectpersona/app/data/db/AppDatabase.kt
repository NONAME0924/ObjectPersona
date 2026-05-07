package com.objectpersona.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.objectpersona.app.data.db.dao.MessageDao
import com.objectpersona.app.data.db.dao.ObjectDao
import com.objectpersona.app.data.db.entity.MessageEntity
import com.objectpersona.app.data.db.entity.ObjectEntity

/**
 * ObjectPersona Room Database。
 * 儲存物體 Persona 設定與對話歷史，實現跨 Session 記憶持續性。
 */
@Database(
    entities = [ObjectEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun objectDao(): ObjectDao
    abstract fun messageDao(): MessageDao
}
