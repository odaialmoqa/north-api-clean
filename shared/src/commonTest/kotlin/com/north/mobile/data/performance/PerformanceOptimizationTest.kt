package com.north.mobile.data.performance

import com.north.mobile.data.cache.FinancialDataCache
import com.north.mobile.data.cache.FinancialDataCacheImpl
import com.north.mobile.data.cache.CacheType
import com.north.mobile.data.repository.LazyTransactionRepository
import com.north.mobile.data.repository.LazyTransactionRepositoryImpl
import com.north.mobile.data.sync.BatteryOptimizedSyncManager
import com.north.mobile.data.sync.BatteryOptimizedSyncManagerImpl
import com.north.mobile.data.sync.SyncTask
import com.north.mobile.data.sync.SyncTaskType
import com.north.mobile.data.sync.SyncPriority
import com.north.mobile.data.sync.SyncResult
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

class PerformanceOptimizationTest {
    
    private lateinit var financialDataCache: FinancialDataCache
    private lateinit var memoryManager: MemoryManager
    private lateinit var batteryOptimizedSyncManager: BatteryOptimizedSyncManager
    
    @BeforeTest
    fun setup() {
        financialDataCache = FinancialDataCacheImpl()
        memoryManager = MemoryManagerImpl()
        batteryOptimizedSyncManager = BatteryOptimizedSyncManagerImpl()
    }
    
    @Test
    fun testFinancialDataCaching() = runTest {
        // Test account caching
        val account = Account(
            id = "acc1",
            userId = "user1",
            institutionId = "inst1",
            accountType = AccountType.CHECKING,
            accountNumber = "****1234",
            balance = Money(1000.0, Currency.CAD),
            availableBalance = Money(950.0, Currency.CAD),
            currency = Currency.CAD,
            isActive = true,
            lastUpdated = Clock.System.now()
        )
        
        // Cache account
        financialDataCache.cacheAccount(account)
        
        // Retrieve cached account
        val cachedAccount = financialDataCache.getAccount("acc1")
        assertNotNull(cachedAccount)
        assertEquals(account.id, cachedAccount.id)
        assertEquals(account.balance, cachedAccount.balance)
        
        // Test transaction caching
        val transaction = Transaction(
            id = "txn1",
            accountId = "acc1",
            amount = Money(-50.0, Currency.CAD),
            description = "Coffee Shop",
            category = Category.FOOD,
            date = LocalDate(2024, 1, 15)
        )
        
        financialDataCache.cacheTransaction(transaction)
        val cachedTransaction = financialDataCache.getTransaction("txn1")
        assertNotNull(cachedTransaction)
        assertEquals(transaction.id, cachedTransaction.id)
        
        // Test net worth caching
        val netWorth = Money(5000.0, Currency.CAD)
        financialDataCache.cacheNetWorth("user1", netWorth)
        val cachedNetWorth = financialDataCache.getNetWorth("user1")
        assertNotNull(cachedNetWorth)
        assertEquals(netWorth.amount, cachedNetWorth.amount)
        
        // Test cache stats
        val stats = financialDataCache.getCacheStats()
        assertTrue(stats.accountsCount > 0)
        assertTrue(stats.transactionsCount > 0)
        assertTrue(stats.netWorthCount > 0)
        assertTrue(stats.totalMemoryUsage > 0)
    }
    
    @Test
    fun testCacheInvalidation() = runTest {
        // Cache some data
        val account = Account(
            id = "acc1",
            userId = "user1",
            institutionId = "inst1",
            accountType = AccountType.CHECKING,
            accountNumber = "****1234",
            balance = Money(1000.0, Currency.CAD),
            availableBalance = Money(950.0, Currency.CAD),
            currency = Currency.CAD,
            isActive = true,
            lastUpdated = Clock.System.now()
        )
        
        financialDataCache.cacheAccount(account)
        assertNotNull(financialDataCache.getAccount("acc1"))
        
        // Invalidate accounts cache
        financialDataCache.invalidateCache(CacheType.ACCOUNTS)
        assertNull(financialDataCache.getAccount("acc1"))
        
        // Test clearing all cache
        financialDataCache.cacheAccount(account)
        financialDataCache.invalidateCache(CacheType.ALL)
        assertNull(financialDataCache.getAccount("acc1"))
    }
    
