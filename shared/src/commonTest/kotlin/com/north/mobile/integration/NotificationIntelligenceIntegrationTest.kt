package com.north.mobile.integration

import com.north.mobile.data.notification.*
import com.north.mobile.data.repository.NotificationRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.Transaction
import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Category
import kotlinx.datetime.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for the complete notification intelligence system,
 * testing the interaction between all components.
 */
class NotificationIntelligenceIntegrationTest {

    private lateinit var notificationRepository: MockNotificationRepository
    private lateinit var transactionRepository: MockTransactionRepository
    private lateinit var userRepository: MockUserRepository
    private lateinit var behaviorTracker: UserBehaviorTracker
    private lateinit var intelligenceService: NotificationIntelligenceService
    private lateinit var optimizationEngine: NotificationOptimizationEngine

    @BeforeTest
    fun setup() {
        notificationRepository = MockNotificationRepository()
        transactionRepository = MockTransactionRepository()
        userRepository = MockUserRepository()
        
        behaviorTracker = UserBehaviorTrackerImpl(notificationRepository, transactionRepository)
        intelligenceService = NotificationIntelligenceServiceImpl(
            notificationRepository,
            transactionRepository,
            userRepository
        )
        optimizationEngine = NotificationOptimizationEngineImpl(
            notificationRepository,
            behaviorTracker
        )
    }

    @Test
    fun `complete user journey - from new user to personalized notifications`() = runTest {
        val userId = "journey_user"
        val now = Clock.System.now()

        // Step 1: New user starts using the app
        behaviorTracker.recordAppOpen(userId, now.minus(Duration.parse("P7D")))
        behaviorTracker.recordAppOpen(userId, now.minus(Duration.parse("P6D")))
        behaviorTracker.recordAppOpen(userId, now.minus(Duration.parse("P5D")))

        // Step 2: User receives and interacts with notifications
        val notificationId1 = "welcome_notif"
        behaviorTracker.recordNotificationInteraction(
            userId = userId,
            notificationId = notificationId1,
            interactionType = InteractionType.OPENED,
            responseTime = Duration.parse("PT15S")
        )

        val notificationId2 = "tip_notif"
        behaviorTracker.recordNotificationInteraction(
            userId = userId,
            notificationId = notificationId2,
            interactionType = InteractionType.ACTION_TAKEN,
            responseTime = Duration.parse("PT5S")
        )

        // Step 3: User makes transactions
        val transactions = listOf(
            createTransaction("tx1", 45.0, "Coffee", now.minus(Duration.parse("P3D"))),
            createTransaction("tx2", 120.0, "Groceries", now.minus(Duration.parse("P2D"))),
            createTransaction("tx3", 200.0, "Groceries", now.minus(Duration.parse("P1D"))) // Unusual spending
        )
        transactionRepository.recentTransactions[userId] = transactions

        // Step 4: Update spending patterns
        behaviorTracker.updateSpendingPatterns(userId)

        // Step 5: Add location data
        val coffeeShopLocation = UserLocation(43.6532, -79.3832, 10.0)
        behaviorTracker.recordUserLocation(userId, coffeeShopLocation, 45.0, "Coffee")

        // Step 6: Analyze optimal timing
        val optimalTiming = intelligenceService.analyzeOptimalNotificationTiming(userId)
        assertTrue(optimalTiming.confidence > 0.3f) // Should have some confidence with data

        // Step 7: Calculate adaptive frequency
        val frequency = intelligenceService.calculateAdaptiveFrequency(userId)
        assertTrue(frequency.dailyLimit >= 2) // Should allow reasonable notifications

        // Step 8: Generate contextual notifications
        val contextualNotifications = intelligenceService.generateContextualNotifications(userId)
        assertTrue(contextualNotifications.isNotEmpty()) // Should detect unusual spending

        // Step 9: Generate location-based notifications
        val locationNotifications = intelligenceService.generateLocationBasedNotifications(userId, coffeeShopLocation)
        assertTrue(locationNotifications.isNotEmpty()) // Should recognize coffee shop pattern

        // Step 10: Get personalized schedule
        val schedule = intelligenceService.getPersonalizedSchedule(userId)
        assertEquals(userId, schedule.userId)
        assertTrue(schedule.scheduledNotifications.isNotEmpty())

        // Step 11: Optimize notification content
        val baseNotification = contextualNotifications.first()
        val optimizedNotification = optimizationEngine.optimizeNotificationContent(userId, baseNotification)
        assertNotNull(optimizedNotification.optimizedContent)

        // Step 12: Calculate fatigue risk
        val fatigueRisk = optimizationEngine.calculateFatigueRisk(userId)
        assertEquals(FatigueLevel.LOW, fatigueRisk.riskLevel) // New user should have low fatigue

        // Step 13: Get personalized recommendations
        val recommendations = optimizationEngine.getPersonalizedRecommendations(userId)
        assertTrue(recommendations.isNotEmpty())

        // Verify the complete behavior analysis
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        assertTrue(behaviorAnalysis.overallEngagementScore > 0.5f) // Good engagement
        assertTrue(behaviorAnalysis.spendingInsights.topSpendingCategories.containsKey("Groceries"))
        assertEquals(1, behaviorAnalysis.locationInsights.totalUniqueLocations)
    }

