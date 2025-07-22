package com.north.mobile.data.notification

import kotlinx.datetime.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserBehaviorTrackerTest {

    private lateinit var notificationRepository: MockNotificationRepository
    private lateinit var transactionRepository: MockTransactionRepository
    private lateinit var tracker: UserBehaviorTracker

    @BeforeTest
    fun setup() {
        notificationRepository = MockNotificationRepository()
        transactionRepository = MockTransactionRepository()
        tracker = UserBehaviorTrackerImpl(notificationRepository, transactionRepository)
    }

    @Test
    fun `recordAppOpen creates new behavior data for new user`() = runTest {
        // Given
        val userId = "new_user"
        val timestamp = Clock.System.now()

        // When
        tracker.recordAppOpen(userId, timestamp)

        // Then
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(userId, behaviorData.userId)
        assertEquals(listOf(timestamp), behaviorData.appOpenTimes)
        assertEquals(0.5f, behaviorData.engagementScore)
    }

    @Test
    fun `recordAppOpen updates existing behavior data`() = runTest {
        // Given
        val userId = "existing_user"
        val firstTimestamp = Clock.System.now().minus(Duration.parse("PT1H"))
        val secondTimestamp = Clock.System.now()

        // When
        tracker.recordAppOpen(userId, firstTimestamp)
        tracker.recordAppOpen(userId, secondTimestamp)

        // Then
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(2, behaviorData.appOpenTimes.size)
        assertTrue(behaviorData.appOpenTimes.contains(firstTimestamp))
        assertTrue(behaviorData.appOpenTimes.contains(secondTimestamp))
    }

    @Test
    fun `recordAppOpen limits stored app opens to 100`() = runTest {
        // Given
        val userId = "heavy_user"
        val baseTime = Clock.System.now()

        // Create initial behavior data with 100 app opens
        val initialAppOpens = (1..100).map { 
            baseTime.minus(Duration.parse("PT${it}H")) 
        }
        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = initialAppOpens,
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        // When - Add one more app open
        tracker.recordAppOpen(userId, baseTime)

        // Then - Should still have 100 app opens (oldest removed)
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(100, behaviorData.appOpenTimes.size)
        assertTrue(behaviorData.appOpenTimes.contains(baseTime))
        assertFalse(behaviorData.appOpenTimes.contains(initialAppOpens.last())) // Oldest removed
    }

    @Test
    fun `recordNotificationInteraction updates engagement score`() = runTest {
        // Given
        val userId = "interactive_user"
        val notificationId = "test_notif"

        // Create initial behavior data
        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        // When - Record positive interaction
        tracker.recordNotificationInteraction(
            userId = userId,
            notificationId = notificationId,
            interactionType = InteractionType.ACTION_TAKEN,
            responseTime = Duration.parse("PT30S")
        )

        // Then
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(1, behaviorData.notificationInteractions.size)
        assertTrue(behaviorData.engagementScore > 0.5f) // Should increase with positive interaction
    }

    @Test
    fun `recordNotificationInteraction decreases engagement for negative interactions`() = runTest {
        // Given
        val userId = "dismissive_user"
        val notificationId = "test_notif"

        // Create initial behavior data
        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        // When - Record negative interactions
        tracker.recordNotificationInteraction(
            userId = userId,
            notificationId = "${notificationId}_1",
            interactionType = InteractionType.DISMISSED
        )
        tracker.recordNotificationInteraction(
            userId = userId,
            notificationId = "${notificationId}_2",
            interactionType = InteractionType.IGNORED
        )

        // Then
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(2, behaviorData.notificationInteractions.size)
        assertTrue(behaviorData.engagementScore < 0.5f) // Should decrease with negative interactions
    }

    @Test
    fun `recordUserLocation stores location data`() = runTest {
        // Given
        val userId = "mobile_user"
        val location = UserLocation(43.6532, -79.3832, 10.0) // Toronto
        val spendingAmount = 25.50
        val category = "Coffee"

        // Create initial behavior data
        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = 0.5f
        )

        // When
        tracker.recordUserLocation(userId, location, spendingAmount, category)

        // Then
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(1, behaviorData.locationData.size)
        
        val locationPoint = behaviorData.locationData.first()
        assertEquals(location, locationPoint.location)
        assertEquals(spendingAmount, locationPoint.spendingAmount)
        assertEquals(category, locationPoint.category)
    }

    @Test
    fun `recordUserLocation limits stored locations to 500`() = runTest {
        // Given
        val userId = "traveler_user"
        val baseLocation = UserLocation(43.6532, -79.3832, 10.0)

        // Create initial behavior data with 500 location points
        val initialLocations = (1..500).map { 
            LocationDataPoint(
                location = UserLocation(baseLocation.latitude + it * 0.001, baseLocation.longitude, 10.0),
                timestamp = Clock.System.now().minus(Duration.parse("PT${it}M")),
                spendingAmount = null,
                category = null
            )
        }
        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = initialLocations,
            engagementScore = 0.5f
        )

        // When - Add one more location
        val newLocation = UserLocation(44.0, -79.0, 10.0)
        tracker.recordUserLocation(userId, newLocation, 10.0, "Food")

        // Then - Should still have 500 locations (oldest removed)
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        assertNotNull(behaviorData)
        assertEquals(500, behaviorData.locationData.size)
        assertEquals(newLocation, behaviorData.locationData.last().location)
    }

    @Test
    fun `getEngagementScore returns correct score`() = runTest {
        // Given
        val userId = "scored_user"
        val expectedScore = 0.75f

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = emptyList(),
            notificationInteractions = emptyList(),
            spendingPatterns = createDefaultSpendingPatterns(),
            locationData = emptyList(),
            engagementScore = expectedScore
        )

        // When
        val score = tracker.getEngagementScore(userId)

        // Then
        assertEquals(expectedScore, score)
    }

    @Test
    fun `getEngagementScore returns default for unknown user`() = runTest {
        // Given
        val userId = "unknown_user"

        // When
        val score = tracker.getEngagementScore(userId)

        // Then
        assertEquals(0.5f, score) // Default score
    }

    @Test
    fun `analyzeBehaviorPatterns returns comprehensive analysis`() = runTest {
        // Given
        val userId = "analyzed_user"
        val now = Clock.System.now()
        val appOpenTimes = listOf(
            now.minus(Duration.parse("PT8H")),  // 8 hours ago
            now.minus(Duration.parse("PT4H")),  // 4 hours ago
            now.minus(Duration.parse("PT1H"))   // 1 hour ago
        )
        
        val interactions = listOf(
            NotificationInteraction("n1", userId, InteractionType.OPENED, now.minus(Duration.parse("PT2H")), Duration.parse("PT10S")),
            NotificationInteraction("n2", userId, InteractionType.ACTION_TAKEN, now.minus(Duration.parse("PT1H")), Duration.parse("PT5S"))
        )

        notificationRepository.userBehaviorData[userId] = UserBehaviorData(
            userId = userId,
            appOpenTimes = appOpenTimes,
            notificationInteractions = interactions,
            spendingPatterns = SpendingPatternData(
                averageDailySpending = 75.0,
                spendingByCategory = mapOf("Food" to 300.0, "Transport" to 150.0),
                spendingByTimeOfDay = emptyMap(),
                spendingByDayOfWeek = emptyMap(),
                unusualSpendingThreshold = 200.0
            ),
            locationData = emptyList(),
            engagementScore = 0.8f
        )

        // When
        val analysis = tracker.analyzeBehaviorPatterns(userId)

        // Then
        assertEquals(userId, analysis.userId)
        assertEquals(0.8f, analysis.overallEngagementScore)
        assertTrue(analysis.appUsagePattern.averageSessionsPerDay > 0)
        assertTrue(analysis.notificationEngagement.openRate > 0)
        assertTrue(analysis.spendingInsights.topSpendingCategories.isNotEmpty())
        assertTrue(analysis.recommendedNotificationTiming.isNotEmpty())
        assertTrue(analysis.recommendedFrequency > 0)
    }

    @Test
    fun `analyzeBehaviorPatterns returns empty analysis for unknown user`() = runTest {
        // Given
        val userId = "unknown_user"

        // When
        val analysis = tracker.analyzeBehaviorPatterns(userId)

        // Then
        assertEquals(userId, analysis.userId)
        assertEquals(0.5f, analysis.overallEngagementScore)
        assertEquals(listOf(9, 12, 18), analysis.recommendedNotificationTiming)
        assertEquals(2, analysis.recommendedFrequency)
    }

    private fun createDefaultSpendingPatterns(): SpendingPatternData {
        return SpendingPatternData(
            averageDailySpending = 50.0,
            spendingByCategory = emptyMap(),
            spendingByTimeOfDay = emptyMap(),
            spendingByDayOfWeek = emptyMap(),
            unusualSpendingThreshold = 200.0
        )
    }
}