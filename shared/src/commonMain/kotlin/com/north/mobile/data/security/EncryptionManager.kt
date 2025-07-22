package com.north.mobile.data.security

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing encryption keys and encrypted data storage
 * Provides secure key generation, storage, and data encryption/decryption
 */
interface EncryptionManager {
    /**
     * Initialize the encryption manager and generate/retrieve master key
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Generate a new encryption key for database encryption
     */
    suspend fun generateDatabaseKey(): Result<String>
    
    /**
     * Retrieve the database encryption key
     */
    suspend fun getDatabaseKey(): Result<String>
    
    /**
     * Encrypt sensitive data using AES encryption
     */
    suspend fun encrypt(data: String, keyAlias: String = DEFAULT_KEY_ALIAS): Result<EncryptedData>
    
    /**
     * Decrypt previously encrypted data
     */
    suspend fun decrypt(encryptedData: EncryptedData, keyAlias: String = DEFAULT_KEY_ALIAS): Result<String>
    
    /**
     * Check if encryption is available on this device
     */
    fun isEncryptionAvailable(): Boolean
    
    /**
     * Clear all encryption keys (for logout/data deletion)
     */
    suspend fun clearKeys(): Result<Unit>
    
    companion object {
        const val DEFAULT_KEY_ALIAS = "north_default_key"
        const val DATABASE_KEY_ALIAS = "north_database_key"
    }
}

/**
 * Represents encrypted data with initialization vector
 */
data class EncryptedData(
    val encryptedContent: ByteArray,
    val iv: ByteArray,
    val keyAlias: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EncryptedData

        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (keyAlias != other.keyAlias) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedContent.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + keyAlias.hashCode()
        return result
    }
}

/**
 * Exception thrown when encryption operations fail
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)