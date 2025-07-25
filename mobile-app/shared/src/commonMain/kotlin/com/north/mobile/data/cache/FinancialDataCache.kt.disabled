package com.north.mobile.data.cache

import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

/**
 * Efficient caching system for financial data with TTL and memory management
 */
interface FinancialDataCache {
    suspend fun cacheAccount(account: Account)
    suspend fun getAccount(accountId: String): Account?
    suspend fun getCachedAccounts(): List<Account>
    
    suspend fun cacheTransaction(transaction: Transaction)
    suspend fun getTransaction(transactionId: String): Transaction?
    suspend fun getCachedTransactions(accountId: String): List<Transaction>
    
    suspend fun cacheNetWorth(userId: String, netWorth: Money)
    suspend fun getNetWorth(userId: String): Money?
    
    suspend fun cacheInsights(userId: String, insights: List<FinancialInsight>)
    suspend fun getInsights(userId: String): List<FinancialInsight>?
    
    suspend fun invalidateCache(cacheType: CacheType)
    suspend fun clearExpiredEntries()
    suspend fun getCacheStats(): CacheStats
}

enum class CacheType {
    ACCOUNTS,
    TRANSACTIONS,
    NET_WORTH,
    INSIGHTS,
    ALL
}

data class CacheEntry<T>(
    val data: T,
    val timestamp: Instant,
    val ttl: Duration
) {
    val isExpired: Boolean
        get() = Clock.System.now() > timestamp + ttl
}

data class CacheStats(
    val accountsCount: Int,
    val transactionsCount: Int,
    val netWorthCount: Int,
    val insightsCount: Int,
    val totalMemoryUsage: Long,
    val hitRate: Double,
    val missRate: Double
)

data class FinancialInsight(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: Int,
    val actionable: Boolean,
    val createdAt: Instant
)

class FinancialDataCacheImpl : FinancialDataCache {
    
    private val accountCache = mutableMapOf<String, CacheEntry<Account>>()
    private val transactionCache = mutableMapOf<String, CacheEntry<Transaction>>()
    private val transactionsByAccount = mutableMapOf<String, MutableList<String>>()
    private val netWorthCache = mutableMapOf<String, CacheEntry<Money>>()
    private val insightsCache = mutableMapOf<String, CacheEntry<List<FinancialInsight>>>()
    
    private var cacheHits = 0L
    private var cacheMisses = 0L
    
    companion object {
        private val ACCOUNT_TTL = 5.minutes
        private val TRANSACTION_TTL = 10.minutes
        private val NET_WORTH_TTL = 2.minutes
        private val INSIGHTS_TTL = 15.minutes
        private const val MAX_TRANSACTIONS_PER_ACCOUNT = 1000
    }
    
    override suspend fun cacheAccount(account: Account) {
        accountCache[account.id] = CacheEntry(
            data = account,
            timestamp = Clock.System.now(),
            ttl = ACCOUNT_TTL
        )
    }
    
    override suspend fun getAccount(accountId: String): Account? {
        val entry = accountCache[accountId]
        return if (entry != null && !entry.isExpired) {
            cacheHits++
            entry.data
        } else {
            cacheMisses++
            if (entry?.isExpired == true) {
                accountCache.remove(accountId)
            }
            null
        }
    }
    
    override suspend fun getCachedAccounts(): List<Account> {
        return accountCache.values
            .filter { !it.isExpired }
            .map { it.data }
    }
    
    override suspend fun cacheTransaction(transaction: Transaction) {
        // Implement LRU eviction for transactions
        val accountTransactions = transactionsByAccount.getOrPut(transaction.accountId) { mutableListOf() }
        
        if (accountTransactions.size >= MAX_TRANSACTIONS_PER_ACCOUNT) {
            // Remove oldest transaction
            val oldestTransactionId = accountTransactions.removeFirstOrNull()
            oldestTransactionId?.let { transactionCache.remove(it) }
        }
        
        transactionCache[transaction.id] = CacheEntry(
            data = transaction,
            timestamp = Clock.System.now(),
            ttl = TRANSACTION_TTL
        )
        
        accountTransactions.add(transaction.id)
    }
    
    override suspend fun getTransaction(transactionId: String): Transaction? {
        val entry = transactionCache[transactionId]
        return if (entry != null && !entry.isExpired) {
            cacheHits++
            entry.data
        } else {
            cacheMisses++
            if (entry?.isExpired == true) {
                transactionCache.remove(transactionId)
                // Remove from account index
                transactionsByAccount.values.forEach { it.remove(transactionId) }
            }
            null
        }
    }
    
