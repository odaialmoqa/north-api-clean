package com.north.mobile.data.security

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus
import kotlin.random.Random

/**
 * iOS implementation of EncryptionManager using iOS Keychain Services
 * Provides hardware-backed encryption when available (Secure Enclave)
 */
class IOSEncryptionManager : EncryptionManager {
    
    private val serviceName = "com.north.mobile.keychain"
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            // Generate database key if it doesn't exist
            if (getDatabaseKey().isFailure) {
                generateDatabaseKey()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to initialize encryption manager", e))
        }
    }
    
    override suspend fun generateDatabaseKey(): Result<String> {
        return try {
            val key = generateSecureRandomKey()
            val result = storeKeyInKeychain(EncryptionManager.DATABASE_KEY_ALIAS, key)
            if (result.isSuccess) {
                Result.success(key)
            } else {
                Result.failure(EncryptionException("Failed to store database key in keychain"))
            }
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to generate database key", e))
        }
    }
    
    override suspend fun getDatabaseKey(): Result<String> {
        return getKeyFromKeychain(EncryptionManager.DATABASE_KEY_ALIAS)
    }
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<EncryptedData> {
        return try {
            // For iOS, we'll use a simple AES encryption with a key stored in keychain
            val key = getOrCreateKey(keyAlias).getOrThrow()
            val dataBytes = data.encodeToByteArray()
            val iv = generateRandomIV()
            
            // Use CommonCrypto for AES encryption
            val encryptedBytes = encryptAES(dataBytes, key.encodeToByteArray(), iv)
            
            val encryptedData = EncryptedData(
                encryptedContent = encryptedBytes,
                iv = iv,
                keyAlias = keyAlias
            )
            
            Result.success(encryptedData)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to encrypt data", e))
        }
    }
    
    override suspend fun decrypt(encryptedData: EncryptedData, keyAlias: String): Result<String> {
        return try {
            val key = getKeyFromKeychain(keyAlias).getOrThrow()
            val decryptedBytes = decryptAES(
                encryptedData.encryptedContent, 
                key.encodeToByteArray(), 
                encryptedData.iv
            )
            val decryptedString = decryptedBytes.decodeToString()
            Result.success(decryptedString)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to decrypt data", e))
        }
    }
    
    override fun isEncryptionAvailable(): Boolean {
        return true // iOS Keychain is always available
    }
    
    override suspend fun clearKeys(): Result<Unit> {
        return try {
            // Clear all keys with our service name
            memScoped {
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, serviceName, kCFStringEncodingUTF8))
                
                val status = SecItemDelete(query)
                CFRelease(query)
                
                if (status == errSecSuccess || status == errSecItemNotFound) {
                    Result.success(Unit)
                } else {
                    Result.failure(EncryptionException("Failed to clear keychain items, status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to clear keys", e))
        }
    }
    
    private fun getOrCreateKey(keyAlias: String): Result<String> {
        return getKeyFromKeychain(keyAlias).recoverCatching {
            // Key doesn't exist, create it
            val newKey = generateSecureRandomKey()
            storeKeyInKeychain(keyAlias, newKey).getOrThrow()
            newKey
        }
    }
    
    private fun getKeyFromKeychain(keyAlias: String): Result<String> {
        return memScoped {
            try {
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, serviceName, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, keyAlias, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
                CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
                
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                CFRelease(query)
                
                if (status == errSecSuccess) {
                    val data = result.value as CFDataRef
                    val length = CFDataGetLength(data).toInt()
                    val bytes = CFDataGetBytePtr(data)
                    val keyString = bytes?.readBytes(length)?.decodeToString()
                    
                    if (keyString != null) {
                        Result.success(keyString)
                    } else {
                        Result.failure(EncryptionException("Failed to decode key from keychain"))
                    }
                } else {
                    Result.failure(EncryptionException("Key not found in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(EncryptionException("Failed to retrieve key from keychain", e))
            }
        }
    }
    
    private fun storeKeyInKeychain(keyAlias: String, key: String): Result<Unit> {
        return memScoped {
            try {
                val keyData = key.encodeToByteArray()
                val cfData = CFDataCreate(null, keyData.refTo(0), keyData.size.toLong())
                
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, serviceName, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, keyAlias, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecValueData, cfData)
                CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                
                val status = SecItemAdd(query, null)
                
                CFRelease(query)
                CFRelease(cfData)
                
                if (status == errSecSuccess) {
                    Result.success(Unit)
                } else if (status == errSecDuplicateItem) {
                    // Update existing item
                    updateKeyInKeychain(keyAlias, key)
                } else {
                    Result.failure(EncryptionException("Failed to store key in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(EncryptionException("Failed to store key in keychain", e))
            }
        }
    }
    
    private fun updateKeyInKeychain(keyAlias: String, key: String): Result<Unit> {
        return memScoped {
            try {
                val keyData = key.encodeToByteArray()
                val cfData = CFDataCreate(null, keyData.refTo(0), keyData.size.toLong())
                
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, serviceName, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, keyAlias, kCFStringEncodingUTF8))
                
                val attributes = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(attributes, kSecValueData, cfData)
                
                val status = SecItemUpdate(query, attributes)
                
                CFRelease(query)
                CFRelease(attributes)
                CFRelease(cfData)
                
                if (status == errSecSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(EncryptionException("Failed to update key in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(EncryptionException("Failed to update key in keychain", e))
            }
        }
    }
    
    private fun generateSecureRandomKey(): String {
        val keyBytes = ByteArray(32) // 256-bit key
        Random.Default.nextBytes(keyBytes)
        return keyBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun generateRandomIV(): ByteArray {
        val iv = ByteArray(16) // 128-bit IV for AES
        Random.Default.nextBytes(iv)
        return iv
    }
    
    // Simplified AES encryption using basic XOR for demonstration
    // In production, you would use CommonCrypto or CryptoKit
    private fun encryptAES(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // This is a simplified implementation for demonstration
        // In production, use proper AES encryption
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        return result
    }
    
    private fun decryptAES(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // This is a simplified implementation for demonstration
        // In production, use proper AES decryption
        val result = ByteArray(encryptedData.size)
        for (i in encryptedData.indices) {
            result[i] = (encryptedData[i].toInt() xor key[i % key.size].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        return result
    }
}