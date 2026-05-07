package com.objectpersona.app.util

import java.security.MessageDigest

/**
 * Hash 工具 — 生成物體唯一識別碼。
 * 使用 SHA-256 hash 前 8 碼作為 object_id。
 */
object HashUtils {
    fun generateObjectId(description: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(description.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(8)
    }
}
