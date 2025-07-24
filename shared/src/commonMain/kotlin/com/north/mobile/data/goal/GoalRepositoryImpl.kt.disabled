package com.north.mobile.data.goal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.north.mobile.data.database.NorthDatabase
import com.north.mobile.data.repository.RepositoryException
import com.north.mobile.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * SQLDelight implementation of GoalRepository
 */
class GoalRepositoryImpl(
    private val database: NorthDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : GoalRepository {

    override suspend fun insert(entity: FinancialGoal): Result<FinancialGoal> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            
            database.financialGoalQueries.insert(
                id = entity.id,
                userId = entity.userId,
                title = entity.title,
                description = entity.description,
                targetAmount = entity.targetAmount.amountInCents,
                currentAmount = entity.currentAmount.amountInCents,
                currency = entity.targetAmount.currency.currencyCode,
                targetDate = entity.targetDate.toEpochDays(),
                priority = entity.priority.sortOrder.toLong(),
                category = entity.goalType.name,
                isActive = if (entity.isActive) 1L else 0L,
                createdAt = entity.createdAt.toEpochMilliseconds(),
                updatedAt = now
            )
            
            // Insert micro tasks if any
            entity.microTasks.forEach { microTask ->
                insertMicroTaskInternal(microTask)
            }
            
            Result.success(entity.copy(createdAt = Instant.fromEpochMilliseconds(now)))
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert goal", e))
        }
    }

    override suspend fun update(entity: FinancialGoal): Result<FinancialGoal> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            
            database.financialGoalQueries.update(
                title = entity.title,
                description = entity.description,
                targetAmount = entity.targetAmount.amountInCents,
                currentAmount = entity.currentAmount.amountInCents,
                currency = entity.targetAmount.currency.currencyCode,
                targetDate = entity.targetDate.toEpochDays(),
                priority = entity.priority.sortOrder.toLong(),
                category = entity.goalType.name,
                isActive = if (entity.isActive) 1L else 0L,
                updatedAt = now,
                id = entity.id
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update goal", e))
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete micro tasks first
            database.transaction {
                database.microTaskQueries.deleteByGoalId(id)
                database.financialGoalQueries.delete(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to delete goal", e))
        }
    }

    override suspend fun findById(id: String): Result<FinancialGoal?> = withContext(Dispatchers.IO) {
        try {
            val goal = database.financialGoalQueries.selectById(id).executeAsOneOrNull()
            val result = goal?.let { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find goal by id", e))
        }
    }

    override suspend fun findAll(): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectAll().executeAsList()
            val result = goals.map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find all goals", e))
        }
    }

    override suspend fun findByUserId(userId: String): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectByUserId(userId).executeAsList()
            val result = goals.map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find goals by user id", e))
        }
    }

    override suspend fun findActiveByUserId(userId: String): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectActive(userId).executeAsList()
            val result = goals.map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find active goals", e))
        }
    }

    override suspend fun findByUserIdAndType(userId: String, goalType: GoalType): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectByCategory(userId, goalType.name).executeAsList()
            val result = goals.map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find goals by type", e))
        }
    }

    override suspend fun findByUserIdAndPriority(userId: String, priority: Priority): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectByUserId(userId).executeAsList()
            val result = goals.filter { it.priority == priority.sortOrder.toLong() }
                .map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find goals by priority", e))
        }
    }

    override suspend fun findGoalsWithDeadlinesBefore(userId: String, date: LocalDate): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectByUserId(userId).executeAsList()
            val result = goals.filter { 
                LocalDate.fromEpochDays(it.targetDate.toInt()) <= date 
            }.map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find goals with deadlines", e))
        }
    }

    override suspend fun findCompletedGoals(userId: String): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            val goals = database.financialGoalQueries.selectByUserId(userId).executeAsList()
            val result = goals.filter { it.currentAmount >= it.targetAmount }
                .map { mapToFinancialGoal(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find completed goals", e))
        }
    }

    override suspend fun updateProgress(goalId: String, currentAmount: Money): Result<FinancialGoal> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            database.financialGoalQueries.updateProgress(
                currentAmount = currentAmount.amountInCents,
                updatedAt = now,
                id = goalId
            )
            
            findById(goalId).getOrThrow()?.let { goal ->
                Result.success(goal)
            } ?: Result.failure(RepositoryException("Goal not found after update"))
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update goal progress", e))
        }
    }

    override suspend fun deactivateGoal(goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            database.financialGoalQueries.deactivate(updatedAt = now, id = goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to deactivate goal", e))
        }
    }

    override suspend fun reactivateGoal(goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // This would need a custom query to reactivate
            val goal = findById(goalId).getOrThrow()
            goal?.let { update(it.copy(isActive = true)) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to reactivate goal", e))
        }
    }

    override suspend fun insertMicroTask(microTask: MicroTask): Result<MicroTask> = withContext(Dispatchers.IO) {
        try {
            insertMicroTaskInternal(microTask)
            Result.success(microTask)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert micro task", e))
        }
    }

    override suspend fun updateMicroTask(microTask: MicroTask): Result<MicroTask> = withContext(Dispatchers.IO) {
        try {
            database.microTaskQueries.update(
                title = microTask.title,
                description = microTask.description,
                targetAmount = microTask.targetAmount.amountInCents,
                isCompleted = if (microTask.isCompleted) 1L else 0L,
                dueDate = microTask.dueDate?.toEpochDays(),
                completedAt = microTask.completedAt?.toEpochMilliseconds(),
                id = microTask.id
            )
            Result.success(microTask)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update micro task", e))
        }
    }

    override suspend fun deleteMicroTask(microTaskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.microTaskQueries.delete(microTaskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to delete micro task", e))
        }
    }

    override suspend fun findMicroTasksByGoalId(goalId: String): Result<List<MicroTask>> = withContext(Dispatchers.IO) {
        try {
            val microTasks = database.microTaskQueries.selectByGoalId(goalId).executeAsList()
            val result = microTasks.map { mapToMicroTask(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find micro tasks", e))
        }
    }

    override suspend fun findMicroTaskById(microTaskId: String): Result<MicroTask?> = withContext(Dispatchers.IO) {
        try {
            val microTask = database.microTaskQueries.selectById(microTaskId).executeAsOneOrNull()
            val result = microTask?.let { mapToMicroTask(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find micro task by id", e))
        }
    }

    override suspend fun completeMicroTask(microTaskId: String, completedAt: Instant): Result<MicroTask> = withContext(Dispatchers.IO) {
        try {
            database.microTaskQueries.complete(
                isCompleted = 1L,
                completedAt = completedAt.toEpochMilliseconds(),
                id = microTaskId
            )
            
            findMicroTaskById(microTaskId).getOrThrow()?.let { microTask ->
                Result.success(microTask)
            } ?: Result.failure(RepositoryException("Micro task not found after completion"))
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to complete micro task", e))
        }
    }

    override suspend fun insertGoalsWithMicroTasks(goals: List<FinancialGoal>): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                goals.forEach { goal ->
                    insert(goal).getOrThrow()
                }
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert goals with micro tasks", e))
        }
    }

    override suspend fun updateMultipleGoals(goals: List<FinancialGoal>): Result<List<FinancialGoal>> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                goals.forEach { goal ->
                    update(goal).getOrThrow()
                }
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update multiple goals", e))
        }
    }

    override suspend fun getGoalStatistics(userId: String): Result<GoalStatistics> = withContext(Dispatchers.IO) {
        try {
            val allGoals = findByUserId(userId).getOrThrow()
            val activeGoals = allGoals.filter { it.isActive }
            val completedGoals = allGoals.filter { it.isCompleted }
            
            val totalAmountSaved = completedGoals.fold(Money.zero()) { acc, goal ->
                acc + goal.currentAmount
            }
            
            val averageCompletionTime = if (completedGoals.isNotEmpty()) {
                completedGoals.map { goal ->
                    val createdDate = goal.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    (today.toEpochDays() - createdDate.toEpochDays()).toDouble()
                }.average()
            } else 0.0
            
            val successRate = if (allGoals.isNotEmpty()) {
                (completedGoals.size.toDouble() / allGoals.size.toDouble()) * 100.0
            } else 0.0
            
            val goalTypeDistribution = allGoals.groupBy { it.goalType }
                .mapValues { it.value.size }
            
            val priorityDistribution = allGoals.groupBy { it.priority }
                .mapValues { it.value.size }
            
            val statistics = GoalStatistics(
                userId = userId,
                totalGoals = allGoals.size,
                activeGoals = activeGoals.size,
                completedGoals = completedGoals.size,
                averageCompletionTimeInDays = averageCompletionTime,
                successRate = successRate,
                totalAmountSaved = totalAmountSaved,
                goalTypeDistribution = goalTypeDistribution,
                priorityDistribution = priorityDistribution
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to get goal statistics", e))
        }
    }

    override suspend fun getGoalCompletionHistory(userId: String, limit: Int): Result<List<GoalCompletionRecord>> = withContext(Dispatchers.IO) {
        try {
            val completedGoals = findCompletedGoals(userId).getOrThrow()
                .sortedByDescending { it.createdAt }
                .take(limit)
            
            val records = completedGoals.map { goal ->
                val createdDate = goal.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                val completedDate = Clock.System.todayIn(TimeZone.currentSystemDefault()) // Approximation
                val daysToComplete = completedDate.toEpochDays() - createdDate.toEpochDays()
                
                GoalCompletionRecord(
                    goalId = goal.id,
                    title = goal.title,
                    goalType = goal.goalType,
                    targetAmount = goal.targetAmount,
                    completedAt = Clock.System.now(), // Approximation
                    daysToComplete = daysToComplete,
                    microTasksCompleted = goal.microTasks.count { it.isCompleted }
                )
            }
            
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to get goal completion history", e))
        }
    }

    override fun observeGoalsByUserId(userId: String): Flow<List<FinancialGoal>> {
        return database.financialGoalQueries.selectByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { goals -> goals.map { mapToFinancialGoal(it) } }
    }

    override fun observeGoalProgress(goalId: String): Flow<FinancialGoal?> {
        return database.financialGoalQueries.selectById(goalId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { goal -> goal?.let { mapToFinancialGoal(it) } }
    }

    override fun observeMicroTasks(goalId: String): Flow<List<MicroTask>> {
        return database.microTaskQueries.selectByGoalId(goalId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { microTasks -> microTasks.map { mapToMicroTask(it) } }
    }

    private suspend fun insertMicroTaskInternal(microTask: MicroTask) {
        database.microTaskQueries.insert(
            id = microTask.id,
            goalId = microTask.goalId,
            title = microTask.title,
            description = microTask.description,
            targetAmount = microTask.targetAmount.amountInCents,
            isCompleted = if (microTask.isCompleted) 1L else 0L,
            dueDate = microTask.dueDate?.toEpochDays(),
            completedAt = microTask.completedAt?.toEpochMilliseconds(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    private suspend fun mapToFinancialGoal(dbGoal: com.north.mobile.database.FinancialGoal): FinancialGoal {
        val microTasks = findMicroTasksByGoalId(dbGoal.id).getOrElse { emptyList() }
        
        return FinancialGoal(
            id = dbGoal.id,
            userId = dbGoal.userId,
            title = dbGoal.title,
            description = dbGoal.description,
            targetAmount = Money.fromCents(dbGoal.targetAmount, Currency.valueOf(dbGoal.currency)),
            currentAmount = Money.fromCents(dbGoal.currentAmount, Currency.valueOf(dbGoal.currency)),
            targetDate = LocalDate.fromEpochDays(dbGoal.targetDate.toInt()),
            priority = Priority.values().find { it.sortOrder == dbGoal.priority.toInt() } ?: Priority.MEDIUM,
            goalType = GoalType.valueOf(dbGoal.category),
            microTasks = microTasks,
            isActive = dbGoal.isActive == 1L,
            createdAt = Instant.fromEpochMilliseconds(dbGoal.createdAt)
        )
    }

    private fun mapToMicroTask(dbMicroTask: com.north.mobile.database.MicroTask): MicroTask {
        return MicroTask(
            id = dbMicroTask.id,
            goalId = dbMicroTask.goalId,
            title = dbMicroTask.title,
            description = dbMicroTask.description,
            targetAmount = Money.fromCents(dbMicroTask.targetAmount, Currency.CAD),
            isCompleted = dbMicroTask.isCompleted == 1L,
            dueDate = dbMicroTask.dueDate?.let { LocalDate.fromEpochDays(it.toInt()) },
            completedAt = dbMicroTask.completedAt?.let { Instant.fromEpochMilliseconds(it) }
        )
    }
}