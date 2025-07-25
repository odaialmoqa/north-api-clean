package com.north.mobile.data.notification

import com.north.mobile.data.repository.NotificationRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.Transaction
import kotlinx.datetime.*
import kotlinx.coroutines.flow.first
import kotlin.math.*

class NotificationIntelligenceServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) : NotificationIntelligenceService {

    override suspend fun analyzeOptimalNotificationTiming(userId: String): OptimalTimingResult {
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        val appOpenTimes = behaviorData?.appOpenTimes ?: emptyList()
        
        if (appOpenTimes.isEmpty()) {
            // Default timing for new users
            return OptimalTimingResult(
                preferredHours = listOf(9, 12, 18), // Morning, lunch, evening
                preferredDays = DayOfWeek.values().toList(),
                timeZone = TimeZone.currentSystemDefault(),
                confidence = 0.3f
            )
        }
        
        // Analyze app usage patterns
        val hourFrequency = mutableMapOf<Int, Int>()
        val dayFrequency = mutableMapOf<DayOfWeek, Int>()
        
        appOpenTimes.forEach { instant ->
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = dateTime.hour
            val day = dateTime.dayOfWeek
            
            hourFrequency[hour] = hourFrequency.getOrDefault(hour, 0) + 1
            dayFrequency[day] = dayFrequency.getOrDefault(day, 0) + 1
        }
        
        // Find top 3 hours with highest usage
        val preferredHours = hourFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Find days with above-average usage
        val averageDayUsage = dayFrequency.values.average()
        val preferredDays = dayFrequency.entries
            .filter { it.value >= averageDayUsage }
            .map { it.key }
        
        // Calculate confidence based on data consistency
        val totalSessions = appOpenTimes.size
        val confidence = when {
            totalSessions < 10 -> 0.4f
            totalSessions < 50 -> 0.6f
            totalSessions < 100 -> 0.8f
            else -> 0.9f
        }
        
        return OptimalTimingResult(
            preferredHours = preferredHours.ifEmpty { listOf(9, 12, 18) },
            preferredDays = preferredDays.ifEmpty { DayOfWeek.values().toList() },
            timeZone = TimeZone.currentSystemDefault(),
            confidence = confidence
        )
    }

    override suspend fun calculateAdaptiveFrequency(userId: String): NotificationFrequencySettings {
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        val engagementScore = behaviorData?.engagementScore ?: 0.5f
        
        // Calculate interaction rate
        val interactions = behaviorData?.notificationInteractions ?: emptyList()
        val totalNotifications = notificationRepository.getTotalNotificationsSent(userId)
        val interactionRate = if (totalNotifications > 0) {
            interactions.count { it.interactionType != InteractionType.IGNORED }.toFloat() / totalNotifications
        } else {
            0.5f // Default for new users
        }
        
        // Adaptive frequency based on engagement
        val baseFrequency = when {
            engagementScore > 0.8f && interactionRate > 0.7f -> {
                // High engagement - more frequent notifications
                NotificationFrequencySettings(
                    dailyLimit = 5,
                    weeklyLimit = 25,
                    minimumIntervalMinutes = 120,
                    engagementBasedMultiplier = 1.2f
                )
            }
            engagementScore > 0.5f && interactionRate > 0.4f -> {
                // Medium engagement - moderate frequency
                NotificationFrequencySettings(
                    dailyLimit = 3,
                    weeklyLimit = 15,
                    minimumIntervalMinutes = 180,
                    engagementBasedMultiplier = 1.0f
                )
            }
            else -> {
                // Low engagement - reduced frequency
                NotificationFrequencySettings(
                    dailyLimit = 2,
                    weeklyLimit = 8,
                    minimumIntervalMinutes = 360,
                    engagementBasedMultiplier = 0.7f
                )
            }
        }
        
        return baseFrequency
    }

