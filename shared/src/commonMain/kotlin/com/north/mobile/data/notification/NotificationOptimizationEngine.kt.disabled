package com.north.mobile.data.notification

import com.north.mobile.data.repository.NotificationRepository
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Engine responsible for optimizing notification delivery using machine learning techniques
 * and A/B testing to maximize user engagement while minimizing notification fatigue.
 */
interface NotificationOptimizationEngine {
    
    /**
     * Optimizes notification timing based on historical performance
     */
    suspend fun optimizeNotificationTiming(
        userId: String,
        notificationType: ContextualNotificationType
    ): OptimizedTiming
    
    /**
     * Determines the best notification content variant
     */
    suspend fun optimizeNotificationContent(
        userId: String,
        baseNotification: ContextualNotification
    ): OptimizedNotification
    
    /**
     * Calculates notification fatigue risk
     */
    suspend fun calculateFatigueRisk(userId: String): FatigueRisk
    
    /**
     * Runs A/B tests on notification variants
     */
    suspend fun runNotificationABTest(
        userId: String,
        variants: List<NotificationVariant>
    ): ABTestResult
    
    /**
     * Updates optimization models based on user feedback
     */
    suspend fun updateOptimizationModels(
        userId: String,
        feedback: NotificationFeedback
    )
    
    /**
     * Gets personalized notification recommendations
     */
    suspend fun getPersonalizedRecommendations(userId: String): List<PersonalizedRecommendation>
}

