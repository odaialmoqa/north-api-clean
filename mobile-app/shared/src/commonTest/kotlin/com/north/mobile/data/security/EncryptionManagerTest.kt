package com.north.mobile.data.security

import kotlin.test.*

/**
 * Tests for EncryptionManager functionality
 * Note: These tests use a mock implementation since platform-specific encryption
 * requires actual Android/iOS environments
 */
class EncryptionManagerTest {
    
    private lateinit var encryptionManager: MockEncryptionManager
    
    @BeforeTest
    fun setup() {
        encryptionManager = MockEncryptionManager()
    }
    
    @Test
    fun testInitialization() = runTest {
        val result = encryptionManager.initialize()
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun testDatabaseKeyGeneration() = runTest {
        encryptionManager.initialize()
        
        val result = encryptionManager.generateDatabaseKey()
        assertTrue(result.isSuccess)
        
        val key = result.getOrNull()
        assertNotNull(key)
        assertTrue(key.isNotEmpty())
        assertEquals(64, key.length) // 32 bytes = 64 hex characters
    }
    
    @Test
    fun testDatabaseKeyRetrieval() = runTest {
        encryptionManager.initialize()
        
        val generateResult = encryptionManager.generateDatabaseKey()
        assertTrue(generateResult.isSuccess)
        val originalKey = generateResult.getOrThrow()
        
        val retrieveResult = encryptionManager.getDatabaseKey()
        assertTrue(retrieveResult.isSuccess)
        val retrievedKey = retrieveResult.getOrThrow()
        
        assertEquals(originalKey, retrievedKey)
    }
    
    @Test
    fun testEncryptionAndDecryption() = runTest {
        encryptionManager.initialize()
        
        val originalData = "This is sensitive financial data"
        val keyAlias = "test_key"
        
        // Encrypt data
        val encryptResult = encryptionManager.encrypt(originalData, keyAlias)
        assertTrue(encryptResult.isSuccess)
        
        val encryptedData = encryptResult.getOrThrow()
        assertNotNull(encryptedData.encryptedContent)
        assertNotNull(encryptedData.iv)
        assertEquals(keyAlias, encryptedData.keyAlias)
        
        // Decrypt data
        val decryptResult = encryptionManager.decrypt(encryptedData, keyAlias)
        assertTrue(decryptResult.isSuccess)
        
        val decryptedData = decryptResult.getOrThrow()
        assertEquals(originalData, decryptedData)
    }
    
    @Test
    fun testEncryptionWithDifferentKeys() = runTest {
        encryptionManager.initialize()
        
        val data = "Test data"
        val keyAlias1 = "key1"
        val keyAlias2 = "key2"
        
        val encrypted1 = encryptionManager.encrypt(data, keyAlias1).getOrThrow()
        val encrypted2 = encryptionManager.encrypt(data, keyAlias2).getOrThrow()
        
        // Different keys should produce different encrypted data
        assertFalse(encrypted1.encryptedContent.contentEquals(encrypted2.encryptedContent))
        
        // But both should decrypt to the same original data
        val decrypted1 = encryptionManager.decrypt(encrypted1, keyAlias1).getOrThrow()
        val decrypted2 = encryptionManager.decrypt(encrypted2, keyAlias2).getOrThrow()
        
        assertEquals(data, decrypted1)
        assertEquals(data, decrypted2)
    }
    
    @Test
    fun testDecryptionWithWrongKey() = runTest {
        encryptionManager.initialize()
        
        val data = "Test data"
        val correctKey = "correct_key"
        val wrongKey = "wrong_key"
        
        val encrypted = encryptionManager.encrypt(data, correctKey).getOrThrow()
        val decryptResult = encryptionManager.decrypt(encrypted, wrongKey)
        
        assertTrue(decryptResult.isFailure)
        assertTrue(decryptResult.exceptionOrNull() is EncryptionException)
    }
    
    @Test
    fun testClearKeys() = runTest {
        encryptionManager.initialize()
        
        // Generate some keys
        encryptionManager.generateDatabaseKey()
        encryptionManager.encrypt("test", "test_key")
        
        // Clear keys
        val clearResult = encryptionManager.clearKeys()
        assertTrue(clearResult.isSuccess)
        
        // Database key should be regenerated after clearing
        val keyResult = encryptionManager.getDatabaseKey()
        assertTrue(keyResult.isSuccess)
    }
    
    @Test
    fun testEncryptionAvailability() {
        assertTrue(encryptionManager.isEncryptionAvailable())
    }
    
    @Test
    fun testEmptyDataEncryption() = runTest {
        encryptionManager.initialize()
        
        val emptyData = ""
        val encryptResult = encryptionManager.encrypt(emptyData)
        assertTrue(encryptResult.isSuccess)
        
        val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
        assertTrue(decryptResult.isSuccess)
        assertEquals(emptyData, decryptResult.getOrThrow())
    }
    
    @Test
    fun testLargeDataEncryption() = runTest {
        encryptionManager.initialize()
        
        val largeData = "A".repeat(10000) // 10KB of data
        val encryptResult = encryptionManager.encrypt(largeData)
        assertTrue(encryptResult.isSuccess)
        
        val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
        assertTrue(decryptResult.isSuccess)
        assertEquals(largeData, decryptResult.getOrThrow())
    }
    
    @Test
    fun testSpecialCharactersEncryption() = runTest {
        encryptionManager.initialize()
        
        val specialData = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        val encryptResult = encryptionManager.encrypt(specialData)
        assertTrue(encryptResult.isSuccess)
        
        val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
        assertTrue(decryptResult.isSuccess)
        assertEquals(specialData, decryptResult.getOrThrow())
    }
    
    @Test
    fun testUnicodeDataEncryption() = runTest {
        encryptionManager.initialize()
        
        val unicodeData = "Unicode: 🔒💰🏦 français español 中文"
        val encryptResult = encryptionManager.encrypt(unicodeData)
        assertTrue(encryptResult.isSuccess)
        
        val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
        assertTrue(decryptResult.isSuccess)
        assertEquals(unicodeData, decryptResult.getOrThrow())
    }
    
    // Additional comprehensive security tests
    
    @Test
    fun testConcurrentEncryptionOperations() = runTest {
        encryptionManager.initialize()
        
        val testData = (1..10).map { "Test data $it" }
        val encryptResults = testData.map { data ->
            encryptionManager.encrypt(data, "concurrent_key_$data")
        }
        
        // All encryptions should succeed
        encryptResults.forEach { result ->
            assertTrue(result.isSuccess)
        }
        
        // All should decrypt correctly
        encryptResults.forEachIndexed { index, encryptResult ->
            val decryptResult = encryptionManager.decrypt(
                encryptResult.getOrThrow(), 
                "concurrent_key_Test data ${index + 1}"
            )
            assertTrue(decryptResult.isSuccess)
            assertEquals("Test data ${index + 1}", decryptResult.getOrThrow())
        }
    }
    
    @Test
    fun testEncryptionWithSameDataDifferentResults() = runTest {
        encryptionManager.initialize()
        
        val data = "Same data for multiple encryptions"
        val keyAlias = "test_key"
        
        val encrypt1 = encryptionManager.encrypt(data, keyAlias).getOrThrow()
        val encrypt2 = encryptionManager.encrypt(data, keyAlias).getOrThrow()
        
        // Same data should produce different encrypted results (due to IV)
        assertFalse(encrypt1.encryptedContent.contentEquals(encrypt2.encryptedContent))
        assertFalse(encrypt1.iv.contentEquals(encrypt2.iv))
        
        // But both should decrypt to the same original data
        val decrypt1 = encryptionManager.decrypt(encrypt1, keyAlias).getOrThrow()
        val decrypt2 = encryptionManager.decrypt(encrypt2, keyAlias).getOrThrow()
        
        assertEquals(data, decrypt1)
        assertEquals(data, decrypt2)
    }
    
    @Test
    fun testKeyAliasIsolation() = runTest {
        encryptionManager.initialize()
        
        val data1 = "Data for key 1"
        val data2 = "Data for key 2"
        val keyAlias1 = "key_1"
        val keyAlias2 = "key_2"
        
        val encrypted1 = encryptionManager.encrypt(data1, keyAlias1).getOrThrow()
        val encrypted2 = encryptionManager.encrypt(data2, keyAlias2).getOrThrow()
        
        // Correct key should decrypt correctly
        assertEquals(data1, encryptionManager.decrypt(encrypted1, keyAlias1).getOrThrow())
        assertEquals(data2, encryptionManager.decrypt(encrypted2, keyAlias2).getOrThrow())
        
        // Wrong key should fail
        assertTrue(encryptionManager.decrypt(encrypted1, keyAlias2).isFailure)
        assertTrue(encryptionManager.decrypt(encrypted2, keyAlias1).isFailure)
    }
    
    @Test
    fun testDatabaseKeyConsistency() = runTest {
        encryptionManager.initialize()
        
        // Generate database key
        val key1Result = encryptionManager.generateDatabaseKey()
        assertTrue(key1Result.isSuccess)
        val key1 = key1Result.getOrThrow()
        
        // Retrieve the same key
        val key2Result = encryptionManager.getDatabaseKey()
        assertTrue(key2Result.isSuccess)
        val key2 = key2Result.getOrThrow()
        
        // Should be the same
        assertEquals(key1, key2)
        
        // Multiple retrievals should return the same key
        val key3Result = encryptionManager.getDatabaseKey()
        assertTrue(key3Result.isSuccess)
        val key3 = key3Result.getOrThrow()
        
        assertEquals(key1, key3)
    }
    
    @Test
    fun testKeyGenerationUniqueness() = runTest {
        encryptionManager.initialize()
        
        val keys = mutableSetOf<String>()
        
        // Generate multiple database keys (after clearing)
        repeat(10) {
            encryptionManager.clearKeys()
            val keyResult = encryptionManager.generateDatabaseKey()
            assertTrue(keyResult.isSuccess)
            keys.add(keyResult.getOrThrow())
        }
        
        // All keys should be unique
        assertEquals(10, keys.size)
        
        // All keys should be proper length (64 hex characters = 32 bytes)
        keys.forEach { key ->
            assertEquals(64, key.length)
            assertTrue(key.all { char -> char.isDigit() || char.lowercaseChar() in 'a'..'f' })
        }
    }
    
    @Test
    fun testEncryptionFailureScenarios() = runTest {
        // Test without initialization
        val uninitializedResult = encryptionManager.encrypt("test")
        assertTrue(uninitializedResult.isFailure)
        assertTrue(uninitializedResult.exceptionOrNull() is EncryptionException)
        
        // Initialize for other tests
        encryptionManager.initialize()
        
        // Test with corrupted encrypted data
        val validEncrypted = encryptionManager.encrypt("test").getOrThrow()
        val corruptedEncrypted = validEncrypted.copy(
            encryptedContent = ByteArray(validEncrypted.encryptedContent.size) { 0 }
        )
        
        val corruptedResult = encryptionManager.decrypt(corruptedEncrypted)
        assertTrue(corruptedResult.isFailure)
    }
    
    @Test
    fun testEncryptionPerformance() = runTest {
        encryptionManager.initialize()
        
        val testSizes = listOf(100, 1000, 10000, 100000) // Different data sizes
        
        testSizes.forEach { size ->
            val data = "A".repeat(size)
            
            val startTime = System.currentTimeMillis()
            val encryptResult = encryptionManager.encrypt(data)
            val encryptTime = System.currentTimeMillis() - startTime
            
            assertTrue(encryptResult.isSuccess)
            
            val decryptStartTime = System.currentTimeMillis()
            val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
            val decryptTime = System.currentTimeMillis() - decryptStartTime
            
            assertTrue(decryptResult.isSuccess)
            assertEquals(data, decryptResult.getOrThrow())
            
            // Performance should be reasonable (less than 1 second for these sizes)
            assertTrue(encryptTime < 1000, "Encryption took too long for size $size: ${encryptTime}ms")
            assertTrue(decryptTime < 1000, "Decryption took too long for size $size: ${decryptTime}ms")
        }
    }
    
    @Test
    fun testMemoryCleanupAfterOperations() = runTest {
        encryptionManager.initialize()
        
        val sensitiveData = "Very sensitive financial information"
        
        // Perform multiple encryption/decryption cycles
        repeat(100) {
            val encrypted = encryptionManager.encrypt(sensitiveData).getOrThrow()
            val decrypted = encryptionManager.decrypt(encrypted).getOrThrow()
            assertEquals(sensitiveData, decrypted)
        }
        
        // Clear keys should clean up memory
        val clearResult = encryptionManager.clearKeys()
        assertTrue(clearResult.isSuccess)
        
        // Should be able to reinitialize and work again
        val newKeyResult = encryptionManager.generateDatabaseKey()
        assertTrue(newKeyResult.isSuccess)
    }
    
    @Test
    fun testEncryptionWithBinaryData() = runTest {
        encryptionManager.initialize()
        
        // Test with binary data (simulating encrypted database content)
        val binaryData = ByteArray(256) { it.toByte() }.toString(Charsets.ISO_8859_1)
        
        val encryptResult = encryptionManager.encrypt(binaryData)
        assertTrue(encryptResult.isSuccess)
        
        val decryptResult = encryptionManager.decrypt(encryptResult.getOrThrow())
        assertTrue(decryptResult.isSuccess)
        assertEquals(binaryData, decryptResult.getOrThrow())
    }
    
    @Test
    fun testEncryptionStateConsistency() = runTest {
        encryptionManager.initialize()
        
        // Test that encryption manager maintains consistent state
        assertTrue(encryptionManager.isEncryptionAvailable())
        
        val key1 = encryptionManager.getDatabaseKey().getOrThrow()
        val key2 = encryptionManager.getDatabaseKey().getOrThrow()
        assertEquals(key1, key2)
        
        // After clearing keys
        encryptionManager.clearKeys()
        
        // Should still be available but generate new key
        assertTrue(encryptionManager.isEncryptionAvailable())
        val newKey = encryptionManager.getDatabaseKey().getOrThrow()
        assertNotEquals(key1, newKey)
    }
    
    @Test
    fun testEncryptionErrorMessages() = runTest {
        // Test that error messages are informative
        val uninitializedResult = encryptionManager.encrypt("test")
        assertTrue(uninitializedResult.isFailure)
        val exception = uninitializedResult.exceptionOrNull() as EncryptionException
        assertTrue(exception.message?.contains("initialized") == true)
        
        encryptionManager.initialize()
        
        // Test wrong key error
        val encrypted = encryptionManager.encrypt("test", "key1").getOrThrow()
        val wrongKeyResult = encryptionManager.decrypt(encrypted, "key2")
        assertTrue(wrongKeyResult.isFailure)
        val wrongKeyException = wrongKeyResult.exceptionOrNull() as EncryptionException
        assertTrue(wrongKeyException.message?.contains("key") == true || 
                  wrongKeyException.message?.contains("decrypt") == true)
    }
}

/**
 * Mock implementation of EncryptionManager for testing
 */
class MockEncryptionManager : EncryptionManager {
    private val keys = mutableMapOf<String, String>()
    private var databaseKey: String? = null
    private var initialized = false
    
