package com.north.mobile.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * Battery-optimized background sync manager
 */
interface BatteryOptimizedSyncManager {
    suspend fun scheduleSyncTask(task: SyncTask)
    suspend fun cancelSyncTask(taskId: String)
    suspend fun pauseAllSync()
    suspend fun resumeAllSync()
    fun getSyncStatus(): StateFlow<SyncStatus>
    fun getBatteryOptimizationSettings(): BatteryOptimizationSettings
    fun updateBatteryOptimizationSettings(settings: BatteryOptimizationSettings)
}

data class SyncTask(
    val id: String,
    val type: SyncTaskType,
    val priority: SyncPriority,
    val interval: Duration,
    val requiresWifi: Boolean = true,
    val requiresCharging: Boolean = false,
    val maxRetries: Int = 3,
    val backoffMultiplier: Double = 2.0,
    val executor: suspend () -> SyncResult
)

enum class SyncTaskType {
    ACCOUNT_BALANCE,
    TRANSACTION_HISTORY,
    CATEGORIZATION,
    INSIGHTS_GENERATION,
    GOAL_PROGRESS,
    NOTIFICATION_DELIVERY
}

enum class SyncPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

data class SyncResult(
    val success: Boolean,
    val message: String? = null,
    val nextSyncTime: Instant? = null,
    val dataUpdated: Boolean = false
)

data class SyncStatus(
    val isActive: Boolean,
    val activeTasks: List<String>,
    val lastSyncTime: Instant?,
    val nextSyncTime: Instant?,
    val batteryOptimized: Boolean,
    val wifiRequired: Boolean,
    val chargingRequired: Boolean
)

data class BatteryOptimizationSettings(
    val enableBatteryOptimization: Boolean = true,
    val syncOnlyWhenCharging: Boolean = false,
    val syncOnlyOnWifi: Boolean = true,
    val reducedSyncFrequency: Boolean = true,
    val pauseSyncOnLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20, // Percentage
    val maxConcurrentSyncs: Int = 2,
    val adaptiveSync: Boolean = true
)

class BatteryOptimizedSyncManagerImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : BatteryOptimizedSyncManager {
    
    private val scheduledTasks = mutableMapOf<String, ScheduledSyncTask>()
    private val activeSyncs = mutableMapOf<String, Job>()
    
    private var _syncStatus = MutableStateFlow(
        SyncStatus(
            isActive = true,
            activeTasks = emptyList(),
            lastSyncTime = null,
            nextSyncTime = null,
            batteryOptimized = true,
            wifiRequired = true,
            chargingRequired = false
        )
    )
    
    private var batterySettings = BatteryOptimizationSettings()
    private var isPaused = false
    private var batteryLevel = 100 // Mock battery level
    private var isCharging = false // Mock charging status
    private var isWifiConnected = true // Mock wifi status
    
    data class ScheduledSyncTask(
        val task: SyncTask,
        var nextExecutionTime: Instant,
        var retryCount: Int = 0,
        var lastResult: SyncResult? = null
    )
    
    override suspend fun scheduleSyncTask(task: SyncTask) {
        val scheduledTask = ScheduledSyncTask(
            task = task,
            nextExecutionTime = calculateNextExecutionTime(task)
        )
        
        scheduledTasks[task.id] = scheduledTask
        scheduleTaskExecution(scheduledTask)
        updateSyncStatus()
    }
    
    override suspend fun cancelSyncTask(taskId: String) {
        scheduledTasks.remove(taskId)
        activeSyncs[taskId]?.cancel()
        activeSyncs.remove(taskId)
        updateSyncStatus()
    }
    
    override suspend fun pauseAllSync() {
        isPaused = true
        activeSyncs.values.forEach { it.cancel() }
        activeSyncs.clear()
        updateSyncStatus()
    }
    
    override suspend fun resumeAllSync() {
        isPaused = false
        // Reschedule all tasks
        scheduledTasks.values.forEach { scheduledTask ->
            scheduleTaskExecution(scheduledTask)
        }
        updateSyncStatus()
    }
    
    override fun getSyncStatus(): StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    override fun getBatteryOptimizationSettings(): BatteryOptimizationSettings = batterySettings
    
    override fun updateBatteryOptimizationSettings(settings: BatteryOptimizationSettings) {
        batterySettings = settings
        
        // Reschedule tasks based on new settings
        if (settings.enableBatteryOptimization) {
            scope.launch {
                scheduledTasks.values.forEach { scheduledTask ->
                    scheduledTask.nextExecutionTime = calculateNextExecutionTime(scheduledTask.task)
                    scheduleTaskExecution(scheduledTask)
                }
            }
        }
        
        updateSyncStatus()
    }
    
    private fun scheduleTaskExecution(scheduledTask: ScheduledSyncTask) {
        if (isPaused) return
        
        val taskId = scheduledTask.task.id
        
        // Cancel existing job if any
        activeSyncs[taskId]?.cancel()
        
        val job = scope.launch {
            val delayTime = scheduledTask.nextExecutionTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
            if (delayTime > 0) {
                delay(delayTime)
            }
            
            if (shouldExecuteTask(scheduledTask.task)) {
                executeTask(scheduledTask)
            } else {
                // Reschedule for later
                scheduledTask.nextExecutionTime = calculateNextExecutionTime(scheduledTask.task)
                scheduleTaskExecution(scheduledTask)
            }
        }
        
        activeSyncs[taskId] = job
    }
    
