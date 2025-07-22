package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Implementation of GoalProgressVisualizationService
 */
class GoalProgressVisualizationServiceImpl(
    private val goalRepository: GoalRepository,
    private val goalService: GoalService
) : GoalProgressVisualizationService {

    companion object {
        private val MILESTONE_PERCENTAGES = listOf(0.1, 0.25, 0.5, 0.75, 0.9, 1.0)
        private val PROGRESS_RING_COLORS = mapOf(
            0.0 to "#FF6B6B", // Red for low progress
            0.25 to "#FFE66D", // Yellow for quarter progress
            0.5 to "#4ECDC4", // Teal for half progress
            0.75 to "#45B7D1", // Blue for three-quarter progress
            1.0 to "#96CEB4" // Green for completion
        )
    }

    override suspend fun getProgressVisualization(goalId: String): Result<GoalProgressVisualization> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val progressRing = createProgressRingData(goal)
            val milestones = createProgressMilestones(goal)
            val projectionData = calculateProjectionVisualization(goal)
            val animationTriggers = generateAnimationTriggers(goal)
            val statusIndicators = createStatusIndicators(goal)
            
            val visualization = GoalProgressVisualization(
                goalId = goalId,
                progressRing = progressRing,
                milestones = milestones,
                projectionData = projectionData,
                animationTriggers = animationTriggers,
                statusIndicators = statusIndicators
            )
            
            Result.success(visualization)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProgressVisualization(goalId: String, newAmount: Money): Result<GoalProgressVisualization> {
        return try {
            // Update goal progress first
            goalRepository.updateProgress(goalId, newAmount).getOrThrow()
            
            // Check for milestone achievements
            checkMilestoneAchievements(goalId).getOrThrow()
            
            // Get updated visualization
            getProgressVisualization(goalId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkMilestoneAchievements(goalId: String): Result<List<ProgressMilestone>> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val currentProgress = goal.progressPercentage / 100.0
            val achievedMilestones = mutableListOf<ProgressMilestone>()
            
            MILESTONE_PERCENTAGES.forEach { milestonePercentage ->
                if (currentProgress >= milestonePercentage) {
                    val milestone = createMilestone(goal, milestonePercentage)
                    achievedMilestones.add(milestone)
                }
            }
            
            Result.success(achievedMilestones)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun celebrateMilestone(milestoneId: String): Result<CelebrationData> {
        return try {
            // Extract goal ID and milestone percentage from milestone ID
            val parts = milestoneId.split("_")
            val goalId = parts[0]
            val milestonePercentage = parts[2].toDouble()
            
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val celebration = createMilestoneCelebration(goal, milestonePercentage)
            Result.success(celebration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateProjectedCompletion(goalId: String): Result<ProjectionVisualization> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val projection = calculateProjectionVisualization(goal)
            Result.success(projection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProjectionData(goalId: String): Result<ProjectionVisualization> {
        return calculateProjectedCompletion(goalId)
    } 
   override suspend fun generateAdjustmentRecommendations(goalId: String): Result<List<GoalAdjustmentRecommendation>> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val recommendations = mutableListOf<GoalAdjustmentRecommendation>()
            
            // Analyze goal status and generate recommendations
            val progressRate = goal.progressPercentage / 100.0
            val timeElapsed = calculateTimeElapsed(goal)
            val expectedProgress = timeElapsed
            
            when {
                progressRate < expectedProgress * 0.5 -> {
                    // Significantly behind - critical recommendations
                    recommendations.addAll(generateCriticalRecommendations(goal))
                }
                progressRate < expectedProgress * 0.8 -> {
                    // Moderately behind - adjustment recommendations
                    recommendations.addAll(generateModerateRecommendations(goal))
                }
                progressRate >= expectedProgress * 1.2 -> {
                    // Ahead of schedule - optimization recommendations
                    recommendations.addAll(generateOptimizationRecommendations(goal))
                }
                else -> {
                    // On track - maintenance recommendations
                    recommendations.addAll(generateMaintenanceRecommendations(goal))
                }
            }
            
            Result.success(recommendations.sortedByDescending { it.priority.sortOrder })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun applyAdjustmentRecommendation(goalId: String, recommendationId: String): Result<FinancialGoal> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            // Apply the specific recommendation
            val updatedGoal = when {
                recommendationId.contains("increase_weekly") -> {
                    val newWeeklyAmount = goal.weeklyTargetAmount * 1.25
                    goal.copy(targetAmount = goal.targetAmount + (newWeeklyAmount * (goal.daysRemaining / 7)))
                }
                recommendationId.contains("extend_deadline") -> {
                    goal.copy(targetDate = goal.targetDate.plus(DatePeriod(months = 1)))
                }
                recommendationId.contains("reduce_target") -> {
                    goal.copy(targetAmount = goal.targetAmount * 0.9)
                }
                else -> goal
            }
            
            goalRepository.update(updatedGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trackGoalAchievements(userId: String): Result<List<AchievementBadge>> {
        return try {
            val goals = goalRepository.findByUserId(userId).getOrThrow()
            val achievements = mutableListOf<AchievementBadge>()
            
            goals.forEach { goal ->
                achievements.addAll(generateAchievementBadges(goal))
            }
            
            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGoalHistory(userId: String, limit: Int): Result<List<GoalHistoryEntry>> {
        return try {
            val goals = goalRepository.findByUserId(userId).getOrThrow()
            val historyEntries = mutableListOf<GoalHistoryEntry>()
            
            goals.forEach { goal ->
                historyEntries.addAll(generateGoalHistoryEntries(goal))
            }
            
            val sortedHistory = historyEntries
                .sortedByDescending { it.timestamp }
                .take(limit)
            
            Result.success(sortedHistory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnimationTriggers(goalId: String): Result<List<AnimationTrigger>> {
        return try {
            val goal = goalRepository.findById(goalId).getOrThrow()
                ?: return Result.failure(IllegalArgumentException("Goal not found"))
            
            val triggers = generateAnimationTriggers(goal)
            Result.success(triggers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAnimationTriggered(triggerId: String): Result<Unit> {
        return try {
            // In a real implementation, this would update the trigger status in storage
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProgressVisualization(goalId: String): Flow<GoalProgressVisualization> {
        return goalRepository.observeGoalProgress(goalId).map { goal ->
            goal?.let { 
                getProgressVisualization(goalId).getOrNull() 
            } ?: createDefaultVisualization(goalId)
        }
    }

    override fun observeMilestoneAchievements(goalId: String): Flow<List<ProgressMilestone>> {
        return goalRepository.observeGoalProgress(goalId).map { goal ->
            goal?.let {
                checkMilestoneAchievements(goalId).getOrNull() ?: emptyList()
            } ?: emptyList()
        }
    }   
 // Private helper methods

    private fun createProgressRingData(goal: FinancialGoal): ProgressRingData {
        val progress = goal.progressPercentage / 100.0
        val color = getProgressColor(progress)
        
        return ProgressRingData(
            currentProgress = progress,
            ringColor = color,
            backgroundColor = "#F0F0F0",
            strokeWidth = 12.0f,
            radius = 80.0f,
            animationDuration = 1500L,
            showPercentage = true,
            glowEffect = progress >= 0.9
        )
    }

    private fun getProgressColor(progress: Double): String {
        return when {
            progress >= 1.0 -> PROGRESS_RING_COLORS[1.0]!!
            progress >= 0.75 -> PROGRESS_RING_COLORS[0.75]!!
            progress >= 0.5 -> PROGRESS_RING_COLORS[0.5]!!
            progress >= 0.25 -> PROGRESS_RING_COLORS[0.25]!!
            else -> PROGRESS_RING_COLORS[0.0]!!
        }
    }

    private fun createProgressMilestones(goal: FinancialGoal): List<ProgressMilestone> {
        val currentProgress = goal.progressPercentage / 100.0
        
        return MILESTONE_PERCENTAGES.map { percentage ->
            createMilestone(goal, percentage, currentProgress >= percentage)
        }
    }

    private fun createMilestone(goal: FinancialGoal, percentage: Double, isReached: Boolean = false): ProgressMilestone {
        val milestoneAmount = goal.targetAmount * percentage
        val title = when (percentage) {
            0.1 -> "First Steps"
            0.25 -> "Quarter Way"
            0.5 -> "Halfway There"
            0.75 -> "Three Quarters"
            0.9 -> "Almost Done"
            1.0 -> "Goal Achieved"
            else -> "${(percentage * 100).toInt()}% Complete"
        }
        
        val celebrationType = when (percentage) {
            0.1 -> MilestoneCelebrationType.SPARKLES
            0.25 -> MilestoneCelebrationType.BADGE_UNLOCK
            0.5 -> MilestoneCelebrationType.CONFETTI
            0.75 -> MilestoneCelebrationType.FIREWORKS
            0.9 -> MilestoneCelebrationType.LEVEL_UP
            1.0 -> MilestoneCelebrationType.FIREWORKS
            else -> MilestoneCelebrationType.SPARKLES
        }
        
        return ProgressMilestone(
            id = "${goal.id}_milestone_${percentage}",
            goalId = goal.id,
            percentage = percentage,
            title = title,
            description = "Reached ${milestoneAmount.amount} of ${goal.targetAmount.amount} for ${goal.title}",
            isReached = isReached,
            reachedAt = if (isReached) Clock.System.now() else null,
            celebrationType = celebrationType,
            badgeIcon = getBadgeIcon(percentage),
            pointsAwarded = calculateMilestonePoints(percentage)
        )
    }

    private fun getBadgeIcon(percentage: Double): String {
        return when (percentage) {
            0.1 -> "🌟"
            0.25 -> "🏅"
            0.5 -> "🎖️"
            0.75 -> "🏆"
            0.9 -> "💎"
            1.0 -> "👑"
            else -> "⭐"
        }
    }

    private fun calculateMilestonePoints(percentage: Double): Int {
        return when (percentage) {
            0.1 -> 10
            0.25 -> 25
            0.5 -> 50
            0.75 -> 75
            0.9 -> 90
            1.0 -> 100
            else -> (percentage * 100).toInt()
        }
    }

    private fun calculateProjectionVisualization(goal: FinancialGoal): ProjectionVisualization {
        val trendLine = generateTrendLine(goal)
        val projectionLine = generateProjectionLine(goal)
        val projectedDate = calculateProjectedDate(goal)
        val confidence = calculateConfidence(goal)
        val isOnTrack = isGoalOnTrack(goal)
        val riskLevel = calculateRiskLevel(goal)
        val recommendations = generateProjectionRecommendations(goal)
        
        return ProjectionVisualization(
            goalId = goal.id,
            projectedCompletionDate = projectedDate,
            confidenceLevel = confidence,
            trendLine = trendLine,
            projectionLine = projectionLine,
            isOnTrack = isOnTrack,
            riskLevel = riskLevel,
            adjustmentRecommendations = recommendations
        )
    } 
   private fun generateTrendLine(goal: FinancialGoal): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val startDate = goal.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val daysSinceStart = (today.toEpochDays() - startDate.toEpochDays()).toInt()
        
        // Generate historical trend points (simulated for demo)
        for (day in 0..daysSinceStart step 7) { // Weekly points
            val date = startDate.plus(DatePeriod(days = day))
            val progressRatio = if (daysSinceStart > 0) day.toDouble() / daysSinceStart.toDouble() else 0.0
            val amount = goal.currentAmount * progressRatio
            
            points.add(TrendPoint(date, amount, false, 1.0))
        }
        
        return points
    }

    private fun generateProjectionLine(goal: FinancialGoal): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val projectedDate = calculateProjectedDate(goal)
        val daysToProjection = (projectedDate.toEpochDays() - today.toEpochDays()).toInt()
        
        // Generate projection points
        for (day in 0..daysToProjection step 7) { // Weekly points
            val date = today.plus(DatePeriod(days = day))
            val progressRatio = if (daysToProjection > 0) day.toDouble() / daysToProjection.toDouble() else 1.0
            val amount = goal.currentAmount + (goal.remainingAmount * progressRatio)
            val confidence = 1.0 - (progressRatio * 0.3) // Confidence decreases over time
            
            points.add(TrendPoint(date, amount, true, confidence))
        }
        
        return points
    }

    private fun calculateProjectedDate(goal: FinancialGoal): LocalDate {
        if (goal.isCompleted) return Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val remainingAmount = goal.remainingAmount
        if (remainingAmount.isZero) return Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Calculate based on current weekly savings rate
        val weeklyRate = goal.weeklyTargetAmount
        if (weeklyRate.isZero) return goal.targetDate
        
        val weeksNeeded = ceil(remainingAmount.amount / weeklyRate.amount).toInt()
        return Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(weeks = weeksNeeded))
    }

    private fun calculateConfidence(goal: FinancialGoal): Double {
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        
        return when {
            progressRate >= timeElapsed * 1.2 -> 0.95 // Well ahead
            progressRate >= timeElapsed -> 0.85 // On track
            progressRate >= timeElapsed * 0.8 -> 0.65 // Slightly behind
            progressRate >= timeElapsed * 0.6 -> 0.45 // Behind
            else -> 0.25 // Significantly behind
        }.coerceIn(0.0, 1.0)
    }

    private fun isGoalOnTrack(goal: FinancialGoal): Boolean {
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        return progressRate >= timeElapsed * 0.8
    }

    private fun calculateRiskLevel(goal: FinancialGoal): RiskLevel {
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        
        return when {
            progressRate < timeElapsed * 0.4 -> RiskLevel.CRITICAL
            progressRate < timeElapsed * 0.6 -> RiskLevel.HIGH
            progressRate < timeElapsed * 0.8 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    private fun calculateTimeElapsed(goal: FinancialGoal): Double {
        val startDate = goal.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val totalDays = goal.targetDate.toEpochDays() - startDate.toEpochDays()
        val elapsedDays = today.toEpochDays() - startDate.toEpochDays()
        
        return if (totalDays > 0) elapsedDays.toDouble() / totalDays.toDouble() else 0.0
    }

    private fun generateProjectionRecommendations(goal: FinancialGoal): List<GoalAdjustmentRecommendation> {
        val recommendations = mutableListOf<GoalAdjustmentRecommendation>()
        val riskLevel = calculateRiskLevel(goal)
        
        when (riskLevel) {
            RiskLevel.CRITICAL, RiskLevel.HIGH -> {
                recommendations.addAll(generateCriticalRecommendations(goal))
            }
            RiskLevel.MEDIUM -> {
                recommendations.addAll(generateModerateRecommendations(goal))
            }
            RiskLevel.LOW -> {
                recommendations.addAll(generateOptimizationRecommendations(goal))
            }
        }
        
        return recommendations
    }    pri
vate fun generateCriticalRecommendations(goal: FinancialGoal): List<GoalAdjustmentRecommendation> {
        return listOf(
            GoalAdjustmentRecommendation(
                id = "${goal.id}_critical_increase_weekly",
                type = AdjustmentType.INCREASE_WEEKLY_AMOUNT,
                title = "Increase Weekly Savings",
                description = "Double your weekly savings to get back on track",
                impact = "Will help you catch up to your target timeline",
                priority = Priority.CRITICAL,
                estimatedImprovement = 50.0,
                actionRequired = "Increase weekly savings from ${goal.weeklyTargetAmount.amount} to ${(goal.weeklyTargetAmount * 2).amount}",
                isAutomatable = false
            ),
            GoalAdjustmentRecommendation(
                id = "${goal.id}_critical_extend_deadline",
                type = AdjustmentType.EXTEND_DEADLINE,
                title = "Extend Goal Deadline",
                description = "Add 3 months to your target date to make it more achievable",
                impact = "Reduces weekly savings requirement by 30%",
                priority = Priority.HIGH,
                estimatedImprovement = 30.0,
                actionRequired = "Extend deadline from ${goal.targetDate} to ${goal.targetDate.plus(DatePeriod(months = 3))}",
                isAutomatable = true
            )
        )
    }

    private fun generateModerateRecommendations(goal: FinancialGoal): List<GoalAdjustmentRecommendation> {
        return listOf(
            GoalAdjustmentRecommendation(
                id = "${goal.id}_moderate_increase_weekly",
                type = AdjustmentType.INCREASE_WEEKLY_AMOUNT,
                title = "Boost Weekly Savings",
                description = "Increase your weekly savings by 25% to stay on track",
                impact = "Will keep you on schedule for your goal",
                priority = Priority.MEDIUM,
                estimatedImprovement = 25.0,
                actionRequired = "Increase weekly savings to ${(goal.weeklyTargetAmount * 1.25).amount}",
                isAutomatable = false
            )
        )
    }

    private fun generateOptimizationRecommendations(goal: FinancialGoal): List<GoalAdjustmentRecommendation> {
        return listOf(
            GoalAdjustmentRecommendation(
                id = "${goal.id}_optimize_accelerate",
                type = AdjustmentType.INCREASE_WEEKLY_AMOUNT,
                title = "Accelerate Goal Achievement",
                description = "You're ahead of schedule! Consider increasing your target or creating a new goal",
                impact = "Could complete goal 2 months early",
                priority = Priority.LOW,
                estimatedImprovement = 15.0,
                actionRequired = "Consider increasing target amount or accelerating timeline",
                isAutomatable = false
            )
        )
    }

    private fun generateMaintenanceRecommendations(goal: FinancialGoal): List<GoalAdjustmentRecommendation> {
        return listOf(
            GoalAdjustmentRecommendation(
                id = "${goal.id}_maintain_consistency",
                type = AdjustmentType.OPTIMIZE_SAVINGS_STRATEGY,
                title = "Maintain Consistency",
                description = "Keep up your current savings rate to achieve your goal on time",
                impact = "Ensures goal completion as planned",
                priority = Priority.LOW,
                estimatedImprovement = 0.0,
                actionRequired = "Continue current savings pattern",
                isAutomatable = true
            )
        )
    }

    private fun generateAnimationTriggers(goal: FinancialGoal): List<AnimationTrigger> {
        val triggers = mutableListOf<AnimationTrigger>()
        
        // Progress update trigger
        triggers.add(
            AnimationTrigger(
                id = "${goal.id}_progress_update",
                triggerType = AnimationTriggerType.PROGRESS_UPDATE,
                animationType = "progress_ring_fill",
                duration = 1500L,
                shouldTrigger = true,
                triggerCondition = "progress_changed"
            )
        )
        
        // Milestone triggers
        MILESTONE_PERCENTAGES.forEach { percentage ->
            if (goal.progressPercentage / 100.0 >= percentage) {
                triggers.add(
                    AnimationTrigger(
                        id = "${goal.id}_milestone_${percentage}",
                        triggerType = AnimationTriggerType.MILESTONE_REACHED,
                        animationType = "confetti_burst",
                        duration = 3000L,
                        shouldTrigger = true,
                        triggerCondition = "milestone_${percentage}_reached",
                        celebrationData = createMilestoneCelebration(goal, percentage)
                    )
                )
            }
        }
        
        return triggers
    }

    private fun createMilestoneCelebration(goal: FinancialGoal, percentage: Double): CelebrationData {
        val title = when (percentage) {
            0.1 -> "Great Start! 🌟"
            0.25 -> "Quarter Complete! 🏅"
            0.5 -> "Halfway There! 🎖️"
            0.75 -> "Three Quarters Done! 🏆"
            0.9 -> "Almost There! 💎"
            1.0 -> "Goal Achieved! 👑"
            else -> "Milestone Reached! ⭐"
        }
        
        val message = "You've reached ${(percentage * 100).toInt()}% of your ${goal.title} goal!"
        
        return CelebrationData(
            title = title,
            message = message,
            iconUrl = getBadgeIcon(percentage),
            colorScheme = listOf("#FFD700", "#FFA500", "#FF6B6B"),
            soundEffect = "milestone_chime",
            hapticPattern = "success_pattern",
            pointsAwarded = calculateMilestonePoints(percentage),
            badgeEarned = "milestone_${(percentage * 100).toInt()}_percent"
        )
    } 
   private fun createStatusIndicators(goal: FinancialGoal): GoalStatusIndicators {
        val overallStatus = determineOverallStatus(goal)
        val progressStatus = determineProgressStatus(goal)
        val timelineStatus = determineTimelineStatus(goal)
        val riskIndicators = generateRiskIndicators(goal)
        val achievementBadges = generateAchievementBadges(goal)
        val streakData = generateStreakData(goal)
        
        return GoalStatusIndicators(
            goalId = goal.id,
            overallStatus = overallStatus,
            progressStatus = progressStatus,
            timelineStatus = timelineStatus,
            riskIndicators = riskIndicators,
            achievementBadges = achievementBadges,
            streakData = streakData
        )
    }

    private fun determineOverallStatus(goal: FinancialGoal): GoalStatus {
        if (goal.isCompleted) return GoalStatus.COMPLETED
        if (!goal.isActive) return GoalStatus.PAUSED
        
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        
        return when {
            progressRate >= timeElapsed * 1.1 -> GoalStatus.ON_TRACK
            progressRate >= timeElapsed * 0.9 -> GoalStatus.ON_TRACK
            progressRate >= timeElapsed * 0.7 -> GoalStatus.SLIGHTLY_BEHIND
            progressRate >= timeElapsed * 0.5 -> GoalStatus.BEHIND_SCHEDULE
            else -> GoalStatus.CRITICAL
        }
    }

    private fun determineProgressStatus(goal: FinancialGoal): ProgressStatus {
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        
        return when {
            progressRate >= timeElapsed * 1.2 -> ProgressStatus.EXCELLENT
            progressRate >= timeElapsed * 0.9 -> ProgressStatus.GOOD
            progressRate >= timeElapsed * 0.7 -> ProgressStatus.FAIR
            progressRate >= timeElapsed * 0.5 -> ProgressStatus.POOR
            else -> ProgressStatus.STALLED
        }
    }

    private fun determineTimelineStatus(goal: FinancialGoal): TimelineStatus {
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        
        return when {
            progressRate >= timeElapsed * 1.2 -> TimelineStatus.AHEAD_OF_SCHEDULE
            progressRate >= timeElapsed * 0.9 -> TimelineStatus.ON_SCHEDULE
            progressRate >= timeElapsed * 0.7 -> TimelineStatus.SLIGHTLY_DELAYED
            progressRate >= timeElapsed * 0.5 -> TimelineStatus.SIGNIFICANTLY_DELAYED
            else -> TimelineStatus.OVERDUE
        }
    }

    private fun generateRiskIndicators(goal: FinancialGoal): List<RiskIndicator> {
        val indicators = mutableListOf<RiskIndicator>()
        
        // Timeline risk
        if (goal.daysRemaining < 30 && !goal.isCompleted) {
            indicators.add(
                RiskIndicator(
                    id = "${goal.id}_timeline_risk",
                    type = RiskType.TIMELINE_RISK,
                    severity = RiskSeverity.HIGH,
                    title = "Deadline Approaching",
                    description = "Your goal deadline is less than 30 days away",
                    recommendation = "Consider increasing your savings rate or extending the deadline"
                )
            )
        }
        
        // Progress stall risk
        val progressRate = goal.progressPercentage / 100.0
        val timeElapsed = calculateTimeElapsed(goal)
        if (progressRate < timeElapsed * 0.5) {
            indicators.add(
                RiskIndicator(
                    id = "${goal.id}_progress_stall",
                    type = RiskType.PROGRESS_STALL,
                    severity = RiskSeverity.CRITICAL,
                    title = "Progress Significantly Behind",
                    description = "Your progress is significantly behind schedule",
                    recommendation = "Review your savings strategy and consider adjustments"
                )
            )
        }
        
        return indicators
    }

    private fun generateAchievementBadges(goal: FinancialGoal): List<AchievementBadge> {
        val badges = mutableListOf<AchievementBadge>()
        
        // Progress badges
        val progressPercentage = goal.progressPercentage
        when {
            progressPercentage >= 100.0 -> {
                badges.add(
                    AchievementBadge(
                        id = "${goal.id}_completed",
                        title = "Goal Achiever",
                        description = "Completed ${goal.title}",
                        iconUrl = "👑",
                        earnedAt = Clock.System.now(),
                        category = BadgeCategory.MILESTONE,
                        rarity = BadgeRarity.EPIC,
                        pointsValue = 100
                    )
                )
            }
            progressPercentage >= 75.0 -> {
                badges.add(
                    AchievementBadge(
                        id = "${goal.id}_three_quarters",
                        title = "Almost There",
                        description = "Reached 75% of ${goal.title}",
                        iconUrl = "🏆",
                        earnedAt = Clock.System.now(),
                        category = BadgeCategory.PROGRESS,
                        rarity = BadgeRarity.RARE,
                        pointsValue = 75
                    )
                )
            }
            progressPercentage >= 50.0 -> {
                badges.add(
                    AchievementBadge(
                        id = "${goal.id}_halfway",
                        title = "Halfway Hero",
                        description = "Reached 50% of ${goal.title}",
                        iconUrl = "🎖️",
                        earnedAt = Clock.System.now(),
                        category = BadgeCategory.PROGRESS,
                        rarity = BadgeRarity.UNCOMMON,
                        pointsValue = 50
                    )
                )
            }
        }
        
        return badges
    }

    private fun generateStreakData(goal: FinancialGoal): StreakData {
        // Simulated streak data - in real implementation, this would be tracked
        return StreakData(
            goalId = goal.id,
            currentStreak = 7, // 7 days of consistent progress
            longestStreak = 14,
            streakType = StreakType.WEEKLY_SAVINGS,
            lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            streakStartDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = 7)),
            isActive = true,
            riskOfBreaking = false
        )
    }

    private fun generateGoalHistoryEntries(goal: FinancialGoal): List<GoalHistoryEntry> {
        val entries = mutableListOf<GoalHistoryEntry>()
        
        // Goal creation entry
        entries.add(
            GoalHistoryEntry(
                id = "${goal.id}_created",
                goalId = goal.id,
                eventType = GoalHistoryEventType.GOAL_CREATED,
                timestamp = goal.createdAt,
                description = "Goal '${goal.title}' created with target of ${goal.targetAmount.amount}"
            )
        )
        
        // Progress update entries (simulated)
        val progressUpdates = listOf(0.1, 0.25, 0.5, 0.75)
        progressUpdates.forEach { percentage ->
            if (goal.progressPercentage / 100.0 >= percentage) {
                entries.add(
                    GoalHistoryEntry(
                        id = "${goal.id}_progress_${percentage}",
                        goalId = goal.id,
                        eventType = GoalHistoryEventType.MILESTONE_REACHED,
                        timestamp = Clock.System.now(),
                        description = "Reached ${(percentage * 100).toInt()}% of goal",
                        newValue = "${(percentage * 100).toInt()}%"
                    )
                )
            }
        }
        
        // Goal completion entry
        if (goal.isCompleted) {
            entries.add(
                GoalHistoryEntry(
                    id = "${goal.id}_completed",
                    goalId = goal.id,
                    eventType = GoalHistoryEventType.GOAL_COMPLETED,
                    timestamp = Clock.System.now(),
                    description = "Goal '${goal.title}' completed successfully!"
                )
            )
        }
        
        return entries
    }

    private fun createDefaultVisualization(goalId: String): GoalProgressVisualization {
        return GoalProgressVisualization(
            goalId = goalId,
            progressRing = ProgressRingData(
                currentProgress = 0.0,
                ringColor = "#FF6B6B",
                backgroundColor = "#F0F0F0"
            ),
            milestones = emptyList(),
            projectionData = ProjectionVisualization(
                goalId = goalId,
                projectedCompletionDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                confidenceLevel = 0.0,
                trendLine = emptyList(),
                projectionLine = emptyList(),
                isOnTrack = false,
                riskLevel = RiskLevel.LOW,
                adjustmentRecommendations = emptyList()
            ),
            animationTriggers = emptyList(),
            statusIndicators = GoalStatusIndicators(
                goalId = goalId,
                overallStatus = GoalStatus.ON_TRACK,
                progressStatus = ProgressStatus.FAIR,
                timelineStatus = TimelineStatus.ON_SCHEDULE,
                riskIndicators = emptyList(),
                achievementBadges = emptyList()
            )
        )
    }
}