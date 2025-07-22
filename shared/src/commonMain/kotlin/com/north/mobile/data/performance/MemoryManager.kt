package com.north.mobile.data.performance

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.minutes

/**
 * Memory management and leak detection system
 */
interface MemoryManager {
    fun trackObject(key: String, obj: Any)
    fun releaseObject(key: String)
    fun forceGarbageCollection()
    fun getMemoryStats(): MemoryStats
    fun startMemoryMonitoring()
    fun stopMemoryMonitoring()
    fun getMemoryLeaks(): List<MemoryLeak>
}

data class MemoryStats(
    val trackedObjects: Int,
    val estimatedMemoryUsage: Long,
    val gcCount: Int,
    val potentialLeaks: Int
)

data class MemoryLeak(
    val key: String,
    val className: String,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val estimatedSize: Long
)

class MemoryManagerImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : MemoryManager {
    
    private val trackedObjects = mutableMapOf<String, TrackedObject>()
    private val _memoryStats = MutableStateFlow(MemoryStats(0, 0L, 0, 0))
    val memoryStats: StateFlow<MemoryStats> = _memoryStats.asStateFlow()
    
    private var monitoringJob: Job? = null
    private var gcCount = 0
    
    data class TrackedObject(
        val obj: Any,
        val className: String,
        val createdAt: Long,
        var lastAccessedAt: Long,
        val estimatedSize: Long
    )
    
    companion object {
        private const val MEMORY_CHECK_INTERVAL = 30_000L // 30 seconds
        private const val LEAK_THRESHOLD_MS = 300_000L // 5 minutes
    }
    
    override fun trackObject(key: String, obj: Any) {
        val now = System.currentTimeMillis()
        trackedObjects[key] = TrackedObject(
            obj = obj,
            className = obj::class.simpleName ?: "Unknown",
            createdAt = now,
            lastAccessedAt = now,
            estimatedSize = estimateObjectSize(obj)
        )
        updateMemoryStats()
    }
    
    override fun releaseObject(key: String) {
        trackedObjects.remove(key)
        updateMemoryStats()
    }
    
    override fun forceGarbageCollection() {
        // Platform-specific GC implementation would go here
        // For now, we'll simulate it
        gcCount++
        cleanupExpiredObjects()
        updateMemoryStats()
    }
    
    override fun getMemoryStats(): MemoryStats {
        return _memoryStats.value
    }
    
    override fun startMemoryMonitoring() {
        stopMemoryMonitoring() // Stop existing monitoring
        
        monitoringJob = scope.launch {
            while (isActive) {
                delay(MEMORY_CHECK_INTERVAL)
                performMemoryCheck()
            }
        }
    }
    
    override fun stopMemoryMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    override fun getMemoryLeaks(): List<MemoryLeak> {
        val now = System.currentTimeMillis()
        return trackedObjects.entries
            .filter { (_, tracked) -> 
                now - tracked.lastAccessedAt > LEAK_THRESHOLD_MS 
            }
            .map { (key, tracked) ->
                MemoryLeak(
                    key = key,
                    className = tracked.className,
                    createdAt = tracked.createdAt,
                    lastAccessedAt = tracked.lastAccessedAt,
                    estimatedSize = tracked.estimatedSize
                )
            }
    }
    
    private fun performMemoryCheck() {
        cleanupExpiredObjects()
        updateMemoryStats()
        
        val leaks = getMemoryLeaks()
        if (leaks.isNotEmpty()) {
            // Log potential memory leaks
            println("Potential memory leaks detected: ${leaks.size}")
            leaks.forEach { leak ->
                println("Leak: ${leak.key} (${leak.className}) - ${leak.estimatedSize} bytes")
            }
        }
    }
    
    private fun cleanupExpiredObjects() {
        val now = System.currentTimeMillis()
        val expiredKeys = trackedObjects.entries
            .filter { (_, tracked) -> 
                now - tracked.lastAccessedAt > LEAK_THRESHOLD_MS * 2 // Double threshold for cleanup
            }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            trackedObjects.remove(key)
        }
    }
    
    private fun updateMemoryStats() {
        val totalMemory = trackedObjects.values.sumOf { it.estimatedSize }
        val potentialLeaks = getMemoryLeaks().size
        
        _memoryStats.value = MemoryStats(
            trackedObjects = trackedObjects.size,
            estimatedMemoryUsage = totalMemory,
            gcCount = gcCount,
            potentialLeaks = potentialLeaks
        )
    }
    
    private fun estimateObjectSize(obj: Any): Long {
        // Rough estimation based on object type
        return when (obj) {
            is String -> obj.length * 2L + 40L // 2 bytes per char + overhead
            is List<*> -> obj.size * 50L + 100L // Rough estimate for list overhead
            is Map<*, *> -> obj.size * 100L + 200L // Rough estimate for map overhead
            else -> 100L // Default estimate
        }
    }
}

/**
 * Memory-aware disposable resource manager
 */
class DisposableResourceManager {
    private val resources = mutableMapOf<String, DisposableResource>()
    
    interface DisposableResource {
        fun dispose()
    }
    
    fun addResource(key: String, resource: DisposableResource) {
        // Dispose existing resource if present
        resources[key]?.dispose()
        resources[key] = resource
    }
    
    fun removeResource(key: String) {
        resources.remove(key)?.dispose()
    }
    
    fun disposeAll() {
        resources.values.forEach { it.dispose() }
        resources.clear()
    }
    
    fun getResourceCount(): Int = resources.size
}

/**
 * Weak reference holder to prevent memory leaks
 */
class WeakReferenceHolder<T : Any> {
    private val references = mutableMapOf<String, java.lang.ref.WeakReference<T>>()
    
    fun put(key: String, value: T) {
        references[key] = java.lang.ref.WeakReference(value)
    }
    
    fun get(key: String): T? {
        val ref = references[key]
        val value = ref?.get()
        if (value == null) {
            references.remove(key) // Clean up dead reference
        }
        return value
    }
    
    fun remove(key: String) {
        references.remove(key)
    }
    
    fun clear() {
        references.clear()
    }
    
    fun cleanupDeadReferences() {
        val deadKeys = references.entries
            .filter { it.value.get() == null }
            .map { it.key }
        
        deadKeys.forEach { references.remove(it) }
    }
    
    fun size(): Int = references.size
}

/**
 * Coroutine scope manager to prevent job leaks
 */
class ScopeManager {
    private val scopes = mutableMapOf<String, CoroutineScope>()
    
    fun createScope(key: String, context: CoroutineContext = Dispatchers.Default): CoroutineScope {
        cancelScope(key) // Cancel existing scope if present
        val scope = CoroutineScope(context + SupervisorJob())
        scopes[key] = scope
        return scope
    }
    
    fun getScope(key: String): CoroutineScope? = scopes[key]
    
    fun cancelScope(key: String) {
        scopes.remove(key)?.coroutineContext?.get(Job)?.cancel()
    }
    
    fun cancelAllScopes() {
        scopes.values.forEach { scope ->
            scope.coroutineContext.get(Job)?.cancel()
        }
        scopes.clear()
    }
    
    fun getScopeCount(): Int = scopes.size
}