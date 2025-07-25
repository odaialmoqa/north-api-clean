package com.north.mobile.data.gamification

import com.north.mobile.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Service interface for advanced streak tracking, micro-win detection, and streak recovery.
 */
interface StreakTrackingService {
    
    /**
     * Updates a streak and handles risk assessment.
     * @param userId The user's ID
     * @param streakType The type of streak to update
     * @param actionDate The date when the action was performed
     * @return Result containing the updated streak with risk assessment
     */
    suspend fun updateStreakWithRiskAssessment(
        userId: String,
        streakType: StreakType,
        actionDate: LocalDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
    ): Result<StreakUpdateResult>
    
    /**
     * Analyzes all user streaks for risk levels and schedules reminders.
     * @param userId The user's ID
     * @return List of streaks at risk with recommended actions
     */
    suspend fun analyzeStreakRisks(userId: String): Result<List<StreakRiskAnalysis>>
    
    /**
     * Generates personalized micro-win opportunities based on user behavior.
     * @param userId The user's ID
     * @param limit Maximum number of opportunities to return
     * @return List of personalized micro-win opportunities
     */
    suspend fun generatePersonalizedMicroWins(userId: String, limit: Int = 5): Result<List<MicroWinOpportunity>>
    
    /**
     * Detects and awards micro-wins for completed actions.
     * @param userId The user's ID
     * @param action The action that was completed
     * @param contextData Additional context about the action
     * @return List of micro-wins that were triggered
     */
    suspend fun detectAndAwardMicroWins(
        userId: String,
        action: UserAction,
        contextData: Map<String, String> = emptyMap()
    ): Result<List<MicroWinResult>>
    
    /**
     * Initiates streak recovery process for a broken streak.
     * @param userId The user's ID
     * @param brokenStreakId The ID of the broken streak
     * @return Result containing the recovery plan
     */
    suspend fun initiateStreakRecovery(userId: String, brokenStreakId: String): Result<StreakRecovery>
    
    /**
     * Processes a recovery action and updates recovery progress.
     * @param userId The user's ID
     * @param recoveryId The recovery process ID
     * @param action The recovery action completed
     * @return Result containing updated recovery status
     */
    suspend fun processRecoveryAction(
        userId: String,
        recoveryId: String,
        action: UserAction
    ): Result<RecoveryActionResult>
    
    /**
     * Schedules gentle reminders for streaks at risk.
     * @param userId The user's ID
     * @param streakId The streak ID
     * @param reminderType The type of reminder to send
     * @return Result containing the scheduled reminder
     */
    suspend fun scheduleStreakReminder(
        userId: String,
        streakId: String,
        reminderType: ReminderType
    ): Result<StreakReminder>
    
    /**
     * Gets all active streak reminders for a user.
     * @param userId The user's ID
     * @return List of active reminders
     */
    suspend fun getActiveReminders(userId: String): Result<List<StreakReminder>>
    
    /**
     * Marks a reminder as read/acknowledged.
     * @param userId The user's ID
     * @param reminderId The reminder ID
     * @return Result indicating success
     */
    suspend fun acknowledgeReminder(userId: String, reminderId: String): Result<Unit>
    
    /**
     * Gets streak statistics and insights for the user.
     * @param userId The user's ID
     * @return Comprehensive streak statistics
     */
    suspend fun getStreakStatistics(userId: String): Result<StreakStatistics>
    
    /**
     * Celebrates streak milestones with appropriate intensity.
     * @param userId The user's ID
     * @param streak The streak that reached a milestone
     * @return Celebration event for the milestone
     */
    suspend fun celebrateStreakMilestone(userId: String, streak: Streak): Result<CelebrationEvent>
}

/**
 * Result of updating a streak with risk assessment.
 */
data class StreakUpdateResult(
    val streak: Streak,
    val wasExtended: Boolean,
    val wasBroken: Boolean,
    val newRiskLevel: StreakRiskLevel,
    val celebrationEvent: CelebrationEvent?,
    val reminderScheduled: StreakReminder?
)

/**
 * Analysis of streak risk with recommendations.
 */
data class StreakRiskAnalysis(
    val streak: Streak,
    val riskLevel: StreakRiskLevel,
    val daysSinceLastActivity: Int,
    val recommendedActions: List<String>,
    val reminderMessage: String,
    val urgencyScore: Int // 1-10, higher is more urgent
)

/**
 * Result of a micro-win detection and award.
 */
data class MicroWinResult(
    val microWin: MicroWinOpportunity,
    val pointsAwarded: Int,
    val celebrationEvent: CelebrationEvent,
    val streaksAffected: List<Streak>
)

/**
 * Result of processing a recovery action.
 */
data class RecoveryActionResult(
    val recovery: StreakRecovery,
    val actionProcessed: RecoveryAction,
    val isRecoveryComplete: Boolean,
    val newStreakStarted: Streak?,
    val celebrationEvent: CelebrationEvent?
)

/**
 * Comprehensive streak statistics for a user.
 */
data class StreakStatistics(
    val totalActiveStreaks: Int,
    val longestCurrentStreak: Streak?,
    val longestEverStreak: Streak?,
    val totalStreakDays: Int,
    val averageStreakLength: Double,
    val streaksByType: Map<StreakType, List<Streak>>,
    val riskDistribution: Map<StreakRiskLevel, Int>,
    val recoverySuccessRate: Double,
    val weeklyStreakTrend: List<Int>, // Last 7 days
    val monthlyMilestones: List<StreakMilestone>
)

/**
 * Represents a streak milestone achievement.
 */
data class StreakMilestone(
    val streakType: StreakType,
    val count: Int,
    val achievedAt: Instant,
    val isPersonalRecord: Boolean
)