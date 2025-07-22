package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Category

/**
 * Service for managing transaction categories including custom categories
 */
interface CategoryManagementService {
    /**
     * Get all available categories (default + custom)
     */
    suspend fun getAllCategories(): List<Category>
    
    /**
     * Get only custom categories created by the user
     */
    suspend fun getCustomCategories(): List<Category>
    
    /**
     * Create a new custom category
     */
    suspend fun createCustomCategory(
        name: String,
        parentCategoryId: String? = null,
        color: String? = null,
        icon: String? = null
    ): CategoryManagementResult
    
    /**
     * Update an existing custom category
     */
    suspend fun updateCustomCategory(category: Category): CategoryManagementResult
    
    /**
     * Delete a custom category and reassign transactions
     */
    suspend fun deleteCustomCategory(
        categoryId: String,
        reassignToCategoryId: String? = null
    ): CategoryManagementResult
    
    /**
     * Get category usage statistics
     */
    suspend fun getCategoryUsageStats(): List<CategoryUsageStats>
    
    /**
     * Suggest category improvements based on usage patterns
     */
    suspend fun suggestCategoryImprovements(): List<CategorySuggestion>
    
    /**
     * Merge two categories together
     */
    suspend fun mergeCategories(
        sourceCategoryId: String,
        targetCategoryId: String
    ): CategoryManagementResult
}

/**
 * Result of category management operations
 */
sealed class CategoryManagementResult {
    data class Success(val category: Category) : CategoryManagementResult()
    data class Error(val message: String, val code: CategoryErrorCode) : CategoryManagementResult()
}

/**
 * Error codes for category management operations
 */
enum class CategoryErrorCode {
    CATEGORY_NOT_FOUND,
    CATEGORY_NAME_EXISTS,
    INVALID_PARENT_CATEGORY,
    CANNOT_DELETE_DEFAULT_CATEGORY,
    CATEGORY_IN_USE,
    INVALID_COLOR_FORMAT,
    INVALID_CATEGORY_NAME
}

/**
 * Statistics about category usage
 */
data class CategoryUsageStats(
    val category: Category,
    val transactionCount: Int,
    val totalAmount: com.north.mobile.domain.model.Money,
    val averageAmount: com.north.mobile.domain.model.Money,
    val lastUsed: kotlinx.datetime.LocalDate?,
    val usageFrequency: UsageFrequency
)

/**
 * Frequency of category usage
 */
enum class UsageFrequency {
    NEVER,
    RARELY,      // Less than once per month
    OCCASIONALLY, // 1-4 times per month
    REGULARLY,    // 1-2 times per week
    FREQUENTLY    // 3+ times per week
}

/**
 * Suggestion for category improvements
 */
data class CategorySuggestion(
    val type: CategorySuggestionType,
    val title: String,
    val description: String,
    val actionable: Boolean = true,
    val relatedCategoryIds: List<String> = emptyList()
)

/**
 * Types of category suggestions
 */
enum class CategorySuggestionType {
    CREATE_SUBCATEGORY,
    MERGE_SIMILAR_CATEGORIES,
    DELETE_UNUSED_CATEGORY,
    RENAME_CATEGORY,
    SPLIT_BROAD_CATEGORY
}

/**
 * Implementation of category management service
 */
class CategoryManagementServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val transactionHistoryProvider: TransactionHistoryProvider
) : CategoryManagementService {
    
    override suspend fun getAllCategories(): List<Category> {
        return categoryRepository.getAllCategories()
    }
    
    override suspend fun getCustomCategories(): List<Category> {
        return categoryRepository.getCustomCategories()
    }
    
    override suspend fun createCustomCategory(
        name: String,
        parentCategoryId: String?,
        color: String?,
        icon: String?
    ): CategoryManagementResult {
        // Validate category name
        if (name.isBlank()) {
            return CategoryManagementResult.Error(
                "Category name cannot be empty",
                CategoryErrorCode.INVALID_CATEGORY_NAME
            )
        }
        
        // Check if category name already exists
        val existingCategories = getAllCategories()
        if (existingCategories.any { it.name.equals(name, ignoreCase = true) }) {
            return CategoryManagementResult.Error(
                "Category with name '$name' already exists",
                CategoryErrorCode.CATEGORY_NAME_EXISTS
            )
        }
        
        // Validate parent category if provided
        if (parentCategoryId != null) {
            val parentCategory = categoryRepository.getCategoryById(parentCategoryId)
            if (parentCategory == null) {
                return CategoryManagementResult.Error(
                    "Parent category not found",
                    CategoryErrorCode.INVALID_PARENT_CATEGORY
                )
            }
        }
        
        // Validate color format if provided
        if (color != null && !isValidHexColor(color)) {
            return CategoryManagementResult.Error(
                "Invalid color format. Use hex format (#RRGGBB)",
                CategoryErrorCode.INVALID_COLOR_FORMAT
            )
        }
        
        // Create new category
        val categoryId = generateCategoryId(name)
        val newCategory = Category(
            id = categoryId,
            name = name,
            parentCategoryId = parentCategoryId,
            color = color,
            icon = icon,
            isCustom = true
        )
        
        val createdCategory = categoryRepository.createCustomCategory(newCategory)
        return CategoryManagementResult.Success(createdCategory)
    }
    
    override suspend fun updateCustomCategory(category: Category): CategoryManagementResult {
        if (!category.isCustom) {
            return CategoryManagementResult.Error(
                "Cannot update default categories",
                CategoryErrorCode.CANNOT_DELETE_DEFAULT_CATEGORY
            )
        }
        
        // Validate category exists
        val existingCategory = categoryRepository.getCategoryById(category.id)
        if (existingCategory == null) {
            return CategoryManagementResult.Error(
                "Category not found",
                CategoryErrorCode.CATEGORY_NOT_FOUND
            )
        }
        
        // Validate color format if provided
        if (category.color != null && !isValidHexColor(category.color)) {
            return CategoryManagementResult.Error(
                "Invalid color format. Use hex format (#RRGGBB)",
                CategoryErrorCode.INVALID_COLOR_FORMAT
            )
        }
        
        val updatedCategory = categoryRepository.updateCategory(category)
        return CategoryManagementResult.Success(updatedCategory)
    }
    
    override suspend fun deleteCustomCategory(
        categoryId: String,
        reassignToCategoryId: String?
    ): CategoryManagementResult {
        // Validate category exists and is custom
        val category = categoryRepository.getCategoryById(categoryId)
        if (category == null) {
            return CategoryManagementResult.Error(
                "Category not found",
                CategoryErrorCode.CATEGORY_NOT_FOUND
            )
        }
        
        if (!category.isCustom) {
            return CategoryManagementResult.Error(
                "Cannot delete default categories",
                CategoryErrorCode.CANNOT_DELETE_DEFAULT_CATEGORY
            )
        }
        
        // Check if category is in use
        val transactionsUsingCategory = transactionHistoryProvider.getTransactionsByCategory(categoryId)
        if (transactionsUsingCategory.isNotEmpty() && reassignToCategoryId == null) {
            return CategoryManagementResult.Error(
                "Category is in use by ${transactionsUsingCategory.size} transactions. " +
                "Please specify a category to reassign transactions to.",
                CategoryErrorCode.CATEGORY_IN_USE
            )
        }
        
        // Validate reassignment category if provided
        if (reassignToCategoryId != null) {
            val reassignCategory = categoryRepository.getCategoryById(reassignToCategoryId)
            if (reassignCategory == null) {
                return CategoryManagementResult.Error(
                    "Reassignment category not found",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )
            }
        }
        
        // TODO: Reassign transactions to new category
        // This would require a transaction update service
        
        // Delete the category
        categoryRepository.deleteCategory(categoryId)
        
        return CategoryManagementResult.Success(category)
    }
    
    override suspend fun getCategoryUsageStats(): List<CategoryUsageStats> {
        val categories = getAllCategories()
        val allTransactions = transactionHistoryProvider.getAllTransactions()
        
        return categories.map { category ->
            val categoryTransactions = allTransactions.filter { it.category.id == category.id }
            
            val totalAmount = categoryTransactions
                .map { it.amount }
                .fold(com.north.mobile.domain.model.Money.ZERO) { acc, amount -> acc + amount }
            
            val averageAmount = if (categoryTransactions.isNotEmpty()) {
                com.north.mobile.domain.model.Money.fromCents(
                    totalAmount.cents / categoryTransactions.size
                )
            } else {
                com.north.mobile.domain.model.Money.ZERO
            }
            
            val lastUsed = categoryTransactions.maxByOrNull { it.date }?.date
            
            val usageFrequency = calculateUsageFrequency(categoryTransactions.size)
            
            CategoryUsageStats(
                category = category,
                transactionCount = categoryTransactions.size,
                totalAmount = totalAmount,
                averageAmount = averageAmount,
                lastUsed = lastUsed,
                usageFrequency = usageFrequency
            )
        }.sortedByDescending { it.transactionCount }
    }
    
    override suspend fun suggestCategoryImprovements(): List<CategorySuggestion> {
        val suggestions = mutableListOf<CategorySuggestion>()
        val usageStats = getCategoryUsageStats()
        
        // Suggest deleting unused custom categories
        val unusedCustomCategories = usageStats.filter { 
            it.category.isCustom && it.usageFrequency == UsageFrequency.NEVER 
        }
        
        unusedCustomCategories.forEach { stats ->
            suggestions.add(
                CategorySuggestion(
                    type = CategorySuggestionType.DELETE_UNUSED_CATEGORY,
                    title = "Delete unused category",
                    description = "Category '${stats.category.name}' has never been used",
                    relatedCategoryIds = listOf(stats.category.id)
                )
            )
        }
        
        // Suggest creating subcategories for heavily used categories
        val heavilyUsedCategories = usageStats.filter { 
            it.transactionCount > 50 && it.category.parentCategoryId == null 
        }
        
        heavilyUsedCategories.forEach { stats ->
            suggestions.add(
                CategorySuggestion(
                    type = CategorySuggestionType.CREATE_SUBCATEGORY,
                    title = "Create subcategories",
                    description = "Category '${stats.category.name}' has ${stats.transactionCount} transactions. " +
                            "Consider creating subcategories for better organization.",
                    relatedCategoryIds = listOf(stats.category.id)
                )
            )
        }
        
        // Suggest merging similar low-usage categories
        val lowUsageCategories = usageStats.filter { 
            it.category.isCustom && it.transactionCount in 1..5 
        }
        
        if (lowUsageCategories.size >= 2) {
            suggestions.add(
                CategorySuggestion(
                    type = CategorySuggestionType.MERGE_SIMILAR_CATEGORIES,
                    title = "Merge similar categories",
                    description = "You have ${lowUsageCategories.size} custom categories with very few transactions. " +
                            "Consider merging similar ones.",
                    relatedCategoryIds = lowUsageCategories.map { it.category.id }
                )
            )
        }
        
        return suggestions
    }
    
    override suspend fun mergeCategories(
        sourceCategoryId: String,
        targetCategoryId: String
    ): CategoryManagementResult {
        // Validate both categories exist
        val sourceCategory = categoryRepository.getCategoryById(sourceCategoryId)
        val targetCategory = categoryRepository.getCategoryById(targetCategoryId)
        
        if (sourceCategory == null) {
            return CategoryManagementResult.Error(
                "Source category not found",
                CategoryErrorCode.CATEGORY_NOT_FOUND
            )
        }
        
        if (targetCategory == null) {
            return CategoryManagementResult.Error(
                "Target category not found",
                CategoryErrorCode.CATEGORY_NOT_FOUND
            )
        }
        
        // Cannot merge default categories
        if (!sourceCategory.isCustom) {
            return CategoryManagementResult.Error(
                "Cannot merge default categories",
                CategoryErrorCode.CANNOT_DELETE_DEFAULT_CATEGORY
            )
        }
        
        // TODO: Reassign all transactions from source to target category
        // This would require a transaction update service
        
        // Delete the source category
        categoryRepository.deleteCategory(sourceCategoryId)
        
        return CategoryManagementResult.Success(targetCategory)
    }
    
    private fun generateCategoryId(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }
    
    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }
    
    private fun calculateUsageFrequency(transactionCount: Int): UsageFrequency {
        return when {
            transactionCount == 0 -> UsageFrequency.NEVER
            transactionCount < 4 -> UsageFrequency.RARELY
            transactionCount < 16 -> UsageFrequency.OCCASIONALLY
            transactionCount < 52 -> UsageFrequency.REGULARLY
            else -> UsageFrequency.FREQUENTLY
        }
    }
}