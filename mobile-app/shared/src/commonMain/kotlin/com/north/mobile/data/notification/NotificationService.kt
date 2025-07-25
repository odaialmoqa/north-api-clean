package com.north.mobile.data.notification

import kotlinx.datetime.Instant

interface NotificationService {
    suspend fun scheduleNotification(schedule: NotificationSchedule): Result<String>
    suspend fun cancelNotification(notificationId: String): Result<Unit>
    suspend fun sendImmediateNotification(userId: String, content: NotificationContent): Result<NotificationDeliveryResult>
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit>
    suspend fun getNotificationPreferences(userId: String): Result<NotificationPreferences>
    suspend fun getScheduledNotifications(userId: String): Result<List<NotificationSchedule>>
    suspend fun processEngagementNotifications(): Result<List<NotificationDeliveryResult>>
    suspend fun processStreakRiskNotifications(): Result<List<NotificationDeliveryResult>>
    suspend fun processGoalProgressNotifications(): Result<List<NotificationDeliveryResult>>
    suspend fun processMilestoneNotifications(): Result<List<NotificationDeliveryResult>>
}

interface NotificationTemplateService {
    fun getTemplate(type: NotificationType): NotificationTemplate
    fun generatePersonalizedContent(
        template: NotificationTemplate,
        userData: UserEngagementData
    ): NotificationContent
    fun getStreakRiskMessage(streakType: String, daysActive: Int): NotificationContent
    fun getEngagementReminderMessage(userData: UserEngagementData): NotificationContent
    fun getGoalProgressMessage(goalName: String, progress: Double): NotificationContent
    fun getMilestoneMessage(milestone: String): NotificationContent
}

interface PushNotificationProvider {
    suspend fun sendNotification(
        userId: String,
        content: NotificationContent
    ): Result<NotificationDeliveryResult>
    suspend fun subscribeToTopic(userId: String, topic: String): Result<Unit>
    suspend fun unsubscribeFromTopic(userId: String, topic: String): Result<Unit>
    suspend fun updateDeviceToken(userId: String, token: String): Result<Unit>
}