package com.north.mobile.data.notification

import com.north.mobile.data.gamification.GamificationService
import com.north.mobile.data.goal.GoalService
import com.north.mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import kotlin.random.Random

class NotificationServiceImpl(
    private val pushProvider: PushNotificationProvider,
    private val templateService: NotificationTemplateService,
    private val gamificationService: GamificationService,
    private val goalService: GoalService,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) : NotificationService {

    override suspend fun scheduleNotification(schedule: NotificationSchedule): Result<String> {
        return try {
            val preferences = getNotificationPreferences(schedule.userId).getOrNull()
            if (preferences?.enabledTypes?.contains(schedule.type) != true) {
                return Result.failure(Exception("Notification type disabled for user"))
            }

            if (isInQuietHours(schedule.scheduledTime, preferences)) {
                val adjustedSchedule = adjustForQuietHours(schedule, preferences)
                notificationRepository.saveScheduledNotification(adjustedSchedule)
                Result.success(adjustedSchedule.id)
            } else {
                notificationRepository.saveScheduledNotification(schedule)
                Result.success(schedule.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelNotification(notificationId: String): Result<Unit> {
        return try {
            notificationRepository.cancelNotification(notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendImmediateNotification(
        userId: String,
        content: NotificationContent
    ): Result<NotificationDeliveryResult> {
        return try {
            val preferences = getNotificationPreferences(userId).getOrNull()
            if (preferences == null) {
                return Result.failure(Exception("User preferences not found"))
            }

            // Check daily notification limit
            val todayCount = notificationRepository.getTodayNotificationCount(userId)
            if (todayCount >= preferences.maxDailyNotifications) {
                return Result.failure(Exception("Daily notification limit reached"))
            }

            // Check quiet hours
            val now = Clock.System.now()
            if (isInQuietHours(now, preferences)) {
                // Schedule for after quiet hours instead
                val scheduledTime = getNextAvailableTime(preferences)
                val schedule = NotificationSchedule(
                    id = generateNotificationId(),
                    userId = userId,
                    type = NotificationType.ENGAGEMENT_REMINDER,
                    content = content,
                    scheduledTime = scheduledTime
                )
                scheduleNotification(schedule)
                return Result.success(NotificationDeliveryResult(
                    notificationId = schedule.id,
                    success = true,
                    deliveredAt = null
                ))
            }

            pushProvider.sendNotification(userId, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            notificationRepository.saveNotificationPreferences(preferences)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotificationPreferences(userId: String): Result<NotificationPreferences> {
        return try {
            val preferences = notificationRepository.getNotificationPreferences(userId)
                ?: NotificationPreferences(userId = userId)
            Result.success(preferences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getScheduledNotifications(userId: String): Result<List<NotificationSchedule>> {
        return try {
            val notifications = notificationRepository.getScheduledNotifications(userId)
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processEngagementNotifications(): Result<List<NotificationDeliveryResult>> {
        return try {
            val results = mutableListOf<NotificationDeliveryResult>()
            val inactiveUsers = notificationRepository.getInactiveUsers()

            for (userId in inactiveUsers) {
                val preferences = getNotificationPreferences(userId).getOrNull()
                if (preferences?.engagementRemindersEnabled != true) continue

                val userData = getUserEngagementData(userId)
                val content = templateService.getEngagementReminderMessage(userData)
                
                val result = sendImmediateNotification(userId, content).getOrNull()
                if (result != null) {
                    results.add(result)
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processStreakRiskNotifications(): Result<List<NotificationDeliveryResult>> {
        return try {
            val results = mutableListOf<NotificationDeliveryResult>()
            val usersAtRisk = notificationRepository.getUsersWithStreaksAtRisk()

            for ((userId, streaksAtRisk) in usersAtRisk) {
                val preferences = getNotificationPreferences(userId).getOrNull()
                if (preferences?.streakReminderEnabled != true) continue

                for (streak in streaksAtRisk) {
                    val content = templateService.getStreakRiskMessage(streak.type, streak.currentCount)
                    val result = sendImmediateNotification(userId, content).getOrNull()
                    if (result != null) {
                        results.add(result)
                    }
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processGoalProgressNotifications(): Result<List<NotificationDeliveryResult>> {
        return try {
            val results = mutableListOf<NotificationDeliveryResult>()
            val goalUpdates = notificationRepository.getGoalProgressUpdates()

            for ((userId, goals) in goalUpdates) {
                val preferences = getNotificationPreferences(userId).getOrNull()
                if (preferences?.goalProgressEnabled != true) continue

                for (goal in goals) {
                    val content = templateService.getGoalProgressMessage(goal.title, goal.progressPercentage)
                    val result = sendImmediateNotification(userId, content).getOrNull()
                    if (result != null) {
                        results.add(result)
                    }
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processMilestoneNotifications(): Result<List<NotificationDeliveryResult>> {
        return try {
            val results = mutableListOf<NotificationDeliveryResult>()
            val milestones = notificationRepository.getNewMilestones()

            for ((userId, userMilestones) in milestones) {
                val preferences = getNotificationPreferences(userId).getOrNull()
                if (preferences?.milestoneEnabled != true) continue

                for (milestone in userMilestones) {
                    val content = templateService.getMilestoneMessage(milestone.description)
                    val result = sendImmediateNotification(userId, content).getOrNull()
                    if (result != null) {
                        results.add(result)
                    }
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserEngagementData(userId: String): UserEngagementData {
        val user = userRepository.getUser(userId)
        val gamificationData = gamificationService.getUserProfile(userId).first()
        val goals = goalService.getUserGoals(userId).first()

        return UserEngagementData(
            userId = userId,
            lastAppOpen = user?.profile?.lastAppOpen ?: Clock.System.now(),
            lastActionTime = gamificationData?.lastActivity ?: Clock.System.now(),
            currentStreaks = gamificationData?.currentStreaks?.map { it.type } ?: emptyList(),
            streaksAtRisk = gamificationData?.currentStreaks?.filter { it.isAtRisk() }?.map { it.type } ?: emptyList(),
            goalProgress = goals.associate { it.id to it.progressPercentage },
            recentMilestones = gamificationData?.achievements?.takeLast(3)?.map { it.title } ?: emptyList(),
            availableMicroWins = emptyList() // TODO: Get from gamification service
        )
    }

    private fun isInQuietHours(time: Instant, preferences: NotificationPreferences): Boolean {
        val localTime = time.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = localTime.hour

        return if (preferences.quietHoursStart < preferences.quietHoursEnd) {
            hour >= preferences.quietHoursStart && hour < preferences.quietHoursEnd
        } else {
            hour >= preferences.quietHoursStart || hour < preferences.quietHoursEnd
        }
    }

    private fun adjustForQuietHours(
        schedule: NotificationSchedule,
        preferences: NotificationPreferences
    ): NotificationSchedule {
        val nextAvailableTime = getNextAvailableTime(preferences)
        return schedule.copy(scheduledTime = nextAvailableTime)
    }

    private fun getNextAvailableTime(preferences: NotificationPreferences): Instant {
        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())
        
        val nextAvailable = if (localNow.hour < preferences.quietHoursEnd) {
            localNow.date.atTime(preferences.quietHoursEnd, 0)
        } else {
            localNow.date.plus(1, DateTimeUnit.DAY).atTime(preferences.quietHoursEnd, 0)
        }
        
        return nextAvailable.toInstant(TimeZone.currentSystemDefault())
    }

    private fun generateNotificationId(): String {
        return "notif_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"
    }
}

// Extension function to check if streak is at risk
private fun com.north.mobile.domain.model.Streak.isAtRisk(): Boolean {
    val now = Clock.System.now()
    val hoursSinceLastActivity = (now - this.lastUpdated).inWholeHours
    return hoursSinceLastActivity > 20 // At risk if no activity in 20+ hours
}