    private suspend fun executeTask(scheduledTask: ScheduledSyncTask) {
        val task = scheduledTask.task
        
        try {
            // Limit concurrent syncs for battery optimization
            while (activeSyncs.size > batterySettings.maxConcurrentSyncs) {
                delay(1000) // Wait for other syncs to complete
            }
            
            val result = withTimeout(30_000) { // 30 second timeout
                task.executor()
            }
            
            scheduledTask.lastResult = result
            scheduledTask.retryCount = 0
            
            if (result.success) {
                // Schedule next execution
                scheduledTask.nextExecutionTime = result.nextSyncTime 
                    ?: calculateNextExecutionTime(task)
                scheduleTaskExecution(scheduledTask)
            } else {
                handleTaskFailure(scheduledTask)
            }
            
        } catch (e: Exception) {
            handleTaskFailure(scheduledTask, e)
        } finally {
            activeSyncs.remove(task.id)
            updateSyncStatus()
        }
    }
    
    private suspend fun handleTaskFailure(scheduledTask: ScheduledSyncTask, exception: Exception? = null) {
        val task = scheduledTask.task
        scheduledTask.retryCount++
        
        if (scheduledTask.retryCount <= task.maxRetries) {
            // Calculate backoff delay
            val backoffDelay = (task.interval.inWholeMilliseconds * 
                kotlin.math.pow(task.backoffMultiplier, scheduledTask.retryCount.toDouble())).toLong()
            
            scheduledTask.nextExecutionTime = Clock.System.now().plus(
                Duration.parse("${backoffDelay}ms")
            )
            
            scheduleTaskExecution(scheduledTask)
        } else {
            // Max retries reached, remove task or schedule for much later
            when (task.priority) {
                SyncPriority.CRITICAL -> {
                    // Keep trying critical tasks with longer intervals
                    scheduledTask.nextExecutionTime = Clock.System.now().plus(1.hours)
                    scheduledTask.retryCount = 0
                    scheduleTaskExecution(scheduledTask)
                }
                else -> {
                    // Remove non-critical tasks that keep failing
                    scheduledTasks.remove(task.id)
                }
            }
        }
    }
    
    private fun shouldExecuteTask(task: SyncTask): Boolean {
        if (!batterySettings.enableBatteryOptimization) {
            return true
        }
        
        // Check battery level
        if (batterySettings.pauseSyncOnLowBattery && 
            batteryLevel <= batterySettings.lowBatteryThreshold) {
            return false
        }
        
        // Check charging requirement
        if (batterySettings.syncOnlyWhenCharging && !isCharging) {
            return false
        }
        
        // Check wifi requirement
        if (batterySettings.syncOnlyOnWifi && !isWifiConnected) {
            return false
        }
        
        // Check task-specific requirements
        if (task.requiresCharging && !isCharging) {
            return false
        }
        
        if (task.requiresWifi && !isWifiConnected) {
            return false
        }
        
        return true
    }
    
    private fun calculateNextExecutionTime(task: SyncTask): Instant {
        val baseInterval = if (batterySettings.reducedSyncFrequency) {
            // Increase interval by 50% for battery optimization
            Duration.parse("${(task.interval.inWholeMilliseconds * 1.5).toLong()}ms")
        } else {
            task.interval
        }
        
        // Adaptive sync: adjust based on data change frequency
        val adaptiveMultiplier = if (batterySettings.adaptiveSync) {
            when (task.type) {
                SyncTaskType.ACCOUNT_BALANCE -> 1.0 // Keep frequent for balance updates
                SyncTaskType.TRANSACTION_HISTORY -> 1.2 // Slightly less frequent
                SyncTaskType.CATEGORIZATION -> 2.0 // Much less frequent
                SyncTaskType.INSIGHTS_GENERATION -> 3.0 // Least frequent
                SyncTaskType.GOAL_PROGRESS -> 1.5
                SyncTaskType.NOTIFICATION_DELIVERY -> 0.8 // More frequent for notifications
            }
        } else {
            1.0
        }
        
        val adjustedInterval = Duration.parse(
            "${(baseInterval.inWholeMilliseconds * adaptiveMultiplier).toLong()}ms"
        )
        
        return Clock.System.now().plus(adjustedInterval)
    }
    
    private fun updateSyncStatus() {
        val nextSyncTime = scheduledTasks.values
            .minByOrNull { it.nextExecutionTime }
            ?.nextExecutionTime
        
        val lastSyncTime = scheduledTasks.values
            .mapNotNull { it.lastResult }
            .maxByOrNull { Clock.System.now() } // This would need proper timestamp tracking
            ?.let { Clock.System.now() } // Placeholder
        
        _syncStatus.value = SyncStatus(
            isActive = !isPaused,
            activeTasks = activeSyncs.keys.toList(),
            lastSyncTime = lastSyncTime,
            nextSyncTime = nextSyncTime,
            batteryOptimized = batterySettings.enableBatteryOptimization,
            wifiRequired = batterySettings.syncOnlyOnWifi,
            chargingRequired = batterySettings.syncOnlyWhenCharging
        )
    }
    
    // Mock methods for battery and connectivity status
    fun updateBatteryLevel(level: Int) {
        batteryLevel = level
        if (batterySettings.pauseSyncOnLowBattery && level <= batterySettings.lowBatteryThreshold) {
            scope.launch { pauseAllSync() }
        }
    }
    
    fun updateChargingStatus(charging: Boolean) {
        isCharging = charging
        if (charging && isPaused) {
            scope.launch { resumeAllSync() }
        }
    }
    
    fun updateWifiStatus(connected: Boolean) {
        isWifiConnected = connected
    }
}