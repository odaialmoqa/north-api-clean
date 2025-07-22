package com.north.mobile.data.repository

import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.database.NorthDatabase
import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Repository for User data operations with encryption support
 */
interface UserRepository : Repository<User, String> {
    suspend fun findByEmail(email: String): Result<User?>
    suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit>
    suspend fun updateGamificationProfile(userId: String, profile: GamificationProfile): Result<Unit>
}

class UserRepositoryImpl(
    private val database: NorthDatabase,
    private val encryptionManager: EncryptionManager
) : UserRepository {
    
    override suspend fun insert(entity: User): Result<User> {
        return try {
            val now = Clock.System.now()
            
            // Encrypt sensitive data
            val encryptedSin = entity.profile.sin?.let { sin ->
                encryptionManager.encrypt(sin, "user_sin_${entity.id}").getOrThrow()
            }
            
            database.userQueries.insert(
                id = entity.id,
                email = entity.email,
                firstName = entity.profile.firstName,
                lastName = entity.profile.lastName,
                phoneNumber = entity.profile.phoneNumber,
                dateOfBirth = entity.profile.dateOfBirth?.toString(),
                currency = entity.preferences.currency.name,
                language = entity.preferences.language,
                notificationsEnabled = if (entity.preferences.notificationsEnabled) 1L else 0L,
                biometricAuthEnabled = if (entity.preferences.biometricAuthEnabled) 1L else 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
            
            // Store encrypted SIN separately if provided
            encryptedSin?.let {
                // Store in secure preferences or separate encrypted table
                // For now, we'll skip this as it requires additional secure storage
            }
            
            // Initialize gamification profile
            database.gamificationQueries.insertGamificationProfile(
                userId = entity.id,
                level = entity.gamificationData.level.toLong(),
                totalPoints = entity.gamificationData.totalPoints.toLong(),
                lastActivity = entity.gamificationData.lastActivity?.toEpochMilliseconds(),
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert user", e))
        }
    }
    
    override suspend fun update(entity: User): Result<User> {
        return try {
            val now = Clock.System.now()
            
            database.userQueries.update(
                email = entity.email,
                firstName = entity.profile.firstName,
                lastName = entity.profile.lastName,
                phoneNumber = entity.profile.phoneNumber,
                dateOfBirth = entity.profile.dateOfBirth?.toString(),
                currency = entity.preferences.currency.name,
                language = entity.preferences.language,
                notificationsEnabled = if (entity.preferences.notificationsEnabled) 1L else 0L,
                biometricAuthEnabled = if (entity.preferences.biometricAuthEnabled) 1L else 0L,
                updatedAt = now.toEpochMilliseconds(),
                id = entity.id
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update user", e))
        }
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            database.userQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to delete user", e))
        }
    }
    
    override suspend fun findById(id: String): Result<User?> {
        return try {
            val userRow = database.userQueries.selectById(id).executeAsOneOrNull()
            val gamificationRow = database.gamificationQueries.selectGamificationProfile(id).executeAsOneOrNull()
            
            if (userRow != null) {
                val user = mapToUser(userRow, gamificationRow)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find user by id", e))
        }
    }
    
    override suspend fun findAll(): Result<List<User>> {
        return try {
            val users = database.userQueries.selectAll().executeAsList().map { userRow ->
                val gamificationRow = database.gamificationQueries.selectGamificationProfile(userRow.id).executeAsOneOrNull()
                mapToUser(userRow, gamificationRow)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find all users", e))
        }
    }
    
    override suspend fun findByEmail(email: String): Result<User?> {
        return try {
            val userRow = database.userQueries.selectAll().executeAsList()
                .find { it.email == email }
            
            if (userRow != null) {
                val gamificationRow = database.gamificationQueries.selectGamificationProfile(userRow.id).executeAsOneOrNull()
                val user = mapToUser(userRow, gamificationRow)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find user by email", e))
        }
    }
    
    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        return try {
            val userRow = database.userQueries.selectById(userId).executeAsOneOrNull()
                ?: return Result.failure(RepositoryException("User not found"))
            
            val now = Clock.System.now()
            
            database.userQueries.update(
                email = userRow.email,
                firstName = userRow.firstName,
                lastName = userRow.lastName,
                phoneNumber = userRow.phoneNumber,
                dateOfBirth = userRow.dateOfBirth,
                currency = preferences.currency.name,
                language = preferences.language,
                notificationsEnabled = if (preferences.notificationsEnabled) 1L else 0L,
                biometricAuthEnabled = if (preferences.biometricAuthEnabled) 1L else 0L,
                updatedAt = now.toEpochMilliseconds(),
                id = userId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update user preferences", e))
        }
    }
    
    override suspend fun updateGamificationProfile(userId: String, profile: GamificationProfile): Result<Unit> {
        return try {
            val now = Clock.System.now()
            
            database.gamificationQueries.updateGamificationProfile(
                level = profile.level.toLong(),
                totalPoints = profile.totalPoints.toLong(),
                lastActivity = profile.lastActivity?.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds(),
                userId = userId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update gamification profile", e))
        }
    }
    
    private fun mapToUser(
        userRow: com.north.mobile.database.User,
        gamificationRow: com.north.mobile.database.GamificationProfile?
    ): User {
        val profile = UserProfile(
            firstName = userRow.firstName,
            lastName = userRow.lastName,
            phoneNumber = userRow.phoneNumber,
            dateOfBirth = userRow.dateOfBirth?.let { LocalDate.parse(it) },
            sin = null, // Would need to decrypt if stored
            postalCode = null // Not stored in current schema
        )
        
        val preferences = UserPreferences(
            currency = Currency.valueOf(userRow.currency),
            language = userRow.language,
            notificationsEnabled = userRow.notificationsEnabled == 1L,
            biometricAuthEnabled = userRow.biometricAuthEnabled == 1L
        )
        
        val gamificationData = gamificationRow?.let {
            GamificationProfile(
                level = it.level.toInt(),
                totalPoints = it.totalPoints.toInt(),
                currentStreaks = emptyList(), // Would need to load separately
                achievements = emptyList(), // Would need to load separately
                lastActivity = it.lastActivity?.let { timestamp -> Instant.fromEpochMilliseconds(timestamp) } ?: Clock.System.now()
            )
        } ?: GamificationProfile(
            level = 1,
            totalPoints = 0,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        
        return User(
            id = userRow.id,
            email = userRow.email,
            profile = profile,
            preferences = preferences,
            gamificationData = gamificationData
        )
    }
}