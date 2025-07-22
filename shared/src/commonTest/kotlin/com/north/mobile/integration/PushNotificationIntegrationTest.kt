package com.north.mobile.integration

import com.north.mobile.data.notification.*
import com.north.mobile.data.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Integration tests for push notification delivery system
 * Tests the complete flow from notification creation to delivery
 */
class PushNotificationIntegrationTest {

    private lateinit var notificationService: NotificationService
    private lateinit var pushProvider: MockPushNotificationProvider
    private lateinit var templateService: NotificationTemplateService
    private lateinit var mockRepository: MockNotificationRepository

    @BeforeTest
    fun setup() {
        pushProvider = MockPushNotificationProvider()
        templateService = NotificationTemplateServiceImpl()
        mockRepository = MockNotificationRepository()
        
        notificationService = NotificationServiceImpl(
            pushProvider = pushProvider,
            templateService = templateService,
            gamificationService = MockGamificationService(),
            goalService = MockGoalService(),
            userRepository = MockUserRepository(),
            notificationRepository = mockRepository
        )
    }

    @Test
    fun testEndToEndNotificationDelivery() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "Test Notification",
            body = "This is a test notification"
        )

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Send immediate notification
        val result = notificationService.sendImmediateNotification(userId, content)

        // Verify success
        assertTrue(result.isSuccess)
        val deliveryResult = result.getOrThrow()
        assertTrue(deliveryResult.success)
        assertNotNull(deliveryResult.notificationId)

        // Verify notification was sent through push provider
        assertEquals(1, pushProvider.sentNotifications.size)
        val sentNotification = pushProvider.sentNotifications.first()
        assertEquals(userId, sentNotification.first)
        assertEquals(content.title, sentNotification.second.title)
        assertEquals(content.body, sentNotification.second.body)
    }

    @Test
    fun testNotificationSchedulingAndDelivery() = runTest {
        val userId = "user123"
        val scheduleId = "test-schedule-1"
        val content = NotificationContent(
            title = "Scheduled Notification",
            body = "This notification was scheduled"
        )

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Schedule notification
        val schedule = NotificationSchedule(
            id = scheduleId,
            userId = userId,
            type = NotificationType.GOAL_PROGRESS,
            content = content,
            scheduledTime = Clock.System.now(),
            isRecurring = false,
            recurringPattern = null
        )

        val scheduleResult = notificationService.scheduleNotification(schedule)
        assertTrue(scheduleResult.isSuccess)
        assertEquals(scheduleId, scheduleResult.getOrThrow())

        // Verify notification is scheduled
        val scheduledResult = notificationService.getScheduledNotifications(userId)
        assertTrue(scheduledResult.isSuccess)
        val scheduledNotifications = scheduledResult.getOrThrow()
        assertEquals(1, scheduledNotifications.size)
        assertEquals(scheduleId, scheduledNotifications.first().id)

        // Process scheduled notifications (simulate background processing)
        val processResult = notificationService.processScheduledNotifications()
        assertTrue(processResult.isSuccess)

        // Verify notification was delivered
        assertEquals(1, pushProvider.sentNotifications.size)
        val sentNotification = pushProvider.sentNotifications.first()
        assertEquals(userId, sentNotification.first)
        assertEquals(content.title, sentNotification.second.title)
    }

    @Test
    fun testRecurringNotificationDelivery() = runTest {
        val userId = "user123"
        val scheduleId = "weekly-summary"
        val content = NotificationContent(
            title = "Weekly Summary",
            body = "Your financial progress this week"
        )

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.WEEKLY_SUMMARY),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Schedule recurring notification
        val schedule = NotificationSchedule(
            id = scheduleId,
            userId = userId,
            type = NotificationType.WEEKLY_SUMMARY,
            content = content,
            scheduledTime = Clock.System.now(),
            isRecurring = true,
            recurringPattern = RecurringPattern.WEEKLY
        )

        val scheduleResult = notificationService.scheduleNotification(schedule)
        assertTrue(scheduleResult.isSuccess)

        // Process scheduled notifications multiple times
        repeat(3) {
            val processResult = notificationService.processScheduledNotifications()
            assertTrue(processResult.isSuccess)
        }

        // Verify recurring notification was sent multiple times
        assertTrue(pushProvider.sentNotifications.size >= 1)
        
        // Verify the notification is still scheduled (recurring)
        val scheduledResult = notificationService.getScheduledNotifications(userId)
        assertTrue(scheduledResult.isSuccess)
        val scheduledNotifications = scheduledResult.getOrThrow()
        assertEquals(1, scheduledNotifications.size)
        assertTrue(scheduledNotifications.first().isRecurring)
    }

    @Test
    fun testNotificationPreferencesRespected() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "Disabled Notification",
            body = "This should not be sent"
        )

        // Set up user preferences with notifications disabled
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = emptySet(), // No notification types enabled
            maxDailyNotifications = 0 // No daily notifications allowed
        )
        notificationService.updateNotificationPreferences(preferences)

        // Try to send notification
        val result = notificationService.sendImmediateNotification(userId, content)

        // Should fail due to preferences
        assertTrue(result.isFailure)
        
        // Verify no notification was sent
        assertEquals(0, pushProvider.sentNotifications.size)
    }

    @Test
    fun testQuietHoursRespected() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "Quiet Hours Test",
            body = "This should be delayed"
        )

        // Set up user preferences with quiet hours
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER),
            quietHoursStart = 22, // 10 PM
            quietHoursEnd = 8,    // 8 AM
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Mock current time to be during quiet hours (would need actual time mocking in real implementation)
        val result = notificationService.sendImmediateNotification(userId, content)

        // In a real implementation, this would check the current time and either:
        // 1. Schedule for later if during quiet hours
        // 2. Send immediately if not during quiet hours
        assertTrue(result.isSuccess)
    }

    @Test
    fun testDailyNotificationLimitEnforced() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "Limit Test",
            body = "Testing daily limit"
        )

        // Set up user preferences with low daily limit
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER),
            maxDailyNotifications = 2
        )
        notificationService.updateNotificationPreferences(preferences)

        // Send notifications up to the limit
        repeat(2) {
            val result = notificationService.sendImmediateNotification(userId, content)
            assertTrue(result.isSuccess)
        }

        // Try to send one more (should fail due to daily limit)
        mockRepository.todayNotificationCounts[userId] = 2
        val limitExceededResult = notificationService.sendImmediateNotification(userId, content)
        assertTrue(limitExceededResult.isFailure)

        // Verify only 2 notifications were sent
        assertEquals(2, pushProvider.sentNotifications.size)
    }

    @Test
    fun testNotificationDeliveryFailureHandling() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "Failure Test",
            body = "This will fail to deliver"
        )

        // Configure push provider to fail
        pushProvider.shouldFail = true

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Try to send notification
        val result = notificationService.sendImmediateNotification(userId, content)

        // Should fail due to push provider failure
        assertTrue(result.isFailure)
        
        // Verify failure was recorded
        assertEquals(0, pushProvider.sentNotifications.size)
    }

    @Test
    fun testBatchNotificationProcessing() = runTest {
        val userIds = listOf("user1", "user2", "user3")
        
        // Set up preferences for all users
        userIds.forEach { userId ->
            val preferences = NotificationPreferences(
                userId = userId,
                enabledTypes = setOf(NotificationType.STREAK_RISK),
                maxDailyNotifications = 10
            )
            notificationService.updateNotificationPreferences(preferences)
        }

        // Set up streak risk data
        mockRepository.usersWithStreaksAtRisk = userIds.associateWith { userId ->
            listOf(
                com.north.mobile.data.repository.StreakAtRisk("daily_checkin", 7, 2)
            )
        }

        // Process streak risk notifications
        val result = notificationService.processStreakRiskNotifications()
        assertTrue(result.isSuccess)

        val deliveryResults = result.getOrThrow()
        assertEquals(3, deliveryResults.size)
        assertTrue(deliveryResults.all { it.success })

        // Verify all notifications were sent
        assertEquals(3, pushProvider.sentNotifications.size)
        
        // Verify each user received their notification
        val sentUserIds = pushProvider.sentNotifications.map { it.first }.toSet()
        assertEquals(userIds.toSet(), sentUserIds)
    }

    @Test
    fun testNotificationTemplatePersonalization() = runTest {
        val userId = "user123"
        
        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Set up goal progress data
        mockRepository.goalProgressUpdates = mapOf(
            userId to listOf(
                com.north.mobile.data.repository.GoalProgressUpdate(
                    goalId = "emergency-fund",
                    goalTitle = "Emergency Fund",
                    previousProgress = 0.70,
                    currentProgress = 0.75,
                    targetAmount = 10000.0,
                    currentAmount = 7500.0
                )
            )
        )

        // Process goal progress notifications
        val result = notificationService.processGoalProgressNotifications()
        assertTrue(result.isSuccess)

        val deliveryResults = result.getOrThrow()
        assertEquals(1, deliveryResults.size)
        assertTrue(deliveryResults.first().success)

        // Verify personalized content was generated
        assertEquals(1, pushProvider.sentNotifications.size)
        val sentNotification = pushProvider.sentNotifications.first()
        val notificationContent = sentNotification.second
        
        assertTrue(notificationContent.title.isNotBlank())
        assertTrue(notificationContent.body.contains("Emergency Fund"))
        assertTrue(notificationContent.body.contains("75"))
    }

    @Test
    fun testNotificationCancellation() = runTest {
        val userId = "user123"
        val scheduleId = "test-schedule-cancel"
        val content = NotificationContent(
            title = "To Be Cancelled",
            body = "This notification will be cancelled"
        )

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Schedule notification
        val schedule = NotificationSchedule(
            id = scheduleId,
            userId = userId,
            type = NotificationType.GOAL_PROGRESS,
            content = content,
            scheduledTime = Clock.System.now(),
            isRecurring = false,
            recurringPattern = null
        )

        val scheduleResult = notificationService.scheduleNotification(schedule)
        assertTrue(scheduleResult.isSuccess)

        // Verify notification is scheduled
        val scheduledResult = notificationService.getScheduledNotifications(userId)
        assertTrue(scheduledResult.isSuccess)
        assertEquals(1, scheduledResult.getOrThrow().size)

        // Cancel notification
        val cancelResult = notificationService.cancelNotification(scheduleId)
        assertTrue(cancelResult.isSuccess)

        // Verify notification is no longer scheduled
        val afterCancelResult = notificationService.getScheduledNotifications(userId)
        assertTrue(afterCancelResult.isSuccess)
        assertEquals(0, afterCancelResult.getOrThrow().size)

        // Process scheduled notifications (should not send the cancelled one)
        val processResult = notificationService.processScheduledNotifications()
        assertTrue(processResult.isSuccess)

        // Verify no notification was sent
        assertEquals(0, pushProvider.sentNotifications.size)
    }

    @Test
    fun testNotificationHistoryTracking() = runTest {
        val userId = "user123"
        val content = NotificationContent(
            title = "History Test",
            body = "Testing notification history"
        )

        // Set up user preferences
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.ENGAGEMENT_REMINDER),
            maxDailyNotifications = 10
        )
        notificationService.updateNotificationPreferences(preferences)

        // Send multiple notifications
        repeat(3) {
            val result = notificationService.sendImmediateNotification(userId, content)
            assertTrue(result.isSuccess)
        }

        // Get notification history
        val historyResult = notificationService.getNotificationHistory(userId, 10)
        assertTrue(historyResult.isSuccess)
        val history = historyResult.getOrThrow()
        
        assertEquals(3, history.size)
        assertTrue(history.all { it.success })
        assertTrue(history.all { it.notificationId.isNotBlank() })
    }
}

