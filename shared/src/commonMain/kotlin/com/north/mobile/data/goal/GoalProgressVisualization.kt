package com.north.mobile.data.goal

import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Priority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Visual progress indicator data for goal tracking
 */
@Serializable
data class GoalProgressVisualization(
    val goalId: String,
    val progressRing: ProgressRingData,
    val milestones: List<ProgressMilestone>,
    val projectionData: ProjectionVisualization,
    val animationTriggers: List<AnimationTrigger>,
    val statusIndicators: GoalStatusIndicators
)

/**
 * Progress ring visualization data
 */
@Serializable
data class ProgressRingData(
    val currentProgress: Double, // 0.0 to 1.0
    val targetProgress: Double = 1.0,
    val ringColor: String,
    val backgroundColor: String,
    val strokeWidth: Float = 8.0f,
    val radius: Float = 60.0f,
    val animationDuration: Long = 1000L,
    val showPercentage: Boolean = true,
    val glowEffect: Boolean = false
)

/**
 * Progress milestone for celebration points
 */
@Serializable
data class ProgressMilestone(
    val id: String,
    val goalId: String,
    val percentage: Double, // 0.0 to 1.0
    val title: String,
    val description: String,
    val isReached: Boolean = false,
    val reachedAt: Instant? = null,
    val celebrationType: MilestoneCelebrationType,
    val badgeIcon: String? = null,
    val pointsAwarded: Int = 0
)

@Serializable
enum class MilestoneCelebrationType {
    SPARKLES,
    CONFETTI,
    FIREWORKS,
    BADGE_UNLOCK,
    LEVEL_UP,
    STREAK_CELEBRATION
}

/**
 * Projection visualization data
 */
@Serializable
data class ProjectionVisualization(
    val goalId: String,
    val projectedCompletionDate: LocalDate,
    val confidenceLevel: Double, // 0.0 to 1.0
    val trendLine: List<TrendPoint>,
    val projectionLine: List<TrendPoint>,
    val isOnTrack: Boolean,
    val riskLevel: RiskLevel,
    val adjustmentRecommendations: List<GoalAdjustmentRecommendation>
)

/**
 * Trend point for visualization charts
 */
@Serializable
data class TrendPoint(
    val date: LocalDate,
    val amount: Money,
    val isProjected: Boolean = false,
    val confidence: Double = 1.0
)

@Serializable
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Goal adjustment recommendation
 */
@Serializable
data class GoalAdjustmentRecommendation(
    val id: String,
    val type: AdjustmentType,
    val title: String,
    val description: String,
    val impact: String,
    val priority: Priority,
    val estimatedImprovement: Double, // percentage improvement
    val actionRequired: String,
    val isAutomatable: Boolean = false
)

@Serializable
enum class AdjustmentType {
    INCREASE_WEEKLY_AMOUNT,
    EXTEND_DEADLINE,
    REDUCE_TARGET_AMOUNT,
    PAUSE_GOAL,
    MERGE_WITH_SIMILAR_GOAL,
    BREAK_INTO_SMALLER_GOALS,
    ADJUST_PRIORITY,
    OPTIMIZE_SAVINGS_STRATEGY
}

/**
 * Animation trigger for UI celebrations
 */
@Serializable
data class AnimationTrigger(
    val id: String,
    val triggerType: AnimationTriggerType,
    val animationType: String,
    val duration: Long = 2000L,
    val delay: Long = 0L,
    val shouldTrigger: Boolean = false,
    val triggerCondition: String,
    val celebrationData: CelebrationData? = null
)

@Serializable
enum class AnimationTriggerType {
    PROGRESS_UPDATE,
    MILESTONE_REACHED,
    GOAL_COMPLETED,
    STREAK_ACHIEVED,
    LEVEL_UP,
    BADGE_EARNED,
    RISK_ALERT,
    RECOMMENDATION_AVAILABLE
}

/**
 * Celebration data for animations
 */
@Serializable
data class CelebrationData(
    val title: String,
    val message: String,
    val iconUrl: String? = null,
    val colorScheme: List<String> = emptyList(),
    val soundEffect: String? = null,
    val hapticPattern: String? = null,
    val pointsAwarded: Int = 0,
    val badgeEarned: String? = null
)

/**
 * Goal status indicators for UI
 */
@Serializable
data class GoalStatusIndicators(
    val goalId: String,
    val overallStatus: GoalStatus,
    val progressStatus: ProgressStatus,
    val timelineStatus: TimelineStatus,
    val riskIndicators: List<RiskIndicator>,
    val achievementBadges: List<AchievementBadge>,
    val streakData: StreakData? = null
)

@Serializable
enum class GoalStatus {
    ON_TRACK,
    SLIGHTLY_BEHIND,
    BEHIND_SCHEDULE,
    CRITICAL,
    COMPLETED,
    PAUSED,
    CANCELLED
}

@Serializable
enum class ProgressStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    STALLED
}

@Serializable
enum class TimelineStatus {
    AHEAD_OF_SCHEDULE,
    ON_SCHEDULE,
    SLIGHTLY_DELAYED,
    SIGNIFICANTLY_DELAYED,
    OVERDUE
}

/**
 * Risk indicator for goal tracking
 */
@Serializable
data class RiskIndicator(
    val id: String,
    val type: RiskType,
    val severity: RiskSeverity,
    val title: String,
    val description: String,
    val recommendation: String,
    val isActionable: Boolean = true
)

@Serializable
enum class RiskType {
    TIMELINE_RISK,
    BUDGET_RISK,
    PROGRESS_STALL,
    EXTERNAL_FACTOR,
    COMPETING_GOALS,
    SEASONAL_IMPACT
}

@Serializable
enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Achievement badge for gamification
 */
@Serializable
data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val earnedAt: Instant,
    val category: BadgeCategory,
    val rarity: BadgeRarity,
    val pointsValue: Int
)

@Serializable
enum class BadgeCategory {
    PROGRESS,
    CONSISTENCY,
    MILESTONE,
    SPEED,
    EFFICIENCY,
    DEDICATION
}

@Serializable
enum class BadgeRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Streak data for consistency tracking
 */
@Serializable
data class StreakData(
    val goalId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val streakType: StreakType,
    val lastActivityDate: LocalDate,
    val streakStartDate: LocalDate,
    val isActive: Boolean = true,
    val riskOfBreaking: Boolean = false
)

@Serializable
enum class StreakType {
    DAILY_PROGRESS,
    WEEKLY_SAVINGS,
    MONTHLY_MILESTONE,
    CONSISTENT_CONTRIBUTIONS,
    GOAL_ENGAGEMENT
}