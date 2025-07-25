package com.north.mobile.data.gamification

import com.north.mobile.data.repository.GamificationRepository
import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Implementation of the StreakTrackingService interface.
 */
class StreakTrackingServiceImpl(
    private val gamificationRepository: GamificationRepository,
    private val celebrationManager: CelebrationManager
) : StreakTrackingService {
    
    companion object {
        // Risk assessment thresholds (in days)
        private const val LOW_RISK_THRESHOLD = 1
        private const val MEDIUM_RISK_THRESHOLD = 2
        private const val HIGH_RISK_THRESHOLD = 3
        
        // Streak milestone thresholds for celebrations
        private val CELEBRATION_MILESTONES = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        
        // Recovery window (days to complete recovery)
        private const val RECOVERY_WINDOW_DAYS = 3
        
        // Micro-win point values by difficulty
        private val MICRO_WIN_POINTS = mapOf(
            MicroWinDifficulty.EASY to 5,
            MicroWinDifficulty.MEDIUM to 10,
            MicroWinDifficulty.HARD to 20
        )
    }
    
    override suspend fun updateStreakWithRiskAssessment(
        userId: String,
        streakType: StreakType,
        actionDate: LocalDate
    ): Result<StreakUpdateResult> {
        return try {
            val existingStreak = gamificationRepository.getStreak(userId, streakType)
            val now = Clock.System.now()
            
            val updatedStreak = if (existingStreak != null) {
                processExistingStreak(existingStreak, actionDate)
            } else {
                createNewStreak(userId, streakType, actionDate)
            }
            
            // Assess risk level
            val riskLevel = assessStreakRisk(updatedStreak, actionDate)
            val streakWithRisk = updatedStreak.copy(riskLevel = riskLevel)
            
            // Save updated streak
            gamificationRepository.updateStreak(streakWithRisk, userId)
            
            // Check for celebration
            val celebrationEvent = if (shouldCelebrateMilestone(streakWithRisk)) {
                celebrateStreakMilestone(userId, streakWithRisk).getOrNull()
            } else null
            
            // Schedule reminder if needed
            val reminderScheduled = if (riskLevel != StreakRiskLevel.SAFE) {
                scheduleRiskReminder(userId, streakWithRisk)
            } else null
            
            val wasExtended = existingStreak?.currentCount ?: 0 < streakWithRisk.currentCount
            val wasBroken = existingStreak != null && streakWithRisk.currentCount == 1 && existingStreak.currentCount > 1
            
            Result.success(
                StreakUpdateResult(
                    streak = streakWithRisk,
                    wasExtended = wasExtended,
                    wasBroken = wasBroken,
                    newRiskLevel = riskLevel,
                    celebrationEvent = celebrationEvent,
                    reminderScheduled = reminderScheduled
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeStreakRisks(userId: String): Result<List<StreakRiskAnalysis>> {
        return try {
            val activeStreaks = gamificationRepository.getActiveStreaks(userId)
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            
            val riskAnalyses = activeStreaks.map { streak ->
                val daysSinceLastActivity = today.toEpochDays() - streak.lastActivityDate.toEpochDays()
                val riskLevel = assessStreakRisk(streak, today)
                val urgencyScore = calculateUrgencyScore(streak, daysSinceLastActivity)
                
                StreakRiskAnalysis(
                    streak = streak,
                    riskLevel = riskLevel,
                    daysSinceLastActivity = daysSinceLastActivity.toInt(),
                    recommendedActions = getRecommendedActions(streak.type),
                    reminderMessage = generateReminderMessage(streak, riskLevel),
                    urgencyScore = urgencyScore
                )
            }.sortedByDescending { it.urgencyScore }
            
            Result.success(riskAnalyses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generatePersonalizedMicroWins(userId: String, limit: Int): Result<List<MicroWinOpportunity>> {
        return try {
            val profile = gamificationRepository.getGamificationProfile(userId)
            val activeStreaks = gamificationRepository.getActiveStreaks(userId)
            val recentActions = gamificationRepository.getPointsHistory(userId, 20)
            
            val microWins = mutableListOf<MicroWinOpportunity>()
            
            // Generate streak-maintenance micro-wins
            microWins.addAll(generateStreakMaintenanceMicroWins(activeStreaks))
            
            // Generate habit-building micro-wins
            microWins.addAll(generateHabitBuildingMicroWins(recentActions))
            
            // Generate exploration micro-wins
            microWins.addAll(generateExplorationMicroWins(profile))
            
            // Generate recovery micro-wins if needed
            val recoveries = gamificationRepository.getActiveRecoveries(userId)
            microWins.addAll(generateRecoveryMicroWins(recoveries))
            
            // Personalize and prioritize
            val personalizedMicroWins = microWins
                .distinctBy { it.actionType }
                .sortedByDescending { calculateMicroWinPriority(it, profile, activeStreaks) }
                .take(limit)
            
            Result.success(personalizedMicroWins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun detectAndAwardMicroWins(
        userId: String,
        action: UserAction,
        contextData: Map<String, String>
    ): Result<List<MicroWinResult>> {
        return try {
            val microWinResults = mutableListOf<MicroWinResult>()
            
            // Check for action-specific micro-wins
            val actionMicroWins = detectActionMicroWins(action, contextData)
            
            for (microWin in actionMicroWins) {
                val pointsAwarded = MICRO_WIN_POINTS[microWin.difficulty] ?: 5
                
                // Create celebration event
                val celebrationEvent = celebrationManager.celebrateMicroWin(
                    microWin.title,
                    pointsAwarded
                )
                
                // Update affected streaks
                val affectedStreaks = updateStreaksForAction(userId, action)
                
                microWinResults.add(
                    MicroWinResult(
                        microWin = microWin,
                        pointsAwarded = pointsAwarded,
                        celebrationEvent = celebrationEvent,
                        streaksAffected = affectedStreaks
                    )
                )
            }
            
            Result.success(microWinResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun initiateStreakRecovery(userId: String, brokenStreakId: String): Result<StreakRecovery> {
        return try {
            val brokenStreak = gamificationRepository.getStreakById(brokenStreakId)
                ?: return Result.failure(IllegalArgumentException("Streak not found"))
            
            val recovery = StreakRecovery(
                id = generateId(),
                userId = userId,
                originalStreakId = brokenStreakId,
                streakType = brokenStreak.type,
                brokenAt = Clock.System.now(),
                recoveryStarted = Clock.System.now(),
                originalCount = brokenStreak.bestCount,
                recoveryActions = emptyList()
            )
            
            gamificationRepository.createStreakRecovery(recovery)
            
            // Schedule recovery reminders
            scheduleRecoveryReminders(userId, recovery)
            
            Result.success(recovery)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun processRecoveryAction(
        userId: String,
        recoveryId: String,
        action: UserAction
    ): Result<RecoveryActionResult> {
        return try {
            val recovery = gamificationRepository.getStreakRecovery(recoveryId)
                ?: return Result.failure(IllegalArgumentException("Recovery not found"))
            
            val recoveryAction = RecoveryAction(
                id = generateId(),
                actionType = action,
                completedAt = Clock.System.now(),
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.MEDIUM] ?: 10,
                description = "Recovery action: ${getActionDescription(action)}"
            )
            
            val updatedRecovery = recovery.copy(
                recoveryActions = recovery.recoveryActions + recoveryAction
            )
            
            // Check if recovery is complete (3 actions within recovery window)
            val isComplete = updatedRecovery.recoveryActions.size >= 3
            val newStreak = if (isComplete) {
                // Start new streak
                val newStreak = Streak(
                    id = generateId(),
                    type = recovery.streakType,
                    currentCount = 1,
                    bestCount = recovery.originalCount,
                    lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    isActive = true,
                    riskLevel = StreakRiskLevel.SAFE,
                    recoveryAttempts = recovery.recoveryActions.size
                )
                
                gamificationRepository.updateStreak(newStreak, userId)
                newStreak
            } else null
            
            val finalRecovery = if (isComplete) {
                updatedRecovery.copy(
                    recoveryCompleted = Clock.System.now(),
                    isSuccessful = true
                )
            } else updatedRecovery
            
            gamificationRepository.updateStreakRecovery(finalRecovery)
            
            // Create celebration if recovery is complete
            val celebrationEvent = if (isComplete) {
                celebrationManager.celebrateStreak(recovery.streakType, 1, false)
            } else {
                celebrationManager.celebrateMicroWin("Recovery Progress", recoveryAction.pointsAwarded)
            }
            
            Result.success(
                RecoveryActionResult(
                    recovery = finalRecovery,
                    actionProcessed = recoveryAction,
                    isRecoveryComplete = isComplete,
                    newStreakStarted = newStreak,
                    celebrationEvent = celebrationEvent
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun scheduleStreakReminder(
        userId: String,
        streakId: String,
        reminderType: ReminderType
    ): Result<StreakReminder> {
        return try {
            val streak = gamificationRepository.getStreakById(streakId)
                ?: return Result.failure(IllegalArgumentException("Streak not found"))
            
            val reminder = StreakReminder(
                id = generateId(),
                userId = userId,
                streakId = streakId,
                streakType = streak.type,
                reminderType = reminderType,
                message = generateReminderMessage(streak, streak.riskLevel),
                scheduledFor = calculateReminderTime(reminderType, streak.riskLevel)
            )
            
            gamificationRepository.createStreakReminder(reminder)
            Result.success(reminder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveReminders(userId: String): Result<List<StreakReminder>> {
        return try {
            val reminders = gamificationRepository.getActiveReminders(userId)
            Result.success(reminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun acknowledgeReminder(userId: String, reminderId: String): Result<Unit> {
        return try {
            gamificationRepository.markReminderAsRead(reminderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getStreakStatistics(userId: String): Result<StreakStatistics> {
        return try {
            val activeStreaks = gamificationRepository.getActiveStreaks(userId)
            val allStreaks = gamificationRepository.getAllUserStreaks(userId)
            val recoveries = gamificationRepository.getAllRecoveries(userId)
            
            val longestCurrent = activeStreaks.maxByOrNull { it.currentCount }
            val longestEver = allStreaks.maxByOrNull { it.bestCount }
            val totalStreakDays = allStreaks.sumOf { it.bestCount }
            val averageLength = if (allStreaks.isNotEmpty()) totalStreakDays.toDouble() / allStreaks.size else 0.0
            
            val streaksByType = activeStreaks.groupBy { it.type }
            val riskDistribution = activeStreaks.groupBy { it.riskLevel }.mapValues { it.value.size }
            
            val successfulRecoveries = recoveries.count { it.isSuccessful }
            val recoverySuccessRate = if (recoveries.isNotEmpty()) {
                successfulRecoveries.toDouble() / recoveries.size
            } else 0.0
            
            val weeklyTrend = calculateWeeklyStreakTrend(activeStreaks)
            val monthlyMilestones = calculateMonthlyMilestones(allStreaks)
            
            Result.success(
                StreakStatistics(
                    totalActiveStreaks = activeStreaks.size,
                    longestCurrentStreak = longestCurrent,
                    longestEverStreak = longestEver,
                    totalStreakDays = totalStreakDays,
                    averageStreakLength = averageLength,
                    streaksByType = streaksByType,
                    riskDistribution = riskDistribution,
                    recoverySuccessRate = recoverySuccessRate,
                    weeklyStreakTrend = weeklyTrend,
                    monthlyMilestones = monthlyMilestones
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun celebrateStreakMilestone(userId: String, streak: Streak): Result<CelebrationEvent> {
        return try {
            val isNewRecord = streak.currentCount > streak.bestCount
            val celebrationEvent = celebrationManager.celebrateStreak(
                streak.type,
                streak.currentCount,
                isNewRecord
            )
            Result.success(celebrationEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Private helper methods
    
    private fun processExistingStreak(existingStreak: Streak, actionDate: LocalDate): Streak {
        val daysDifference = actionDate.toEpochDays() - existingStreak.lastActivityDate.toEpochDays()
        
        return when {
            daysDifference == 0L -> existingStreak // Same day, no change
            daysDifference == 1L -> {
                // Consecutive day, increment streak
                val newCount = existingStreak.currentCount + 1
                existingStreak.copy(
                    currentCount = newCount,
                    bestCount = maxOf(existingStreak.bestCount, newCount),
                    lastActivityDate = actionDate,
                    recoveryAttempts = 0 // Reset recovery attempts on successful continuation
                )
            }
            else -> {
                // Streak broken, reset to 1
                existingStreak.copy(
                    currentCount = 1,
                    lastActivityDate = actionDate,
                    riskLevel = StreakRiskLevel.SAFE,
                    recoveryAttempts = existingStreak.recoveryAttempts + 1
                )
            }
        }
    }
    
    private fun createNewStreak(userId: String, streakType: StreakType, actionDate: LocalDate): Streak {
        return Streak(
            id = generateId(),
            type = streakType,
            currentCount = 1,
            bestCount = 1,
            lastActivityDate = actionDate,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE,
            recoveryAttempts = 0
        )
    }
    
    private fun assessStreakRisk(streak: Streak, currentDate: LocalDate): StreakRiskLevel {
        val daysSinceLastActivity = currentDate.toEpochDays() - streak.lastActivityDate.toEpochDays()
        
        return when {
            daysSinceLastActivity <= LOW_RISK_THRESHOLD -> StreakRiskLevel.SAFE
            daysSinceLastActivity <= MEDIUM_RISK_THRESHOLD -> StreakRiskLevel.LOW_RISK
            daysSinceLastActivity <= HIGH_RISK_THRESHOLD -> StreakRiskLevel.MEDIUM_RISK
            daysSinceLastActivity > HIGH_RISK_THRESHOLD -> StreakRiskLevel.HIGH_RISK
            else -> StreakRiskLevel.SAFE
        }
    }
    
    private fun shouldCelebrateMilestone(streak: Streak): Boolean {
        return CELEBRATION_MILESTONES.contains(streak.currentCount)
    }
    
    private suspend fun scheduleRiskReminder(userId: String, streak: Streak): StreakReminder? {
        val reminderType = when (streak.riskLevel) {
            StreakRiskLevel.LOW_RISK -> ReminderType.GENTLE_NUDGE
            StreakRiskLevel.MEDIUM_RISK -> ReminderType.MOTIVATION_BOOST
            StreakRiskLevel.HIGH_RISK -> ReminderType.STREAK_RISK_ALERT
            else -> return null
        }
        
        return scheduleStreakReminder(userId, streak.id, reminderType).getOrNull()
    }
    
    private fun calculateUrgencyScore(streak: Streak, daysSinceLastActivity: Long): Int {
        val baseScore = when (streak.riskLevel) {
            StreakRiskLevel.SAFE -> 1
            StreakRiskLevel.LOW_RISK -> 3
            StreakRiskLevel.MEDIUM_RISK -> 6
            StreakRiskLevel.HIGH_RISK -> 9
            StreakRiskLevel.BROKEN -> 10
        }
        
        // Adjust based on streak length (longer streaks are more urgent to maintain)
        val lengthMultiplier = when {
            streak.currentCount >= 30 -> 2
            streak.currentCount >= 7 -> 1.5
            else -> 1.0
        }
        
        return (baseScore * lengthMultiplier).toInt().coerceIn(1, 10)
    }
    
    private fun getRecommendedActions(streakType: StreakType): List<String> {
        return when (streakType) {
            StreakType.DAILY_CHECK_IN -> listOf(
                "Check your account balance",
                "Review today's transactions",
                "Open the app for a quick look"
            )
            StreakType.UNDER_BUDGET -> listOf(
                "Review your spending for today",
                "Check your budget progress",
                "Consider a small saving instead of a purchase"
            )
            StreakType.GOAL_PROGRESS -> listOf(
                "Make a small contribution to your goal",
                "Review your goal timeline",
                "Update your goal progress"
            )
            StreakType.TRANSACTION_CATEGORIZATION -> listOf(
                "Categorize recent transactions",
                "Review uncategorized expenses",
                "Clean up your transaction history"
            )
            StreakType.SAVINGS_CONTRIBUTION -> listOf(
                "Make a small savings contribution",
                "Transfer money to savings",
                "Set up an automatic transfer"
            )
            else -> listOf("Continue your positive financial habits")
        }
    }
    
    private fun generateReminderMessage(streak: Streak, riskLevel: StreakRiskLevel): String {
        val streakName = getStreakDisplayName(streak.type)
        
        return when (riskLevel) {
            StreakRiskLevel.LOW_RISK -> "Your ${streak.currentCount}-day $streakName streak is going strong! Keep it up! 🔥"
            StreakRiskLevel.MEDIUM_RISK -> "Don't let your ${streak.currentCount}-day $streakName streak slip away! A quick action can keep it alive. 💪"
            StreakRiskLevel.HIGH_RISK -> "Your ${streak.currentCount}-day $streakName streak needs attention! Take action now to keep your momentum. ⚡"
            else -> "Keep up your great $streakName habits! 🌟"
        }
    }
    
    private fun getStreakDisplayName(streakType: StreakType): String {
        return when (streakType) {
            StreakType.DAILY_CHECK_IN -> "daily check-in"
            StreakType.UNDER_BUDGET -> "budget adherence"
            StreakType.GOAL_PROGRESS -> "goal progress"
            StreakType.TRANSACTION_CATEGORIZATION -> "transaction organizing"
            StreakType.SAVINGS_CONTRIBUTION -> "savings"
            StreakType.WEEKLY_BUDGET_ADHERENCE -> "weekly budget"
            StreakType.DAILY_SAVINGS -> "daily savings"
            StreakType.WEEKLY_GOAL_PROGRESS -> "weekly goal progress"
            StreakType.MICRO_WIN_COMPLETION -> "micro-win completion"
            StreakType.FINANCIAL_HEALTH_CHECK -> "financial health check"
        }
    }
    
    private fun generateStreakMaintenanceMicroWins(activeStreaks: List<Streak>): List<MicroWinOpportunity> {
        return activeStreaks.filter { it.riskLevel != StreakRiskLevel.SAFE }.map { streak ->
            MicroWinOpportunity(
                id = "maintain_${streak.id}",
                title = "Maintain ${getStreakDisplayName(streak.type)} streak",
                description = "Keep your ${streak.currentCount}-day streak alive!",
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.EASY] ?: 5,
                actionType = getActionForStreakType(streak.type),
                difficulty = MicroWinDifficulty.EASY,
                estimatedTimeMinutes = 2,
                isPersonalized = true,
                contextData = mapOf(
                    "streakId" to streak.id,
                    "streakCount" to streak.currentCount.toString(),
                    "riskLevel" to streak.riskLevel.name
                )
            )
        }
    }
    
    private fun generateHabitBuildingMicroWins(recentActions: List<PointsHistoryEntry>): List<MicroWinOpportunity> {
        val actionCounts = recentActions.groupBy { it.action }.mapValues { it.value.size }
        val underutilizedActions = UserAction.values().filter { (actionCounts[it] ?: 0) < 3 }
        
        return underutilizedActions.take(2).map { action ->
            MicroWinOpportunity(
                id = "habit_${action.name}",
                title = "Build ${getActionDescription(action)} habit",
                description = "Try ${getActionDescription(action)} to build a positive routine",
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.MEDIUM] ?: 10,
                actionType = action,
                difficulty = MicroWinDifficulty.MEDIUM,
                estimatedTimeMinutes = 5,
                isPersonalized = true
            )
        }
    }
    
    private fun generateExplorationMicroWins(profile: GamificationProfile?): List<MicroWinOpportunity> {
        return listOf(
            MicroWinOpportunity(
                id = "explore_insights",
                title = "Discover new insights",
                description = "Explore your spending patterns and trends",
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.MEDIUM] ?: 10,
                actionType = UserAction.REVIEW_INSIGHTS,
                difficulty = MicroWinDifficulty.MEDIUM,
                estimatedTimeMinutes = 3
            )
        )
    }
    
    private fun generateRecoveryMicroWins(recoveries: List<StreakRecovery>): List<MicroWinOpportunity> {
        return recoveries.filter { !it.isSuccessful }.map { recovery ->
            MicroWinOpportunity(
                id = "recovery_${recovery.id}",
                title = "Recover ${getStreakDisplayName(recovery.streakType)} streak",
                description = "Get back on track with your ${getStreakDisplayName(recovery.streakType)} habits",
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.HARD] ?: 20,
                actionType = getActionForStreakType(recovery.streakType),
                difficulty = MicroWinDifficulty.HARD,
                estimatedTimeMinutes = 10,
                isPersonalized = true,
                contextData = mapOf(
                    "recoveryId" to recovery.id,
                    "originalCount" to recovery.originalCount.toString()
                )
            )
        }
    }
    
    private fun calculateMicroWinPriority(
        microWin: MicroWinOpportunity,
        profile: GamificationProfile?,
        activeStreaks: List<Streak>
    ): Int {
        var priority = 0
        
        // Higher priority for personalized micro-wins
        if (microWin.isPersonalized) priority += 10
        
        // Higher priority for streak maintenance
        if (microWin.contextData.containsKey("streakId")) priority += 15
        
        // Higher priority for recovery
        if (microWin.contextData.containsKey("recoveryId")) priority += 20
        
        // Adjust based on difficulty (easier tasks get slight priority)
        priority += when (microWin.difficulty) {
            MicroWinDifficulty.EASY -> 5
            MicroWinDifficulty.MEDIUM -> 3
            MicroWinDifficulty.HARD -> 1
        }
        
        return priority
    }
    
    private fun detectActionMicroWins(action: UserAction, contextData: Map<String, String>): List<MicroWinOpportunity> {
        val microWins = mutableListOf<MicroWinOpportunity>()
        
        // Basic action micro-win
        microWins.add(
            MicroWinOpportunity(
                id = "action_${action.name}_${generateId()}",
                title = "Completed ${getActionDescription(action)}",
                description = "Great job staying on top of your finances!",
                pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.EASY] ?: 5,
                actionType = action,
                difficulty = MicroWinDifficulty.EASY,
                estimatedTimeMinutes = 1
            )
        )
        
        // Context-specific micro-wins
        when (action) {
            UserAction.CATEGORIZE_TRANSACTION -> {
                val count = contextData["transactionCount"]?.toIntOrNull() ?: 1
                if (count >= 5) {
                    microWins.add(
                        MicroWinOpportunity(
                            id = "bulk_categorize_${generateId()}",
                            title = "Bulk Organizer",
                            description = "Categorized $count transactions in one go!",
                            pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.MEDIUM] ?: 10,
                            actionType = action,
                            difficulty = MicroWinDifficulty.MEDIUM,
                            estimatedTimeMinutes = 5
                        )
                    )
                }
            }
            UserAction.MAKE_SAVINGS_CONTRIBUTION -> {
                val amount = contextData["amount"]?.toDoubleOrNull()
                if (amount != null && amount >= 50.0) {
                    microWins.add(
                        MicroWinOpportunity(
                            id = "significant_savings_${generateId()}",
                            title = "Significant Saver",
                            description = "Made a meaningful contribution to your future!",
                            pointsAwarded = MICRO_WIN_POINTS[MicroWinDifficulty.HARD] ?: 20,
                            actionType = action,
                            difficulty = MicroWinDifficulty.HARD,
                            estimatedTimeMinutes = 1
                        )
                    )
                }
            }
            else -> { /* No additional context-specific micro-wins */ }
        }
        
        return microWins
    }
    
    private suspend fun updateStreaksForAction(userId: String, action: UserAction): List<Streak> {
        val streakTypes = getStreakTypesForAction(action)
        val updatedStreaks = mutableListOf<Streak>()
        
        for (streakType in streakTypes) {
            updateStreakWithRiskAssessment(userId, streakType).getOrNull()?.let { result ->
                updatedStreaks.add(result.streak)
            }
        }
        
        return updatedStreaks
    }
    
    private fun getStreakTypesForAction(action: UserAction): List<StreakType> {
        return when (action) {
            UserAction.CHECK_BALANCE -> listOf(StreakType.DAILY_CHECK_IN)
            UserAction.CATEGORIZE_TRANSACTION -> listOf(StreakType.TRANSACTION_CATEGORIZATION)
            UserAction.UPDATE_GOAL -> listOf(StreakType.GOAL_PROGRESS)
            UserAction.MAKE_SAVINGS_CONTRIBUTION -> listOf(StreakType.SAVINGS_CONTRIBUTION, StreakType.DAILY_SAVINGS)
            UserAction.SET_BUDGET -> listOf(StreakType.UNDER_BUDGET)
            UserAction.COMPLETE_MICRO_TASK -> listOf(StreakType.MICRO_WIN_COMPLETION)
            UserAction.REVIEW_INSIGHTS -> listOf(StreakType.FINANCIAL_HEALTH_CHECK)
            else -> emptyList()
        }
    }
    
    private fun getActionForStreakType(streakType: StreakType): UserAction {
        return when (streakType) {
            StreakType.DAILY_CHECK_IN -> UserAction.CHECK_BALANCE
            StreakType.UNDER_BUDGET, StreakType.WEEKLY_BUDGET_ADHERENCE -> UserAction.SET_BUDGET
            StreakType.GOAL_PROGRESS, StreakType.WEEKLY_GOAL_PROGRESS -> UserAction.UPDATE_GOAL
            StreakType.TRANSACTION_CATEGORIZATION -> UserAction.CATEGORIZE_TRANSACTION
            StreakType.SAVINGS_CONTRIBUTION, StreakType.DAILY_SAVINGS -> UserAction.MAKE_SAVINGS_CONTRIBUTION
            StreakType.MICRO_WIN_COMPLETION -> UserAction.COMPLETE_MICRO_TASK
            StreakType.FINANCIAL_HEALTH_CHECK -> UserAction.REVIEW_INSIGHTS
        }
    }
    
    private fun getActionDescription(action: UserAction): String {
        return when (action) {
            UserAction.CHECK_BALANCE -> "balance check"
            UserAction.CATEGORIZE_TRANSACTION -> "transaction categorization"
            UserAction.UPDATE_GOAL -> "goal update"
            UserAction.LINK_ACCOUNT -> "account linking"
            UserAction.COMPLETE_MICRO_TASK -> "micro-task completion"
            UserAction.REVIEW_INSIGHTS -> "insights review"
            UserAction.SET_BUDGET -> "budget setting"
            UserAction.MAKE_SAVINGS_CONTRIBUTION -> "savings contribution"
        }
    }
    
    private suspend fun scheduleRecoveryReminders(userId: String, recovery: StreakRecovery) {
        // Schedule reminders for recovery process
        val reminderTimes = listOf(
            Clock.System.now().plus(4, DateTimeUnit.HOUR), // 4 hours later
            Clock.System.now().plus(1, DateTimeUnit.DAY),   // 1 day later
            Clock.System.now().plus(2, DateTimeUnit.DAY)    // 2 days later
        )
        
        for ((index, time) in reminderTimes.withIndex()) {
            val reminder = StreakReminder(
                id = generateId(),
                userId = userId,
                streakId = recovery.originalStreakId,
                streakType = recovery.streakType,
                reminderType = ReminderType.RECOVERY_SUPPORT,
                message = "Recovery step ${index + 1}: ${getRecoveryMessage(recovery.streakType, index)}",
                scheduledFor = time
            )
            gamificationRepository.createStreakReminder(reminder)
        }
    }
    
    private fun getRecoveryMessage(streakType: StreakType, step: Int): String {
        val actionName = getStreakDisplayName(streakType)
        return when (step) {
            0 -> "Take a small step to restart your $actionName habit"
            1 -> "Keep building momentum with your $actionName recovery"
            2 -> "Final push! Complete your $actionName recovery today"
            else -> "Continue your $actionName recovery journey"
        }
    }
    
    private fun calculateReminderTime(reminderType: ReminderType, riskLevel: StreakRiskLevel): Instant {
        val now = Clock.System.now()
        
        return when (reminderType) {
            ReminderType.GENTLE_NUDGE -> now.plus(2, DateTimeUnit.HOUR)
            ReminderType.MOTIVATION_BOOST -> now.plus(1, DateTimeUnit.HOUR)
            ReminderType.STREAK_RISK_ALERT -> now.plus(30, DateTimeUnit.MINUTE)
            ReminderType.RECOVERY_SUPPORT -> now.plus(4, DateTimeUnit.HOUR)
        }
    }
    
    private fun calculateWeeklyStreakTrend(activeStreaks: List<Streak>): List<Int> {
        // Simplified implementation - in real app, this would analyze historical data
        return (0..6).map { day ->
            activeStreaks.count { streak ->
                val daysSinceLastActivity = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays() - 
                    streak.lastActivityDate.toEpochDays()
                daysSinceLastActivity.toInt() == day
            }
        }
    }
    
    private fun calculateMonthlyMilestones(allStreaks: List<Streak>): List<StreakMilestone> {
        // Simplified implementation - in real app, this would analyze historical milestone data
        return allStreaks.filter { it.bestCount in CELEBRATION_MILESTONES }.map { streak ->
            StreakMilestone(
                streakType = streak.type,
                count = streak.bestCount,
                achievedAt = Clock.System.now(), // Would be actual achievement time
                isPersonalRecord = true
            )
        }.take(5)
    }
    
    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString() + Random.nextInt(1000)
    }
}