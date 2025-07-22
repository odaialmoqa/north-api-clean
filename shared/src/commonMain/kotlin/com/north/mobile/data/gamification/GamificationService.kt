package com.north.mobile.data.gamification

import com.north.mobile.domain.model.*
import kotlinx.datetime.Instant

/**
 * Service interface for managing gamification features including points, levels, achievements, and streaks.
 */
interface GamificationService {
    
    /**
     * Awards points to a user for completing a specific action.
     * @param userId The user's ID
     * @param action The action that was completed
     * @param points The number of points to award (optional, will use default if not provided)
     * @param description Optional description of the action
     * @return Result containing the updated gamification profile
     */
    suspend fun awardPoints(
        userId: String,
        action: UserAction,
        points: Int? = null,
        description: String? = null
    ): Result<PointsResult>
    
    /**
     * Gets the current gamification profile for a user.
     * @param userId The user's ID
     * @return The user's gamification profile
     */
    suspend fun getGamificationProfile(userId: String): Result<GamificationProfile>
    
    /**
     * Updates a user's streak for a specific type.
     * @param userId The user's ID
     * @param streakType The type of streak to update
     * @return Result containing the updated streak
     */
    suspend fun updateStreak(userId: String, streakType: StreakType): Result<Streak>
    
    /**
     * Checks if a user should level up based on their current points.
     * @param userId The user's ID
     * @return Result containing level up information if applicable
     */
    suspend fun checkLevelUp(userId: String): Result<LevelUpResult?>
    
    /**
     * Unlocks an achievement for a user.
     * @param userId The user's ID
     * @param achievementType The type of achievement to unlock
     * @return Result containing the unlocked achievement
     */
    suspend fun unlockAchievement(userId: String, achievementType: AchievementType): Result<Achievement>
    
    /**
     * Gets all available micro-wins for a user.
     * @param userId The user's ID
     * @return List of available micro-wins
     */
    suspend fun getAvailableMicroWins(userId: String): Result<List<MicroWin>>
    
    /**
     * Gets the user's points history.
     * @param userId The user's ID
     * @param limit Maximum number of entries to return
     * @return List of points history entries
     */
    suspend fun getPointsHistory(userId: String, limit: Int = 50): Result<List<PointsHistoryEntry>>
    
    /**
     * Calculates the points required for the next level.
     * @param currentLevel The user's current level
     * @return Points required for next level
     */
    fun getPointsRequiredForNextLevel(currentLevel: Int): Int
    
    /**
     * Gets the current level based on total points.
     * @param totalPoints The user's total points
     * @return The calculated level
     */
    fun getLevelFromPoints(totalPoints: Int): Int
}

/**
 * Result of awarding points to a user.
 */
data class PointsResult(
    val pointsAwarded: Int,
    val totalPoints: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val newAchievements: List<Achievement> = emptyList(),
    val updatedStreaks: List<Streak> = emptyList()
)

/**
 * Result of a level up event.
 */
data class LevelUpResult(
    val oldLevel: Int,
    val newLevel: Int,
    val pointsRequired: Int,
    val totalPoints: Int,
    val unlockedFeatures: List<String> = emptyList(),
    val celebrationMessage: String
)

/**
 * Entry in the points history.
 */
data class PointsHistoryEntry(
    val id: String,
    val points: Int,
    val action: UserAction,
    val description: String?,
    val earnedAt: Instant
)

/**
 * Types of achievements that can be unlocked.
 */
enum class AchievementType {
    FIRST_GOAL_CREATED,
    FIRST_ACCOUNT_LINKED,
    SAVINGS_MILESTONE_100,
    SAVINGS_MILESTONE_500,
    SAVINGS_MILESTONE_1000,
    BUDGET_ADHERENCE_WEEK,
    BUDGET_ADHERENCE_MONTH,
    TRANSACTION_CATEGORIZER,
    GOAL_ACHIEVER,
    STREAK_MASTER_7,
    STREAK_MASTER_30,
    FINANCIAL_HEALTH_CHAMPION,
    MICRO_WIN_COLLECTOR,
    ENGAGEMENT_SUPERSTAR
}