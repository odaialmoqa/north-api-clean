package com.north.mobile.data.notification

import com.north.mobile.domain.model.Transaction
import com.north.mobile.domain.model.User
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow

/**
 * Service responsible for intelligent notification scheduling and personalization
 * based on user behavior patterns and engagement metrics.
 */
interface NotificationIntelligenceService {
    
    /**
     * Analyzes user behavior to determine optimal notification timing
     */
    suspend fun analyzeOptimalNotificationTiming(userId: String): OptimalTimingResult
    
    /**
     * Determines appropriate notification frequency based on user engagement
     */
    suspend fun calculateAdaptiveFrequency(userId: String): NotificationFrequencySettings
    
    /**
     * Creates contextual notifications based on spending patterns
     */
    suspend fun generateContextualNotifications(userId: String): List<ContextualNotification>
    
    /**
     * Generates location-based financial insights notifications
     */
    suspend fun generateLocationBasedNotifications(
        userId: String, 
        location: UserLocation?
    ): List<LocationBasedNotification>
    
    /**
     * Tracks notification effectiveness and optimizes future delivery
     */
    suspend fun trackNotificationEffectiveness(
        notificationId: String,
        interaction: NotificationInteraction
    )
    
    /**
     * Gets personalized notification schedule for a user
     */
    suspend fun getPersonalizedSchedule(userId: String): NotificationSchedule
    
    /**
     * Updates user behavior data for notification optimization
     */
    suspend fun updateUserBehaviorData(userId: String, behaviorData: UserBehaviorData)
}

data class OptimalTimingResult(
    val preferredHours: List<Int>, // Hours of day (0-23)
    val preferredDays: List<DayOfWeek>,
    val timeZone: TimeZone,
    val confidence: Float // 0.0 to 1.0
)

data class NotificationFrequencySettings(
    val dailyLimit: Int,
    val weeklyLimit: Int,
    val minimumIntervalMinutes: Int,
    val engagementBasedMultiplier: Float
)

data class ContextualNotification(
    val id: String,
    val type: ContextualNotificationType,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val triggerCondition: SpendingTrigger,
    val scheduledTime: Instant?,
    val expiresAt: Instant
)

data class LocationBasedNotification(
    val id: String,
    val title: String,
    val message: String,
    val location: UserLocation,
    val radius: Double, // meters
    val relevantInsight: String,
    val actionable: Boolean
)

data class NotificationSchedule(
    val userId: String,
    val scheduledNotifications: List<ScheduledNotification>,
    val nextOptimalTime: Instant,
    val currentFrequencySettings: NotificationFrequencySettings
)

data class ScheduledNotification(
    val id: String,
    val type: NotificationType,
    val scheduledTime: Instant,
    val content: NotificationContent,
    val priority: NotificationPriority
)

data class UserBehaviorData(
    val userId: String,
    val appOpenTimes: List<Instant>,
    val notificationInteractions: List<NotificationInteraction>,
    val spendingPatterns: SpendingPatternData,
    val locationData: List<LocationDataPoint>,
    val engagementScore: Float
)

data class SpendingPatternData(
    val averageDailySpending: Double,
    val spendingByCategory: Map<String, Double>,
    val spendingByTimeOfDay: Map<Int, Double>,
    val spendingByDayOfWeek: Map<DayOfWeek, Double>,
    val unusualSpendingThreshold: Double
)

data class LocationDataPoint(
    val location: UserLocation,
    val timestamp: Instant,
    val spendingAmount: Double?,
    val category: String?
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double
)

data class SpendingTrigger(
    val type: TriggerType,
    val threshold: Double,
    val category: String?,
    val timeWindow: Duration
)

data class NotificationInteraction(
    val notificationId: String,
    val userId: String,
    val interactionType: InteractionType,
    val timestamp: Instant,
    val responseTime: Duration?
)

enum class ContextualNotificationType {
    SPENDING_ALERT,
    BUDGET_WARNING,
    SAVINGS_OPPORTUNITY,
    GOAL_PROGRESS,
    UNUSUAL_ACTIVITY,
    STREAK_RISK,
    MICRO_WIN_AVAILABLE
}

enum class TriggerType {
    SPENDING_THRESHOLD_EXCEEDED,
    UNUSUAL_SPENDING_DETECTED,
    BUDGET_LIMIT_APPROACHING,
    SAVINGS_GOAL_OPPORTUNITY,
    STREAK_AT_RISK,
    MICRO_WIN_AVAILABLE
}

enum class InteractionType {
    OPENED,
    DISMISSED,
    ACTION_TAKEN,
    IGNORED,
    SNOOZED
}

enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}