package com.north.mobile.data.notification

import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class NotificationScheduler(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private var schedulerJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        
        isRunning = true
        schedulerJob = scope.launch {
            while (isActive) {
                try {
                    processScheduledNotifications()
                    delay(15.minutes) // Check every 15 minutes
                } catch (e: Exception) {
                    // Log error but continue running
                    delay(5.minutes) // Wait 5 minutes before retrying
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        schedulerJob?.cancel()
        schedulerJob = null
    }

    private suspend fun processScheduledNotifications() {
        // Process different types of notifications
        processEngagementReminders()
        processStreakRiskNotifications()
        processGoalProgressNotifications()
        processMilestoneNotifications()
        processWeeklySummaries()
    }

    private suspend fun processEngagementReminders() {
        try {
            notificationService.processEngagementNotifications()
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun processStreakRiskNotifications() {
        try {
            notificationService.processStreakRiskNotifications()
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun processGoalProgressNotifications() {
        try {
            notificationService.processGoalProgressNotifications()
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun processMilestoneNotifications() {
        try {
            notificationService.processMilestoneNotifications()
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun processWeeklySummaries() {
        val now = Clock.System.now()
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Send weekly summaries on Sunday evenings
        if (localNow.dayOfWeek == DayOfWeek.SUNDAY && localNow.hour == 19) {
            try {
                // This would be implemented to send weekly summary notifications
                // notificationService.processWeeklySummaryNotifications()
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}

class NotificationTriggerService(
    private val notificationService: NotificationService
) {
    
    suspend fun onUserAction(userId: String, action: UserAction) {
        when (action) {
            is UserAction.AppOpened -> handleAppOpened(userId)
            is UserAction.GoalProgressUpdated -> handleGoalProgress(userId, action.goalId, action.progress)
            is UserAction.StreakAchieved -> handleStreakAchievement(userId, action.streakType)
            is UserAction.MilestoneReached -> handleMilestone(userId, action.milestone)
        }
    }

    private suspend fun handleAppOpened(userId: String) {
        // Cancel any pending engagement reminders since user is now active
        // This would require additional repository methods to find and cancel notifications
    }

    private suspend fun handleGoalProgress(userId: String, goalId: String, progress: Double) {
        // Check if this is a significant progress update (e.g., 25%, 50%, 75%, 90%)
        val significantMilestones = listOf(0.25, 0.5, 0.75, 0.9)
        if (significantMilestones.any { kotlin.math.abs(progress - it) < 0.01 }) {
            // Schedule a goal progress notification
            val content = NotificationContent(
                title = "Great progress! 🎯",
                body = "You've reached ${(progress * 100).toInt()}% of your goal!"
            )
            notificationService.sendImmediateNotification(userId, content)
        }
    }

    private suspend fun handleStreakAchievement(userId: String, streakType: String) {
        // Send immediate celebration for streak milestones
        val content = NotificationContent(
            title = "Streak milestone! 🔥",
            body = "Amazing work on your $streakType streak!"
        )
        notificationService.sendImmediateNotification(userId, content)
    }

    private suspend fun handleMilestone(userId: String, milestone: String) {
        // Send immediate milestone celebration
        val content = NotificationContent(
            title = "🎉 Milestone achieved!",
            body = "Congratulations! You've achieved: $milestone"
        )
        notificationService.sendImmediateNotification(userId, content)
    }
}

sealed class UserAction {
    object AppOpened : UserAction()
    data class GoalProgressUpdated(val goalId: String, val progress: Double) : UserAction()
    data class StreakAchieved(val streakType: String) : UserAction()
    data class MilestoneReached(val milestone: String) : UserAction()
}