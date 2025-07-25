package com.north.mobile.data.gamification

import com.north.mobile.data.repository.GamificationRepository
import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.math.pow

/**
 * Implementation of the GamificationService interface.
 */
class GamificationServiceImpl(
    private val gamificationRepository: GamificationRepository
) : GamificationService {
    
    companion object {
        // Base points for different actions
        private val ACTION_POINTS = mapOf(
            UserAction.CHECK_BALANCE to 5,
            UserAction.CATEGORIZE_TRANSACTION to 10,
            UserAction.UPDATE_GOAL to 15,
            UserAction.LINK_ACCOUNT to 50,
            UserAction.COMPLETE_MICRO_TASK to 20,
            UserAction.REVIEW_INSIGHTS to 10,
            UserAction.SET_BUDGET to 25,
            UserAction.MAKE_SAVINGS_CONTRIBUTION to 30
        )
        
        // Level progression formula: level = floor(sqrt(totalPoints / 100)) + 1
        private const val POINTS_PER_LEVEL_BASE = 100
        
        // Achievement definitions
        private val ACHIEVEMENT_DEFINITIONS = mapOf(
            AchievementType.FIRST_GOAL_CREATED to AchievementDefinition(
                title = "Goal Setter",
                description = "Created your first financial goal",
                badgeIcon = "🎯",
                category = AchievementCategory.GOAL_ACHIEVEMENT,
                pointsAwarded = 50
            ),
            AchievementType.FIRST_ACCOUNT_LINKED to AchievementDefinition(
                title = "Connected",
                description = "Linked your first bank account",
                badgeIcon = "🔗",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 100
            ),
            AchievementType.SAVINGS_MILESTONE_100 to AchievementDefinition(
                title = "Saver",
                description = "Saved your first $100",
                badgeIcon = "💰",
                category = AchievementCategory.SAVINGS,
                pointsAwarded = 75
            ),
            AchievementType.SAVINGS_MILESTONE_500 to AchievementDefinition(
                title = "Super Saver",
                description = "Saved $500 towards your goals",
                badgeIcon = "💎",
                category = AchievementCategory.SAVINGS,
                pointsAwarded = 150
            ),
            AchievementType.SAVINGS_MILESTONE_1000 to AchievementDefinition(
                title = "Savings Champion",
                description = "Reached $1,000 in savings",
                badgeIcon = "👑",
                category = AchievementCategory.SAVINGS,
                pointsAwarded = 250
            ),
            AchievementType.BUDGET_ADHERENCE_WEEK to AchievementDefinition(
                title = "Budget Keeper",
                description = "Stayed under budget for a full week",
                badgeIcon = "📊",
                category = AchievementCategory.BUDGETING,
                pointsAwarded = 100
            ),
            AchievementType.BUDGET_ADHERENCE_MONTH to AchievementDefinition(
                title = "Budget Master",
                description = "Stayed under budget for a full month",
                badgeIcon = "🏆",
                category = AchievementCategory.BUDGETING,
                pointsAwarded = 300
            ),
            AchievementType.TRANSACTION_CATEGORIZER to AchievementDefinition(
                title = "Organizer",
                description = "Categorized 50 transactions",
                badgeIcon = "📋",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 125
            ),
            AchievementType.GOAL_ACHIEVER to AchievementDefinition(
                title = "Goal Crusher",
                description = "Achieved your first financial goal",
                badgeIcon = "🎉",
                category = AchievementCategory.GOAL_ACHIEVEMENT,
                pointsAwarded = 500
            ),
            AchievementType.STREAK_MASTER_7 to AchievementDefinition(
                title = "Streak Starter",
                description = "Maintained a 7-day streak",
                badgeIcon = "🔥",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 100
            ),
            AchievementType.STREAK_MASTER_30 to AchievementDefinition(
                title = "Streak Legend",
                description = "Maintained a 30-day streak",
                badgeIcon = "⚡",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 400
            ),
            AchievementType.FINANCIAL_HEALTH_CHAMPION to AchievementDefinition(
                title = "Health Champion",
                description = "Improved your financial health score",
                badgeIcon = "💪",
                category = AchievementCategory.FINANCIAL_HEALTH,
                pointsAwarded = 200
            ),
            AchievementType.MICRO_WIN_COLLECTOR to AchievementDefinition(
                title = "Micro Win Master",
                description = "Completed 25 micro-wins",
                badgeIcon = "⭐",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 150
            ),
            AchievementType.ENGAGEMENT_SUPERSTAR to AchievementDefinition(
                title = "Engagement Superstar",
                description = "Used the app for 30 consecutive days",
                badgeIcon = "🌟",
                category = AchievementCategory.ENGAGEMENT,
                pointsAwarded = 350
            )
        )
    }
    
    override suspend fun awardPoints(
        userId: String,
        action: UserAction,
        points: Int?,
        description: String?
    ): Result<PointsResult> {
        return try {
            val pointsToAward = points ?: ACTION_POINTS[action] ?: 0
            
            // Get current profile or create new one
            val currentProfile = gamificationRepository.getGamificationProfile(userId)
                ?: createInitialProfile(userId)
            
            val newTotalPoints = currentProfile.totalPoints + pointsToAward
            val oldLevel = currentProfile.level
            val newLevel = getLevelFromPoints(newTotalPoints)
            val leveledUp = newLevel > oldLevel
            
            // Update profile
            val updatedProfile = currentProfile.copy(
                totalPoints = newTotalPoints,
                level = newLevel,
                lastActivity = Clock.System.now()
            )
            
            gamificationRepository.updateGamificationProfile(updatedProfile, userId)
            
            // Record points history
            gamificationRepository.addPointsHistory(
                PointsHistoryEntry(
                    id = generateId(),
                    points = pointsToAward,
                    action = action,
                    description = description,
                    earnedAt = Clock.System.now()
                ),
                userId
            )
            
            // Check for new achievements
            val newAchievements = checkForNewAchievements(userId, action, newTotalPoints)
            
            // Update streaks if applicable
            val updatedStreaks = updateRelevantStreaks(userId, action)
            
            Result.success(
                PointsResult(
                    pointsAwarded = pointsToAward,
                    totalPoints = newTotalPoints,
                    newLevel = newLevel,
                    leveledUp = leveledUp,
                    newAchievements = newAchievements,
                    updatedStreaks = updatedStreaks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getGamificationProfile(userId: String): Result<GamificationProfile> {
        return try {
            val profile = gamificationRepository.getGamificationProfile(userId)
                ?: createInitialProfile(userId)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateStreak(userId: String, streakType: StreakType): Result<Streak> {
        return try {
            val existingStreak = gamificationRepository.getStreak(userId, streakType)
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            val updatedStreak = if (existingStreak != null) {
                val lastActivityDate = existingStreak.lastActivityDate
                val daysDifference = today.toEpochDays() - lastActivityDate.toEpochDays()
                
                when {
                    daysDifference == 0 -> existingStreak // Same day, no change
                    daysDifference == 1 -> {
                        // Consecutive day, increment streak
                        val newCount = existingStreak.currentCount + 1
                        existingStreak.copy(
                            currentCount = newCount,
                            bestCount = maxOf(existingStreak.bestCount, newCount),
                            lastActivityDate = today
                        )
                    }
                    else -> {
                        // Streak broken, reset to 1
                        existingStreak.copy(
                            currentCount = 1,
                            lastActivityDate = today
                        )
                    }
                }
            } else {
                // Create new streak
                Streak(
                    id = generateId(),
                    type = streakType,
                    currentCount = 1,
                    bestCount = 1,
                    lastActivityDate = today,
                    isActive = true
                )
            }
            
            gamificationRepository.updateStreak(updatedStreak, userId)
            Result.success(updatedStreak)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkLevelUp(userId: String): Result<LevelUpResult?> {
        return try {
            val profile = gamificationRepository.getGamificationProfile(userId)
                ?: return Result.success(null)
            
            val currentLevel = getLevelFromPoints(profile.totalPoints)
            
            if (currentLevel > profile.level) {
                val levelUpResult = LevelUpResult(
                    oldLevel = profile.level,
                    newLevel = currentLevel,
                    pointsRequired = getPointsRequiredForLevel(currentLevel),
                    totalPoints = profile.totalPoints,
                    unlockedFeatures = getUnlockedFeatures(currentLevel),
                    celebrationMessage = generateLevelUpMessage(currentLevel)
                )
                Result.success(levelUpResult)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun unlockAchievement(userId: String, achievementType: AchievementType): Result<Achievement> {
        return try {
            // Check if achievement already exists
            val existingAchievement = gamificationRepository.getAchievement(userId, achievementType)
            if (existingAchievement != null) {
                return Result.success(existingAchievement)
            }
            
            val definition = ACHIEVEMENT_DEFINITIONS[achievementType]
                ?: throw IllegalArgumentException("Unknown achievement type: $achievementType")
            
            val achievement = Achievement(
                id = generateId(),
                title = definition.title,
                description = definition.description,
                badgeIcon = definition.badgeIcon,
                pointsAwarded = definition.pointsAwarded,
                unlockedAt = Clock.System.now(),
                category = definition.category
            )
            
            gamificationRepository.addAchievement(achievement, userId, achievementType)
            
            // Award points for the achievement
            awardPoints(userId, UserAction.COMPLETE_MICRO_TASK, definition.pointsAwarded, "Achievement unlocked: ${definition.title}")
            
            Result.success(achievement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAvailableMicroWins(userId: String): Result<List<MicroWin>> {
        return try {
            val profile = gamificationRepository.getGamificationProfile(userId)
                ?: createInitialProfile(userId)
            
            val microWins = generateMicroWins(userId, profile)
            Result.success(microWins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPointsHistory(userId: String, limit: Int): Result<List<PointsHistoryEntry>> {
        return try {
            val history = gamificationRepository.getPointsHistory(userId, limit)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getPointsRequiredForNextLevel(currentLevel: Int): Int {
        return getPointsRequiredForLevel(currentLevel + 1)
    }
    
    override fun getLevelFromPoints(totalPoints: Int): Int {
        return floor(kotlin.math.sqrt(totalPoints.toDouble() / POINTS_PER_LEVEL_BASE)).toInt() + 1
    }
    
    private fun getPointsRequiredForLevel(level: Int): Int {
        return ((level - 1).toDouble().pow(2) * POINTS_PER_LEVEL_BASE).toInt()
    }
    
    private suspend fun createInitialProfile(userId: String): GamificationProfile {
        val profile = GamificationProfile(
            level = 1,
            totalPoints = 0,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        gamificationRepository.createGamificationProfile(profile, userId)
        return profile
    }
    
    private suspend fun checkForNewAchievements(
        userId: String,
        action: UserAction,
        totalPoints: Int
    ): List<Achievement> {
        val newAchievements = mutableListOf<Achievement>()
        
        // Check action-based achievements
        when (action) {
            UserAction.LINK_ACCOUNT -> {
                if (gamificationRepository.getAchievement(userId, AchievementType.FIRST_ACCOUNT_LINKED) == null) {
                    unlockAchievement(userId, AchievementType.FIRST_ACCOUNT_LINKED).getOrNull()?.let {
                        newAchievements.add(it)
                    }
                }
            }
            UserAction.UPDATE_GOAL -> {
                if (gamificationRepository.getAchievement(userId, AchievementType.FIRST_GOAL_CREATED) == null) {
                    unlockAchievement(userId, AchievementType.FIRST_GOAL_CREATED).getOrNull()?.let {
                        newAchievements.add(it)
                    }
                }
            }
            else -> { /* Handle other action-based achievements */ }
        }
        
        // Check points-based achievements
        checkPointsMilestones(userId, totalPoints, newAchievements)
        
        return newAchievements
    }
    
    private suspend fun checkPointsMilestones(
        userId: String,
        totalPoints: Int,
        achievements: MutableList<Achievement>
    ) {
        val milestones = listOf(
            100 to AchievementType.SAVINGS_MILESTONE_100,
            500 to AchievementType.SAVINGS_MILESTONE_500,
            1000 to AchievementType.SAVINGS_MILESTONE_1000
        )
        
        for ((points, achievementType) in milestones) {
            if (totalPoints >= points && gamificationRepository.getAchievement(userId, achievementType) == null) {
                unlockAchievement(userId, achievementType).getOrNull()?.let {
                    achievements.add(it)
                }
            }
        }
    }
    
    private suspend fun updateRelevantStreaks(userId: String, action: UserAction): List<Streak> {
        val updatedStreaks = mutableListOf<Streak>()
        
        val streakType = when (action) {
            UserAction.CHECK_BALANCE -> StreakType.DAILY_CHECK_IN
            UserAction.CATEGORIZE_TRANSACTION -> StreakType.TRANSACTION_CATEGORIZATION
            UserAction.MAKE_SAVINGS_CONTRIBUTION -> StreakType.SAVINGS_CONTRIBUTION
            else -> null
        }
        
        streakType?.let {
            updateStreak(userId, it).getOrNull()?.let { streak ->
                updatedStreaks.add(streak)
            }
        }
        
        return updatedStreaks
    }
    
    private fun generateMicroWins(userId: String, profile: GamificationProfile): List<MicroWin> {
        return listOf(
            MicroWin(
                id = "check_balance_${generateId()}",
                title = "Check Your Balance",
                description = "Stay on top of your finances",
                pointsAwarded = ACTION_POINTS[UserAction.CHECK_BALANCE] ?: 5,
                actionType = UserAction.CHECK_BALANCE
            ),
            MicroWin(
                id = "categorize_transaction_${generateId()}",
                title = "Categorize 3 Transactions",
                description = "Help us understand your spending",
                pointsAwarded = (ACTION_POINTS[UserAction.CATEGORIZE_TRANSACTION] ?: 10) * 3,
                actionType = UserAction.CATEGORIZE_TRANSACTION
            ),
            MicroWin(
                id = "review_insights_${generateId()}",
                title = "Review Your Insights",
                description = "Discover new ways to save",
                pointsAwarded = ACTION_POINTS[UserAction.REVIEW_INSIGHTS] ?: 10,
                actionType = UserAction.REVIEW_INSIGHTS
            )
        )
    }
    
    private fun getUnlockedFeatures(level: Int): List<String> {
        return when (level) {
            2 -> listOf("Advanced Goal Tracking")
            3 -> listOf("Spending Insights")
            5 -> listOf("Custom Categories")
            10 -> listOf("Premium Analytics")
            else -> emptyList()
        }
    }
    
    private fun generateLevelUpMessage(level: Int): String {
        return when (level) {
            2 -> "🎉 Welcome to Level 2! You're getting the hang of this!"
            3 -> "🚀 Level 3 achieved! Your financial journey is taking off!"
            5 -> "⭐ Level 5! You're becoming a financial superstar!"
            10 -> "👑 Level 10! You're a true financial champion!"
            else -> "🎊 Level $level! Keep up the amazing work!"
        }
    }
    
    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString() + (0..999).random()
    }
}

/**
 * Internal data class for achievement definitions.
 */
private data class AchievementDefinition(
    val title: String,
    val description: String,
    val badgeIcon: String,
    val category: AchievementCategory,
    val pointsAwarded: Int
)