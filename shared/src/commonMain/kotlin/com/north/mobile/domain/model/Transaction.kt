package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import com.north.mobile.domain.validation.ValidationUtils
import com.north.mobile.domain.validation.combine
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Money,
    val description: String,
    val category: Category,
    val date: LocalDate,
    val isRecurring: Boolean = false,
    val merchantName: String? = null,
    val location: String? = null,
    val transactionType: TransactionType = if (amount.isNegative) TransactionType.DEBIT else TransactionType.CREDIT,
    val status: TransactionStatus = TransactionStatus.POSTED,
    val reference: String? = null // Bank reference number
) {
    fun validate(): ValidationResult {
        val validations = mutableListOf<ValidationResult>()
        
        // Validate required fields
        validations.add(
            if (id.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Transaction ID cannot be blank")
        )
        
        validations.add(
            if (accountId.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Account ID cannot be blank")
        )
        
        validations.add(
            if (description.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Transaction description cannot be blank")
        )
        
        // Validate amount is not zero
        validations.add(
            if (!amount.isZero) ValidationResult.Valid 
            else ValidationResult.Invalid("Transaction amount cannot be zero")
        )
        
        // Validate category
        validations.add(category.validate())
        
        return validations.combine()
    }
    
    val isDebit: Boolean
        get() = amount.isNegative
    
    val isCredit: Boolean
        get() = amount.isPositive
    
    val absoluteAmount: Money
        get() = amount.absoluteValue
    
    val displayDescription: String
        get() = merchantName ?: description
}

@Serializable
data class Category(
    val id: String,
    val name: String,
    val parentCategoryId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val isCustom: Boolean = false
) {
    fun validate(): ValidationResult {
        val validations = listOf(
            if (id.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Category ID cannot be blank"),
            if (ValidationUtils.isValidName(name)) ValidationResult.Valid 
            else ValidationResult.Invalid("Category name cannot be blank"),
            // Validate hex color format if provided
            color?.let { colorValue ->
                if (colorValue.matches(Regex("^#[0-9A-Fa-f]{6}$"))) ValidationResult.Valid
                else ValidationResult.Invalid("Color must be in hex format (#RRGGBB)")
            } ?: ValidationResult.Valid
        )
        return validations.combine()
    }
    
    companion object {
        // Canadian-specific categories
        val UNCATEGORIZED = Category("uncategorized", "Uncategorized", color = "#9E9E9E")
        val FOOD = Category("food", "Food & Dining", color = "#FF9800")
        val GROCERIES = Category("groceries", "Groceries", parentCategoryId = "food", color = "#FFC107")
        val RESTAURANTS = Category("restaurants", "Restaurants", parentCategoryId = "food", color = "#FF5722")
        val TRANSPORT = Category("transport", "Transportation", color = "#2196F3")
        val GAS = Category("gas", "Gas & Fuel", parentCategoryId = "transport", color = "#1976D2")
        val PUBLIC_TRANSIT = Category("public_transit", "Public Transit", parentCategoryId = "transport", color = "#03A9F4")
        val SHOPPING = Category("shopping", "Shopping", color = "#E91E63")
        val ENTERTAINMENT = Category("entertainment", "Entertainment", color = "#9C27B0")
        val BILLS = Category("bills", "Bills & Utilities", color = "#607D8B")
        val HYDRO = Category("hydro", "Hydro/Electricity", parentCategoryId = "bills", color = "#455A64")
        val INTERNET = Category("internet", "Internet & Phone", parentCategoryId = "bills", color = "#546E7A")
        val HEALTHCARE = Category("healthcare", "Healthcare", color = "#4CAF50")
        val EDUCATION = Category("education", "Education", color = "#FF9800")
        val TRAVEL = Category("travel", "Travel", color = "#00BCD4")
        val INCOME = Category("income", "Income", color = "#4CAF50")
        val SALARY = Category("salary", "Salary", parentCategoryId = "income", color = "#388E3C")
        val INVESTMENT = Category("investment", "Investment", color = "#795548")
        val RRSP = Category("rrsp", "RRSP Contribution", parentCategoryId = "investment", color = "#5D4037")
        val TFSA = Category("tfsa", "TFSA Contribution", parentCategoryId = "investment", color = "#6D4C41")
        
        fun getDefaultCategories(): List<Category> = listOf(
            UNCATEGORIZED, FOOD, GROCERIES, RESTAURANTS, TRANSPORT, GAS, PUBLIC_TRANSIT,
            SHOPPING, ENTERTAINMENT, BILLS, HYDRO, INTERNET, HEALTHCARE, EDUCATION,
            TRAVEL, INCOME, SALARY, INVESTMENT, RRSP, TFSA
        )
    }
}

@Serializable
enum class TransactionType {
    DEBIT,
    CREDIT
}

@Serializable
enum class TransactionStatus {
    PENDING,
    POSTED,
    CANCELLED,
    FAILED
}