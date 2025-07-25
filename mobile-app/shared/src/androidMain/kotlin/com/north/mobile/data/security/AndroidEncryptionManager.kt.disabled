package com.north.mobile.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.random.Random

/**
 * Android implementation of EncryptionManager using Android Keystore
 * Provides hardware-backed encryption when available
 */
class AndroidEncryptionManager(private val context: Context) : EncryptionManager {
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            // Ensure master key is created
            masterKey
            
            // Generate database key if it doesn't exist
            if (!encryptedPrefs.contains(DATABASE_KEY_PREF)) {
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
            encryptedPrefs.edit()
                .putString(DATABASE_KEY_PREF, key)
                .apply()
            Result.success(key)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to generate database key", e))
        }
    }
    
    override suspend fun getDatabaseKey(): Result<String> {
        return try {
            val key = encryptedPrefs.getString(DATABASE_KEY_PREF, null)
                ?: return generateDatabaseKey()
            Result.success(key)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to retrieve database key", e))
        }
    }
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<EncryptedData> {
        return try {
            val secretKey = getOrCreateSecretKey(keyAlias)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
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
            val secretKey = getSecretKey(keyAlias)
                ?: return Result.failure(EncryptionException("Secret key not found for alias: $keyAlias"))
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivSpec = IvParameterSpec(encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.encryptedContent)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            
            Result.success(decryptedString)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to decrypt data", e))
        }
    }
    
    override fun isEncryptionAvailable(): Boolean {
        return try {
            // Check if Android Keystore is available
            KeyStore.getInstance(ANDROID_KEYSTORE)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearKeys(): Result<Unit> {
        return try {
            // Clear encrypted preferences
            encryptedPrefs.edit().clear().apply()
            
            // Delete keys from Android Keystore
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith(KEY_ALIAS_PREFIX)) {
                    keyStore.deleteEntry(alias)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(EncryptionException("Failed to clear keys", e))
        }
    }
    
    private fun getOrCreateSecretKey(keyAlias: String): SecretKey {
        val fullAlias = "$KEY_ALIAS_PREFIX$keyAlias"
        
        return try {
            // Try to get existing key
            getSecretKey(keyAlias) ?: run {
                // Generate new key
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    fullAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            throw EncryptionException("Failed to get or create secret key", e)
        }
    }
    
    private fun getSecretKey(keyAlias: String): SecretKey? {
        val fullAlias = "$KEY_ALIAS_PREFIX$keyAlias"
        return try {
            keyStore.getKey(fullAlias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateSecureRandomKey(): String {
        val keyBytes = ByteArray(32) // 256-bit key
        Random.Default.nextBytes(keyBytes)
        return keyBytes.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALIAS_PREFIX = "north_"
        private const val PREFS_NAME = "north_secure_prefs"
        private const val DATABASE_KEY_PREF = "database_key"
    }
}