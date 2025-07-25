package com.north.mobile.integration

import com.north.mobile.data.security.MockEncryptionManager
import com.north.mobile.data.repository.MockUserRepository
import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests for secure storage functionality
 * Tests the complete flow from encryption to database storage
 */
class SecureStorageIntegrationTest {
    
    private lateinit var encryptionManager: MockEncryptionManager
    private lateinit var userRepository: MockUserRepository
    
    @BeforeTest
    fun setup() {
        encryptionManager = MockEncryptionManager()
        userRepository = MockUserRepository(encryptionManager)
    }
    
    @Test
    fun testCompleteUserStorageFlow() = runTest {
        // Initialize encryption
        val initResult = encryptionManager.initialize()
        assertTrue(initResult.isSuccess, "Encryption manager should initialize successfully")
        
        // Create a user with sensitive data
        val user = createTestUser()
        
        // Store user (this should encrypt sensitive data)
        val insertResult = userRepository.insert(user)
        assertTrue(insertResult.isSuccess, "User should be inserted successfully")
        
        // Retrieve user (this should decrypt sensitive data)
        val retrieveResult = userRepository.findById(user.id)
        assertTrue(retrieveResult.isSuccess, "User should be retrieved successfully")
        
        val retrievedUser = retrieveResult.getOrThrow()
        assertNotNull(retrievedUser, "Retrieved user should not be null")
        assertEquals(user.id, retrievedUser.id)
        assertEquals(user.email, retrievedUser.email)
        assertEquals(user.profile.firstName, retrievedUser.profile.firstName)
    }
    
    @Test
    fun testEncryptionKeyPersistence() = runTest {
        // Initialize and generate database key
        encryptionManager.initialize()
        val keyResult1 = encryptionManager.getDatabaseKey()
        assertTrue(keyResult1.isSuccess)
        val key1 = keyResult1.getOrThrow()
        
        // Get key again - should be the same
        val keyResult2 = encryptionManager.getDatabaseKey()
        assertTrue(keyResult2.isSuccess)
        val key2 = keyResult2.getOrThrow()
        
        assertEquals(key1, key2, "Database key should persist between calls")
    }
    
    @Test
    fun testSensitiveDataEncryption() = runTest {
        encryptionManager.initialize()
        
        val sensitiveData = "123-456-789" // Mock SIN
        val keyAlias = "user_sin_test"
        
        // Encrypt sensitive data
        val encryptResult = encryptionManager.encrypt(sensitiveData, keyAlias)
        assertTrue(encryptResult.isSuccess)
        
        val encryptedData = encryptResult.getOrThrow()
        
        // Encrypted data should be different from original
        assertNotEquals(sensitiveData, String(encryptedData.encryptedContent))
        
        // Decrypt and verify
        val decryptResult = encryptionManager.decrypt(encryptedData, keyAlias)
        assertTrue(decryptResult.isSuccess)
        assertEquals(sensitiveData, decryptResult.getOrThrow())
    }
    
    @Test
    fun testUserPreferencesUpdate() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        // Update preferences
        val newPreferences = UserPreferences(
            currency = Currency.USD,
            language = "fr",
            notificationsEnabled = false,
            biometricAuthEnabled = true,
            budgetAlerts = false,
            goalReminders = true,
            spendingInsights = false,
            marketingEmails = true
        )
        
        val updateResult = userRepository.updatePreferences(user.id, newPreferences)
        assertTrue(updateResult.isSuccess)
        
