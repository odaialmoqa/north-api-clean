package com.north.mobile.di

import com.north.mobile.data.auth.AuthenticationManager
import com.north.mobile.data.auth.IOSAuthenticationManager
import com.north.mobile.data.database.DatabaseDriverFactory
import com.north.mobile.data.database.IOSDatabaseDriverFactory
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.data.security.IOSEncryptionManager
import com.north.mobile.data.plaid.PlaidLinkHandlerFactory
import com.north.mobile.data.plaid.IOSPlaidLinkHandlerFactory
import com.north.mobile.database.NorthDatabase
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Encryption Manager
    single<EncryptionManager> { IOSEncryptionManager() }
    
    // Authentication Manager
    single<AuthenticationManager> { IOSAuthenticationManager(get()) }
    
    // Database Driver Factory
    single<DatabaseDriverFactory> { IOSDatabaseDriverFactory() }
    
    // Plaid Link Handler Factory
    single<PlaidLinkHandlerFactory> { IOSPlaidLinkHandlerFactory() }
    
    // Database
    single<NorthDatabase> { 
        val driverFactory = get<DatabaseDriverFactory>()
        val encryptionManager = get<EncryptionManager>()
        val driver = runBlocking { driverFactory.createDriver(encryptionManager) }
        NorthDatabase(driver)
    }
    
    // iOS-specific services
    single { IOSPerformanceMonitor() }
    single { IOSNotificationManager() }
    single { IOSWidgetDataProvider() }
    single { IOSSiriShortcutsManager() }
}

// iOS-specific service interfaces and implementations
interface IOSPerformanceMonitor {
    fun startMonitoring()
    fun stopMonitoring()
    fun getCurrentMemoryUsage(): Double
    fun getCurrentCPUUsage(): Double
    fun getThermalState(): String
}

class IOSPerformanceMonitorImpl : IOSPerformanceMonitor {
    override fun startMonitoring() {
        // Implementation would integrate with Swift PerformanceMonitor
    }
    
    override fun stopMonitoring() {
        // Implementation would integrate with Swift PerformanceMonitor
    }
    
    override fun getCurrentMemoryUsage(): Double {
        // Implementation would get memory usage from iOS
        return 0.0
    }
    
    override fun getCurrentCPUUsage(): Double {
        // Implementation would get CPU usage from iOS
        return 0.0
    }
    
    override fun getThermalState(): String {
        // Implementation would get thermal state from iOS
        return "nominal"
    }
}

interface IOSNotificationManager {
    suspend fun scheduleNotification(title: String, body: String, delay: Long)
    suspend fun cancelAllNotifications()
    suspend fun requestPermissions(): Boolean
}

class IOSNotificationManagerImpl : IOSNotificationManager {
    override suspend fun scheduleNotification(title: String, body: String, delay: Long) {
        // Implementation would integrate with iOS notification system
    }
    
    override suspend fun cancelAllNotifications() {
        // Implementation would cancel iOS notifications
    }
    
    override suspend fun requestPermissions(): Boolean {
        // Implementation would request notification permissions
        return true
    }
}

interface IOSWidgetDataProvider {
    suspend fun updateWidgetData(netWorth: Double, monthlyChange: Double, currentStreak: Int, goalProgress: Double)
    suspend fun refreshAllWidgets()
}

class IOSWidgetDataProviderImpl : IOSWidgetDataProvider {
    override suspend fun updateWidgetData(netWorth: Double, monthlyChange: Double, currentStreak: Int, goalProgress: Double) {
        // Implementation would update widget data
    }
    
    override suspend fun refreshAllWidgets() {
        // Implementation would refresh all widgets
    }
}

interface IOSSiriShortcutsManager {
    suspend fun donateShortcut(identifier: String, title: String, subtitle: String)
    suspend fun removeShortcut(identifier: String)
    suspend fun removeAllShortcuts()
}

class IOSSiriShortcutsManagerImpl : IOSSiriShortcutsManager {
    override suspend fun donateShortcut(identifier: String, title: String, subtitle: String) {
        // Implementation would donate Siri shortcut
    }
    
    override suspend fun removeShortcut(identifier: String) {
        // Implementation would remove Siri shortcut
    }
    
    override suspend fun removeAllShortcuts() {
        // Implementation would remove all Siri shortcuts
    }
}

// Provide default implementations
fun IOSPerformanceMonitor(): IOSPerformanceMonitor = IOSPerformanceMonitorImpl()
fun IOSNotificationManager(): IOSNotificationManager = IOSNotificationManagerImpl()
fun IOSWidgetDataProvider(): IOSWidgetDataProvider = IOSWidgetDataProviderImpl()
fun IOSSiriShortcutsManager(): IOSSiriShortcutsManager = IOSSiriShortcutsManagerImpl()