class NotificationOptimizationEngineImpl(
    private val notificationRepository: NotificationRepository,
    private val behaviorTracker: UserBehaviorTracker
) : NotificationOptimizationEngine {

    override suspend fun optimizeNotificationTiming(
        userId: String,
        notificationType: ContextualNotificationType
    ): OptimizedTiming {
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val historicalPerformance = getHistoricalPerformance(userId, notificationType)
        
        // Calculate optimal timing based on multiple factors
        val userPeakHours = behaviorAnalysis.appUsagePattern.peakHours
        val engagementTimes = behaviorAnalysis.notificationEngagement.preferredInteractionTimes
        val historicalBestTimes = historicalPerformance.bestPerformingHours
        
        // Weighted scoring system
        val hourScores = (0..23).associateWith { hour ->
            var score = 0.0
            
            // User peak hours (40% weight)
            if (hour in userPeakHours) {
                score += 0.4 * (1.0 - (userPeakHours.indexOf(hour) * 0.2))
            }
            
            // Historical engagement times (35% weight)
            if (hour in engagementTimes) {
                score += 0.35 * (1.0 - (engagementTimes.indexOf(hour) * 0.15))
            }
            
            // Historical performance for this notification type (25% weight)
            if (hour in historicalBestTimes) {
                score += 0.25 * historicalPerformance.getHourPerformance(hour)
            }
            
            score
        }
        
        val optimalHours = hourScores.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        val confidence = calculateTimingConfidence(behaviorAnalysis, historicalPerformance)
        
        return OptimizedTiming(
            optimalHours = optimalHours,
            confidence = confidence,
            reasoning = buildTimingReasoning(userPeakHours, engagementTimes, historicalBestTimes),
            fallbackHours = listOf(9, 12, 18) // Safe defaults
        )
    }

    override suspend fun optimizeNotificationContent(
        userId: String,
        baseNotification: ContextualNotification
    ): OptimizedNotification {
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val userPreferences = inferUserPreferences(behaviorAnalysis)
        
        // Generate content variants based on user preferences
        val variants = generateContentVariants(baseNotification, userPreferences)
        
        // Score variants based on predicted performance
        val scoredVariants = variants.map { variant ->
            val score = calculateContentScore(variant, userPreferences, behaviorAnalysis)
            ScoredVariant(variant, score)
        }.sortedByDescending { it.score }
        
        val bestVariant = scoredVariants.firstOrNull()?.variant ?: baseNotification
        
        return OptimizedNotification(
            originalNotification = baseNotification,
            optimizedContent = bestVariant,
            confidence = calculateContentConfidence(scoredVariants),
            alternatives = scoredVariants.drop(1).take(2).map { it.variant },
            optimizationReason = buildContentReasoning(userPreferences)
        )
    }

    override suspend fun calculateFatigueRisk(userId: String): FatigueRisk {
        val recentInteractions = notificationRepository.getNotificationHistory(userId, 50)
        val behaviorData = notificationRepository.getUserBehaviorData(userId)
        
        if (recentInteractions.isEmpty()) {
            return FatigueRisk(
                riskLevel = FatigueLevel.LOW,
                score = 0.2f,
                factors = listOf("New user - no interaction history"),
                recommendations = listOf("Start with moderate notification frequency")
            )
        }
        
        // Calculate fatigue indicators
        val recentDismissalRate = calculateRecentDismissalRate(recentInteractions)
        val notificationFrequency = calculateRecentFrequency(recentInteractions)
        val engagementTrend = calculateEngagementTrend(recentInteractions)
        val responseTimeIncrease = calculateResponseTimeIncrease(recentInteractions)
        
        // Weighted fatigue score
        val fatigueScore = (
            recentDismissalRate * 0.3f +
            (notificationFrequency / 10.0f).coerceAtMost(1.0f) * 0.25f +
            (1.0f - engagementTrend) * 0.25f +
            responseTimeIncrease * 0.2f
        ).coerceIn(0.0f, 1.0f)
        
        val riskLevel = when {
            fatigueScore > 0.7f -> FatigueLevel.HIGH
            fatigueScore > 0.4f -> FatigueLevel.MEDIUM
            else -> FatigueLevel.LOW
        }
        
        val factors = buildFatigueFactors(recentDismissalRate, notificationFrequency, engagementTrend)
        val recommendations = buildFatigueRecommendations(riskLevel, factors)
        
        return FatigueRisk(
            riskLevel = riskLevel,
            score = fatigueScore,
            factors = factors,
            recommendations = recommendations
        )
    }

    override suspend fun runNotificationABTest(
        userId: String,
        variants: List<NotificationVariant>
    ): ABTestResult {
        if (variants.size < 2) {
            throw IllegalArgumentException("A/B test requires at least 2 variants")
        }
        
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val userSegment = determineUserSegment(behaviorAnalysis)
        
        // Select variant based on user segment and randomization
        val selectedVariant = selectVariantForUser(userId, variants, userSegment)
        
        // Predict performance for each variant
        val variantPredictions = variants.map { variant ->
            val predictedPerformance = predictVariantPerformance(variant, behaviorAnalysis)
            VariantPrediction(variant, predictedPerformance)
        }
        
        return ABTestResult(
            testId = generateTestId(),
            userId = userId,
            selectedVariant = selectedVariant,
            allVariants = variants,
            predictions = variantPredictions,
            userSegment = userSegment,
            confidence = calculateABTestConfidence(variantPredictions)
        )
    }

    override suspend fun updateOptimizationModels(
        userId: String,
        feedback: NotificationFeedback
    ) {
        // Update user behavior data with feedback
        val interaction = NotificationInteraction(
            notificationId = feedback.notificationId,
            userId = userId,
            interactionType = feedback.interactionType,
            timestamp = feedback.timestamp,
            responseTime = feedback.responseTime
        )
        
        behaviorTracker.recordNotificationInteraction(
            userId = userId,
            notificationId = feedback.notificationId,
            interactionType = feedback.interactionType,
            responseTime = feedback.responseTime
        )
        
        // Update effectiveness tracking
        notificationRepository.updateNotificationEffectiveness(
            feedback.notificationId,
            calculateEffectivenessFromFeedback(feedback)
        )
        
        // Update optimization parameters based on feedback
        updatePersonalizationParameters(userId, feedback)
    }

    override suspend fun getPersonalizedRecommendations(userId: String): List<PersonalizedRecommendation> {
        val behaviorAnalysis = behaviorTracker.analyzeBehaviorPatterns(userId)
        val fatigueRisk = calculateFatigueRisk(userId)
        val recommendations = mutableListOf<PersonalizedRecommendation>()
        
        // Timing recommendations
        if (behaviorAnalysis.appUsagePattern.usageConsistency > 0.7f) {
            recommendations.add(
                PersonalizedRecommendation(
                    type = RecommendationType.TIMING_OPTIMIZATION,
                    title = "Optimize Notification Timing",
                    description = "Your app usage is consistent. We can optimize notification timing for better engagement.",
                    impact = ImpactLevel.MEDIUM,
                    confidence = 0.8f
                )
            )
        }
        
        // Frequency recommendations
        if (fatigueRisk.riskLevel == FatigueLevel.HIGH) {
            recommendations.add(
                PersonalizedRecommendation(
                    type = RecommendationType.FREQUENCY_REDUCTION,
                    title = "Reduce Notification Frequency",
                    description = "You might be experiencing notification fatigue. Consider reducing frequency.",
                    impact = ImpactLevel.HIGH,
                    confidence = 0.9f
                )
            )
        }
        
        // Content recommendations
        if (behaviorAnalysis.notificationEngagement.actionRate < 0.3f) {
            recommendations.add(
                PersonalizedRecommendation(
                    type = RecommendationType.CONTENT_OPTIMIZATION,
                    title = "Improve Notification Content",
                    description = "Your notification engagement is low. Let's try more personalized content.",
                    impact = ImpactLevel.MEDIUM,
                    confidence = 0.7f
                )
            )
        }
        
        return recommendations
    }

    // Helper methods
    private suspend fun getHistoricalPerformance(
        userId: String,
        notificationType: ContextualNotificationType
    ): HistoricalPerformance {
        val history = notificationRepository.getNotificationHistory(userId, 100)
        val typeSpecificHistory = history.filter { 
            it.notificationContent.data["type"] == notificationType.name 
        }
        
        if (typeSpecificHistory.isEmpty()) {
            return HistoricalPerformance.empty()
        }
        
        val hourPerformance = typeSpecificHistory
            .groupBy { it.deliveredAt.toLocalDateTime(TimeZone.currentSystemDefault()).hour }
            .mapValues { (_, notifications) ->
                notifications.map { it.effectiveness ?: 0.0f }.average().toFloat()
            }
        
        val bestHours = hourPerformance.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        return HistoricalPerformance(
            bestPerformingHours = bestHours,
            hourPerformanceMap = hourPerformance,
            totalNotifications = typeSpecificHistory.size,
            averageEffectiveness = typeSpecificHistory.mapNotNull { it.effectiveness }.average().toFloat()
        )
    }

    private fun calculateTimingConfidence(
        behaviorAnalysis: BehaviorAnalysis,
        historicalPerformance: HistoricalPerformance
    ): Float {
        val dataPoints = behaviorAnalysis.appUsagePattern.averageSessionsPerDay * 30 // Approximate data points
        val consistency = behaviorAnalysis.appUsagePattern.usageConsistency
        val historicalData = historicalPerformance.totalNotifications.toFloat() / 100.0f
        
        return ((dataPoints / 100.0f).coerceAtMost(1.0f) * 0.4f +
                consistency * 0.4f +
                historicalData.coerceAtMost(1.0f) * 0.2f).coerceIn(0.0f, 1.0f)
    }

    private fun buildTimingReasoning(
        userPeakHours: List<Int>,
        engagementTimes: List<Int>,
        historicalBestTimes: List<Int>
    ): String {
        val reasons = mutableListOf<String>()
        
        if (userPeakHours.isNotEmpty()) {
            reasons.add("Based on your app usage patterns (peak hours: ${userPeakHours.joinToString()})")
        }
        
        if (engagementTimes.isNotEmpty()) {
            reasons.add("Historical engagement times: ${engagementTimes.joinToString()}")
        }
        
        if (historicalBestTimes.isNotEmpty()) {
            reasons.add("Best performing times for similar notifications: ${historicalBestTimes.joinToString()}")
        }
        
        return reasons.joinToString("; ")
    }

    private fun inferUserPreferences(behaviorAnalysis: BehaviorAnalysis): UserPreferences {
        return UserPreferences(
            prefersShortMessages = behaviorAnalysis.notificationEngagement.averageResponseTime < Duration.parse("PT30S"),
            prefersActionableContent = behaviorAnalysis.notificationEngagement.actionRate > 0.5f,
            prefersPersonalizedContent = behaviorAnalysis.overallEngagementScore > 0.6f,
            preferredTone = if (behaviorAnalysis.overallEngagementScore > 0.7f) "friendly" else "professional",
            spendingFocusAreas = behaviorAnalysis.spendingInsights.topSpendingCategories.keys.take(3).toList()
        )
    }

    private fun generateContentVariants(
        baseNotification: ContextualNotification,
        preferences: UserPreferences
    ): List<ContextualNotification> {
        val variants = mutableListOf<ContextualNotification>()
        
        // Original
        variants.add(baseNotification)
        
        // Short version
        if (preferences.prefersShortMessages) {
            variants.add(
                baseNotification.copy(
                    title = shortenText(baseNotification.title),
                    message = shortenText(baseNotification.message)
                )
            )
        }
        
        // Personalized version
        if (preferences.prefersPersonalizedContent) {
            variants.add(
                baseNotification.copy(
                    title = personalizeText(baseNotification.title, preferences),
                    message = personalizeText(baseNotification.message, preferences)
                )
            )
        }
        
        // Action-focused version
        if (preferences.prefersActionableContent) {
            variants.add(
                baseNotification.copy(
                    title = makeActionable(baseNotification.title),
                    message = makeActionable(baseNotification.message)
                )
            )
        }
        
        return variants
    }

    private fun calculateContentScore(
        variant: ContextualNotification,
        preferences: UserPreferences,
        behaviorAnalysis: BehaviorAnalysis
    ): Float {
        var score = 0.5f // Base score
        
        // Length preference
        if (preferences.prefersShortMessages && variant.message.length < 100) {
            score += 0.2f
        }
        
        // Personalization preference
        if (preferences.prefersPersonalizedContent && containsPersonalization(variant)) {
            score += 0.2f
        }
        
        // Action preference
        if (preferences.prefersActionableContent && containsActionableLanguage(variant)) {
            score += 0.2f
        }
        
        // Relevance to spending focus areas
        val relevanceScore = calculateRelevanceScore(variant, preferences.spendingFocusAreas)
        score += relevanceScore * 0.1f
        
        return score.coerceIn(0.0f, 1.0f)
    }

    private fun calculateContentConfidence(scoredVariants: List<ScoredVariant>): Float {
        if (scoredVariants.size < 2) return 0.5f
        
        val topScore = scoredVariants.first().score
        val secondScore = scoredVariants.getOrNull(1)?.score ?: 0.0f
        
        return (topScore - secondScore).coerceIn(0.0f, 1.0f)
    }

    private fun buildContentReasoning(preferences: UserPreferences): String {
        val reasons = mutableListOf<String>()
        
        if (preferences.prefersShortMessages) reasons.add("prefers concise messages")
        if (preferences.prefersActionableContent) reasons.add("responds well to actionable content")
        if (preferences.prefersPersonalizedContent) reasons.add("engages more with personalized content")
        
        return "Optimized based on user preferences: ${reasons.joinToString(", ")}"
    }

    // Additional helper methods would continue here...
    // For brevity, I'll include key data classes and continue with the implementation

    private fun calculateRecentDismissalRate(history: List<NotificationDeliveryResult>): Float {
        val recent = history.take(20)
        if (recent.isEmpty()) return 0.0f
        
        val dismissals = recent.count { it.interactionType == "dismissed" }
        return dismissals.toFloat() / recent.size
    }

    private fun calculateRecentFrequency(history: List<NotificationDeliveryResult>): Float {
        if (history.isEmpty()) return 0.0f
        
        val recent = history.take(50)
        val timeSpan = Duration.between(recent.last().deliveredAt, recent.first().deliveredAt)
        val days = timeSpan.inWholeDays.coerceAtLeast(1)
        
        return recent.size.toFloat() / days
    }

    private fun calculateEngagementTrend(history: List<NotificationDeliveryResult>): Float {
        if (history.size < 10) return 0.5f
        
        val recent = history.take(10)
        val older = history.drop(10).take(10)
        
        val recentEngagement = recent.count { it.interactionType in listOf("opened", "action_taken") }.toFloat() / recent.size
        val olderEngagement = if (older.isNotEmpty()) {
            older.count { it.interactionType in listOf("opened", "action_taken") }.toFloat() / older.size
        } else {
            0.5f
        }
        
        return (recentEngagement / olderEngagement.coerceAtLeast(0.1f)).coerceIn(0.0f, 2.0f) / 2.0f
    }

    private fun calculateResponseTimeIncrease(history: List<NotificationDeliveryResult>): Float {
        // Simplified implementation
        return 0.0f // Would calculate actual response time trends
    }

    private fun buildFatigueFactors(
        dismissalRate: Float,
        frequency: Float,
        engagementTrend: Float
    ): List<String> {
        val factors = mutableListOf<String>()
        
        if (dismissalRate > 0.5f) factors.add("High dismissal rate (${(dismissalRate * 100).toInt()}%)")
        if (frequency > 5.0f) factors.add("High notification frequency (${frequency.toInt()} per day)")
        if (engagementTrend < 0.5f) factors.add("Declining engagement trend")
        
        return factors
    }

    private fun buildFatigueRecommendations(riskLevel: FatigueLevel, factors: List<String>): List<String> {
        return when (riskLevel) {
            FatigueLevel.HIGH -> listOf(
                "Reduce notification frequency by 50%",
                "Focus on high-priority notifications only",
                "Consider a notification break period"
            )
            FatigueLevel.MEDIUM -> listOf(
                "Reduce notification frequency by 25%",
                "Improve notification relevance",
                "Test different content formats"
            )
            FatigueLevel.LOW -> listOf(
                "Continue current strategy",
                "Monitor engagement trends",
                "Consider slight frequency increase if engagement is high"
            )
        }
    }

    private fun determineUserSegment(behaviorAnalysis: BehaviorAnalysis): String {
        return when {
            behaviorAnalysis.overallEngagementScore > 0.8f -> "HIGH_ENGAGEMENT"
            behaviorAnalysis.overallEngagementScore > 0.5f -> "MEDIUM_ENGAGEMENT"
            else -> "LOW_ENGAGEMENT"
        }
    }

    private fun selectVariantForUser(
        userId: String,
        variants: List<NotificationVariant>,
        userSegment: String
    ): NotificationVariant {
        // Simple hash-based selection for consistent A/B testing
        val hash = userId.hashCode()
        val index = abs(hash) % variants.size
        return variants[index]
    }

    private fun predictVariantPerformance(
        variant: NotificationVariant,
        behaviorAnalysis: BehaviorAnalysis
    ): Float {
        // Simplified prediction model
        var score = 0.5f
        
        if (variant.isPersonalized) score += 0.2f
        if (variant.hasActionableContent) score += 0.15f
        if (variant.isShort && behaviorAnalysis.notificationEngagement.averageResponseTime < Duration.parse("PT30S")) {
            score += 0.1f
        }
        
        return score.coerceIn(0.0f, 1.0f)
    }

    private fun calculateABTestConfidence(predictions: List<VariantPrediction>): Float {
        if (predictions.size < 2) return 0.5f
        
        val scores = predictions.map { it.predictedPerformance }.sorted()
        val topScore = scores.last()
        val secondScore = scores.getOrNull(scores.size - 2) ?: 0.0f
        
        return (topScore - secondScore).coerceIn(0.0f, 1.0f)
    }

    private fun generateTestId(): String {
        return "abtest_${Clock.System.now().toEpochMilliseconds()}"
    }

    private fun calculateEffectivenessFromFeedback(feedback: NotificationFeedback): Float {
        return when (feedback.interactionType) {
            InteractionType.ACTION_TAKEN -> 1.0f
            InteractionType.OPENED -> 0.7f
            InteractionType.SNOOZED -> 0.3f
            InteractionType.DISMISSED -> 0.1f
            InteractionType.IGNORED -> 0.0f
        }
    }

    private suspend fun updatePersonalizationParameters(userId: String, feedback: NotificationFeedback) {
        // Update user-specific optimization parameters based on feedback
        // This would involve updating ML model weights or preference scores
    }

    // Text processing helper methods
    private fun shortenText(text: String): String {
        return if (text.length > 50) {
            text.take(47) + "..."
        } else {
            text
        }
    }

    private fun personalizeText(text: String, preferences: UserPreferences): String {
        // Add personalization based on user preferences
        return text // Simplified implementation
    }

    private fun makeActionable(text: String): String {
        // Make text more actionable
        return text // Simplified implementation
    }

    private fun containsPersonalization(notification: ContextualNotification): Boolean {
        return notification.message.contains("you", ignoreCase = true) ||
               notification.message.contains("your", ignoreCase = true)
    }

    private fun containsActionableLanguage(notification: ContextualNotification): Boolean {
        val actionWords = listOf("check", "review", "update", "save", "optimize", "improve")
        return actionWords.any { notification.message.contains(it, ignoreCase = true) }
    }

    private fun calculateRelevanceScore(notification: ContextualNotification, focusAreas: List<String>): Float {
        val relevantAreas = focusAreas.count { area ->
            notification.message.contains(area, ignoreCase = true)
        }
        return if (focusAreas.isNotEmpty()) {
            relevantAreas.toFloat() / focusAreas.size
        } else {
            0.0f
        }
    }
}