    @Test
    fun testMemoryManagement() = runTest {
        // Track objects
        val testObject1 = "Test String 1"
        val testObject2 = listOf(1, 2, 3, 4, 5)
        
        memoryManager.trackObject("obj1", testObject1)
        memoryManager.trackObject("obj2", testObject2)
        
        val stats = memoryManager.getMemoryStats()
        assertEquals(2, stats.trackedObjects)
        assertTrue(stats.estimatedMemoryUsage > 0)
        
        // Release object
        memoryManager.releaseObject("obj1")
        val updatedStats = memoryManager.getMemoryStats()
        assertEquals(1, updatedStats.trackedObjects)
        
        // Test garbage collection
        memoryManager.forceGarbageCollection()
        val gcStats = memoryManager.getMemoryStats()
        assertTrue(gcStats.gcCount > 0)
    }
    
    @Test
    fun testMemoryLeakDetection() = runTest {
        // This test would need to simulate time passing to detect leaks
        val testObject = "Potential leak object"
        memoryManager.trackObject("leak_obj", testObject)
        
        // In a real scenario, we'd wait for the leak threshold time
        // For testing, we can check that the object is tracked
        val stats = memoryManager.getMemoryStats()
        assertEquals(1, stats.trackedObjects)
        
        // Check for potential leaks (would be 0 initially)
        val leaks = memoryManager.getMemoryLeaks()
        assertEquals(0, leaks.size) // No leaks initially
    }
    
    @Test
    fun testBatteryOptimizedSync() = runTest {
        var syncExecuted = false
        
        val syncTask = SyncTask(
            id = "test_sync",
            type = SyncTaskType.ACCOUNT_BALANCE,
            priority = SyncPriority.NORMAL,
            interval = 1.minutes,
            requiresWifi = true,
            requiresCharging = false,
            executor = {
                syncExecuted = true
                SyncResult(success = true, message = "Sync completed")
            }
        )
        
        // Schedule sync task
        batteryOptimizedSyncManager.scheduleSyncTask(syncTask)
        
        val syncStatus = batteryOptimizedSyncManager.getSyncStatus().value
        assertTrue(syncStatus.isActive)
        assertTrue(syncStatus.batteryOptimized)
        assertTrue(syncStatus.wifiRequired)
        assertFalse(syncStatus.chargingRequired)
    }
    
    @Test
    fun testBatteryOptimizationSettings() = runTest {
        val settings = batteryOptimizedSyncManager.getBatteryOptimizationSettings()
        assertTrue(settings.enableBatteryOptimization)
        assertTrue(settings.syncOnlyOnWifi)
        assertFalse(settings.syncOnlyWhenCharging)
        
        // Update settings
        val newSettings = settings.copy(
            syncOnlyWhenCharging = true,
            lowBatteryThreshold = 15
        )
        
        batteryOptimizedSyncManager.updateBatteryOptimizationSettings(newSettings)
        val updatedSettings = batteryOptimizedSyncManager.getBatteryOptimizationSettings()
        assertTrue(updatedSettings.syncOnlyWhenCharging)
        assertEquals(15, updatedSettings.lowBatteryThreshold)
    }
    
    @Test
    fun testSyncPauseAndResume() = runTest {
        // Pause all sync
        batteryOptimizedSyncManager.pauseAllSync()
        val pausedStatus = batteryOptimizedSyncManager.getSyncStatus().value
        assertFalse(pausedStatus.isActive)
        
        // Resume sync
        batteryOptimizedSyncManager.resumeAllSync()
        val resumedStatus = batteryOptimizedSyncManager.getSyncStatus().value
        assertTrue(resumedStatus.isActive)
    }
    
    @Test
    fun testDisposableResourceManager() {
        val resourceManager = DisposableResourceManager()
        var disposed = false
        
        val testResource = object : DisposableResourceManager.DisposableResource {
            override fun dispose() {
                disposed = true
            }
        }
        
        resourceManager.addResource("test_resource", testResource)
        assertEquals(1, resourceManager.getResourceCount())
        
        resourceManager.removeResource("test_resource")
        assertTrue(disposed)
        assertEquals(0, resourceManager.getResourceCount())
    }
    
    @Test
    fun testWeakReferenceHolder() {
        val weakHolder = WeakReferenceHolder<String>()
        val testString = "Test String"
        
        weakHolder.put("test_key", testString)
        assertEquals(1, weakHolder.size())
        
        val retrieved = weakHolder.get("test_key")
        assertEquals(testString, retrieved)
        
        weakHolder.remove("test_key")
        assertEquals(0, weakHolder.size())
    }
    
    @Test
    fun testScopeManager() {
        val scopeManager = ScopeManager()
        
        val scope = scopeManager.createScope("test_scope")
        assertNotNull(scope)
        assertEquals(1, scopeManager.getScopeCount())
        
        val retrievedScope = scopeManager.getScope("test_scope")
        assertEquals(scope, retrievedScope)
        
        scopeManager.cancelScope("test_scope")
        assertEquals(0, scopeManager.getScopeCount())
    }
}