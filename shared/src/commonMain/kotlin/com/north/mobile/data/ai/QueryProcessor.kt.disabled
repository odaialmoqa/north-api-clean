package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.DateRange
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Processes natural language queries to extract intent and parameters
 */
class QueryProcessor {
    
    private val affordabilityKeywords = listOf(
        "can i afford", "afford", "budget for", "spend on", "buy", "purchase"
    )
    
    private val spendingAnalysisKeywords = listOf(
        "spending", "spent", "expenses", "money on", "breakdown", "analysis"
    )
    
    private val goalProgressKeywords = listOf(
        "goal", "progress", "target", "saving", "achievement", "milestone"
    )
    
    private val budgetStatusKeywords = listOf(
        "budget", "remaining", "left", "over budget", "under budget"
    )
    
    private val transactionExplanationKeywords = listOf(
        "transaction", "charge", "payment", "why did i", "what was", "explain"
    )
    
    private val savingsAdviceKeywords = listOf(
        "save", "savings", "emergency fund", "how much should i save"
    )
    
    private val optimizationKeywords = listOf(
        "optimize", "reduce", "cut", "lower", "save money", "cheaper"
    )
    
    private val amountPatterns = listOf(
        Regex("""\$(\d+(?:,\d{3})*(?:\.\d{2})?)"""), // $1,234.56
        Regex("""(\d+(?:,\d{3})*(?:\.\d{2})?) dollars?"""), // 1234 dollars
        Regex("""(\d+(?:,\d{3})*(?:\.\d{2})?)""") // 1234
    )
    
    private val timeframePatterns = mapOf(
        "today" to { today: LocalDate -> DateRange(today, today) },
        "yesterday" to { today: LocalDate -> DateRange(today.minus(1, DateTimeUnit.DAY), today.minus(1, DateTimeUnit.DAY)) },
        "this week" to { today: LocalDate -> 
            val startOfWeek = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
            DateRange(startOfWeek, today)
        },
        "last week" to { today: LocalDate ->
            val startOfLastWeek = today.minus(today.dayOfWeek.ordinal + 7, DateTimeUnit.DAY)
            val endOfLastWeek = startOfLastWeek.plus(6, DateTimeUnit.DAY)
            DateRange(startOfLastWeek, endOfLastWeek)
        },
        "this month" to { today: LocalDate ->
            val startOfMonth = LocalDate(today.year, today.month, 1)
            DateRange(startOfMonth, today)
        },
        "last month" to { today: LocalDate ->
            val lastMonth = today.minus(1, DateTimeUnit.MONTH)
            val startOfLastMonth = LocalDate(lastMonth.year, lastMonth.month, 1)
            val endOfLastMonth = startOfLastMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            DateRange(startOfLastMonth, endOfLastMonth)
        },
        "last 30 days" to { today: LocalDate -> DateRange(today.minus(30, DateTimeUnit.DAY), today) },
        "last 7 days" to { today: LocalDate -> DateRange(today.minus(7, DateTimeUnit.DAY), today) }
    )
    
    private val categoryKeywords = mapOf(
        "food" to Category.FOOD,
        "dining" to Category.RESTAURANTS,
        "restaurant" to Category.RESTAURANTS,
        "groceries" to Category.GROCERIES,
        "grocery" to Category.GROCERIES,
        "transport" to Category.TRANSPORT,
        "transportation" to Category.TRANSPORT,
        "gas" to Category.GAS,
        "fuel" to Category.GAS,
        "shopping" to Category.SHOPPING,
        "entertainment" to Category.ENTERTAINMENT,
        "bills" to Category.BILLS,
        "utilities" to Category.BILLS,
        "healthcare" to Category.HEALTHCARE,
        "health" to Category.HEALTHCARE,
        "travel" to Category.TRAVEL,
        "education" to Category.EDUCATION
    )
    
