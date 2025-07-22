package com.north.mobile.domain.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Serializable
data class GamificationProfile(
    val level: Int = 1,
    val totalPoints: Int = 0,
    val currentStreaks: List<Streak> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val lastActivity: Instant
)

@Serializable
data class Streak(
    val id: String,
    val type: StreakType,
    val currentCount: Int,
    val bestCount: Int,
    val lastActivityDate: LocalDate,
    val isActive: Boolean = true,
    val riskLevel: StreakRiskLevel = StreakRiskLevel.SAFE,
    val recoveryAttempts: Int = 0,
    val lastReminderSent: Instant? = null
)

@Serializable
enum class StreakType {
    DAILY_CHECK_IN,
    UNDER_BUDGET,
    GOAL_PROGRESS,
    TRANSACTION_CATEGORIZATION,
    SAVINGS_CONTRIBUTION,
    WEEKLY_BUDGET_ADHERENCE,
    DAILY_SAVINGS,
    WEEKLY_GOAL_PROGRESS,
    MICRO_WIN_COMPLETION,
    FINANCIAL_HEALTH_CHECK
}

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val badgeIcon: String,
    val pointsAwarded: Int,
    val unlockedAt: Instant,
    val category: AchievementCategory
)

@Serializable
enum class AchievementCategory {
    SAVINGS,
    BUDGETING,
    GOAL_ACHIEVEMENT,
    ENGAGEMENT,
    FINANCIAL_HEALTH
}

@Serializable
data class MicroWin(
    val id: String,
    val title: String,
    val description: String,
    val pointsAwarded: Int,
    val actionType: UserAction,
    val isCompleted: Boolean = false
)

@Serializable
enum class UserAction {
    CHECK_BALANCE,
    CATEGORIZE_TRANSACTION,
    UPDATE_GOAL,
    LINK_ACCOUNT,
    COMPLETE_MICRO_TASK,
    REVIEW_INSIGHTS,
    SET_BUDGET,
    MAKE_SAVINGS_CONTRIBUTION
}

@Serializable
enum class StreakRiskLevel {
    SAFE,           // Streak is healthy, no risk
    LOW_RISK,       // Slight risk, gentle reminder might help
    MEDIUM_RISK,    // Moderate risk, more prominent reminder needed
    HIGH_RISK,      // High risk of breaking, urgent reminder
    BROKEN          // Streak has been broken
}

@Serializable
data class StreakReminder(
    val id: String,
    val userId: String,
    val streakId: String,
    val streakType: StreakType,
    val reminderType: ReminderType,
    val message: String,
    val scheduledFor: Instant,
    val sentAt: Instant? = null,
    val isRead: Boolean = false
)

@Serializable
enum class ReminderType {
    GENTLE_NUDGE,       // Soft reminder to maintain streak
    MOTIVATION_BOOST,   // Encouraging message about progress
    STREAK_RISK_ALERT,  // Warning about potential streak loss
    RECOVERY_SUPPORT    // Help after a streak break
}

@Serializable
data class MicroWinOpportunity(
    val id: String,
    val title: String,
    val description: String,
    val pointsAwarded: Int,
    val actionType: UserAction,
    val difficulty: MicroWinDifficulty,
    val estimatedTimeMinutes: Int,
    val expiresAt: Instant? = null,
    val isPersonalized: Boolean = false,
    val contextData: Map<String, String> = emptyMap()
)

@Serializable
enum class MicroWinDifficulty {
    EASY,       // 1-2 minutes, simple action
    MEDIUM,     // 3-5 minutes, requires some thought
    HARD        // 5+ minutes, more complex task
}

@Serializable
data class StreakRecovery(
    val id: String,
    val userId: String,
    val originalStreakId: String,
    val streakType: StreakType,
    val brokenAt: Instant,
    val recoveryStarted: Instant,
    val recoveryCompleted: Instant? = null,
    val originalCount: Int,
    val recoveryActions: List<RecoveryAction> = emptyList(),
    val isSuccessful: Boolean = false
)

@Serializable
data class RecoveryAction(
    val id: String,
    val actionType: UserAction,
    val completedAt: Instant,
    val pointsAwarded: Int,
    val description: String
)