    override suspend fun generateContextualNotifications(userId: String): List<ContextualNotification> {
        val notifications = mutableListOf<ContextualNotification>()
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        val spendingPatterns = behaviorData?.spendingPatterns
        
        if (spendingPatterns != null) {
            // Check for unusual spending
            val recentTransactions = transactionRepository.getRecentTransactions(userId, Duration.parse("P7D"))
            val recentSpending = recentTransactions.sumOf { it.amount.amount }
            
            if (recentSpending > spendingPatterns.unusualSpendingThreshold) {
                notifications.add(
                    ContextualNotification(
                        id = generateNotificationId(),
                        type = ContextualNotificationType.UNUSUAL_ACTIVITY,
                        title = "Unusual Spending Detected",
                        message = "You've spent ${formatCurrency(recentSpending)} this week, which is higher than usual. Would you like to review your transactions?",
                        priority = NotificationPriority.MEDIUM,
                        triggerCondition = SpendingTrigger(
                            type = TriggerType.UNUSUAL_SPENDING_DETECTED,
                            threshold = spendingPatterns.unusualSpendingThreshold,
                            category = null,
                            timeWindow = Duration.parse("P7D")
                        ),
                        scheduledTime = null,
                        expiresAt = Clock.System.now().plus(Duration.parse("P1D"))
                    )
                )
            }
            
            // Check for savings opportunities
            val categorySpending = spendingPatterns.spendingByCategory
            categorySpending.forEach { (category, amount) ->
                val averageForCategory = spendingPatterns.averageDailySpending * 0.3 // Assume 30% of daily spending per category
                if (amount > averageForCategory * 1.5) {
                    notifications.add(
                        ContextualNotification(
                            id = generateNotificationId(),
                            type = ContextualNotificationType.SAVINGS_OPPORTUNITY,
                            title = "Savings Opportunity in $category",
                            message = "You're spending more on $category than usual. Consider setting a budget to save money.",
                            priority = NotificationPriority.LOW,
                            triggerCondition = SpendingTrigger(
                                type = TriggerType.SAVINGS_GOAL_OPPORTUNITY,
                                threshold = averageForCategory * 1.5,
                                category = category,
                                timeWindow = Duration.parse("P30D")
                            ),
                            scheduledTime = null,
                            expiresAt = Clock.System.now().plus(Duration.parse("P3D"))
                        )
                    )
                }
            }
        }
        
        // Check for streak risks
        val streakData = notificationRepository.getUserStreakData(userId)
        streakData?.let { streaks ->
            streaks.filter { it.isAtRisk() }.forEach { streak ->
                notifications.add(
                    ContextualNotification(
                        id = generateNotificationId(),
                        type = ContextualNotificationType.STREAK_RISK,
                        title = "Don't Break Your Streak! 🔥",
                        message = "Your ${streak.name} streak is at ${streak.currentCount} days. Keep it going!",
                        priority = NotificationPriority.HIGH,
                        triggerCondition = SpendingTrigger(
                            type = TriggerType.STREAK_AT_RISK,
                            threshold = 0.0,
                            category = null,
                            timeWindow = Duration.parse("PT24H")
                        ),
                        scheduledTime = null,
                        expiresAt = Clock.System.now().plus(Duration.parse("PT12H"))
                    )
                )
            }
        }
        
        return notifications
    }

    override suspend fun generateLocationBasedNotifications(
        userId: String,
        location: UserLocation?
    ): List<LocationBasedNotification> {
        if (location == null) return emptyList()
        
        val notifications = mutableListOf<LocationBasedNotification>()
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        val locationHistory = behaviorData?.locationData ?: emptyList()
        
        // Find frequently visited locations with spending
        val frequentLocations = locationHistory
            .filter { it.spendingAmount != null && it.spendingAmount > 0 }
            .groupBy { locationKey(it.location) }
            .filter { it.value.size >= 3 } // Visited at least 3 times
        
        frequentLocations.forEach { (locationKey, visits) ->
            val avgSpending = visits.mapNotNull { it.spendingAmount }.average()
            val commonCategory = visits.mapNotNull { it.category }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key
            
            val distance = calculateDistance(location, visits.first().location)
            
            if (distance <= 500.0) { // Within 500 meters
                notifications.add(
                    LocationBasedNotification(
                        id = generateNotificationId(),
                        title = "Spending Insight",
                        message = "You typically spend ${formatCurrency(avgSpending)} here on $commonCategory. Budget accordingly!",
                        location = visits.first().location,
                        radius = 500.0,
                        relevantInsight = "Historical spending pattern at this location",
                        actionable = true
                    )
                )
            }
        }
        
        return notifications
    }

    override suspend fun trackNotificationEffectiveness(
        notificationId: String,
        interaction: NotificationInteraction
    ) {
        // Store interaction data
        notificationRepository.saveNotificationInteraction(interaction)
        
        // Update effectiveness metrics
        val notification = notificationRepository.getNotificationById(notificationId)
        if (notification != null) {
            val effectiveness = calculateNotificationEffectiveness(notificationId)
            notificationRepository.updateNotificationEffectiveness(notificationId, effectiveness)
        }
        
        // Update user behavior data
        val userId = interaction.userId
        val currentBehavior = notificationRepository.getUserBehaviorData(userId)
        if (currentBehavior != null) {
            val updatedInteractions = currentBehavior.notificationInteractions + interaction
            val updatedBehavior = currentBehavior.copy(
                notificationInteractions = updatedInteractions,
                engagementScore = calculateEngagementScore(updatedInteractions)
            )
            notificationRepository.saveUserBehaviorData(updatedBehavior)
        }
    }