    fun processQuery(query: String): ProcessedQuery {
        val normalizedQuery = query.lowercase().trim()
        
        val intent = determineIntent(normalizedQuery)
        val extractedAmount = extractAmount(normalizedQuery)
        val extractedCategory = extractCategory(normalizedQuery)
        val extractedTimeframe = extractTimeframe(normalizedQuery)
        
        return ProcessedQuery(
            originalQuery = query,
            normalizedQuery = normalizedQuery,
            intent = intent,
            extractedAmount = extractedAmount,
            extractedCategory = extractedCategory,
            extractedTimeframe = extractedTimeframe,
            confidence = calculateConfidence(normalizedQuery, intent)
        )
    }
    
    private fun determineIntent(query: String): QueryIntent {
        return when {
            affordabilityKeywords.any { query.contains(it) } -> QueryIntent.AFFORDABILITY_CHECK
            spendingAnalysisKeywords.any { query.contains(it) } -> QueryIntent.SPENDING_ANALYSIS
            goalProgressKeywords.any { query.contains(it) } -> QueryIntent.GOAL_PROGRESS
            budgetStatusKeywords.any { query.contains(it) } -> QueryIntent.BUDGET_STATUS
            transactionExplanationKeywords.any { query.contains(it) } -> QueryIntent.TRANSACTION_EXPLANATION
            savingsAdviceKeywords.any { query.contains(it) } -> QueryIntent.SAVINGS_ADVICE
            optimizationKeywords.any { query.contains(it) } -> QueryIntent.OPTIMIZATION_SUGGESTION
            else -> QueryIntent.GENERAL_INSIGHT
        }
    }
    
    private fun extractAmount(query: String): Money? {
        for (pattern in amountPatterns) {
            val match = pattern.find(query)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null) {
                    return Money.fromDollars(amount)
                }
            }
        }
        return null
    }
    
    private fun extractCategory(query: String): Category? {
        for ((keyword, category) in categoryKeywords) {
            if (query.contains(keyword)) {
                return category
            }
        }
        return null
    }
    
    private fun extractTimeframe(query: String): DateRange? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        for ((keyword, rangeFunction) in timeframePatterns) {
            if (query.contains(keyword)) {
                return rangeFunction(today)
            }
        }
        return null
    }
    
    private fun calculateConfidence(query: String, intent: QueryIntent): Float {
        val keywordMatches = when (intent) {
            QueryIntent.AFFORDABILITY_CHECK -> affordabilityKeywords.count { query.contains(it) }
            QueryIntent.SPENDING_ANALYSIS -> spendingAnalysisKeywords.count { query.contains(it) }
            QueryIntent.GOAL_PROGRESS -> goalProgressKeywords.count { query.contains(it) }
            QueryIntent.BUDGET_STATUS -> budgetStatusKeywords.count { query.contains(it) }
            QueryIntent.TRANSACTION_EXPLANATION -> transactionExplanationKeywords.count { query.contains(it) }
            QueryIntent.SAVINGS_ADVICE -> savingsAdviceKeywords.count { query.contains(it) }
            QueryIntent.OPTIMIZATION_SUGGESTION -> optimizationKeywords.count { query.contains(it) }
            QueryIntent.GENERAL_INSIGHT -> 0
        }
        
        return min(0.9f, 0.5f + (keywordMatches * 0.2f))
    }
}

/**
 * Processed query with extracted information
 */
data class ProcessedQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val intent: QueryIntent,
    val extractedAmount: Money?,
    val extractedCategory: Category?,
    val extractedTimeframe: DateRange?,
    val confidence: Float
)

/**
 * Types of query intents
 */
enum class QueryIntent {
    AFFORDABILITY_CHECK,
    SPENDING_ANALYSIS,
    GOAL_PROGRESS,
    BUDGET_STATUS,
    TRANSACTION_EXPLANATION,
    SAVINGS_ADVICE,
    OPTIMIZATION_SUGGESTION,
    GENERAL_INSIGHT
}