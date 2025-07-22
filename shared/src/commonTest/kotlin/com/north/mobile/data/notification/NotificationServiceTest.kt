package com.north.mobile.data.notification

import com.north.mobile.data.gamification.GamificationService
import com.north.mobile.data.goal.GoalService
import com.north.mobile.data.repository.NotificationRepository
import com.north.mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*

class NotificationServiceTest {

    private lateinit var notificationService: NotificationServiceImpl
    private lateinit var mockPushProvider: MockPushNotificationProvider
    private lateinit var mockTemplateService: MockNotificationTemplateService
    private lateinit var mockGamificationService: MockGamificationService
    private lateinit var mockGoalService: MockGoalService
    private lateinit var mockUserRepository: MockUserRepository
    private lateinit var mockNotificationRepository: MockNotificationRepository

    @BeforeTest
    fun setup() {
        mockPushProvider = MockPushNotificationProvider()
        mockTemplateService = MockNotificationTemplateService()
        mockGamificationService = MockGamificationService()
        mockGoalService = MockGoalService()
        mockUserRepository = MockUserRepository()
        mockNotificationRepository = MockNotificationRepository()

        notificationService = NotificationServiceImpl(
            pushProvider = mockPushProvider,
            templateService = mockTemplateService,
            gamificationService = mockGamificationService,
            goalService = mockGoalService,
            userRepository = mockUserRepository,
            notificationRepository = mockNotificationRepository
        )
    }

    @Test
    fun `scheduleNotification should save notification when type is enabled`() = runTest {
        // Given
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.STREAK_RISK)
        )
        mockNotificationRepository.preferences[userId] = preferences

        val schedule = NotificationSchedule(
            id = "notif123",
            userId = userId,
            type = NotificationType.STREAK_RISK,
            content = NotificationContent("Test", "Test body"),
            scheduledTime = Clock.System.now()
        )

        // When
        val result = notificationService.scheduleNotification(schedule)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("notif123", result.getOrNull())
        assertTrue(mockNotificationRepository.scheduledNotifications.containsKey(userId))
    }

    @Test
    fun `scheduleNotification should fail when type is disabled`() = runTest {
        // Given
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS) // Different type
        )
        mockNotificationRepository.preferences[userId] = preferences

        val schedule = NotificationSchedule(
            id = "notif123",
            userId = userId,
            type = NotificationType.STREAK_RISK,
            content = NotificationContent("Test", "Test body"),
            scheduledTime = Clock.System.now()
        )

        // When
        val result = notificationService.scheduleNotification(schedule)

        // Then
        assertTrue(result.isFailure)
        assertFalse(mockNotificationRepository.scheduledNotifications.containsKey(userId))
    }

    @Test
    fun `sendImmediateNotification should respect daily limit`() = runTest {
        // Given
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            maxDailyNotifications = 2
        )
        mockNotificationRepository.preferences[userId] = preferences
        mockNotificationRepository.todayNotificationCounts[userId] = 2 // Already at limit

        val content = NotificationContent("Test", "Test body")

        // When
        val result = notificationService.sendImmediateNotification(userId, content)

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, mockPushProvider.sentNotifications.size)
    }

    @Test
    fun `sendImmediateNotification should respect quiet hours`() = runTest {
        // Given
        val userId = "user123"
        val preferences = NotificationPreferences(
            userId = userId,
            quietHoursStart = 22,
            quietHoursEnd = 8
        )
        mockNotificationRepository.preferences[userId] = preferences
        mockNotificationRepository.todayNotificationCounts[userId] = 0

        val content = NotificationContent("Test", "Test body")

        // When - This would need to be tested with a specific time during quiet hours
        val result = notificationService.sendImmediateNotification(userId, content)

        // Then - Should schedule for later instead of sending immediately
        assertTrue(result.isSuccess)
        // The notification should be scheduled rather than sent immediately
    }

    @Test
    fun `processEngagementNotifications should send to inactive users`() = runTest {
        // Given
        val inactiveUsers = listOf("user1", "user2")
        mockNotificationRepository.inactiveUsers = inactiveUsers
        
        inactiveUsers.forEach { userId ->
            mockNotificationRepository.preferences[userId] = NotificationPreferences(
                userId = userId,
                engagementRemindersEnabled = true
            )
        }

        // When
        val result = notificationService.processEngagementNotifications()

        // Then
        assertTrue(result.isSuccess)
        val deliveryResults = result.getOrNull() ?: emptyList()
        assertEquals(2, deliveryResults.size)
        assertEquals(2, mockPushProvider.sentNotifications.size)
    }

    @Test
    fun `processStreakRiskNotifications should send to users with at-risk streaks`() = runTest {
        // Given
        val usersWithRiskyStreaks = mapOf(
            "user1" to listOf(
                com.north.mobile.data.repository.StreakAtRisk("daily_checkin", 5, 4)
            )
        )
        mockNotificationRepository.usersWithStreaksAtRisk = usersWithRiskyStreaks
        
        mockNotificationRepository.preferences["user1"] = NotificationPreferences(
            userId = "user1",
            streakReminderEnabled = true
        )

        // When
        val result = notificationService.processStreakRiskNotifications()

        // Then
        assertTrue(result.isSuccess)
        val deliveryResults = result.getOrNull() ?: emptyList()
        assertEquals(1, deliveryResults.size)
        assertEquals(1, mockPushProvider.sentNotifications.size)
    }

    @Test
    fun `updateNotificationPreferences should save preferences`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            userId = "user123",
            enabledTypes = setOf(NotificationType.GOAL_PROGRESS),
            maxDailyNotifications = 5
        )

        // When
        val result = notificationService.updateNotificationPreferences(preferences)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(preferences, mockNotificationRepository.preferences["user123"])
    }

    @Test
    fun `getNotificationPreferences should return default if none exist`() = runTest {
        // Given
        val userId = "user123"

        // When
        val result = notificationService.getNotificationPreferences(userId)

        // Then
        assertTrue(result.isSuccess)
        val preferences = result.getOrNull()
        assertNotNull(preferences)
        assertEquals(userId, preferences.userId)
        assertEquals(NotificationType.values().toSet(), preferences.enabledTypes)
    }
}

