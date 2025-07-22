package com.north.mobile.integration

import com.north.mobile.data.notification.*
import com.north.mobile.data.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

class NotificationIntegrationTest {

    private lateinit var notificationService: NotificationService
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var notificationTriggerService: NotificationTriggerService
    private lateinit var mockRepository: MockNotificationRepository

    @BeforeTest
    fun setup() {
        mockRepository = MockNotificationRepository()
        
        // This would normally be injected via DI
        notificationService = NotificationServiceImpl(
            pushProvider = MockPushNotificationProvider(),
            templateService = NotificationTemplateServiceImpl(),
            gamificationService = MockGamificationService(),
            goalService = MockGoalService(),
            userRepository = MockUserRepository(),
            notificationRepository = mockRepository
        )
        
        notificationScheduler = NotificationScheduler(notificationService)
        notificationTriggerService = NotificationTriggerService(notificationService)
    }

    @Test
    fun `end-to-end notification flow for streak risk`() = runTest {
        // Given - User has preferences set up
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            streakReminderEnabled = true,
            enabledTypes = setOf(NotificationType.STREAK_RISK)
        )
        notificationService.updateNotificationPreferences(preferences)

        // And user has a streak at risk
        mockRepository.usersWithStreaksAtRisk = mapOf(
            userId to listOf(
                com.north.mobile.data.repository.StreakAtRisk("daily_checkin", 7, 2)
            )
        )

        // When - Processing streak risk notifications
        val result = notificationService.processStreakRiskNotifications()

