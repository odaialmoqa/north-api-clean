package com.north.mobile.data.notification

import com.north.mobile.data.repository.NotificationRepository
import com.north.mobile.data.repository.TransactionRepository
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Service responsible for tracking user behavior patterns to optimize notification delivery
 */
interface UserBehaviorTracker {
    
    /**
     * Records when user opens the app
     */
    suspend fun recordAppOpen(userId: String, timestamp: Instant = Clock.System.now())
    
    /**
     * Records user interaction with notifications
     */
    suspend fun recordNotificationInteraction(
        userId: String,
        notificationId: String,
        interactionType: InteractionType,
        responseTime: Duration? = null
    )
    
    /**
     * Records user location for location-based insights
     */
    suspend fun recordUserLocation(
        userId: String,
        location: UserLocation,
        spendingAmount: Double? = null,
        category: String? = null
    )
    
    /**
     * Updates spending patterns based on new transactions
     */
    suspend fun updateSpendingPatterns(userId: String)
    
    /**
     * Gets current engagement score for user
     */
    suspend fun getEngagementScore(userId: String): Float
    
    /**
     * Analyzes user behavior and returns insights
     */
    suspend fun analyzeBehaviorPatterns(userId: String): BehaviorAnalysis
}

class UserBehaviorTrackerImpl(
    private val notificationRepository: NotificationRepository,
    private val transactionRepository: TransactionRepository
) : UserBehaviorTracker {

    override suspend fun recordAppOpen(userId: String, timestamp: Instant) {
        val currentBehavior = notificationRepository.getUserBehaviorData(userId)
        val updatedBehavior = if (currentBehavior != null) {
            val updatedOpenTimes = (currentBehavior.appOpenTimes + timestamp)
                .takeLast(100) // Keep last 100 app opens
            currentBehavior.copy(appOpenTimes = updatedOpenTimes)
        } else {
            UserBehaviorData(
                userId = userId,
                appOpenTimes = listOf(timestamp),
                notificationInteractions = emptyList(),
                spendingPatterns = createDefaultSpendingPatterns(),
                locationData = emptyList(),
                engagementScore = 0.5f
            )
        }
        
        notificationRepository.saveUserBehaviorData(updatedBehavior)
    }

    override suspend fun recordNotificationInteraction(
        userId: String,
        notificationId: String,
        interactionType: InteractionType,
        responseTime: Duration?
    ) {
        val interaction = NotificationInteraction(
            notificationId = notificationId,
            userId = userId,
            interactionType = interactionType,
            timestamp = Clock.System.now(),
            responseTime = responseTime
        )
        
        notificationRepository.saveNotificationInteraction(interaction)
        
        // Update user behavior data
        val currentBehavior = notificationRepository.getUserBehaviorData(userId)
        if (currentBehavior != null) {
            val updatedInteractions = (currentBehavior.notificationInteractions + interaction)
                .takeLast(200) // Keep last 200 interactions
            val updatedEngagementScore = calculateEngagementScore(updatedInteractions)
            
            val updatedBehavior = currentBehavior.copy(
                notificationInteractions = updatedInteractions,
                engagementScore = updatedEngagementScore
            )
            
            notificationRepository.saveUserBehaviorData(updatedBehavior)
        }
    }

    override suspend fun recordUserLocation(
        userId: String,
        location: UserLocation,
        spendingAmount: Double?,
        category: String?
    ) {
        val currentBehavior = notificationRepository.getUserBehaviorData(userId)
        if (currentBehavior != null) {
            val locationPoint = LocationDataPoint(
                location = location,
                timestamp = Clock.System.now(),
                spendingAmount = spendingAmount,
                category = category
            )
            
            val updatedLocationData = (currentBehavior.locationData + locationPoint)
                .takeLast(500) // Keep last 500 location points
            
            val updatedBehavior = currentBehavior.copy(locationData = updatedLocationData)
            notificationRepository.saveUserBehaviorData(updatedBehavior)
        }
    }

    override suspend fun updateSpendingPatterns(userId: String) {
        val transactions = transactionRepository.getRecentTransactions(userId, Duration.parse("P90D"))
        val spendingPatterns = analyzeSpendingPatterns(transactions)
        
        val currentBehavior = notificationRepository.getUserBehaviorData(userId)
        if (currentBehavior != null) {
            val updatedBehavior = currentBehavior.copy(spendingPatterns = spendingPatterns)
            notificationRepository.saveUserBehaviorData(updatedBehavior)
        }
    }

    override suspend fun getEngagementScore(userId: String): Float {
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        return behaviorData?.engagementScore ?: 0.5f
    }

    override suspend fun analyzeBehaviorPatterns(userId: String): BehaviorAnalysis {
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
            ?: return BehaviorAnalysis.empty(userId)
        
        val appUsagePattern = analyzeAppUsagePattern(behaviorData.appOpenTimes)
        val notificationEngagement = analyzeNotificationEngagement(behaviorData.notificationInteractions)
        val spendingInsights = analyzeSpendingBehavior(behaviorData.spendingPatterns)
        val locationInsights = analyzeLocationBehavior(behaviorData.locationData)
        
        return BehaviorAnalysis(
            userId = userId,
            appUsagePattern = appUsagePattern,
            notificationEngagement = notificationEngagement,
            spendingInsights = spendingInsights,
            locationInsights = locationInsights,
            overallEngagementScore = behaviorData.engagementScore,
            recommendedNotificationTiming = calculateOptimalTiming(behaviorData),
            recommendedFrequency = calculateOptimalFrequency(behaviorData)
        )
    }

    private fun createDefaultSpendingPatterns(): SpendingPatternData {
        return SpendingPatternData(
            averageDailySpending = 0.0,
            spendingByCategory = emptyMap(),
            spendingByTimeOfDay = emptyMap(),
            spendingByDayOfWeek = emptyMap(),
            unusualSpendingThreshold = 0.0
        )
    }

    private fun calculateEngagementScore(interactions: List<NotificationInteraction>): Float {
        if (interactions.isEmpty()) return 0.5f
        
        val recentInteractions = interactions.filter { 
            it.timestamp > Clock.System.now().minus(Duration.parse("P30D"))
        }
        
        if (recentInteractions.isEmpty()) return 0.3f
        
        val positiveWeight = 1.0f
        val neutralWeight = 0.5f
        val negativeWeight = -0.5f
        
        val score = recentInteractions.sumOf { interaction ->
            when (interaction.interactionType) {
                InteractionType.ACTION_TAKEN -> positiveWeight.toDouble()
                InteractionType.OPENED -> positiveWeight * 0.8
                InteractionType.SNOOZED -> neutralWeight.toDouble()
                InteractionType.DISMISSED -> negativeWeight.toDouble()
                InteractionType.IGNORED -> negativeWeight * 0.5
            }
        }
        
        val maxPossibleScore = recentInteractions.size * positiveWeight
        val normalizedScore = (score / maxPossibleScore).toFloat()
        
        return normalizedScore.coerceIn(0.0f, 1.0f)
    }

    private suspend fun analyzeSpendingPatterns(transactions: List<com.north.mobile.domain.model.Transaction>): SpendingPatternData {
        if (transactions.isEmpty()) return createDefaultSpendingPatterns()
        
        val totalSpending = transactions.sumOf { it.amount.amount }
        val dayCount = transactions.map { it.date }.distinct().size.coerceAtLeast(1)
        val averageDailySpending = totalSpending / dayCount
        
        // Group by category
        val spendingByCategory = transactions
            .groupBy { it.category.name }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.amount } }
        
        // Group by time of day
        val spendingByTimeOfDay = transactions
            .groupBy { it.date.atTime(12, 0).hour } // Simplified - using noon as default
            .mapValues { (_, txns) -> txns.sumOf { it.amount.amount } }
        
        // Group by day of week
        val spendingByDayOfWeek = transactions
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.amount } }
        
        // Calculate unusual spending threshold (2 standard deviations above mean)
        val dailySpendingAmounts = transactions
            .groupBy { it.date }
            .values
            .map { txns -> txns.sumOf { it.amount.amount } }
        
        val mean = dailySpendingAmounts.average()
        val variance = dailySpendingAmounts.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val unusualThreshold = mean + (2 * stdDev)
        
        return SpendingPatternData(
            averageDailySpending = averageDailySpending,
            spendingByCategory = spendingByCategory,
            spendingByTimeOfDay = spendingByTimeOfDay,
            spendingByDayOfWeek = spendingByDayOfWeek,
            unusualSpendingThreshold = unusualThreshold
        )
    }

    private fun analyzeAppUsagePattern(appOpenTimes: List<Instant>): AppUsagePattern {
        if (appOpenTimes.isEmpty()) {
            return AppUsagePattern(
                peakHours = listOf(9, 12, 18),
                averageSessionsPerDay = 0.0,
                mostActiveDay = DayOfWeek.MONDAY,
                usageConsistency = 0.0f
            )
        }
        
        val timeZone = TimeZone.currentSystemDefault()
        val hourFrequency = mutableMapOf<Int, Int>()
        val dayFrequency = mutableMapOf<DayOfWeek, Int>()
        
        appOpenTimes.forEach { instant ->
            val dateTime = instant.toLocalDateTime(timeZone)
            hourFrequency[dateTime.hour] = hourFrequency.getOrDefault(dateTime.hour, 0) + 1
            dayFrequency[dateTime.dayOfWeek] = dayFrequency.getOrDefault(dateTime.dayOfWeek, 0) + 1
        }
        
        val peakHours = hourFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        val mostActiveDay = dayFrequency.maxByOrNull { it.value }?.key ?: DayOfWeek.MONDAY
        
        val totalDays = appOpenTimes.map { 
            it.toLocalDateTime(timeZone).date 
        }.distinct().size.coerceAtLeast(1)
        
        val averageSessionsPerDay = appOpenTimes.size.toDouble() / totalDays
        
        // Calculate consistency (how evenly distributed usage is across days)
        val dailyCounts = dayFrequency.values
        val mean = dailyCounts.average()
        val variance = dailyCounts.map { (it - mean) * (it - mean) }.average()
        val consistency = if (variance == 0.0) 1.0f else (1.0f / (1.0f + variance.toFloat()))
        
        return AppUsagePattern(
            peakHours = peakHours,
            averageSessionsPerDay = averageSessionsPerDay,
            mostActiveDay = mostActiveDay,
            usageConsistency = consistency
        )
    }

    private fun analyzeNotificationEngagement(interactions: List<NotificationInteraction>): NotificationEngagementPattern {
        if (interactions.isEmpty()) {
            return NotificationEngagementPattern(
                openRate = 0.0f,
                actionRate = 0.0f,
                dismissalRate = 0.0f,
                averageResponseTime = Duration.ZERO,
                preferredInteractionTimes = emptyList()
            )
        }
        
        val totalInteractions = interactions.size
        val openCount = interactions.count { it.interactionType == InteractionType.OPENED }
        val actionCount = interactions.count { it.interactionType == InteractionType.ACTION_TAKEN }
        val dismissalCount = interactions.count { it.interactionType == InteractionType.DISMISSED }
        
        val responseTimes = interactions.mapNotNull { it.responseTime }
        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            Duration.milliseconds(responseTimes.map { it.inWholeMilliseconds }.average().toLong())
        } else {
            Duration.ZERO
        }
        
        val timeZone = TimeZone.currentSystemDefault()
        val interactionHours = interactions.map { 
            it.timestamp.toLocalDateTime(timeZone).hour 
        }
        val preferredTimes = interactionHours
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        return NotificationEngagementPattern(
            openRate = openCount.toFloat() / totalInteractions,
            actionRate = actionCount.toFloat() / totalInteractions,
            dismissalRate = dismissalCount.toFloat() / totalInteractions,
            averageResponseTime = averageResponseTime,
            preferredInteractionTimes = preferredTimes
        )
    }

    private fun analyzeSpendingBehavior(spendingPatterns: SpendingPatternData): SpendingBehaviorInsights {
        return SpendingBehaviorInsights(
            topSpendingCategories = spendingPatterns.spendingByCategory
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .associate { it.key to it.value },
            spendingTrend = calculateSpendingTrend(spendingPatterns),
            riskCategories = identifyRiskCategories(spendingPatterns)
        )
    }

    private fun analyzeLocationBehavior(locationData: List<LocationDataPoint>): LocationBehaviorInsights {
        val spendingLocations = locationData.filter { it.spendingAmount != null && it.spendingAmount > 0 }
        
        val frequentSpendingLocations = spendingLocations
            .groupBy { locationKey(it.location) }
            .filter { it.value.size >= 3 }
            .mapValues { (_, points) ->
                FrequentLocation(
                    location = points.first().location,
                    visitCount = points.size,
                    averageSpending = points.mapNotNull { it.spendingAmount }.average(),
                    commonCategories = points.mapNotNull { it.category }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                )
            }
        
        return LocationBehaviorInsights(
            frequentSpendingLocations = frequentSpendingLocations.values.toList(),
            totalUniqueLocations = locationData.map { locationKey(it.location) }.distinct().size
        )
    }

    private fun calculateOptimalTiming(behaviorData: UserBehaviorData): List<Int> {
        val appOpenTimes = behaviorData.appOpenTimes
        if (appOpenTimes.isEmpty()) return listOf(9, 12, 18)
        
        val timeZone = TimeZone.currentSystemDefault()
        val hourFrequency = appOpenTimes
            .map { it.toLocalDateTime(timeZone).hour }
            .groupingBy { it }
            .eachCount()
        
        return hourFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    private fun calculateOptimalFrequency(behaviorData: UserBehaviorData): Int {
        val engagementScore = behaviorData.engagementScore
        return when {
            engagementScore > 0.8f -> 5 // High engagement - up to 5 notifications per day
            engagementScore > 0.5f -> 3 // Medium engagement - up to 3 notifications per day
            else -> 2 // Low engagement - up to 2 notifications per day
        }
    }

    private fun calculateSpendingTrend(patterns: SpendingPatternData): String {
        // Simplified trend calculation
        return when {
            patterns.averageDailySpending > 100.0 -> "HIGH_SPENDING"
            patterns.averageDailySpending > 50.0 -> "MODERATE_SPENDING"
            else -> "LOW_SPENDING"
        }
    }

    private fun identifyRiskCategories(patterns: SpendingPatternData): List<String> {
        val totalSpending = patterns.spendingByCategory.values.sum()
        return patterns.spendingByCategory
            .filter { (_, amount) -> amount / totalSpending > 0.3 } // Categories > 30% of total spending
            .keys
            .toList()
    }

    private fun locationKey(location: UserLocation): String {
        val lat = (location.latitude * 1000).toInt() / 1000.0
        val lng = (location.longitude * 1000).toInt() / 1000.0
        return "${lat}_${lng}"
    }
}