    override suspend fun getPersonalizedSchedule(userId: String): NotificationSchedule {
        val optimalTiming = analyzeOptimalNotificationTiming(userId)
        val frequencySettings = calculateAdaptiveFrequency(userId)
        val contextualNotifications = generateContextualNotifications(userId)
        
        val scheduledNotifications = mutableListOf<ScheduledNotification>()
        val now = Clock.System.now()
        
        // Schedule contextual notifications at optimal times
        contextualNotifications.forEach { contextual ->
            val nextOptimalTime = findNextOptimalTime(optimalTiming, now)
            scheduledNotifications.add(
                ScheduledNotification(
                    id = contextual.id,
                    type = NotificationType.CONTEXTUAL,
                    scheduledTime = nextOptimalTime,
                    content = NotificationContent(
                        title = contextual.title,
                        message = contextual.message,
                        data = mapOf("type" to contextual.type.name)
                    ),
                    priority = contextual.priority
                )
            )
        }
        
        return NotificationSchedule(
            userId = userId,
            scheduledNotifications = scheduledNotifications,
            nextOptimalTime = findNextOptimalTime(optimalTiming, now),
            currentFrequencySettings = frequencySettings
        )
    }

    override suspend fun updateUserBehaviorData(userId: String, behaviorData: UserBehaviorData) {
        notificationRepository.saveUserBehaviorData(behaviorData)
    }

    private fun generateNotificationId(): String {
        return "notif_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }

    private fun formatCurrency(amount: Double): String {
        return "$%.2f CAD".format(amount)
    }

    private fun locationKey(location: UserLocation): String {
        // Round to ~100m precision for grouping nearby locations
        val lat = (location.latitude * 1000).roundToInt() / 1000.0
        val lng = (location.longitude * 1000).roundToInt() / 1000.0
        return "${lat}_${lng}"
    }

    private fun calculateDistance(loc1: UserLocation, loc2: UserLocation): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val lat1Rad = Math.toRadians(loc1.latitude)
        val lat2Rad = Math.toRadians(loc2.latitude)
        val deltaLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val deltaLng = Math.toRadians(loc2.longitude - loc1.longitude)
        
        val a = sin(deltaLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }

    private suspend fun calculateNotificationEffectiveness(notificationId: String): Float {
        val interactions = notificationRepository.getNotificationInteractions(notificationId)
        if (interactions.isEmpty()) return 0.0f
        
        val positiveInteractions = interactions.count { 
            it.interactionType in listOf(InteractionType.OPENED, InteractionType.ACTION_TAKEN)
        }
        
        return positiveInteractions.toFloat() / interactions.size
    }

    private fun calculateEngagementScore(interactions: List<NotificationInteraction>): Float {
        if (interactions.isEmpty()) return 0.5f
        
        val recentInteractions = interactions.filter { 
            it.timestamp > Clock.System.now().minus(Duration.parse("P30D"))
        }
        
        if (recentInteractions.isEmpty()) return 0.3f
        
        val positiveScore = recentInteractions.count { 
            it.interactionType in listOf(InteractionType.OPENED, InteractionType.ACTION_TAKEN)
        }.toFloat()
        
        val negativeScore = recentInteractions.count { 
            it.interactionType in listOf(InteractionType.DISMISSED, InteractionType.IGNORED)
        }.toFloat()
        
        val totalScore = positiveScore - (negativeScore * 0.5f)
        val maxPossibleScore = recentInteractions.size.toFloat()
        
        return (totalScore / maxPossibleScore).coerceIn(0.0f, 1.0f)
    }

    private fun findNextOptimalTime(timing: OptimalTimingResult, after: Instant): Instant {
        val timeZone = timing.timeZone
        val currentDateTime = after.toLocalDateTime(timeZone)
        
        // Find next preferred hour today or tomorrow
        val preferredHours = timing.preferredHours.sorted()
        val nextHourToday = preferredHours.firstOrNull { it > currentDateTime.hour }
        
        return if (nextHourToday != null) {
            // Schedule for later today
            currentDateTime.date.atTime(nextHourToday, 0).toInstant(timeZone)
        } else {
            // Schedule for tomorrow's first preferred hour
            val tomorrow = currentDateTime.date.plus(1, DateTimeUnit.DAY)
            tomorrow.atTime(preferredHours.first(), 0).toInstant(timeZone)
        }
    }
}

// Extension function for streak risk detection
private fun StreakData.isAtRisk(): Boolean {
    val hoursSinceLastActivity = Duration.between(this.lastActivityTime, Clock.System.now()).inWholeHours
    return hoursSinceLastActivity > 20 // At risk if no activity for 20+ hours
}

// Data classes for streak management
data class StreakData(
    val name: String,
    val currentCount: Int,
    val lastActivityTime: Instant
)