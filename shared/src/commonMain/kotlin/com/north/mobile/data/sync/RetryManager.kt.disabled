package com.north.mobile.data.sync

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Manages retry logic for failed sync operations
 */
interface RetryManager {
    /**
     * Execute an operation with retry logic
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Duration = 1.seconds,
        maxDelay: Duration = 30.seconds,
        backoffMultiplier: Double = 2.0,
        jitterRange: Double = 0.1,
        retryCondition: (Throwable) -> Boolean = ::defaultRetryCondition,
        operation: suspend () -> Result<T>
    ): Result<T>
    
    /**
     * Check if an error should trigger a retry
     */
    fun shouldRetry(error: Throwable, attempt: Int, maxAttempts: Int): Boolean
    
    /**
     * Calculate delay for next retry attempt
     */
    fun calculateDelay(
        attempt: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        backoffMultiplier: Double,
        jitterRange: Double
    ): Duration
}

class RetryManagerImpl : RetryManager {
    
    override suspend fun <T> withRetry(
        maxAttempts: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        backoffMultiplier: Double,
        jitterRange: Double,
        retryCondition: (Throwable) -> Boolean,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = operation()
                
                // If operation succeeded, return result
                if (result.isSuccess) {
                    return result
                }
                
                // If operation failed, check if we should retry
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    lastException = exception
                    
                    // Don't retry on last attempt
                    if (attempt == maxAttempts - 1) {
                        return result
                    }
                    
                    // Check if this error type should be retried
                    if (!retryCondition(exception)) {
                        return result
                    }
                    
                    // Calculate delay and wait before next attempt
                    val delay = calculateDelay(attempt, initialDelay, maxDelay, backoffMultiplier, jitterRange)
                    delay(delay)
                } else {
                    // No exception but failed result - don't retry
                    return result
                }
                
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on last attempt
                if (attempt == maxAttempts - 1) {
                    return Result.failure(e)
                }
                
                // Check if this error type should be retried
                if (!retryCondition(e)) {
                    return Result.failure(e)
                }
                
                // Calculate delay and wait before next attempt
                val delay = calculateDelay(attempt, initialDelay, maxDelay, backoffMultiplier, jitterRange)
                delay(delay)
            }
        }
        
        // This should never be reached, but just in case
        return Result.failure(lastException ?: RuntimeException("Max retry attempts exceeded"))
    }
    
    override fun shouldRetry(error: Throwable, attempt: Int, maxAttempts: Int): Boolean {
        if (attempt >= maxAttempts) return false
        
        return when (error) {
            is SyncError.NetworkError -> true
            is SyncError.RateLimitError -> true
            is SyncError.UnknownError -> true
            is SyncError.AuthenticationError -> false // Don't retry auth errors
            is SyncError.ValidationError -> false // Don't retry validation errors
            is SyncError.DataConflictError -> false // Don't retry conflict errors
            else -> defaultRetryCondition(error)
        }
    }
    
    override fun calculateDelay(
        attempt: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        backoffMultiplier: Double,
        jitterRange: Double
    ): Duration {
        // Exponential backoff: delay = initialDelay * (backoffMultiplier ^ attempt)
        val exponentialDelay = initialDelay.inWholeMilliseconds * 
            backoffMultiplier.pow(attempt.toDouble())
        
        // Cap at maximum delay
        val cappedDelay = min(exponentialDelay, maxDelay.inWholeMilliseconds.toDouble())
        
        // Add jitter to avoid thundering herd problem
        val jitter = cappedDelay * jitterRange * (Random.nextDouble() * 2 - 1) // -jitterRange to +jitterRange
        val finalDelay = cappedDelay + jitter
        
        return max(0.0, finalDelay).toLong().milliseconds
    }
    
    companion object {
        /**
         * Default retry condition - retry on network-related errors
         */
        fun defaultRetryCondition(error: Throwable): Boolean {
            return when (error) {
                is java.net.SocketTimeoutException -> true
                is java.net.ConnectException -> true
                is java.net.UnknownHostException -> true
                is java.io.IOException -> true
                else -> error.message?.contains("timeout", ignoreCase = true) == true ||
                        error.message?.contains("connection", ignoreCase = true) == true ||
                        error.message?.contains("network", ignoreCase = true) == true
            }
        }
    }
}

/**
 * Retry configuration for different types of operations
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val backoffMultiplier: Double = 2.0,
    val jitterRange: Double = 0.1
) {
    companion object {
        /**
         * Conservative retry config for critical operations
         */
        val CONSERVATIVE = RetryConfig(
            maxAttempts = 2,
            initialDelay = 2.seconds,
            maxDelay = 10.seconds,
            backoffMultiplier = 1.5,
            jitterRange = 0.05
        )
        
        /**
         * Aggressive retry config for non-critical operations
         */
        val AGGRESSIVE = RetryConfig(
            maxAttempts = 5,
            initialDelay = 500.milliseconds,
            maxDelay = 60.seconds,
            backoffMultiplier = 2.5,
            jitterRange = 0.2
        )
        
        /**
         * Quick retry config for fast operations
         */
        val QUICK = RetryConfig(
            maxAttempts = 3,
            initialDelay = 100.milliseconds,
            maxDelay = 5.seconds,
            backoffMultiplier = 2.0,
            jitterRange = 0.1
        )
    }
}