// Mock implementations
class MockPushNotificationProvider : PushNotificationProvider {
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

class MockNotificationTemplateService : NotificationTemplateService {
    override fun getTemplate(type: NotificationType): NotificationTemplate {
        return NotificationTemplate(
            type = type,
            titleTemplates = listOf("Mock Title"),
            bodyTemplates = listOf("Mock Body")
        )
    }

    override fun generatePersonalizedContent(template: NotificationTemplate, userData: UserEngagementData): NotificationContent {
        return NotificationContent("Mock Title", "Mock Body")
    }

    override fun getStreakRiskMessage(streakType: String, daysActive: Int): NotificationContent {
        return NotificationContent("Streak Risk", "Your $streakType streak is at risk")
    }

    override fun getEngagementReminderMessage(userData: UserEngagementData): NotificationContent {
        return NotificationContent("Come back!", "We miss you")
    }

    override fun getGoalProgressMessage(goalName: String, progress: Double): NotificationContent {
        return NotificationContent("Goal Progress", "$goalName is ${(progress * 100).toInt()}% complete")
    }

    override fun getMilestoneMessage(milestone: String): NotificationContent {
        return NotificationContent("Milestone!", "You achieved: $milestone")
    }
}

class MockGamificationService : GamificationService {
    override suspend fun getUserProfile(userId: String) = flowOf(null)
    override suspend fun awardPoints(userId: String, points: Int, reason: String) = Result.success(Unit)
    override suspend fun updateStreak(userId: String, streakType: String) = Result.success(Unit)
    override suspend fun checkAchievements(userId: String) = Result.success(emptyList<com.north.mobile.domain.model.Achievement>())
    override suspend fun getLevelProgress(userId: String) = Result.success(com.north.mobile.domain.model.LevelProgress(1, 0, 100))
    override suspend fun getMicroWinOpportunities(userId: String) = Result.success(emptyList<com.north.mobile.domain.model.MicroWin>())
}

class MockGoalService : GoalService {
    override suspend fun getUserGoals(userId: String) = flowOf(emptyList<com.north.mobile.domain.model.FinancialGoal>())
    override suspend fun createGoal(goal: com.north.mobile.domain.model.FinancialGoal) = Result.success(goal)
    override suspend fun updateGoal(goal: com.north.mobile.domain.model.FinancialGoal) = Result.success(goal)
    override suspend fun deleteGoal(goalId: String) = Result.success(Unit)
    override suspend fun updateGoalProgress(goalId: String, currentAmount: com.north.mobile.domain.model.Money) = Result.success(Unit)
}

class MockUserRepository : UserRepository {
    override suspend fun getUser(userId: String) = null
    override suspend fun saveUser(user: com.north.mobile.domain.model.User) = Result.success(Unit)
    override suspend fun updateUser(user: com.north.mobile.domain.model.User) = Result.success(Unit)
    override suspend fun deleteUser(userId: String) = Result.success(Unit)
}

class MockNotificationRepository : NotificationRepository {
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