    @Test
    fun `high engagement user receives optimized notifications`() = runTest {
        val userId = "high_engagement_user"
        val now = Clock.System.now()

        // Simulate high engagement user behavior
        repeat(20) { day ->
            val dayOffset = Duration.parse("P${day}D")
            // Multiple app opens per day
            behaviorTracker.recordAppOpen(userId, now.minus(dayOffset).plus(Duration.parse("PT8H")))
            behaviorTracker.recordAppOpen(userId, now.minus(dayOffset).plus(Duration.parse("PT12H")))
            behaviorTracker.recordAppOpen(userId, now.minus(dayOffset).plus(Duration.parse("PT18H")))

            // Positive notification interactions
            behaviorTracker.recordNotificationInteraction(
                userId = userId,
                notificationId = "notif_day_$day",
                interactionType = if (day % 3 == 0) InteractionType.ACTION_TAKEN else InteractionType.OPENED,
                responseTime = Duration.parse("PT${5 + (day % 10)}S")
            )
        }

        // When analyzing this user
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val frequency = intelligenceService.calculateAdaptiveFrequency(userId)
        val fatigueRisk = optimizationEngine.calculateFatigueRisk(userId)

        // Then
        assertTrue(behaviorAnalysis.overallEngagementScore > 0.7f) // High engagement
        assertTrue(frequency.dailyLimit >= 4) // More notifications allowed
        assertEquals(FatigueLevel.LOW, fatigueRisk.riskLevel) // Low fatigue despite high frequency
        assertTrue(behaviorAnalysis.appUsagePattern.usageConsistency > 0.5f) // Consistent usage
    }

    @Test
    fun `low engagement user receives reduced notifications`() = runTest {
        val userId = "low_engagement_user"
        val now = Clock.System.now()

        // Simulate low engagement user behavior
        repeat(10) { day ->
            val dayOffset = Duration.parse("P${day}D")
            // Infrequent app opens
            if (day % 3 == 0) {
                behaviorTracker.recordAppOpen(userId, now.minus(dayOffset).plus(Duration.parse("PT10H")))
            }

            // Mostly negative notification interactions
            behaviorTracker.recordNotificationInteraction(
                userId = userId,
                notificationId = "notif_day_$day",
                interactionType = when (day % 4) {
                    0 -> InteractionType.DISMISSED
                    1 -> InteractionType.IGNORED
                    2 -> InteractionType.DISMISSED
                    else -> InteractionType.OPENED
                },
                responseTime = Duration.parse("PT${30 + day}S")
            )
        }

        // When analyzing this user
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val frequency = intelligenceService.calculateAdaptiveFrequency(userId)
        val fatigueRisk = optimizationEngine.calculateFatigueRisk(userId)

        // Then
        assertTrue(behaviorAnalysis.overallEngagementScore < 0.4f) // Low engagement
        assertTrue(frequency.dailyLimit <= 2) // Fewer notifications allowed
        assertTrue(fatigueRisk.riskLevel != FatigueLevel.LOW) // Some fatigue risk
        assertTrue(fatigueRisk.recommendations.any { it.contains("Reduce") })
    }

    @Test
    fun `location-based spending patterns trigger relevant notifications`() = runTest {
        val userId = "location_user"
        val now = Clock.System.now()

        // Simulate regular coffee shop visits
        val coffeeShopLocation = UserLocation(43.6532, -79.3832, 10.0)
        repeat(5) { visit ->
            behaviorTracker.recordUserLocation(
                userId = userId,
                location = UserLocation(
                    coffeeShopLocation.latitude + (visit * 0.0001), // Slight variation
                    coffeeShopLocation.longitude + (visit * 0.0001),
                    10.0
                ),
                spendingAmount = 4.50 + (visit * 0.25), // Consistent spending
                category = "Coffee"
            )
        }

        // Simulate grocery store visits
        val groceryLocation = UserLocation(43.6600, -79.3900, 10.0)
        repeat(3) { visit ->
            behaviorTracker.recordUserLocation(
                userId = userId,
                location = UserLocation(
                    groceryLocation.latitude + (visit * 0.0001),
                    groceryLocation.longitude + (visit * 0.0001),
                    10.0
                ),
                spendingAmount = 80.0 + (visit * 10.0),
                category = "Groceries"
            )
        }

        // When user is near coffee shop
        val nearCoffeeShop = UserLocation(43.6533, -79.3833, 10.0)
        val locationNotifications = intelligenceService.generateLocationBasedNotifications(userId, nearCoffeeShop)

        // Then
        assertTrue(locationNotifications.isNotEmpty())
        val coffeeNotification = locationNotifications.find { it.message.contains("Coffee") }
        assertNotNull(coffeeNotification)
        assertTrue(coffeeNotification.actionable)

        // When user is near grocery store
        val nearGroceryStore = UserLocation(43.6601, -79.3901, 10.0)
        val groceryNotifications = intelligenceService.generateLocationBasedNotifications(userId, nearGroceryStore)

        // Then
        assertTrue(groceryNotifications.isNotEmpty())
        val groceryNotification = groceryNotifications.find { it.message.contains("Groceries") }
        assertNotNull(groceryNotification)
    }

