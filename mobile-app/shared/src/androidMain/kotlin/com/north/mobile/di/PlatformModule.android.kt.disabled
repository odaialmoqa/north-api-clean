package com.north.mobile.di

import com.north.mobile.data.auth.AndroidAuthenticationManager
import com.north.mobile.data.auth.AuthenticationManager
import com.north.mobile.data.database.AndroidDatabaseDriverFactory
import com.north.mobile.data.database.DatabaseDriverFactory
import com.north.mobile.data.security.AndroidEncryptionManager
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.database.NorthDatabase
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Encryption Manager
    single<EncryptionManager> { AndroidEncryptionManager(androidContext()) }
    
    // Authentication Manager
    single<AuthenticationManager> { AndroidAuthenticationManager(androidContext(), get()) }
    
    // Database Driver Factory
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    
    // Database
    single<NorthDatabase> { 
        val driverFactory = get<DatabaseDriverFactory>()
        val encryptionManager = get<EncryptionManager>()
        val driver = runBlocking { driverFactory.createDriver(encryptionManager) }
        NorthDatabase(driver)
    }
}