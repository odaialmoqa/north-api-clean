package com.north.mobile.di

import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.api.FinancialApiService
import com.north.mobile.data.api.InsightsApiService
import com.north.mobile.data.api.TransactionAnalysisService
import com.north.mobile.data.repository.AuthRepository
import com.north.mobile.data.repository.InsightsRepository
import com.north.mobile.data.service.FinancialAnalysisService
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
    single { InsightsApiService(get()) }
    single { TransactionAnalysisService(get()) }
    
    // Repositories
    single { AuthRepository(get()) }
    single { InsightsRepository(get(), get()) }
    
    // Services
    single { FinancialAnalysisService(get(), get()) }
}