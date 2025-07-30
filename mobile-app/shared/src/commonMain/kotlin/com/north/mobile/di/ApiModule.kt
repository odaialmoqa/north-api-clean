package com.north.mobile.di

import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.api.FinancialApiService
import com.north.mobile.data.repository.AuthRepository
import org.koin.dsl.module

/**
 * Dependency injection module for API services and repositories
 */
val apiModule = module {
    
    // API Client
    single { ApiClient() }
    
    // API Services
    single { AuthApiService(get()) }
    single { FinancialApiService(get()) }
    
    // Repositories
    single { AuthRepository(get()) }
    // TODO: Re-enable FinancialRepository after build fix
    // single { FinancialRepository(get(), get()) }
}