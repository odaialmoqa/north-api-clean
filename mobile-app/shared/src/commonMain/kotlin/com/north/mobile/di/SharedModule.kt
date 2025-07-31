package com.north.mobile.di

// import com.north.mobile.data.auth.SessionManager
// import com.north.mobile.data.auth.SessionManagerImpl
// import com.north.mobile.data.database.DatabaseMigrationManager
// import com.north.mobile.data.repository.UserRepository
// import com.north.mobile.data.repository.UserRepositoryImpl
// import com.north.mobile.data.repository.AccountRepository
// import com.north.mobile.data.repository.AccountRepositoryImpl
import com.north.mobile.data.plaid.PlaidService
import com.north.mobile.data.api.ApiClient
// import com.north.mobile.data.plaid.AccountLinkingManager
// import com.north.mobile.data.plaid.AccountLinkingManagerImpl
import com.north.mobile.data.plaid.PlaidEnvironment
import com.north.mobile.config.PlaidConfig
// import com.north.mobile.database.NorthDatabase
// import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    // Include API module
    // includes(apiModule)
    // Settings
    // single<Settings> { Settings() }
    
    // HTTP Client for API calls
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
    
    // Database Migration Manager
    // single { DatabaseMigrationManager() }
    
    // Database - will be provided by platform-specific modules
    // single<NorthDatabase> { ... }
    
    // Authentication and Session Management
    // single<SessionManager> { SessionManagerImpl(get(), get()) }
    
    // Repositories
    // single<UserRepository> { UserRepositoryImpl(get(), get()) }
    // single<AccountRepository> { AccountRepositoryImpl(get()) }
    
    // API Client
    single<ApiClient> { ApiClient() }
    
    // Plaid Integration
    single<PlaidService> { 
        PlaidService(get())
    }
    
    // single<AccountLinkingManager> { 
    //     AccountLinkingManagerImpl(
    //         plaidService = get(),
    //         accountRepository = get(),
    //         secureStorage = get()
    //     )
    // }
}

// Platform-specific modules will extend this
expect val platformModule: Module