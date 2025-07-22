package com.north.mobile.data.notification

import com.north.mobile.data.repository.NotificationRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.Transaction
import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Category
import kotlinx.datetime.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class NotificationIntelligenceServiceTest {

    private lateinit var notificationRepository: MockNotificationRepository
    private lateinit var transactionRepository: MockTransactionRepository
    private lateinit var userRepository: MockUserRepository
    private lateinit var service: NotificationIntelligenceService

    @BeforeTest
    fun setup() {
        notificationRepository = MockNotificationRepository()
        transactionRepository = MockTransactionRepository()
        userRepository = MockUserRepository()
        service = NotificationIntelligenceServiceImpl(
            notificationRepository,
            transactionRepository,
            userRepository
        )
    }

    @Test
    fun `analyzeOptimalNotificationTiming returns default timing for new users`() = runTest {
        // Given
        val userId = "new_user"
        notificationRepository.userBehaviorData[userId] = null

        // When
        val result = service.analyzeOptimalNotificationTiming(userId)

        // Then
        assertEquals(listOf(9, 12, 18), result.preferredHours)
        assertEquals(0.3f, result.confidence)
        assertTrue(result.preferredDays.containsAll(DayOfWeek.values().toList()))
    }

    @Test
    fun `analyzeOptimalNotificationTiming uses app usage patterns`() = runTest {
        // Given
        val userId = "active_user"
        val now = Clock.System.now()
        val appOpenTimes = listOf(
            now.minus(Duration.parse("P1D")).plus(Duration.parse("PT8H")), // 8 AM yesterday
            now.minus(Duration.parse("P1D")).plus(Duration.parse("PT13H")), // 1 PM yesterday
            now.minus(Duration.parse("P1D")).plus(Duration.parse("PT19H")), // 7 PM yesterday
            now.plus(Duration.parse("PT8H")), // 8 AM today
            now.plus(Duration.parse("PT13H")), // 1 PM today
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = appOpenTimes,
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.7f
        )

        // When
        val result = service.analyzeOptimalNotificationTiming(userId)

        // Then
        assertTrue(result.preferredHours.contains(8))
        assertTrue(result.preferredHours.contains(13))
        assertTrue(result.confidence > 0.5f)
    }

    @Test
    fun `calculateAdaptiveFrequency adjusts based on engagement`() = runTest {
        // Given - High engagement user
        val userId = "high_engagement_user"
        val interactions = listOf(
            createNotificationInteraction("notif1", InteractionType.OPENED),
            createNotificationInteraction("notif2", InteractionType.ACTION_TAKEN),
            createNotificationInteraction("notif3", InteractionType.OPENED),
            createNotificationInteraction("notif4", InteractionType.ACTION_TAKEN)
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = interactions,
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.9f
        )
        notificationRepository.totalNotificationsSent[userId] = 4

        // When
        val result = service.calculateAdaptiveFrequency(userId)

        // Then
        assertEquals(5, result.dailyLimit) // High engagement = more notifications
        assertEquals(25, result.weeklyLimit)
        assertEquals(1.2f, result.engagementBasedMultiplier)
    }

    @Test
    fun `calculateAdaptiveFrequency reduces frequency for low engagement`() = runTest {
        // Given - Low engagement user
        val userId = "low_engagement_user"
        val interactions = listOf(
            createNotificationInteraction("notif1", InteractionType.DISMISSED),
            createNotificationInteraction("notif2", InteractionType.IGNORED),
            createNotificationInteraction("notif3", InteractionType.DISMISSED)
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = interactions,
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.2f
        )
        notificationRepository.totalNotificationsSent[userId] = 10

        // When
        val result = service.calculateAdaptiveFrequency(userId)

        // Then
        assertEquals(2, result.dailyLimit) // Low engagement = fewer notifications
        assertEquals(8, result.weeklyLimit)
        assertEquals(0.7f, result.engagementBasedMultiplier)
    }

    @Test
    fun `generateContextualNotifications detects unusual spending`() = runTest {
        // Given
        val userId = "spender_user"
        val spendingPatterns = SpendingPatternData(
            averageDailySpending = 50.0,
            spendingByCategory = mapOf("Food" to 200.0, "Transport" to 100.0),
            spendingByTimeOfDay = emptyMap(),
            spendingByDayOfWeek = emptyMap(),
            unusualSpendingThreshold = 300.0
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = spendingPatterns,
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        // Mock recent transactions with high spending
        val recentTransactions = listOf(
            createTransaction("tx1", 150.0),
            createTransaction("tx2", 200.0)
        )
        transactionRepository.recentTransactions[userId] = recentTransactions

        // When
        val notifications = service.generateContextualNotifications(userId)

        // Then
        assertTrue(notifications.isNotEmpty())
        val unusualSpendingNotif = notifications.find { 
            it.type == ContextualNotificationType.UNUSUAL_ACTIVITY 
        }
        assertNotNull(unusualSpendingNotif)
        assertTrue(unusualSpendingNotif.title.contains("Unusual Spending"))
    }

    @Test
    fun `generateContextualNotifications identifies savings opportunities`() = runTest {
        // Given
        val userId = "opportunity_user"
        val spendingPatterns = SpendingPatternData(
            averageDailySpending = 60.0,
            spendingByCategory = mapOf(
                "Food" to 500.0, // High spending in food category
                "Transport" to 100.0
            ),
            spendingByTimeOfDay = emptyMap(),
            spendingByDayOfWeek = emptyMap(),
            unusualSpendingThreshold = 1000.0
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = spendingPatterns,
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        transactionRepository.recentTransactions[userId] = listOf(
            createTransaction("tx1", 50.0)
        )

        // When
        val notifications = service.generateContextualNotifications(userId)

        // Then
        val savingsOpportunity = notifications.find { 
            it.type == ContextualNotificationType.SAVINGS_OPPORTUNITY 
        }
        assertNotNull(savingsOpportunity)
        assertTrue(savingsOpportunity.message.contains("Food"))
    }

    @Test
    fun `generateLocationBasedNotifications creates relevant insights`() = runTest {
        // Given
        val userId = "location_user"
        val currentLocation = UserLocation(43.6532, -79.3832, 10.0) // Toronto
        val historicalLocation = UserLocation(43.6530, -79.3830, 10.0) // Very close to current
        
        val locationData = listOf(
            LocationDataPoint(historicalLocation, Clock.System.now().minus(Duration.parse("P1D")), 25.0, "Coffee"),
            LocationDataPoint(historicalLocation, Clock.System.now().minus(Duration.parse("P2D")), 30.0, "Coffee"),
            LocationDataPoint(historicalLocation, Clock.System.now().minus(Duration.parse("P3D")), 20.0, "Coffee")
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = locationData,
            engagementScore = 0.5f
        )

        // When
        val notifications = service.generateLocationBasedNotifications(userId, currentLocation)

        // Then
        assertTrue(notifications.isNotEmpty())
        val locationNotif = notifications.first()
        assertTrue(locationNotif.message.contains("Coffee"))
        assertTrue(locationNotif.actionable)
    }

    @Test
    fun `trackNotificationEffectiveness updates user behavior`() = runTest {
        // Given
        val userId = "tracking_user"
        val notificationId = "test_notif"
        val interaction = NotificationInteraction(
            notificationId = notificationId,
            userId = userId,
            interactionType = InteractionType.ACTION_TAKEN,
            timestamp = Clock.System.now(),
            responseTime = Duration.parse("PT30S")
        )

        notificationRepository.notifications[notificationId] = NotificationDeliveryResult(
            notificationId = notificationId,
            success = true,
            deliveredAt = Clock.System.now(),
            userId = userId
        )

        // When
        service.trackNotificationEffectiveness(notificationId, interaction)

        // Then
        assertTrue(notificationRepository.interactions.containsKey(notificationId))
        assertTrue(notificationRepository.effectiveness.containsKey(notificationId))
        assertEquals(1.0f, notificationRepository.effectiveness[notificationId])
    }

    @Test
    fun `getPersonalizedSchedule creates optimized schedule`() = runTest {
        // Given
        val userId = "schedule_user"
        val appOpenTimes = listOf(
            Clock.System.now().minus(Duration.parse("PT12H")), // 12 hours ago
            Clock.System.now().minus(Duration.parse("PT6H"))   // 6 hours ago
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = appOpenTimes,
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.6f
        )

        transactionRepository.recentTransactions[userId] = emptyList()

        // When
        val schedule = service.getPersonalizedSchedule(userId)

        // Then
        assertEquals(userId, schedule.userId)
        assertNotNull(schedule.nextOptimalTime)
        assertTrue(schedule.currentFrequencySettings.dailyLimit > 0)
    }

    // Helper methods
    private fun createDefaultSpendingPatterns(): SpendingPatternData {
        return SpendingPatternData(
            averageDailySpending = 50.0,
            spendingByCategory = emptyMap(),
            spendingByTimeOfDay = emptyMap(),
            spendingByDayOfWeek = emptyMap(),
            unusualSpendingThreshold = 200.0
        )
    }

    private fun createNotificationInteraction(
        notificationId: String,
        interactionType: InteractionType
    ): NotificationInteraction {
        return NotificationInteraction(
            notificationId = notificationId,
            userId = "test_user",
            interactionType = interactionType,
            timestamp = Clock.System.now(),
            responseTime = Duration.parse("PT15S")
        )
    }

    private fun createTransaction(id: String, amount: Double): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = Money(amount, "CAD"),
            description = "Test transaction",
            category = Category("Food", "🍽️"),
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
            isRecurring = false
        )
    }
}