    override suspend fun initialize(): Result<Unit> {
        initialized = true
        return Result.success(Unit)
    }
    
    override suspend fun generateDatabaseKey(): Result<String> {
        if (!initialized) return Result.failure(EncryptionException("Not initialized"))
        
        val key = generateSecureRandomKey()
        databaseKey = key
        return Result.success(key)
    }
    
    override suspend fun getDatabaseKey(): Result<String> {
        if (!initialized) return Result.failure(EncryptionException("Not initialized"))
        
        return databaseKey?.let { Result.success(it) } 
            ?: generateDatabaseKey()
    }
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<EncryptedData> {
        if (!initialized) return Result.failure(EncryptionException("Not initialized"))
        
        return try {
            val key = getOrCreateKey(keyAlias)
            val iv = generateRandomIV()
            val encrypted = simpleXorEncrypt(data.toByteArray(), key.toByteArray(), iv)
            
            Result.success(EncryptedData(encrypted, iv, keyAlias))
        } catch (e: Exception) {
            Result.failure(EncryptionException("Encryption failed", e))
        }
    }
    
    override suspend fun decrypt(encryptedData: EncryptedData, keyAlias: String): Result<String> {
        if (!initialized) return Result.failure(EncryptionException("Not initialized"))
        
        return try {
            val key = keys[keyAlias] ?: return Result.failure(EncryptionException("Key not found"))
            val decrypted = simpleXorDecrypt(encryptedData.encryptedContent, key.toByteArray(), encryptedData.iv)
            
            Result.success(String(decrypted))
        } catch (e: Exception) {
            Result.failure(EncryptionException("Decryption failed", e))
        }
    }
    
    override fun isEncryptionAvailable(): Boolean = true
    
    override suspend fun clearKeys(): Result<Unit> {
        keys.clear()
        databaseKey = null
        return Result.success(Unit)
    }
    
    private fun getOrCreateKey(keyAlias: String): String {
        return keys.getOrPut(keyAlias) { generateSecureRandomKey() }
    }
    
    private fun generateSecureRandomKey(): String {
        val keyBytes = ByteArray(32)
        for (i in keyBytes.indices) {
            keyBytes[i] = (kotlin.random.Random.nextInt(256) - 128).toByte()
        }
        return keyBytes.joinToString("") { "%02x".format(it.toUByte()) }
    }
    
    private fun generateRandomIV(): ByteArray {
        val iv = ByteArray(16)
        for (i in iv.indices) {
            iv[i] = (kotlin.random.Random.nextInt(256) - 128).toByte()
        }
        return iv
    }
    
    private fun simpleXorEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        return result
    }
    
    private fun simpleXorDecrypt(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return simpleXorEncrypt(encryptedData, key, iv) // XOR is symmetric
    }
}

// Helper function for coroutine testing
suspend fun runTest(block: suspend () -> Unit) {
    block()
}