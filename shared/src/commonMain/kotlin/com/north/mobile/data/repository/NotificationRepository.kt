package com.north.mobile.data.repository

import com.north.mobile.data.notification.*
import kotlinx.datetime.Instant

interface NotificationRepository {
    suspend fun saveScheduledNotification(schedule: NotificationSchedule)
    suspend fun cancelNotification(notificationId: String)
    suspend fun getScheduledNotifications(userId: String): List<NotificationSchedule>
    suspend fun saveNotificationPreferences(preferences: NotificationPreferences)
    suspend fun getNotificationPreferences(userId: String): NotificationPreferences?
    suspend fun getTodayNotificationCount(userId: String): Int
    suspend fun getInactiveUsers(): List<String>
    suspend fun getUsersWithStreaksAtRisk(): Map<String, List<StreakAtRisk>>
    suspend fun getGoalProgressUpdates(): Map<String, List<GoalProgressUpdate>>
    suspend fun getNewMilestones(): Map<String, List<MilestoneUpdate>>
    suspend fun recordNotificationDelivery(result: NotificationDeliveryResult)
    suspend fun getNotificationHistory(userId: String, limit: Int = 50): List<NotificationDeliveryResult>
    
    // New methods for notification intelligence
    suspend fun getUserBehaviorData(userId: String): UserBehaviorData?
    suspend fun saveUserBehaviorData(behaviorData: UserBehaviorData)
    suspend fun saveNotificationInteraction(interaction: NotificationInteraction)
    suspend fun getNotificationInteractions(notificationId: String): List<NotificationInteraction>
    suspend fun getTotalNotificationsSent(userId: String): Int
    suspend fun getNotificationById(notificationId: String): NotificationDeliveryResult?
    suspend fun updateNotificationEffectiveness(notificationId: String, effectiveness: Float)
    suspend fun getUserStreakData(userId: String): List<StreakData>?
}

data class StreakAtRisk(
    val type: String,
    val currentCount: Int,
    val hoursUntilExpiry: Int
)

data class GoalProgressUpdate(
    val goalId: String,
    val title: String,
    val progressPercentage: Double,
    val isSignificantUpdate: Boolean
)

data class MilestoneUpdate(
    val milestoneId: String,
    val description: String,
    val achievedAt: Instant
)

data class StreakData(
    val name: String,
    val currentCount: Int,
    val lastActivityTime: Instant
)