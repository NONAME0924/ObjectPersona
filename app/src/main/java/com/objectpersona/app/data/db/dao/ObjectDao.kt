package com.objectpersona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.objectpersona.app.data.db.entity.ObjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 物體資料存取物件。
 * 提供物體的 CRUD 操作與歷史列表查詢。
 */
@Dao
interface ObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: ObjectEntity)

    @Update
    suspend fun updateObject(obj: ObjectEntity)

    @Query("SELECT * FROM objects WHERE object_id = :objectId")
    suspend fun getObjectById(objectId: String): ObjectEntity?

    @Query("SELECT * FROM objects ORDER BY last_active_at DESC")
    fun getAllObjectsFlow(): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM objects ORDER BY last_active_at DESC")
    suspend fun getAllObjects(): List<ObjectEntity>

    @Query("UPDATE objects SET last_active_at = :timestamp WHERE object_id = :objectId")
    suspend fun updateLastActiveTime(objectId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM objects WHERE object_id = :objectId")
    suspend fun deleteObject(objectId: String)

    @Query("SELECT COUNT(*) FROM objects")
    suspend fun getObjectCount(): Int
}