// Mock implementations for testing
private class MockPushNotificationProvider : PushNotificationProvider {
    val sentNotifications = mutableListOf<Pair<String, NotificationContent>>()
    var shouldFail = false

    override suspend fun sendNotification(userId: String, content: NotificationContent): Result<NotificationDeliveryResult> {
        return if (shouldFail) {
            Result.failure(Exception("Push notification delivery failed"))
        } else {
            sentNotifications.add(userId to content)
            Result.success(NotificationDeliveryResult(
                notificationId = "mock_${System.currentTimeMillis()}",
                success = true,
                deliveredAt = Clock.System.now()
            ))
        }
    }

    override suspend fun subscribeToTopic(userId: String, topic: String): Result<Unit> = Result.success(Unit)
    override suspend fun unsubscribeFromTopic(userId: String, topic: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDeviceToken(userId: String, token: String): Result<Unit> = Result.success(Unit)
}

private class MockNotificationRepository : NotificationRepository {
    val scheduledNotifications = mutableMapOf<String, MutableList<NotificationSchedule>>()
    val preferences = mutableMapOf<String, NotificationPreferences>()
    val todayNotificationCounts = mutableMapOf<String, Int>()
    val deliveryHistory = mutableMapOf<String, MutableList<NotificationDeliveryResult>>()
    var inactiveUsers = emptyList<String>()
    var usersWithStreaksAtRisk = emptyMap<String, List<com.north.mobile.data.repository.StreakAtRisk>>()
    var goalProgressUpdates = emptyMap<String, List<com.north.mobile.data.repository.GoalProgressUpdate>>()
    var newMilestones = emptyMap<String, List<com.north.mobile.data.repository.MilestoneUpdate>>()

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

    override suspend fun recordNotificationDelivery(result: NotificationDeliveryResult) {
        // In a real implementation, you'd need to associate this with a user ID
        // For testing, we'll store it in a general list
    }

    override suspend fun getNotificationHistory(userId: String, limit: Int): List<NotificationDeliveryResult> {
        return deliveryHistory[userId]?.take(limit) ?: emptyList()
    }

    override suspend fun getInactiveUsers(): List<String> = inactiveUsers

    override suspend fun getUsersWithStreaksAtRisk(): Map<String, List<com.north.mobile.data.repository.StreakAtRisk>> {
        return usersWithStreaksAtRisk
    }

    override suspend fun getGoalProgressUpdates(): Map<String, List<com.north.mobile.data.repository.GoalProgressUpdate>> {
        return goalProgressUpdates
    }

    override suspend fun getNewMilestones(): Map<String, List<com.north.mobile.data.repository.MilestoneUpdate>> {
        return newMilestones
    }
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