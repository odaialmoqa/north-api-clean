package com.north.mobile.data.notification

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NotificationContent(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val imageUrl: String? = null
)

@Serializable
data class NotificationSchedule(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val content: NotificationContent,
    val scheduledTime: Instant,
    val isRecurring: Boolean = false,
    val recurringPattern: RecurringPattern? = null,
    val isActive: Boolean = true
)

@Serializable
enum class NotificationType {
    STREAK_RISK,
    ENGAGEMENT_REMINDER,
    GOAL_PROGRESS,
    MILESTONE_CELEBRATION,
    MICRO_WIN_AVAILABLE,
    SPENDING_ALERT,
    WEEKLY_SUMMARY,
    CONTEXTUAL,
    LOCATION_BASED
}

@Serializable
enum class RecurringPattern {
    DAILY,
    WEEKLY,
    MONTHLY
}

@Serializable
data class NotificationPreferences(
    val userId: String,
    val enabledTypes: Set<NotificationType> = NotificationType.values().toSet(),
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8,    // 8 AM
    val maxDailyNotifications: Int = 3,
    val streakReminderEnabled: Boolean = true,
    val goalProgressEnabled: Boolean = true,
    val milestoneEnabled: Boolean = true,
    val engagementRemindersEnabled: Boolean = true
)

@Serializable
data class NotificationTemplate(
    val type: NotificationType,
    val titleTemplates: List<String>,
    val bodyTemplates: List<String>,
    val dataKeys: List<String> = emptyList()
)

@Serializable
data class UserEngagementData(
    val userId: String,
    val lastAppOpen: Instant,
    val lastActionTime: Instant,
    val currentStreaks: List<String>,
    val streaksAtRisk: List<String>,
    val goalProgress: Map<String, Double>,
    val recentMilestones: List<String>,
    val availableMicroWins: List<String>
)

@Serializable
data class NotificationDeliveryResult(
    val notificationId: String,
    val success: Boolean,
    val deliveredAt: Instant?,
    val error: String? = null,
    val userId: String = "",
    val interactionType: String? = null,
    val effectiveness: Float? = null,
    val notificationContent: NotificationContent = NotificationContent("", ""),
    val deliveryStatus: DeliveryStatus = DeliveryStatus.DELIVERED
)

@Serializable
enum class DeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED,
    CANCELLED
}