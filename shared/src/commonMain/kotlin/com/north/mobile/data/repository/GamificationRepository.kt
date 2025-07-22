package com.north.mobile.data.repository

import com.north.mobile.data.gamification.AchievementType
import com.north.mobile.data.gamification.PointsHistoryEntry
import com.north.mobile.domain.model.*

/**
 * Repository interface for gamification data persistence.
 */
interface GamificationRepository {
    
    /**
     * Gets the gamification profile for a user.
     * @param userId The user's ID
     * @return The gamification profile or null if not found
     */
    suspend fun getGamificationProfile(userId: String): GamificationProfile?
    
    /**
     * Creates a new gamification profile for a user.
     * @param profile The gamification profile to create
     * @param userId The user's ID
     */
    suspend fun createGamificationProfile(profile: GamificationProfile, userId: String)
    
    /**
     * Updates an existing gamification profile.
     * @param profile The updated gamification profile
     * @param userId The user's ID
     */
    suspend fun updateGamificationProfile(profile: GamificationProfile, userId: String)
    
    /**
     * Gets a specific streak for a user and type.
     * @param userId The user's ID
     * @param streakType The type of streak
     * @return The streak or null if not found
     */
    suspend fun getStreak(userId: String, streakType: StreakType): Streak?
    
    /**
     * Gets all active streaks for a user.
     * @param userId The user's ID
     * @return List of active streaks
     */
    suspend fun getActiveStreaks(userId: String): List<Streak>
    
    /**
     * Updates or creates a streak.
     * @param streak The streak to update/create
     * @param userId The user's ID
     */
    suspend fun updateStreak(streak: Streak, userId: String)
    
    /**
     * Gets a specific achievement for a user.
     * @param userId The user's ID
     * @param achievementType The type of achievement
     * @return The achievement or null if not found
     */
    suspend fun getAchievement(userId: String, achievementType: AchievementType): Achievement?
    
    /**
     * Gets all achievements for a user.
     * @param userId The user's ID
     * @return List of achievements
     */
    suspend fun getAchievements(userId: String): List<Achievement>
    
    /**
     * Adds a new achievement for a user.
     * @param achievement The achievement to add
     * @param userId The user's ID
     * @param achievementType The type of achievement
     */
    suspend fun addAchievement(achievement: Achievement, userId: String, achievementType: AchievementType)
    
    /**
     * Adds an entry to the points history.
     * @param entry The points history entry
     * @param userId The user's ID
     */
    suspend fun addPointsHistory(entry: PointsHistoryEntry, userId: String)
    
    /**
     * Gets the points history for a user.
     * @param userId The user's ID
     * @param limit Maximum number of entries to return
     * @return List of points history entries
     */
    suspend fun getPointsHistory(userId: String, limit: Int): List<PointsHistoryEntry>
    
    // Enhanced streak tracking methods
    
    /**
     * Gets a streak by its ID.
     * @param streakId The streak's ID
     * @return The streak or null if not found
     */
    suspend fun getStreakById(streakId: String): Streak?
    
    /**
     * Gets all streaks for a user (including inactive ones).
     * @param userId The user's ID
     * @return List of all user streaks
     */
    suspend fun getAllUserStreaks(userId: String): List<Streak>
    
    /**
     * Creates a new streak recovery process.
     * @param recovery The streak recovery to create
     */
    suspend fun createStreakRecovery(recovery: StreakRecovery)
    
    /**
     * Gets a streak recovery by its ID.
     * @param recoveryId The recovery's ID
     * @return The streak recovery or null if not found
     */
    suspend fun getStreakRecovery(recoveryId: String): StreakRecovery?
    
    /**
     * Updates an existing streak recovery.
     * @param recovery The updated streak recovery
     */
    suspend fun updateStreakRecovery(recovery: StreakRecovery)
    
    /**
     * Gets all active recovery processes for a user.
     * @param userId The user's ID
     * @return List of active recoveries
     */
    suspend fun getActiveRecoveries(userId: String): List<StreakRecovery>
    
    /**
     * Gets all recovery processes for a user.
     * @param userId The user's ID
     * @return List of all recoveries
     */
    suspend fun getAllRecoveries(userId: String): List<StreakRecovery>
    
    /**
     * Creates a new streak reminder.
     * @param reminder The streak reminder to create
     */
    suspend fun createStreakReminder(reminder: StreakReminder)
    
    /**
     * Gets all active reminders for a user.
     * @param userId The user's ID
     * @return List of active reminders
     */
    suspend fun getActiveReminders(userId: String): List<StreakReminder>
    
    /**
     * Marks a reminder as read.
     * @param reminderId The reminder's ID
     */
    suspend fun markReminderAsRead(reminderId: String)
}