// Mock implementations
class MockNotificationRepository : NotificationRepository {
    val userBehaviorData = mutableMapOf<String, UserBehaviorData?>()
    val totalNotificationsSent = mutableMapOf<String, Int>()
    val interactions = mutableMapOf<String, MutableList<NotificationInteraction>>()
    val effectiveness = mutableMapOf<String, Float>()
    val notifications = mutableMapOf<String, NotificationDeliveryResult>()
    val streakData = mutableMapOf<String, List<com.north.mobile.data.repository.StreakData>>()

    override suspend fun saveScheduledNotification(schedule: NotificationSchedule) {}
    override suspend fun cancelNotification(notificationId: String) {}
    override suspend fun getScheduledNotifications(userId: String): List<NotificationSchedule> = emptyList()
    override suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {}
    override suspend fun getNotificationPreferences(userId: String): NotificationPreferences? = null
    override suspend fun getTodayNotificationCount(userId: String): Int = 0
    override suspend fun getInactiveUsers(): List<String> = emptyList()
    override suspend fun getUsersWithStreaksAtRisk(): Map<String, List<com.north.mobile.data.repository.StreakAtRisk>> = emptyMap()
    override suspend fun getGoalProgressUpdates(): Map<String, List<com.north.mobile.data.repository.GoalProgressUpdate>> = emptyMap()
    override suspend fun getNewMilestones(): Map<String, List<com.north.mobile.data.repository.MilestoneUpdate>> = emptyMap()
    override suspend fun recordNotificationDelivery(result: NotificationDeliveryResult) {}
    override suspend fun getNotificationHistory(userId: String, limit: Int): List<NotificationDeliveryResult> = emptyList()