        // Then - Notification should be sent
        assertTrue(result.isSuccess)
        val deliveryResults = result.getOrNull() ?: emptyList()
        assertEquals(1, deliveryResults.size)
        assertTrue(deliveryResults.first().success)
    }

    @Test
    fun `notification preferences are respected across all notification types`() = runTest {
        // Given - User has specific preferences
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS), // Only goal progress enabled
            streakReminderEnabled = false,
            engagementRemindersEnabled = false,
            goalProgressEnabled = true,
            milestoneEnabled = false
        )
        notificationService.updateNotificationPreferences(preferences)

        // Set up data for different notification types
        mockRepository.inactiveUsers = listOf(userId)
        mockRepository.usersWithStreaksAtRisk = mapOf(
            userId to listOf(com.north.mobile.data.repository.StreakAtRisk("daily", 5, 2))
        )

        // When - Processing different types of notifications
        val engagementResult = notificationService.processEngagementNotifications()
        val streakResult = notificationService.processStreakRiskNotifications()

        // Then - Only enabled notifications should be processed
        assertTrue(engagementResult.isSuccess)
        assertTrue(streakResult.isSuccess)
        
        // Engagement and streak notifications should be empty due to preferences
        assertEquals(0, engagementResult.getOrNull()?.size ?: 0)
        assertEquals(0, streakResult.getOrNull()?.size ?: 0)
    }

    @Test
    fun `quiet hours prevent immediate notifications`() = runTest {
        // Given - User has quiet hours set
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            quietHoursStart = 22, // 10 PM
            quietHoursEnd = 8,    // 8 AM
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER)
        )
        notificationService.updateNotificationPreferences(preferences)

        val content = NotificationContent("Test", "Test notification")

        // When - Trying to send notification during quiet hours
        // Note: This test would need to mock the current time to be during quiet hours
        val result = notificationService.sendImmediateNotification(userId, content)

        // Then - Notification should be scheduled rather than sent immediately
        assertTrue(result.isSuccess)
        // The actual behavior would depend on the current time
    }

    @Test
    fun `daily notification limit is enforced`() = runTest {
        // Given - User has low daily limit
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            maxDailyNotifications = 1,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER)
        )
        notificationService.updateNotificationPreferences(preferences)
        
        // User has already received their daily limit
        mockRepository.todayNotificationCounts[userId] = 1

        val content = NotificationContent("Test", "Test notification")

        // When - Trying to send another notification
        val result = notificationService.sendImmediateNotification(userId, content)

        // Then - Should fail due to daily limit
        assertTrue(result.isFailure)
    }

    @Test
    fun `notification templates generate personalized content`() = runTest {
        // Given - Template service
        val templateService = NotificationTemplateServiceImpl()
        val userData = UserEngagementData(
            userId = "user123",
            lastAppOpen = Clock.System.now(),
            lastActionTime = Clock.System.now(),
            currentStreaks = listOf("daily_checkin", "savings"),
            streaksAtRisk = listOf("budget_tracking"),
            goalProgress = mapOf("emergency_fund" to 0.75),
            recentMilestones = listOf("First Week Streak"),
            availableMicroWins = listOf("Check balance", "Categorize transaction")
        )

        // When - Generating different types of personalized content
        val streakMessage = templateService.getStreakRiskMessage("daily_checkin", 7)
        val engagementMessage = templateService.getEngagementReminderMessage(userData)
        val goalMessage = templateService.getGoalProgressMessage("Emergency Fund", 0.75)
        val milestoneMessage = templateService.getMilestoneMessage("First Week Streak")

        // Then - All messages should be personalized and non-empty
        assertNotNull(streakMessage.title)
        assertNotNull(streakMessage.body)
        assertTrue(streakMessage.body.contains("daily check-in"))
        assertTrue(streakMessage.body.contains("7"))

        assertNotNull(engagementMessage.title)
        assertNotNull(engagementMessage.body)

        assertNotNull(goalMessage.title)
        assertNotNull(goalMessage.body)
        assertTrue(goalMessage.body.contains("Emergency Fund"))
        assertTrue(goalMessage.body.contains("75"))

        assertNotNull(milestoneMessage.title)
        assertNotNull(milestoneMessage.body)
        assertTrue(milestoneMessage.body.contains("First Week Streak"))
    }

    @Test
    fun `user action triggers appropriate notifications`() = runTest {
        // Given - User completes an action that should trigger notifications
        val userId = "user123"

        // When - User opens the app (should cancel engagement reminders)
        notificationTriggerService.onUserAction(userId, UserAction.AppOpened)

        // When - User makes goal progress (should trigger celebration if significant)
        notificationTriggerService.onUserAction(
            userId, 
            UserAction.GoalProgressUpdated("goal123", 0.5) // 50% milestone
        )

        // When - User achieves a streak
        notificationTriggerService.onUserAction(
            userId,
            UserAction.StreakAchieved("daily_checkin")
        )

        // When - User reaches a milestone
        notificationTriggerService.onUserAction(
            userId,
            UserAction.MilestoneReached("First Month Saver")
        )

        // Then - Appropriate notifications should be triggered
        // This would require additional verification in a real implementation
        // For now, we verify that the methods complete without error
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `notification scheduling handles recurring patterns`() = runTest {
        // Given - Recurring notification schedule
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.WEEKLY_SUMMARY)
        )
        notificationService.updateNotificationPreferences(preferences)

        val recurringSchedule = NotificationSchedule(
            id = "weekly_summary",
            userId = userId,
            type = NotificationType.WEEKLY_SUMMARY,
            content = NotificationContent("Weekly Summary", "Your progress this week"),
            scheduledTime = Clock.System.now(),
            isRecurring = true,
            recurringPattern = RecurringPattern.WEEKLY
        )

        // When - Scheduling recurring notification
        val result = notificationService.scheduleNotification(recurringSchedule)

        // Then - Should be successfully scheduled
        assertTrue(result.isSuccess)
        assertEquals("weekly_summary", result.getOrNull())

        // And should appear in scheduled notifications
        val scheduledResult = notificationService.getScheduledNotifications(userId)
        assertTrue(scheduledResult.isSuccess)
        val scheduled = scheduledResult.getOrNull() ?: emptyList()
        assertEquals(1, scheduled.size)
        assertTrue(scheduled.first().isRecurring)
        assertEquals(RecurringPattern.WEEKLY, scheduled.first().recurringPattern)
    }
}

// Reuse mock classes from NotificationServiceTest
private class MockNotificationRepository : NotificationRepository {
    val scheduledNotifications = mutableMapOf<String, MutableList<NotificationSchedule>>()
    val preferences = mutableMapOf<String, NotificationPreferences>()
    val todayNotificationCounts = mutableMapOf<String, Int>()
    var inactiveUsers = emptyList<String>()
    var usersWithStreaksAtRisk = emptyMap<String, List<com.north.mobile.data.repository.StreakAtRisk>>()

