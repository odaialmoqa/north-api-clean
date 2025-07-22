package com.north.mobile.data.goal

import com.north.mobile.data.repository.Repository
import com.north.mobile.domain.model.FinancialGoal
import com.north.mobile.domain.model.MicroTask
import com.north.mobile.domain.model.GoalType
import com.north.mobile.domain.model.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for goal data access
 */
interface GoalRepository : Repository<FinancialGoal, String> {
    
    // Goal queries
    suspend fun findByUserId(userId: String): Result<List<FinancialGoal>>
    suspend fun findActiveByUserId(userId: String): Result<List<FinancialGoal>>
    suspend fun findByUserIdAndType(userId: String, goalType: GoalType): Result<List<FinancialGoal>>
    suspend fun findByUserIdAndPriority(userId: String, priority: Priority): Result<List<FinancialGoal>>
    suspend fun findGoalsWithDeadlinesBefore(userId: String, date: LocalDate): Result<List<FinancialGoal>>
    suspend fun findCompletedGoals(userId: String): Result<List<FinancialGoal>>
    
    // Goal progress updates
    suspend fun updateProgress(goalId: String, currentAmount: com.north.mobile.domain.model.Money): Result<FinancialGoal>
    suspend fun deactivateGoal(goalId: String): Result<Unit>
    suspend fun reactivateGoal(goalId: String): Result<Unit>
    
    // Micro-task operations
    suspend fun insertMicroTask(microTask: MicroTask): Result<MicroTask>
    suspend fun updateMicroTask(microTask: MicroTask): Result<MicroTask>
    suspend fun deleteMicroTask(microTaskId: String): Result<Unit>
    suspend fun findMicroTasksByGoalId(goalId: String): Result<List<MicroTask>>
    suspend fun findMicroTaskById(microTaskId: String): Result<MicroTask?>
    suspend fun completeMicroTask(microTaskId: String, completedAt: kotlinx.datetime.Instant): Result<MicroTask>
    
    // Batch operations
    suspend fun insertGoalsWithMicroTasks(goals: List<FinancialGoal>): Result<List<FinancialGoal>>
    suspend fun updateMultipleGoals(goals: List<FinancialGoal>): Result<List<FinancialGoal>>
    
    // Analytics queries
    suspend fun getGoalStatistics(userId: String): Result<GoalStatistics>
    suspend fun getGoalCompletionHistory(userId: String, limit: Int = 10): Result<List<GoalCompletionRecord>>
    
    // Real-time updates
    fun observeGoalsByUserId(userId: String): Flow<List<FinancialGoal>>
    fun observeGoalProgress(goalId: String): Flow<FinancialGoal?>
    fun observeMicroTasks(goalId: String): Flow<List<MicroTask>>
}

/**
 * Goal statistics for analytics
 */
data class GoalStatistics(
    val userId: String,
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val averageCompletionTimeInDays: Double,
    val successRate: Double,
    val totalAmountSaved: com.north.mobile.domain.model.Money,
    val goalTypeDistribution: Map<GoalType, Int>,
    val priorityDistribution: Map<Priority, Int>
)

/**
 * Goal completion record for history tracking
 */
data class GoalCompletionRecord(
    val goalId: String,
    val title: String,
    val goalType: GoalType,
    val targetAmount: com.north.mobile.domain.model.Money,
    val completedAt: kotlinx.datetime.Instant,
    val daysToComplete: Long,
    val microTasksCompleted: Int
)