// Data classes for behavior analysis
data class BehaviorAnalysis(
    val userId: String,
    val appUsagePattern: AppUsagePattern,
    val notificationEngagement: NotificationEngagementPattern,
    val spendingInsights: SpendingBehaviorInsights,
    val locationInsights: LocationBehaviorInsights,
    val overallEngagementScore: Float,
    val recommendedNotificationTiming: List<Int>,
    val recommendedFrequency: Int
) {
    companion object {
        fun empty(userId: String) = BehaviorAnalysis(
            userId = userId,
            appUsagePattern = AppUsagePattern(
                peakHours = listOf(9, 12, 18),
                averageSessionsPerDay = 0.0,
                mostActiveDay = DayOfWeek.MONDAY,
                usageConsistency = 0.0f
            ),
            notificationEngagement = NotificationEngagementPattern(
                openRate = 0.0f,
                actionRate = 0.0f,
                dismissalRate = 0.0f,
                averageResponseTime = Duration.ZERO,
                preferredInteractionTimes = emptyList()
            ),
            spendingInsights = SpendingBehaviorInsights(
                topSpendingCategories = emptyMap(),
                spendingTrend = "UNKNOWN",
                riskCategories = emptyList()
            ),
            locationInsights = LocationBehaviorInsights(
                frequentSpendingLocations = emptyList(),
                totalUniqueLocations = 0
            ),
            overallEngagementScore = 0.5f,
            recommendedNotificationTiming = listOf(9, 12, 18),
            recommendedFrequency = 2
        )
    }
}

data class AppUsagePattern(
    val peakHours: List<Int>,
    val averageSessionsPerDay: Double,
    val mostActiveDay: DayOfWeek,
    val usageConsistency: Float
)

data class NotificationEngagementPattern(
    val openRate: Float,
    val actionRate: Float,
    val dismissalRate: Float,
    val averageResponseTime: Duration,
    val preferredInteractionTimes: List<Int>
)

data class SpendingBehaviorInsights(
    val topSpendingCategories: Map<String, Double>,
    val spendingTrend: String,
    val riskCategories: List<String>
)

data class LocationBehaviorInsights(
    val frequentSpendingLocations: List<FrequentLocation>,
    val totalUniqueLocations: Int
)

data class FrequentLocation(
    val location: UserLocation,
    val visitCount: Int,
    val averageSpending: Double,
    val commonCategories: List<String>
)