    override suspend fun getUserBehaviorData(userId: String): UserBehaviorData? = userBehaviorData[userId]
    override suspend fun saveUserBehaviorData(behaviorData: UserBehaviorData) {
        userBehaviorData[behaviorData.userId] = behaviorData
    }
    override suspend fun saveNotificationInteraction(interaction: NotificationInteraction) {
        interactions.getOrPut(interaction.notificationId) { mutableListOf() }.add(interaction)
    }
    override suspend fun getNotificationInteractions(notificationId: String): List<NotificationInteraction> {
        return interactions[notificationId] ?: emptyList()
    }
    override suspend fun getTotalNotificationsSent(userId: String): Int = totalNotificationsSent[userId] ?: 0
    override suspend fun getNotificationById(notificationId: String): NotificationDeliveryResult? = notifications[notificationId]
    override suspend fun updateNotificationEffectiveness(notificationId: String, effectiveness: Float) {
        this.effectiveness[notificationId] = effectiveness
    }
    override suspend fun getUserStreakData(userId: String): List<com.north.mobile.data.repository.StreakData>? = streakData[userId]
}

class MockTransactionRepository : TransactionRepository {
    val recentTransactions = mutableMapOf<String, List<Transaction>>()

    override suspend fun getRecentTransactions(userId: String, period: Duration): List<Transaction> {
        return recentTransactions[userId] ?: emptyList()
    }

    override suspend fun saveTransaction(transaction: Transaction) {}
    override suspend fun getTransactionById(transactionId: String): Transaction? = null
    override suspend fun getTransactionsByAccount(accountId: String): List<Transaction> = emptyList()
    override suspend fun getTransactionsByCategory(userId: String, category: String): List<Transaction> = emptyList()
    override suspend fun getTransactionsByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<Transaction> = emptyList()
    override suspend fun updateTransaction(transaction: Transaction) {}
    override suspend fun deleteTransaction(transactionId: String) {}
    override suspend fun getTransactionCount(userId: String): Int = 0
    override suspend fun searchTransactions(userId: String, query: String): List<Transaction> = emptyList()
}

class MockUserRepository : UserRepository {
    override suspend fun createUser(user: com.north.mobile.domain.model.User): Result<com.north.mobile.domain.model.User> = Result.success(user)
    override suspend fun getUserById(userId: String): com.north.mobile.domain.model.User? = null
    override suspend fun getUserByEmail(email: String): com.north.mobile.domain.model.User? = null
    override suspend fun updateUser(user: com.north.mobile.domain.model.User): Result<com.north.mobile.domain.model.User> = Result.success(user)
    override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getAllUsers(): List<com.north.mobile.domain.model.User> = emptyList()
}