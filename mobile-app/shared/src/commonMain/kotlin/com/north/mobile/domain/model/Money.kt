package com.north.mobile.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Represents a monetary amount with currency support
 * Amounts are stored as cents/smallest currency unit to avoid floating point precision issues
 */
@Serializable
data class Money(
    val amount: Long, // Amount in cents (for CAD/USD)
    val currency: Currency = Currency.CAD
) : Comparable<Money> {
    
    /**
     * Amount in dollars (with decimal places)
     */
    val dollars: Double
        get() = amount / 100.0
    
    /**
     * Check if amount is positive
     */
    val isPositive: Boolean
        get() = amount > 0
    
    /**
     * Check if amount is negative
     */
    val isNegative: Boolean
        get() = amount < 0
    
    /**
     * Check if amount is zero
     */
    val isZero: Boolean
        get() = amount == 0L
    
    /**
     * Get absolute value of this money amount
     */
    val absoluteValue: Money
        get() = Money(abs(amount), currency)
    
    /**
     * Add two money amounts (must be same currency)
     */
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return Money(amount + other.amount, currency)
    }
    
    /**
     * Subtract two money amounts (must be same currency)
     */
    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies" }
        return Money(amount - other.amount, currency)
    }
    
    /**
     * Multiply money by a factor
     */
    operator fun times(factor: Double): Money {
        return Money((amount * factor).toLong(), currency)
    }
    
    /**
     * Divide money by a factor
     */
    operator fun div(factor: Double): Money {
        require(factor != 0.0) { "Cannot divide by zero" }
        return Money((amount / factor).toLong(), currency)
    }
    
    /**
     * Unary minus operator
     */
    operator fun unaryMinus(): Money {
        return Money(-amount, currency)
    }
    
    /**
     * Compare money amounts (must be same currency)
     */
    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies" }
        return amount.compareTo(other.amount)
    }
    
    /**
     * Format as Canadian dollars with proper formatting
     */
    fun formatCAD(): String {
        val absAmount = abs(amount)
        val dollars = absAmount / 100
        val cents = absAmount % 100
        
        val sign = if (amount < 0) "-" else ""
        
        return when {
            absAmount >= 100_000_000 -> { // 1M or more
                val millions = dollars / 1_000_000.0
                "${sign}$${String.format("%.1f", millions)}M"
            }
            absAmount >= 100_000 -> { // 1K or more
                val thousands = dollars / 1_000.0
                "${sign}$${String.format("%.1f", thousands)}K"
            }
            else -> {
                val formattedDollars = if (dollars >= 1000) {
                    String.format("%,d", dollars)
                } else {
                    dollars.toString()
                }
                "${sign}$${formattedDollars}.${String.format("%02d", cents)}"
            }
        }
    }
    
    /**
     * Format with currency symbol
     */
    fun format(): String {
        return when (currency) {
            Currency.CAD -> formatCAD()
            Currency.USD -> "US" + formatCAD()
        }
    }
    
    /**
     * String representation
     */
    override fun toString(): String = format()
    
    companion object {
        /**
         * Create Money from dollar amount
         */
        fun fromDollars(dollars: Double, currency: Currency = Currency.CAD): Money {
            return Money((dollars * 100).toLong(), currency)
        }
        
        /**
         * Create zero money amount
         */
        fun zero(currency: Currency = Currency.CAD): Money {
            return Money(0, currency)
        }
        
        /**
         * Parse money from string
         * Supports formats like: $10.50, 10.50, $1,234.56, -$10.50
         */
        fun parse(input: String): Money? {
            if (input.isBlank()) return null
            
            try {
                val cleanInput = input.trim()
                    .replace("$", "")
                    .replace(",", "")
                
                val amount = cleanInput.toDoubleOrNull() ?: return null
                return fromDollars(amount)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

/**
 * Supported currencies
 */
@Serializable
enum class Currency(val code: String, val symbol: String, val displayName: String) {
    CAD("CAD", "$", "Canadian Dollar"),
    USD("USD", "$", "US Dollar");
    
    override fun toString(): String = code
}