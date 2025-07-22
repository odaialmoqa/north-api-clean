package com.north.mobile.data.goal

import com.north.mobile.domain.model.FinancialGoal
import com.north.mobile.domain.model.MicroTask
import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Service interface for comprehensive goal management
 */
interface GoalService {
    
    // Goal CRUD operations
    suspend fun createGoal(goal: FinancialGoal): Result<FinancialGoal>
    suspend fun updateGoal(goal: FinancialGoal): Result<FinancialGoal>
    suspend fun deleteGoal(goalId: String): Result<Unit>
    suspend fun getGoal(goalId: String): Result<FinancialGoal?>
    suspend fun getUserGoals(userId: String): Result<List<FinancialGoal>>
    suspend fun getActiveGoals(userId: String): Result<List<FinancialGoal>>
    
    // Goal progress tracking
    suspend fun updateGoalProgress(goalId: String, amount: Money): Result<FinancialGoal>
    suspend fun getGoalProgress(goalId: String): Result<GoalProgress>
    suspend fun getGoalProjection(goalId: String): Result<GoalProjection>
    
    // Micro-task management
    suspend fun createMicroTask(goalId: String, microTask: MicroTask): Result<MicroTask>
    suspend fun completeMicroTask(microTaskId: String): Result<MicroTask>
    suspend fun generateMicroTasks(goalId: String): Result<List<MicroTask>>
    suspend fun getMicroTasks(goalId: String): Result<List<MicroTask>>
    
    // Goal conflict detection and prioritization
    suspend fun detectGoalConflicts(userId: String): Result<List<GoalConflict>>
    suspend fun prioritizeGoals(userId: String, goalIds: List<String>): Result<List<FinancialGoal>>
    suspend fun resolveGoalConflict(conflictId: String, resolution: ConflictResolution): Result<Unit>
    
    // Goal achievement and celebrations
    suspend fun checkGoalAchievements(userId: String): Result<List<GoalAchievement>>
    suspend fun celebrateGoalAchievement(goalId: String): Result<GoalCelebration>
    suspend fun getNextStepSuggestions(goalId: String): Result<List<NextStepSuggestion>>
    
    // Goal analytics and insights
    suspend fun getGoalInsights(userId: String): Result<GoalInsights>
    suspend fun getGoalRecommendations(userId: String): Result<List<GoalRecommendation>>
    
    // Real-time updates
    fun observeGoalProgress(goalId: String): Flow<GoalProgress>
    fun observeUserGoals(userId: String): Flow<List<FinancialGoal>>
}

/**
 * Represents goal progress with detailed metrics
 */
data class GoalProgress(
    val goalId: String,
    val currentAmount: Money,
    val targetAmount: Money,
    val progressPercentage: Double,
    val remainingAmount: Money,
    val daysRemaining: Long,
    val weeklyTargetAmount: Money,
    val monthlyTargetAmount: Money,
    val isOnTrack: Boolean,
    val projectedCompletionDate: LocalDate?,
    val completedMicroTasks: Int,
    val totalMicroTasks: Int
)

/**
 * Represents goal projection based on current progress
 */
data class GoalProjection(
    val goalId: String,
    val projectedCompletionDate: LocalDate,
    val confidenceLevel: Double, // 0.0 to 1.0
    val requiredWeeklyAmount: Money,
    val requiredMonthlyAmount: Money,
    val isAchievable: Boolean,
    val riskFactors: List<String>,
    val recommendations: List<String>
)

/**
 * Represents a conflict between goals
 */
data class GoalConflict(
    val id: String,
    val conflictType: ConflictType,
    val primaryGoalId: String,
    val secondaryGoalId: String,
    val description: String,
    val severity: ConflictSeverity,
    val suggestedResolutions: List<ConflictResolution>
)

enum class ConflictType {
    TIMELINE_OVERLAP,
    BUDGET_COMPETITION,
    PRIORITY_MISMATCH,
    RESOURCE_CONSTRAINT
}

enum class ConflictSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Represents a resolution for a goal conflict
 */
data class ConflictResolution(
    val id: String,
    val type: ResolutionType,
    val description: String,
    val impact: String,
    val adjustments: List<GoalAdjustment>
)

enum class ResolutionType {
    ADJUST_TIMELINE,
    ADJUST_PRIORITY,
    ADJUST_TARGET_AMOUNT,
    MERGE_GOALS,
    PAUSE_GOAL
}

data class GoalAdjustment(
    val goalId: String,
    val field: String,
    val oldValue: String,
    val newValue: String
)

/**
 * Represents a goal achievement
 */
data class GoalAchievement(
    val goalId: String,
    val achievementType: AchievementType,
    val achievedAt: kotlinx.datetime.Instant,
    val celebrationData: GoalCelebration
)

enum class AchievementType {
    GOAL_COMPLETED,
    MILESTONE_REACHED,
    STREAK_ACHIEVED,
    EARLY_COMPLETION
}

/**
 * Represents celebration data for goal achievements
 */
data class GoalCelebration(
    val goalId: String,
    val celebrationType: CelebrationType,
    val title: String,
    val message: String,
    val animationType: String,
    val soundEffect: String?,
    val badgeEarned: String?,
    val pointsAwarded: Int,
    val nextSteps: List<NextStepSuggestion>
)

enum class CelebrationType {
    CONFETTI,
    FIREWORKS,
    SPARKLES,
    TROPHY,
    BADGE_UNLOCK
}

/**
 * Represents next step suggestions after goal achievement
 */
data class NextStepSuggestion(
    val id: String,
    val type: NextStepType,
    val title: String,
    val description: String,
    val actionText: String,
    val priority: Priority
)

enum class NextStepType {
    CREATE_NEW_GOAL,
    INCREASE_TARGET,
    ACCELERATE_TIMELINE,
    DIVERSIFY_SAVINGS,
    CELEBRATE_ACHIEVEMENT
}

/**
 * Represents comprehensive goal insights
 */
data class GoalInsights(
    val userId: String,
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val totalTargetAmount: Money,
    val totalCurrentAmount: Money,
    val overallProgress: Double,
    val goalsOnTrack: Int,
    val goalsOffTrack: Int,
    val averageCompletionTime: Double, // in days
    val successRate: Double, // percentage
    val topPerformingGoalTypes: List<String>,
    val insights: List<String>
)

/**
 * Represents goal recommendations
 */
data class GoalRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority,
    val estimatedImpact: String,
    val actionRequired: String
)

enum class RecommendationType {
    ADJUST_TARGET_AMOUNT,
    EXTEND_TIMELINE,
    INCREASE_CONTRIBUTIONS,
    MERGE_SIMILAR_GOALS,
    CREATE_EMERGENCY_FUND,
    OPTIMIZE_PRIORITY_ORDER
}