// Data classes for optimization
data class OptimizedTiming(
    val optimalHours: List<Int>,
    val confidence: Float,
    val reasoning: String,
    val fallbackHours: List<Int>
)

data class OptimizedNotification(
    val originalNotification: ContextualNotification,
    val optimizedContent: ContextualNotification,
    val confidence: Float,
    val alternatives: List<ContextualNotification>,
    val optimizationReason: String
)

data class FatigueRisk(
    val riskLevel: FatigueLevel,
    val score: Float,
    val factors: List<String>,
    val recommendations: List<String>
)

data class ABTestResult(
    val testId: String,
    val userId: String,
    val selectedVariant: NotificationVariant,
    val allVariants: List<NotificationVariant>,
    val predictions: List<VariantPrediction>,
    val userSegment: String,
    val confidence: Float
)

data class NotificationFeedback(
    val notificationId: String,
    val interactionType: InteractionType,
    val timestamp: Instant,
    val responseTime: Duration?
)

data class PersonalizedRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val impact: ImpactLevel,
    val confidence: Float
)

data class HistoricalPerformance(
    val bestPerformingHours: List<Int>,
    val hourPerformanceMap: Map<Int, Float>,
    val totalNotifications: Int,
    val averageEffectiveness: Float
) {
    fun getHourPerformance(hour: Int): Float = hourPerformanceMap[hour] ?: 0.0f
    
    companion object {
        fun empty() = HistoricalPerformance(
            bestPerformingHours = emptyList(),
            hourPerformanceMap = emptyMap(),
            totalNotifications = 0,
            averageEffectiveness = 0.0f
        )
    }
}

data class UserPreferences(
    val prefersShortMessages: Boolean,
    val prefersActionableContent: Boolean,
    val prefersPersonalizedContent: Boolean,
    val preferredTone: String,
    val spendingFocusAreas: List<String>
)

data class ScoredVariant(
    val variant: ContextualNotification,
    val score: Float
)

data class NotificationVariant(
    val id: String,
    val title: String,
    val message: String,
    val isPersonalized: Boolean,
    val hasActionableContent: Boolean,
    val isShort: Boolean
)

data class VariantPrediction(
    val variant: NotificationVariant,
    val predictedPerformance: Float
)

enum class FatigueLevel {
    LOW, MEDIUM, HIGH
}

enum class RecommendationType {
    TIMING_OPTIMIZATION,
    FREQUENCY_REDUCTION,
    CONTENT_OPTIMIZATION,
    PERSONALIZATION_IMPROVEMENT
}

enum class ImpactLevel {
    LOW, MEDIUM, HIGH
}