    override suspend fun getCachedTransactions(accountId: String): List<Transaction> {
        val transactionIds = transactionsByAccount[accountId] ?: return emptyList()
        
        return transactionIds.mapNotNull { transactionId ->
            val entry = transactionCache[transactionId]
            if (entry != null && !entry.isExpired) {
                entry.data
            } else {
                // Clean up expired entries
                if (entry?.isExpired == true) {
                    transactionCache.remove(transactionId)
                }
                null
            }
        }.also {
            // Clean up the account index
            transactionsByAccount[accountId]?.removeAll { transactionId ->
                !transactionCache.containsKey(transactionId)
            }
        }
    }
    
    override suspend fun cacheNetWorth(userId: String, netWorth: Money) {
        netWorthCache[userId] = CacheEntry(
            data = netWorth,
            timestamp = Clock.System.now(),
            ttl = NET_WORTH_TTL
        )
    }
    
    override suspend fun getNetWorth(userId: String): Money? {
        val entry = netWorthCache[userId]
        return if (entry != null && !entry.isExpired) {
            cacheHits++
            entry.data
        } else {
            cacheMisses++
            if (entry?.isExpired == true) {
                netWorthCache.remove(userId)
            }
            null
        }
    }
    
    override suspend fun cacheInsights(userId: String, insights: List<FinancialInsight>) {
        insightsCache[userId] = CacheEntry(
            data = insights,
            timestamp = Clock.System.now(),
            ttl = INSIGHTS_TTL
        )
    }
    
    override suspend fun getInsights(userId: String): List<FinancialInsight>? {
        val entry = insightsCache[userId]
        return if (entry != null && !entry.isExpired) {
            cacheHits++
            entry.data
        } else {
            cacheMisses++
            if (entry?.isExpired == true) {
                insightsCache.remove(userId)
            }
            null
        }
    }
    
    override suspend fun invalidateCache(cacheType: CacheType) {
        when (cacheType) {
            CacheType.ACCOUNTS -> accountCache.clear()
            CacheType.TRANSACTIONS -> {
                transactionCache.clear()
                transactionsByAccount.clear()
            }
            CacheType.NET_WORTH -> netWorthCache.clear()
            CacheType.INSIGHTS -> insightsCache.clear()
            CacheType.ALL -> {
                accountCache.clear()
                transactionCache.clear()
                transactionsByAccount.clear()
                netWorthCache.clear()
                insightsCache.clear()
            }
        }
    }
    
    override suspend fun clearExpiredEntries() {
        // Clear expired accounts
        accountCache.entries.removeAll { it.value.isExpired }
        
        // Clear expired transactions
        val expiredTransactionIds = transactionCache.entries
            .filter { it.value.isExpired }
            .map { it.key }
        
        expiredTransactionIds.forEach { transactionId ->
            transactionCache.remove(transactionId)
            transactionsByAccount.values.forEach { it.remove(transactionId) }
        }
        
        // Clear expired net worth entries
        netWorthCache.entries.removeAll { it.value.isExpired }
        
        // Clear expired insights
        insightsCache.entries.removeAll { it.value.isExpired }
    }
    
    override suspend fun getCacheStats(): CacheStats {
        val totalRequests = cacheHits + cacheMisses
        val hitRate = if (totalRequests > 0) cacheHits.toDouble() / totalRequests else 0.0
        val missRate = if (totalRequests > 0) cacheMisses.toDouble() / totalRequests else 0.0
        
        return CacheStats(
            accountsCount = accountCache.size,
            transactionsCount = transactionCache.size,
            netWorthCount = netWorthCache.size,
            insightsCount = insightsCache.size,
            totalMemoryUsage = estimateMemoryUsage(),
            hitRate = hitRate,
            missRate = missRate
        )
    }
    
    private fun estimateMemoryUsage(): Long {
        // Rough estimation of memory usage
        val accountMemory = accountCache.size * 500L // ~500 bytes per account
        val transactionMemory = transactionCache.size * 300L // ~300 bytes per transaction
        val netWorthMemory = netWorthCache.size * 100L // ~100 bytes per net worth entry
        val insightsMemory = insightsCache.size * 1000L // ~1KB per insights list
        
        return accountMemory + transactionMemory + netWorthMemory + insightsMemory
    }
}

/**
 * Cache-aware repository wrapper
 */
abstract class CachedRepository<T, ID>(
    protected val cache: FinancialDataCache
) {
    abstract suspend fun fetchFromSource(id: ID): T?
    abstract suspend fun cacheItem(item: T)
    abstract suspend fun getCachedItem(id: ID): T?
    
    suspend fun get(id: ID): T? {
        return getCachedItem(id) ?: fetchFromSource(id)?.also { cacheItem(it) }
    }
}