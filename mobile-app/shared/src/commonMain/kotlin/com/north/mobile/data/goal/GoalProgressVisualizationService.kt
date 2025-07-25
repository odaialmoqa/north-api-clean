package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*

/**
 * Service for goal progress visualization and tracking
 */
// Commented out due to missing types
/*
interface GoalProgressVisualizationService {
    // Progress visualization
    suspend fun getProgressVisualization(goalId: String): Result<GoalProgressVisualization>
    suspend fun updateProgressVisualization(goalId: String, newAmount: Money): Result<GoalProgressVisualization>
    // Milestone detection and celebrations
    suspend fun checkMilestoneAchievements(goalId: String): Result<List<ProgressMilestone>>
    suspend fun celebrateMilestone(milestoneId: String): Result<CelebrationData>
    // Projection calculations
    suspend fun calculateProjectedCompletion(goalId: String): Result<ProjectionVisualization>
    suspend fun updateProjectionData(goalId: String): Result<ProjectionVisualization>
    // Goal adjustment recommendations
    suspend fun generateAdjustmentRecommendations(goalId: String): Result<List<GoalAdjustmentRecommendation>>
    suspend fun applyAdjustmentRecommendation(goalId: String, recommendationId: String): Result<FinancialGoal>
    // Achievement tracking
    suspend fun trackGoalAchievements(userId: String): Result<List<AchievementBadge>>
    suspend fun getGoalHistory(userId: String, limit: Int = 50): Result<List<GoalHistoryEntry>>
    // Animation triggers
    suspend fun getAnimationTriggers(goalId: String): Result<List<AnimationTrigger>>
    suspend fun markAnimationTriggered(triggerId: String): Result<Unit>
    // Real-time updates
    fun observeProgressVisualization(goalId: String): Flow<GoalProgressVisualization>
    fun observeMilestoneAchievements(goalId: String): Flow<List<ProgressMilestone>>
}
*/

/**
 * Goal history entry for tracking
 */
data class GoalHistoryEntry(
    val id: String,
    val goalId: String,
    val eventType: GoalHistoryEventType,
    val timestamp: Instant,
    val description: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class GoalHistoryEventType {
    GOAL_CREATED,
    PROGRESS_UPDATED,
    MILESTONE_REACHED,
    GOAL_COMPLETED,
    GOAL_PAUSED,
    GOAL_RESUMED,
    TARGET_ADJUSTED,
    DEADLINE_EXTENDED,
    PRIORITY_CHANGED,
    MICRO_TASK_COMPLETED,
    RECOMMENDATION_APPLIED,
    CELEBRATION_TRIGGERED
}