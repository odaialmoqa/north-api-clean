package com.north.mobile.data.repository

import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

/**
 * Repository that implements lazy loading for large transaction datasets
 * to optimize memory usage and improve performance
 */
interface LazyTransactionRepository {
    suspend fun loadTransactionPage(
        accountId: String,
        pageSize: Int = 50,
        offset: Int = 0,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): TransactionPage
    
    suspend fun loadMoreTransactions(accountId: String): TransactionPage
    
    fun getTransactionFlow(accountId: String): Flow<List<Transaction>>
    
    suspend fun prefetchNextPage(accountId: String)
    
    suspend fun clearCache(accountId: String)
    
    fun getCacheStatus(accountId: String): CacheStatus
}

data class TransactionPage(
    val transactions: List<Transaction>,
    val hasMore: Boolean,
    val totalCount: Int,
    val currentOffset: Int
)

data class CacheStatus(
    val cachedCount: Int,
    val totalAvailable: Int,
    val isLoading: Boolean,
    val lastUpdated: Long
)

class LazyTransactionRepositoryImpl(
    private val transactionRepository: TransactionRepository
) : LazyTransactionRepository {
    
    private val transactionCache = mutableMapOf<String, MutableList<Transaction>>()
    private val cacheStatus = mutableMapOf<String, MutableStateFlow<CacheStatus>>()
    private val loadingStates = mutableMapOf<String, Boolean>()
    
    companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val PREFETCH_THRESHOLD = 10 // Prefetch when within 10 items of end
    }
    
    override suspend fun loadTransactionPage(
        accountId: String,
        pageSize: Int,
        offset: Int,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): TransactionPage {
        updateLoadingState(accountId, true)
        
        try {
            // Check if we have cached data for this range
            val cachedTransactions = transactionCache[accountId] ?: mutableListOf()
            
            if (offset < cachedTransactions.size) {
                // Return cached data if available
                val endIndex = minOf(offset + pageSize, cachedTransactions.size)
                val pageTransactions = cachedTransactions.subList(offset, endIndex)
                
                return TransactionPage(
                    transactions = pageTransactions,
                    hasMore = endIndex < cachedTransactions.size,
                    totalCount = cachedTransactions.size,
                    currentOffset = endIndex
                )
            }
            
            // Load new data from repository
            val result = transactionRepository.findByAccountIdPaged(
                accountId = accountId,
                limit = pageSize,
                offset = offset,
                startDate = startDate,
                endDate = endDate
            )
            
            val newTransactions = result.getOrThrow()
            
            // Update cache
            if (cachedTransactions.isEmpty() || offset == 0) {
                transactionCache[accountId] = newTransactions.toMutableList()
            } else {
                cachedTransactions.addAll(newTransactions)
            }
            
            val hasMore = newTransactions.size == pageSize
            val totalCount = cachedTransactions.size
            
            updateCacheStatus(accountId, totalCount, hasMore)
            
            return TransactionPage(
                transactions = newTransactions,
                hasMore = hasMore,
                totalCount = totalCount,
                currentOffset = offset + newTransactions.size
            )
            
        } finally {
            updateLoadingState(accountId, false)
        }
    }
    
    override suspend fun loadMoreTransactions(accountId: String): TransactionPage {
        val cached = transactionCache[accountId] ?: mutableListOf()
        return loadTransactionPage(
            accountId = accountId,
            pageSize = DEFAULT_PAGE_SIZE,
            offset = cached.size
        )
    }
    
    override fun getTransactionFlow(accountId: String): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsFlow(accountId)
    }
    
    override suspend fun prefetchNextPage(accountId: String) {
        val cached = transactionCache[accountId] ?: return
        val status = getCacheStatus(accountId)
        
        if (!status.isLoading && cached.size > 0) {
            // Prefetch next page in background
            loadTransactionPage(
                accountId = accountId,
                pageSize = DEFAULT_PAGE_SIZE,
                offset = cached.size
            )
        }
    }
    
    override suspend fun clearCache(accountId: String) {
        transactionCache.remove(accountId)
        cacheStatus.remove(accountId)
        loadingStates.remove(accountId)
    }
    
    override fun getCacheStatus(accountId: String): CacheStatus {
        return cacheStatus[accountId]?.value ?: CacheStatus(
            cachedCount = 0,
            totalAvailable = 0,
            isLoading = false,
            lastUpdated = 0L
        )
    }
    
    private fun updateLoadingState(accountId: String, isLoading: Boolean) {
        loadingStates[accountId] = isLoading
        updateCacheStatus(accountId, getCacheStatus(accountId).cachedCount, false)
    }
    
    private fun updateCacheStatus(accountId: String, cachedCount: Int, hasMore: Boolean) {
        val statusFlow = cacheStatus.getOrPut(accountId) { 
            MutableStateFlow(CacheStatus(0, 0, false, 0L))
        }
        
        statusFlow.value = CacheStatus(
            cachedCount = cachedCount,
            totalAvailable = if (hasMore) cachedCount + 1 else cachedCount,
            isLoading = loadingStates[accountId] ?: false,
            lastUpdated = System.currentTimeMillis()
        )
    }
}