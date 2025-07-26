# Transaction Categorization Implementation

This document describes the implementation of the transaction categorization system with machine learning capabilities for the North mobile app.

## Overview

The transaction categorization system automatically categorizes financial transactions using machine learning models, with special focus on Canadian merchant patterns and spending behaviors. The system includes:

1. **ML-based Transaction Categorization**: Automatically categorizes transactions using rule-based and statistical models
2. **Canadian Merchant Pattern Recognition**: Specialized patterns for Canadian financial institutions and merchants
3. **User Feedback Learning**: Learns from user corrections to improve accuracy over time
4. **Unusual Spending Detection**: Identifies anomalous spending patterns and potential fraud
5. **Category Management**: Allows users to create and manage custom categories

## Architecture

### Core Components

1. **TransactionCategorizationService**: Main service for categorizing transactions
2. **CategorizationModel**: ML model combining rule-based and statistical approaches
3. **CategoryManagementService**: Service for managing categories and customizations
4. **AnomalyDetectionModel**: Model for detecting unusual spending patterns
5. **Training Data Pipeline**: System for managing training data and user feedback

### Key Features

#### 1. Multi-layered Categorization Approach

The system uses a three-tier approach for maximum accuracy:

1. **Rule-based Categorization**: High-confidence rules for common patterns
2. **Canadian Merchant Pattern Matching**: Specialized patterns for Canadian merchants
3. **Statistical Model**: Fallback statistical model for edge cases

#### 2. Canadian-Specific Patterns

The system includes specialized recognition for Canadian merchants and patterns:

- **Major Banks**: RBC, TD, BMO, Scotiabank, CIBC
- **Grocery Chains**: Loblaws, Metro, Sobeys, No Frills, FreshCo
- **Restaurant Chains**: Tim Hortons, Harvey's, Swiss Chalet, Boston Pizza
- **Gas Stations**: Petro-Canada, Esso, Shell Canada, Husky
- **Utilities**: Hydro One, Rogers, Bell, Telus
- **Retail**: Canadian Tire, Shoppers Drug Mart, The Bay

#### 3. Unusual Spending Detection

The system detects various types of unusual spending:

- **Amount Anomalies**: Transactions significantly higher/lower than usual for a category
- **Frequency Anomalies**: Unusual frequency of transactions at the same merchant
- **New Merchants**: First-time transactions at new merchants
- **Potential Duplicates**: Transactions that might be duplicate charges
- **Location Anomalies**: Transactions in unusual locations (when location data available)

## Implementation Details

### Core Service Usage

```kotlin
// Initialize the categorization service
val categorizationService = TransactionCategorizationServiceImpl(
    trainingDataProvider = trainingDataProvider,
    userFeedbackRepository = userFeedbackRepository,
    transactionHistoryProvider = transactionHistoryProvider
)

// Categorize a single transaction
val transaction = Transaction(
    id = "tx123",
    accountId = "account1",
    amount = Money.fromDollars(-4.50),
    description = "TIM HORTONS #1234",
    category = Category.UNCATEGORIZED,
    date = LocalDate.now(),
    merchantName = "Tim Hortons"
)

val result = categorizationService.categorizeTransaction(transaction)
println("Suggested category: ${result.suggestedCategory.name}")
println("Confidence: ${result.confidence}")
println("Reasoning: ${result.reasoning}")

// Batch categorization
val transactions = listOf(transaction1, transaction2, transaction3)
val results = categorizationService.categorizeTransactions(transactions)

// Provide user feedback for learning
categorizationService.provideFeedback("tx123", Category.RESTAURANTS, 1.0f)

// Detect unusual spending
val alerts = categorizationService.detectUnusualSpending(transactions)
alerts.forEach { alert ->
    println("Alert: ${alert.message}")
    println("Severity: ${alert.severity}")
    println("Suggested action: ${alert.suggestedAction}")
}
```

### Category Management

```kotlin
// Initialize category management service
val categoryService = CategoryManagementServiceImpl(
    categoryRepository = categoryRepository,
    transactionHistoryProvider = transactionHistoryProvider
)

// Create custom category
val result = categoryService.createCustomCategory(
    name = "Coffee Shops",
    parentCategoryId = Category.RESTAURANTS.id,
    color = "#8B4513",
    icon = "☕"
)

when (result) {
    is CategoryManagementResult.Success -> {
        println("Created category: ${result.category.name}")
    }
    is CategoryManagementResult.Error -> {
        println("Error: ${result.message}")
    }
}

// Get category usage statistics
val usageStats = categoryService.getCategoryUsageStats()
usageStats.forEach { stats ->
    println("${stats.category.name}: ${stats.transactionCount} transactions")
    println("Total amount: ${stats.totalAmount}")
    println("Usage frequency: ${stats.usageFrequency}")
}

// Get improvement suggestions
val suggestions = categoryService.suggestCategoryImprovements()
suggestions.forEach { suggestion ->
    println("Suggestion: ${suggestion.title}")
    println("Description: ${suggestion.description}")
}
```

