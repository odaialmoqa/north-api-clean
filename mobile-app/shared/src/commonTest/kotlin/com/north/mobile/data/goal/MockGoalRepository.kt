package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock

/**
 * Mock implementation of GoalRepository for testing
 */
class MockGoalRepository : GoalRepository {
    
    private val goals = mutableMapOf<String, FinancialGoal>()
    private val microTasks = mutableMapOf<String, MicroTask>()
    private val goalsFlow = MutableStateFlow<List<FinancialGoal>>(emptyList())
    
    fun insertGoal(goal: FinancialGoal) {
        goals[goal.id] = goal
        updateFlow()
    }
    
    fun insertMicroTask(microTask: MicroTask) {
        microTasks[microTask.id] = microTask
    }
    
    private fun updateFlow() {
        goalsFlow.value = goals.values.toList()
    }

    override suspend fun insert(entity: FinancialGoal): Result<FinancialGoal> {
        goals[entity.id] = entity
        updateFlow()
        return Result.success(entity)
    }

    override suspend fun update(entity: FinancialGoal): Result<FinancialGoal> {
        goals[entity.id] = entity
        updateFlow()
        return Result.success(entity)
    }

    override suspend fun delete(id: String): Result<Unit> {
        goals.remove(id)
        microTasks.values.removeAll { it.goalId == id }
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun findById(id: String): Result<FinancialGoal?> {
        val goal = goals[id]
        val goalMicroTasks = microTasks.values.filter { it.goalId == id }
        val goalWithMicroTasks = goal?.copy(microTasks = goalMicroTasks)
        return Result.success(goalWithMicroTasks)
    }

    override suspend fun findAll(): Result<List<FinancialGoal>> {
        val goalsWithMicroTasks = goals.values.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(goalsWithMicroTasks)
    }

    override suspend fun findByUserId(userId: String): Result<List<FinancialGoal>> {
        val userGoals = goals.values.filter { it.userId == userId }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(userGoals)
    }

    override suspend fun findActiveByUserId(userId: String): Result<List<FinancialGoal>> {
        val activeGoals = goals.values.filter { it.userId == userId && it.isActive }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(activeGoals)
    }

    override suspend fun findByUserIdAndType(userId: String, goalType: GoalType): Result<List<FinancialGoal>> {
        val filteredGoals = goals.values.filter { 
            it.userId == userId && it.goalType == goalType 
        }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(filteredGoals)
    }

    override suspend fun findByUserIdAndPriority(userId: String, priority: Priority): Result<List<FinancialGoal>> {
        val filteredGoals = goals.values.filter { 
            it.userId == userId && it.priority == priority 
        }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(filteredGoals)
    }

    override suspend fun findGoalsWithDeadlinesBefore(userId: String, date: LocalDate): Result<List<FinancialGoal>> {
        val filteredGoals = goals.values.filter { 
            it.userId == userId && it.targetDate <= date 
        }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(filteredGoals)
    }

    override suspend fun findCompletedGoals(userId: String): Result<List<FinancialGoal>> {
        val completedGoals = goals.values.filter { 
            it.userId == userId && it.isCompleted 
        }.map { goal ->
            val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
            goal.copy(microTasks = goalMicroTasks)
        }
        return Result.success(completedGoals)
    }

    override suspend fun updateProgress(goalId: String, currentAmount: Money): Result<FinancialGoal> {
        val goal = goals[goalId] ?: return Result.failure(Exception("Goal not found"))
        val updatedGoal = goal.copy(currentAmount = currentAmount)
        goals[goalId] = updatedGoal
        updateFlow()
        return Result.success(updatedGoal)
    }

    override suspend fun deactivateGoal(goalId: String): Result<Unit> {
        val goal = goals[goalId] ?: return Result.failure(Exception("Goal not found"))
        goals[goalId] = goal.copy(isActive = false)
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun reactivateGoal(goalId: String): Result<Unit> {
        val goal = goals[goalId] ?: return Result.failure(Exception("Goal not found"))
        goals[goalId] = goal.copy(isActive = true)
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun insertMicroTask(microTask: MicroTask): Result<MicroTask> {
        microTasks[microTask.id] = microTask
        return Result.success(microTask)
    }

    override suspend fun updateMicroTask(microTask: MicroTask): Result<MicroTask> {
        microTasks[microTask.id] = microTask
        return Result.success(microTask)
    }

    override suspend fun deleteMicroTask(microTaskId: String): Result<Unit> {
        microTasks.remove(microTaskId)
        return Result.success(Unit)
    }

    override suspend fun findMicroTasksByGoalId(goalId: String): Result<List<MicroTask>> {
        val goalMicroTasks = microTasks.values.filter { it.goalId == goalId }
        return Result.success(goalMicroTasks)
    }

    override suspend fun findMicroTaskById(microTaskId: String): Result<MicroTask?> {
        return Result.success(microTasks[microTaskId])
    }

    override suspend fun completeMicroTask(microTaskId: String, completedAt: kotlinx.datetime.Instant): Result<MicroTask> {
        val microTask = microTasks[microTaskId] ?: return Result.failure(Exception("MicroTask not found"))
        val completedTask = microTask.copy(isCompleted = true, completedAt = completedAt)
        microTasks[microTaskId] = completedTask
        return Result.success(completedTask)
    }

    override suspend fun insertGoalsWithMicroTasks(goals: List<FinancialGoal>): Result<List<FinancialGoal>> {
        goals.forEach { goal ->
            this.goals[goal.id] = goal
            goal.microTasks.forEach { microTask ->
                microTasks[microTask.id] = microTask
            }
        }
        updateFlow()
        return Result.success(goals)
    }

    override suspend fun updateMultipleGoals(goals: List<FinancialGoal>): Result<List<FinancialGoal>> {
        goals.forEach { goal ->
            this.goals[goal.id] = goal
        }
        updateFlow()
        return Result.success(goals)
    }

    override suspend fun getGoalStatistics(userId: String): Result<GoalStatistics> {
        val userGoals = goals.values.filter { it.userId == userId }
        val activeGoals = userGoals.filter { it.isActive }
        val completedGoals = userGoals.filter { it.isCompleted }
        
        val totalAmountSaved = completedGoals.fold(Money.zero()) { acc, goal ->
            acc + goal.currentAmount
        }
        
        val averageCompletionTime = if (completedGoals.isNotEmpty()) {
            completedGoals.map { 30.0 }.average() // Mock 30 days average
        } else 0.0
        
        val successRate = if (userGoals.isNotEmpty()) {
            (completedGoals.size.toDouble() / userGoals.size.toDouble()) * 100.0
        } else 0.0
        
        val goalTypeDistribution = userGoals.groupBy { it.goalType }
            .mapValues { it.value.size }
        
        val priorityDistribution = userGoals.groupBy { it.priority }
            .mapValues { it.value.size }
        
        val statistics = GoalStatistics(
            userId = userId,
            totalGoals = userGoals.size,
            activeGoals = activeGoals.size,
            completedGoals = completedGoals.size,
            averageCompletionTimeInDays = averageCompletionTime,
            successRate = successRate,
            totalAmountSaved = totalAmountSaved,
            goalTypeDistribution = goalTypeDistribution,
            priorityDistribution = priorityDistribution
        )
        
        return Result.success(statistics)
    }

    override suspend fun getGoalCompletionHistory(userId: String, limit: Int): Result<List<GoalCompletionRecord>> {
        val completedGoals = goals.values.filter { it.userId == userId && it.isCompleted }
            .take(limit)
        
        val records = completedGoals.map { goal ->
            GoalCompletionRecord(
                goalId = goal.id,
                title = goal.title,
                goalType = goal.goalType,
                targetAmount = goal.targetAmount,
                completedAt = Clock.System.now(),
                daysToComplete = 30L, // Mock value
                microTasksCompleted = goal.microTasks.count { it.isCompleted }
            )
        }
        
        return Result.success(records)
    }

    override fun observeGoalsByUserId(userId: String): Flow<List<FinancialGoal>> {
        return goalsFlow.map { allGoals ->
            allGoals.filter { it.userId == userId }.map { goal ->
                val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
                goal.copy(microTasks = goalMicroTasks)
            }
        }
    }

    override fun observeGoalProgress(goalId: String): Flow<FinancialGoal?> {
        return goalsFlow.map { allGoals ->
            allGoals.find { it.id == goalId }?.let { goal ->
                val goalMicroTasks = microTasks.values.filter { it.goalId == goal.id }
                goal.copy(microTasks = goalMicroTasks)
            }
        }
    }

    override fun observeMicroTasks(goalId: String): Flow<List<MicroTask>> {
        return goalsFlow.map { 
            microTasks.values.filter { it.goalId == goalId }
        }
    }
}