    @Test
    fun `notification optimization improves over time`() = runTest {
        val userId = "learning_user"
        val now = Clock.System.now()

        // Initial notification with poor performance
        val initialNotification = ContextualNotification(
            id = "initial_notif",
            type = ContextualNotificationType.SPENDING_ALERT,
            title = "Spending Alert",
            message = "You have spent money recently.",
            priority = NotificationPriority.MEDIUM,
            triggerCondition = SpendingTrigger(
                type = TriggerType.SPENDING_THRESHOLD_EXCEEDED,
                threshold = 100.0,
                category = null,
                timeWindow = Duration.parse("P7D")
            ),
            scheduledTime = null,
            expiresAt = now.plus(Duration.parse("P1D"))
        )

        // User dismisses initial notification
        val initialFeedback = NotificationFeedback(
            notificationId = "initial_notif",
            interactionType = InteractionType.DISMISSED,
            timestamp = now,
            responseTime = Duration.parse("PT2S")
        )
        optimizationEngine.updateOptimizationModels(userId, initialFeedback)

        // Generate optimized version
        val optimizedNotification = optimizationEngine.optimizeNotificationContent(userId, initialNotification)

        // The optimized version should be different and potentially better
        assertNotEquals(initialNotification.message, optimizedNotification.optimizedContent.message)
        assertTrue(optimizedNotification.confidence >= 0.0f)
        assertTrue(optimizedNotification.alternatives.isNotEmpty())

        // Simulate positive feedback on optimized notification
        val optimizedFeedback = NotificationFeedback(
            notificationId = optimizedNotification.optimizedContent.id,
            interactionType = InteractionType.ACTION_TAKEN,
            timestamp = now.plus(Duration.parse("PT1H")),
            responseTime = Duration.parse("PT10S")
        )
        optimizationEngine.updateOptimizationModels(userId, optimizedFeedback)

        // User's engagement score should improve
        val finalEngagementScore = behaviorTracker.getEngagementScore(userId)
        assertTrue(finalEngagementScore > 0.5f)
    }

    // Helper methods
    private fun createTransaction(
        id: String,
        amount: Double,
        categoryName: String,
        date: Instant
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = Money(amount, "CAD"),
            description = "Test transaction for $categoryName",
            category = Category(categoryName, "🏷️"),
            date = date.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            isRecurring = false
        )
    }
}

// Reuse mock classes from the other test files
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
    override suspend fun getTodayNotificationCount(userId: String): Int = totalNotificationsSent[userId] ?: 0
    override suspend fun getInactiveUsers(): List<String> = emptyList()
    override suspend fun getUsersWithStreaksAtRisk(): Map<String, List<com.north.mobile.data.repository.StreakAtRisk>> = emptyMap()
    override suspend fun getGoalProgressUpdates(): Map<String, List<com.north.mobile.data.repository.GoalProgressUpdate>> = emptyMap()
    override suspend fun getNewMilestones(): Map<String, List<com.north.mobile.data.repository.MilestoneUpdate>> = emptyMap()
    override suspend fun recordNotificationDelivery(result: NotificationDeliveryResult) {}
    override suspend fun getNotificationHistory(userId: String, limit: Int): List<NotificationDeliveryResult> {
        return interactions.values.flatten()
            .filter { it.userId == userId }
            .take(limit)
            .map { interaction ->
                NotificationDeliveryResult(
                    notificationId = interaction.notificationId,
                    success = true,
                    deliveredAt = interaction.timestamp,
                    userId = userId,
                    interactionType = interaction.interactionType.name.lowercase(),
                    effectiveness = effectiveness[interaction.notificationId]
                )
            }
    }

    override suspend fun getUserBehaviorData(userId: String): UserBehaviorData? = userBehaviorData[userId]
    override suspend fun saveUserBehaviorData(behaviorData: UserBehaviorData) {
        userBehaviorData[behaviorData.userId] = behaviorData
    }
    override suspend fun saveNotificationInteraction(interaction: NotificationInteraction) {
        interactions.getOrPut(interaction.notificationId) { mutableListOf() }.add(interaction)
        // Update total sent count
        totalNotificationsSent[interaction.userId] = (totalNotificationsSent[interaction.userId] ?: 0) + 1
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