## Canadian Merchant Patterns

The system includes comprehensive patterns for Canadian merchants:

### Financial Institutions
- RBC Royal Bank, TD Canada Trust, Bank of Montreal
- Scotiabank, CIBC, Tangerine, President's Choice Financial
- Desjardins (Quebec)

### Grocery Stores
- Loblaws, Metro, Sobeys, IGA
- No Frills, FreshCo, Food Basics
- Costco, Walmart Supercentre

### Restaurants & Fast Food
- Tim Hortons, Harvey's, Swiss Chalet
- Boston Pizza, Kelsey's, Montana's
- A&W Canada, Dairy Queen

### Gas Stations
- Petro-Canada, Esso, Shell Canada
- Husky, Ultramar, Fas Gas

### Utilities & Telecom
- Hydro One, BC Hydro, Hydro-Québec
- Rogers, Bell, Telus, Shaw
- Enbridge, FortisBC

### Retail
- Canadian Tire, Shoppers Drug Mart
- The Bay, Winners, HomeSense
- Rona, Home Depot Canada

## Machine Learning Approach

### Rule-Based System

High-confidence rules for common patterns:

```kotlin
// Example rule for restaurants
CategorizationRule(
    condition = { features ->
        features.descriptionWords.any { word ->
            listOf("restaurant", "cafe", "coffee", "pizza").any { 
                word.contains(it) 
            }
        }
    },
    category = Category.RESTAURANTS,
    confidence = 0.9f,
    reasoning = "Matched restaurant keywords"
)
```

### Statistical Model

Uses feature extraction and statistical analysis:

- **Amount-based features**: Transaction amount, amount ranges
- **Temporal features**: Day of week, time of day, month
- **Text features**: Description words, merchant name analysis
- **Behavioral features**: Recurring patterns, frequency analysis

### Training Data

The system includes Canadian-specific training data:

- Common Canadian merchant transactions
- Typical spending patterns for Canadian consumers
- Regional variations (Quebec, Ontario, BC, etc.)
- Seasonal spending patterns

## Anomaly Detection

### Types of Anomalies Detected

1. **Amount Anomalies**
   - Transactions beyond 2 standard deviations from category mean
   - Severity based on deviation magnitude

2. **Frequency Anomalies**
   - Multiple transactions at same merchant on same day
   - Unusual frequency patterns

3. **Duplicate Detection**
   - Same amount, merchant, and date
   - Potential duplicate charges

4. **New Merchant Alerts**
   - First-time transactions at new merchants
   - Helps identify potential fraud

### Alert Severity Levels

- **LOW**: Minor anomalies, informational
- **MEDIUM**: Moderate anomalies requiring attention
- **HIGH**: Significant anomalies requiring review
- **CRITICAL**: Severe anomalies requiring immediate action

## Testing

The implementation includes comprehensive tests:

### Unit Tests
- Individual component testing
- Canadian merchant pattern validation
- ML model accuracy testing
- Anomaly detection validation

### Integration Tests
- End-to-end workflow testing
- Multi-component interaction testing
- Real-world scenario simulation

### Test Coverage
- Transaction categorization accuracy
- User feedback learning
- Unusual spending detection
- Category management operations
- Canadian-specific pattern matching

## Performance Considerations

### Optimization Strategies

1. **Caching**: Cache frequently used patterns and models
2. **Batch Processing**: Process multiple transactions efficiently
3. **Lazy Loading**: Load models only when needed
4. **Memory Management**: Efficient memory usage for large datasets

### Scalability

- **Model Updates**: Incremental learning without full retraining
- **Data Storage**: Efficient storage of training data and feedback
- **Processing Speed**: Fast categorization for real-time usage

## Future Enhancements

### Planned Improvements

1. **Deep Learning Models**: More sophisticated ML models
2. **Regional Customization**: Province-specific patterns
3. **Seasonal Adjustments**: Seasonal spending pattern recognition
4. **Cross-User Learning**: Anonymous learning from user patterns
5. **Advanced Fraud Detection**: More sophisticated fraud detection

### Integration Points

- **Plaid Integration**: Enhanced merchant data from Plaid
- **Bank APIs**: Direct integration with Canadian bank APIs
- **Government Data**: Integration with CRA tax categories
- **Regional Data**: Provincial and municipal spending patterns

## Compliance and Privacy

### Privacy Protection
- No personal data in training models
- Anonymous pattern recognition
- User consent for feedback usage
- PIPEDA compliance for Canadian users

### Data Security
- Encrypted storage of user feedback
- Secure model training processes
- No sensitive data in logs
- Audit trails for compliance

## Conclusion

The transaction categorization system provides intelligent, Canadian-focused categorization with machine learning capabilities. The system learns from user feedback, detects unusual spending patterns, and provides comprehensive category management features while maintaining privacy and security standards required for financial applications.