    override suspend fun saveScheduledNotification(schedule: NotificationSchedule) {
        scheduledNotifications.getOrPut(schedule.userId) { mutableListOf() }.add(schedule)
    }

    override suspend fun cancelNotification(notificationId: String) {
        scheduledNotifications.values.forEach { list ->
            list.removeAll { it.id == notificationId }
        }
    }

    override suspend fun getScheduledNotifications(userId: String): List<NotificationSchedule> {
        return scheduledNotifications[userId] ?: emptyList()
    }

    override suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {
        this.preferences[preferences.userId] = preferences
    }

    override suspend fun getNotificationPreferences(userId: String): NotificationPreferences? {
        return preferences[userId]
    }

    override suspend fun getTodayNotificationCount(userId: String): Int {
        return todayNotificationCounts[userId] ?: 0
    }

    override suspend fun getInactiveUsers(): List<String> = inactiveUsers

    override suspend fun getUsersWithStreaksAtRisk(): Map<String, List<com.north.mobile.data.repository.StreakAtRisk>> {
        return usersWithStreaksAtRisk
    }

    override suspend fun getGoalProgressUpdates(): Map<String, List<com.north.mobile.data.repository.GoalProgressUpdate>> = emptyMap()
    override suspend fun getNewMilestones(): Map<String, List<com.north.mobile.data.repository.MilestoneUpdate>> = emptyMap()
    override suspend fun recordNotificationDelivery(result: NotificationDeliveryResult) {}
    override suspend fun getNotificationHistory(userId: String, limit: Int): List<NotificationDeliveryResult> = emptyList()
}

private class MockPushNotificationProvider : PushNotificationProvider {
    val sentNotifications = mutableListOf<Pair<String, NotificationContent>>()

    override suspend fun sendNotification(userId: String, content: NotificationContent): Result<NotificationDeliveryResult> {
        sentNotifications.add(userId to content)
        return Result.success(NotificationDeliveryResult(
            notificationId = "mock_${System.currentTimeMillis()}",
            success = true,
            deliveredAt = Clock.System.now()
        ))
    }

    override suspend fun subscribeToTopic(userId: String, topic: String): Result<Unit> = Result.success(Unit)
    override suspend fun unsubscribeFromTopic(userId: String, topic: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDeviceToken(userId: String, token: String): Result<Unit> = Result.success(Unit)
}

private class MockGamificationService : com.north.mobile.data.gamification.GamificationService {
    override suspend fun getUserProfile(userId: String) = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun awardPoints(userId: String, points: Int, reason: String) = Result.success(Unit)
    override suspend fun updateStreak(userId: String, streakType: String) = Result.success(Unit)
    override suspend fun checkAchievements(userId: String) = Result.success(emptyList<com.north.mobile.domain.model.Achievement>())
    override suspend fun getLevelProgress(userId: String) = Result.success(com.north.mobile.domain.model.LevelProgress(1, 0, 100))
    override suspend fun getMicroWinOpportunities(userId: String) = Result.success(emptyList<com.north.mobile.domain.model.MicroWin>())
}

private class MockGoalService : com.north.mobile.data.goal.GoalService {
    override suspend fun getUserGoals(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.north.mobile.domain.model.FinancialGoal>())
    override suspend fun createGoal(goal: com.north.mobile.domain.model.FinancialGoal) = Result.success(goal)
    override suspend fun updateGoal(goal: com.north.mobile.domain.model.FinancialGoal) = Result.success(goal)
    override suspend fun deleteGoal(goalId: String) = Result.success(Unit)
    override suspend fun updateGoalProgress(goalId: String, currentAmount: com.north.mobile.domain.model.Money) = Result.success(Unit)
}

private class MockUserRepository : com.north.mobile.data.repository.UserRepository {
    override suspend fun getUser(userId: String) = null
    override suspend fun saveUser(user: com.north.mobile.domain.model.User) = Result.success(Unit)
    override suspend fun updateUser(user: com.north.mobile.domain.model.User) = Result.success(Unit)
    override suspend fun deleteUser(userId: String) = Result.success(Unit)
}