package com.north.mobile.data.sync

import com.north.mobile.domain.model.Transaction
import com.north.mobile.domain.model.Account
import kotlinx.datetime.Clock

/**
 * Handles conflict resolution during data synchronization
 */
interface ConflictResolver {
    /**
     * Detect conflicts between local and remote transactions
     */
    fun detectTransactionConflict(local: Transaction, remote: Transaction): ConflictDetails?
    
    /**
     * Detect conflicts between local and remote accounts
     */
    fun detectAccountConflict(local: Account, remote: Account): ConflictDetails?
    
    /**
     * Resolve transaction conflicts based on predefined rules
     */
    fun resolveTransactionConflict(conflict: ConflictDetails): ConflictDetails
    
    /**
     * Resolve account conflicts based on predefined rules
     */
    fun resolveAccountConflict(conflict: ConflictDetails): ConflictDetails
}

class ConflictResolverImpl : ConflictResolver {
    
    override fun detectTransactionConflict(local: Transaction, remote: Transaction): ConflictDetails? {
        // Check if transactions are actually different
        if (areTransactionsEqual(local, remote)) {
            return null // No conflict
        }
        
        // Determine conflict type
        val conflictType = when {
            local.id == remote.id && local.amount != remote.amount -> ConflictType.MODIFIED_TRANSACTION
            local.id == remote.id -> ConflictType.MODIFIED_TRANSACTION
            areTransactionsDuplicates(local, remote) -> ConflictType.DUPLICATE_TRANSACTION
            else -> ConflictType.MODIFIED_TRANSACTION
        }
        
        return ConflictDetails(
            conflictType = conflictType,
            localData = local,
            remoteData = remote,
            resolution = ConflictResolution.USE_REMOTE // Default resolution
        )
    }
    
    override fun detectAccountConflict(local: Account, remote: Account): ConflictDetails? {
        if (areAccountsEqual(local, remote)) {
            return null // No conflict
        }
        
        val conflictType = when {
            local.balance != remote.balance -> ConflictType.BALANCE_MISMATCH
            local.isActive != remote.isActive -> ConflictType.ACCOUNT_STATUS_CHANGE
            else -> ConflictType.BALANCE_MISMATCH
        }
        
        return ConflictDetails(
            conflictType = conflictType,
            localData = local,
            remoteData = remote,
            resolution = ConflictResolution.USE_REMOTE // Default resolution
        )
    }
    
    override fun resolveTransactionConflict(conflict: ConflictDetails): ConflictDetails {
        val resolution = when (conflict.conflictType) {
            ConflictType.DUPLICATE_TRANSACTION -> {
                // For duplicates, prefer the one with more complete data
                val local = conflict.localData as Transaction
                val remote = conflict.remoteData as Transaction
                
                when {
                    remote.merchantName != null && local.merchantName == null -> ConflictResolution.USE_REMOTE
                    remote.location != null && local.location == null -> ConflictResolution.USE_REMOTE
                    remote.category != local.category && remote.category.name != "Uncategorized" -> ConflictResolution.USE_REMOTE
                    else -> ConflictResolution.USE_LOCAL
                }
            }
            
            ConflictType.MODIFIED_TRANSACTION -> {
                // For modified transactions, prefer remote (bank data is authoritative)
                ConflictResolution.USE_REMOTE
            }
            
            else -> ConflictResolution.MANUAL_REVIEW_REQUIRED
        }
        
        return conflict.copy(resolution = resolution)
    }
    
    override fun resolveAccountConflict(conflict: ConflictDetails): ConflictDetails {
        val resolution = when (conflict.conflictType) {
            ConflictType.BALANCE_MISMATCH -> {
                // Always prefer remote balance (bank data is authoritative)
                ConflictResolution.USE_REMOTE
            }
            
            ConflictType.ACCOUNT_STATUS_CHANGE -> {
                // Prefer remote status unless local was manually deactivated recently
                val local = conflict.localData as Account
                val remote = conflict.remoteData as Account
                
                if (!local.isActive && remote.isActive) {
                    // Local was deactivated - check if it was recent
                    val now = Clock.System.now()
                    val timeSinceUpdate = now.minus(local.lastUpdated)
                    
                    if (timeSinceUpdate.inWholeHours < 24) {
                        ConflictResolution.USE_LOCAL // Recent manual deactivation
                    } else {
                        ConflictResolution.USE_REMOTE // Old deactivation, bank reactivated
                    }
                } else {
                    ConflictResolution.USE_REMOTE
                }
            }
            
            else -> ConflictResolution.MANUAL_REVIEW_REQUIRED
        }
        
        return conflict.copy(resolution = resolution)
    }
    
    private fun areTransactionsEqual(local: Transaction, remote: Transaction): Boolean {
        return local.id == remote.id &&
                local.amount == remote.amount &&
                local.description == remote.description &&
                local.date == remote.date &&
                local.status == remote.status
    }
    
    private fun areTransactionsDuplicates(local: Transaction, remote: Transaction): Boolean {
        // Consider transactions duplicates if they have the same amount, date, and similar description
        // but different IDs (could happen with different data sources)
        return local.id != remote.id &&
                local.amount == remote.amount &&
                local.date == remote.date &&
                local.accountId == remote.accountId &&
                areSimilarDescriptions(local.description, remote.description)
    }
    
    private fun areAccountsEqual(local: Account, remote: Account): Boolean {
        return local.id == remote.id &&
                local.balance == remote.balance &&
                local.availableBalance == remote.availableBalance &&
                local.isActive == remote.isActive &&
                local.accountType == remote.accountType
    }
    
    private fun areSimilarDescriptions(desc1: String, desc2: String): Boolean {
        // Simple similarity check - could be enhanced with more sophisticated algorithms
        val normalized1 = desc1.lowercase().trim()
        val normalized2 = desc2.lowercase().trim()
        
        return normalized1 == normalized2 || 
               normalized1.contains(normalized2) || 
               normalized2.contains(normalized1) ||
               levenshteinDistance(normalized1, normalized2) <= 3
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) {
            dp[i][0] = i
        }
        
        for (j in 0..len2) {
            dp[0][j] = j
        }
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
}