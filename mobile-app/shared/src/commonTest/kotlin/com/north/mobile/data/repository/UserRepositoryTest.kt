package com.north.mobile.data.repository

import com.north.mobile.data.security.MockEncryptionManager
import com.north.mobile.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Tests for UserRepository functionality
 * Note: These tests use mock implementations since they require database setup
 */
class UserRepositoryTest {
    
    private lateinit var userRepository: MockUserRepository
    private lateinit var encryptionManager: MockEncryptionManager
    
    @BeforeTest
    fun setup() {
        encryptionManager = MockEncryptionManager()
        userRepository = MockUserRepository(encryptionManager)
    }
    
    @Test
    fun testInsertUser() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        val result = userRepository.insert(user)
        
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrThrow())
    }
    
    @Test
    fun testFindUserById() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val result = userRepository.findById(user.id)
        assertTrue(result.isSuccess)
        
        val foundUser = result.getOrThrow()
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser.id)
        assertEquals(user.email, foundUser.email)
    }
    
    @Test
    fun testFindUserByEmail() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val result = userRepository.findByEmail(user.email)
        assertTrue(result.isSuccess)
        
        val foundUser = result.getOrThrow()
        assertNotNull(foundUser)
        assertEquals(user.email, foundUser.email)
    }
    
    @Test
    fun testUpdateUser() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val updatedUser = user.copy(
            profile = user.profile.copy(firstName = "UpdatedName")
        )
        
        val result = userRepository.update(updatedUser)
        assertTrue(result.isSuccess)
        
        val foundUser = userRepository.findById(user.id).getOrThrow()
        assertNotNull(foundUser)
        assertEquals("UpdatedName", foundUser.profile.firstName)
    }
    
    @Test
    fun testUpdateUserPreferences() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val newPreferences = UserPreferences(
            currency = Currency.USD,
            language = "fr",
            notificationsEnabled = false,
            biometricAuthEnabled = true
        )
        
        val result = userRepository.updatePreferences(user.id, newPreferences)
        assertTrue(result.isSuccess)
        
        val foundUser = userRepository.findById(user.id).getOrThrow()
        assertNotNull(foundUser)
        assertEquals(Currency.USD, foundUser.preferences.currency)
        assertEquals("fr", foundUser.preferences.language)
        assertFalse(foundUser.preferences.notificationsEnabled)
        assertTrue(foundUser.preferences.biometricAuthEnabled)
    }
    
    @Test
    fun testUpdateGamificationProfile() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val newGamificationProfile = GamificationProfile(
            level = 5,
            totalPoints = 2500,
            currentStreaks = listOf(
                Streak(
                    id = "streak1",
                    type = StreakType.DAILY_CHECK_IN,
                    currentCount = 7,
                    bestCount = 10,
                    lastUpdated = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                )
            ),
            achievements = listOf(
                Achievement(
                    id = "achievement1",
                    title = "First Goal",
                    description = "Created your first financial goal",
                    badgeIcon = "goal_badge",
                    unlockedAt = kotlinx.datetime.Clock.System.now(),
                    category = AchievementCategory.GOALS
                )
            ),
            lastActivity = kotlinx.datetime.Clock.System.now()
        )
        
        val result = userRepository.updateGamificationProfile(user.id, newGamificationProfile)
        assertTrue(result.isSuccess)
        
        val foundUser = userRepository.findById(user.id).getOrThrow()
        assertNotNull(foundUser)
        assertEquals(5, foundUser.gamificationData.level)
        assertEquals(2500, foundUser.gamificationData.totalPoints)
    }
    
    @Test
    fun testDeleteUser() = runTest {
        encryptionManager.initialize()
        
        val user = createTestUser()
        userRepository.insert(user)
        
        val deleteResult = userRepository.delete(user.id)
        assertTrue(deleteResult.isSuccess)
        
        val findResult = userRepository.findById(user.id)
        assertTrue(findResult.isSuccess)
        assertNull(findResult.getOrThrow())
    }
    
    @Test
    fun testFindAllUsers() = runTest {
        encryptionManager.initialize()
        
        val user1 = createTestUser("user1", "user1@test.com")
        val user2 = createTestUser("user2", "user2@test.com")
        
        userRepository.insert(user1)
        userRepository.insert(user2)
        
        val result = userRepository.findAll()
        assertTrue(result.isSuccess)
        
        val users = result.getOrThrow()
        assertEquals(2, users.size)
        assertTrue(users.any { it.id == user1.id })
        assertTrue(users.any { it.id == user2.id })
    }
    
    @Test
    fun testUserValidation() {
        val validUser = createTestUser()
        val validationResult = validUser.validate()
        assertTrue(validationResult is ValidationResult.Valid)
        
        val invalidUser = validUser.copy(
            email = "invalid-email",
            profile = validUser.profile.copy(firstName = "")
        )
        val invalidValidationResult = invalidUser.validate()
        assertTrue(invalidValidationResult is ValidationResult.Invalid)
    }
    
    @Test
    fun testUserProfileValidation() {
        val validProfile = UserProfile(
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1-416-555-0123",
            postalCode = "M5V 3A8"
        )
        
        val validationResult = validProfile.validate()
        assertTrue(validationResult is ValidationResult.Valid)
        
        val invalidProfile = validProfile.copy(
            firstName = "J", // Too short
            phoneNumber = "invalid-phone"
        )
        
        val invalidValidationResult = invalidProfile.validate()
        assertTrue(invalidValidationResult is ValidationResult.Invalid)
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
                postalCode = "M5V 3A8"
            ),
            preferences = UserPreferences(
                currency = Currency.CAD,
                language = "en",
                notificationsEnabled = true,
                biometricAuthEnabled = false
            ),
            gamificationData = GamificationProfile(
                level = 1,
                totalPoints = 0,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = kotlinx.datetime.Clock.System.now()
            )
        )
    }
}

/**
 * Mock implementation of UserRepository for testing
 */
class MockUserRepository(
    private val encryptionManager: MockEncryptionManager
) : UserRepository {
    
    private val users = mutableMapOf<String, User>()
    
    override suspend fun insert(entity: User): Result<User> {
        return try {
            users[entity.id] = entity
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert user", e))
        }
    }
    
    override suspend fun update(entity: User): Result<User> {
        return try {
            if (users.containsKey(entity.id)) {
                users[entity.id] = entity
                Result.success(entity)
            } else {
                Result.failure(RepositoryException("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update user", e))
        }
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            users.remove(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to delete user", e))
        }
    }
    
    override suspend fun findById(id: String): Result<User?> {
        return try {
            Result.success(users[id])
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find user by id", e))
        }
    }
    
    override suspend fun findAll(): Result<List<User>> {
        return try {
            Result.success(users.values.toList())
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find all users", e))
        }
    }
    
    override suspend fun findByEmail(email: String): Result<User?> {
        return try {
            val user = users.values.find { it.email == email }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find user by email", e))
        }
    }
    
    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        return try {
            val user = users[userId] ?: return Result.failure(RepositoryException("User not found"))
            users[userId] = user.copy(preferences = preferences)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update user preferences", e))
        }
    }
    
    override suspend fun updateGamificationProfile(userId: String, profile: GamificationProfile): Result<Unit> {
        return try {
            val user = users[userId] ?: return Result.failure(RepositoryException("User not found"))
            users[userId] = user.copy(gamificationData = profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update gamification profile", e))
        }
    }
}

// Helper function for coroutine testing
suspend fun runTest(block: suspend () -> Unit) {
    block()
}