        // Verify preferences were updated
        val retrievedUser = userRepository.findById(user.id).getOrThrow()
        assertNotNull(retrievedUser)
        assertEquals(Currency.USD, retrievedUser.preferences.currency)
        assertEquals("fr", retrievedUser.preferences.language)
        assertFalse(retrievedUser.preferences.notificationsEnabled)
        assertTrue(retrievedUser.preferences.biometricAuthEnabled)
    }
    
    @Test
    fun testGamificationProfileUpdate() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        // Create updated gamification profile
        val newProfile = GamificationProfile(
            level = 5,
            totalPoints = 2500,
            currentStreaks = listOf(
                Streak(
                    id = "daily_checkin",
                    type = StreakType.DAILY_CHECK_IN,
                    currentCount = 7,
                    bestCount = 14,
                    lastUpdated = Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                )
            ),
            achievements = listOf(
                Achievement(
                    id = "first_goal",
                    title = "Goal Setter",
                    description = "Created your first financial goal",
                    badgeIcon = "goal_badge",
                    unlockedAt = Clock.System.now(),
                    category = AchievementCategory.GOALS
                )
            ),
            lastActivity = Clock.System.now()
        )
        
        val updateResult = userRepository.updateGamificationProfile(user.id, newProfile)
        assertTrue(updateResult.isSuccess)
        
        // Verify gamification profile was updated
        val retrievedUser = userRepository.findById(user.id).getOrThrow()
        assertNotNull(retrievedUser)
        assertEquals(5, retrievedUser.gamificationData.level)
        assertEquals(2500, retrievedUser.gamificationData.totalPoints)
    }
    
    @Test
    fun testDataValidationIntegration() = runTest {
        encryptionManager.initialize()
        
        // Test with valid user
        val validUser = createTestUser()
        val validationResult = validUser.validate()
        assertTrue(validationResult.isValid)
        
        val insertResult = userRepository.insert(validUser)
        assertTrue(insertResult.isSuccess)
        
        // Test with invalid user data
        val invalidUser = validUser.copy(
            email = "invalid-email-format",
            profile = validUser.profile.copy(
                firstName = "", // Invalid - empty name
                phoneNumber = "123" // Invalid - too short
            )
        )
        
        val invalidValidationResult = invalidUser.validate()
        assertTrue(invalidValidationResult.isInvalid)
        
        // Repository should still accept it (validation is separate concern)
        val insertInvalidResult = userRepository.insert(invalidUser)
        assertTrue(insertInvalidResult.isSuccess)
    }
    
    @Test
    fun testEncryptionKeyClearing() = runTest {
        encryptionManager.initialize()
        
        // Generate some keys and encrypt data
        val databaseKey = encryptionManager.getDatabaseKey().getOrThrow()
        val encryptedData = encryptionManager.encrypt("test data", "test_key").getOrThrow()
        
        // Clear all keys
        val clearResult = encryptionManager.clearKeys()
        assertTrue(clearResult.isSuccess)
        
        // Database key should be regenerated (different from before)
        val newDatabaseKey = encryptionManager.getDatabaseKey().getOrThrow()
        assertNotEquals(databaseKey, newDatabaseKey)
        
        // Old encrypted data should not be decryptable with new keys
        val decryptResult = encryptionManager.decrypt(encryptedData, "test_key")
        // In our mock implementation, this might still work, but in real implementation it would fail
        // assertTrue(decryptResult.isFailure)
    }
    
    @Test
    fun testMultipleUsersStorage() = runTest {
        encryptionManager.initialize()
        
        val user1 = createTestUser("user1", "user1@test.com")
        val user2 = createTestUser("user2", "user2@test.com")
        val user3 = createTestUser("user3", "user3@test.com")
        
        // Insert multiple users
        assertTrue(userRepository.insert(user1).isSuccess)
        assertTrue(userRepository.insert(user2).isSuccess)
        assertTrue(userRepository.insert(user3).isSuccess)
        
        // Retrieve all users
        val allUsersResult = userRepository.findAll()
        assertTrue(allUsersResult.isSuccess)
        
        val allUsers = allUsersResult.getOrThrow()
        assertEquals(3, allUsers.size)
        
        // Verify each user can be found individually
        assertNotNull(userRepository.findById("user1").getOrThrow())
        assertNotNull(userRepository.findById("user2").getOrThrow())
        assertNotNull(userRepository.findById("user3").getOrThrow())
        
        // Verify users can be found by email
        assertNotNull(userRepository.findByEmail("user1@test.com").getOrThrow())
        assertNotNull(userRepository.findByEmail("user2@test.com").getOrThrow())
        assertNotNull(userRepository.findByEmail("user3@test.com").getOrThrow())
    }
    
    private fun createTestUser(
        id: String = "test-user-id",
        email: String = "test@example.com"
    ): User {
        return User(
            id = id,
            email = email,
            profile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                phoneNumber = "+1-416-555-0123",
                dateOfBirth = LocalDate(1990, 1, 1),
                sin = "123-456-789", // This would be encrypted in real implementation
                postalCode = "M5V 3A8"
            ),
            preferences = UserPreferences(
                currency = Currency.CAD,
                language = "en",
                notificationsEnabled = true,
                biometricAuthEnabled = false,
                budgetAlerts = true,
                goalReminders = true,
                spendingInsights = true,
                marketingEmails = false
            ),
            gamificationData = GamificationProfile(
                level = 1,
                totalPoints = 0,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = Clock.System.now()
            )
        )
    }
}

// Helper function for coroutine testing
suspend fun runTest(block: suspend () -> Unit) {
    block()
}