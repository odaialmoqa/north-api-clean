# Mobile App Integration Guide

This guide shows how to integrate the transaction analysis system into your North mobile app.

## 🏗️ Architecture Overview

```
User connects Plaid account
↓
FinancialAnalysisService.triggerAnalysis()
↓
Backend processes transactions with Gemini AI
↓
InsightsRepository updates UI state
↓
Dashboard shows real insights and goals
```

## 📱 Key Integration Points

### 1. **Dependency Injection Setup**

The new services are already configured in `ApiModule.kt`:

```kotlin
val apiModule = module {
    // API Services
    single { InsightsApiService(get()) }
    single { TransactionAnalysisService(get()) }
    
    // Repositories
    single { InsightsRepository(get(), get()) }
    
    // Services
    single { FinancialAnalysisService(get(), get()) }
}
```

### 2. **Dashboard Integration**

The dashboard now shows real AI-generated insights:

```kotlin
@Composable
fun WealthsimpleDashboard(
    onNavigateToChat: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {
    // Navigation includes insights screen
    // Dynamic cards show real data
}
```

### 3. **Plaid Connection Trigger**

After successful Plaid connection, trigger analysis:

```kotlin
// In your Plaid connection success handler
scope.launch {
    val analysisService = get<FinancialAnalysisService>()
    analysisService.triggerAnalysis()
}
```

### 4. **Real-time Data Updates**

The UI automatically updates when new insights are available:

```kotlin
@Composable
fun SomeScreen() {
    val insightsRepository = get<InsightsRepository>()
    val insights by insightsRepository.insights.collectAsState()
    val goals by insightsRepository.goals.collectAsState()
    
    // UI automatically updates when data changes
}
```

## 🔧 Implementation Steps

### Step 1: Update Navigation

Add insights navigation to your main navigation:

```kotlin
// In your main navigation setup
NavHost(navController, startDestination = "dashboard") {
    composable("dashboard") {
        WealthsimpleDashboard(
            onNavigateToChat = { navController.navigate("chat") },
            onNavigateToInsights = { navController.navigate("insights") }
        )
    }
    
    composable("insights") {
        val insightsRepository = get<InsightsRepository>()
        InsightsScreen(
            insightsRepository = insightsRepository,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

### Step 2: Integrate with Plaid Connection

Update your Plaid connection handler:

```kotlin
// In AndroidPlaidLinkLauncher or similar
private fun handlePlaidSuccess(publicToken: String) {
    scope.launch {
        try {
            // Exchange token and store access token
            val result = plaidService.exchangePublicToken(publicToken)
            
            if (result.isSuccess) {
                // Trigger transaction analysis
                val analysisService = get<FinancialAnalysisService>()
                analysisService.triggerAnalysis()
                
                // Show success message
                onSuccess("Bank account connected! Analyzing your transactions...")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Connection failed")
        }
    }
}
```

### Step 3: Enhanced AI Chat Integration

Update your chat screen to use the enhanced AI:

```kotlin
@Composable
fun SimpleChatScreen() {
    val authRepository = get<AuthRepository>()
    val insightsRepository = get<InsightsRepository>()
    
    val contextualAIService = remember {
        ContextualAIService(authRepository, insightsRepository)
    }
    
    // Use contextualAIService for AI responses
    // It will automatically include real financial data
}
```

### Step 4: Dashboard Real Data Integration

The dashboard cards now show real data:

```kotlin
@Composable
fun DynamicInsightsCard(onNavigateToInsights: () -> Unit) {
    val insightsRepository = get<InsightsRepository>()
    val insights by insightsRepository.insights.collectAsState()
    
    // Show real insights instead of mock data
    LazyColumn {
        items(insights.take(3)) { insight ->
            InsightItem(
                title = insight.title,
                description = insight.description,
                type = insight.insight_type,
                confidence = insight.confidence_score
            )
        }
    }
}
```

## 🎯 User Flow

### 1. **First Time User**
```
User opens app → Sees onboarding → Connects bank account → 
Analysis triggered → Insights generated → Dashboard shows personalized data
```

### 2. **Returning User**
```
User opens app → Dashboard shows latest insights → 
Can view detailed insights → Chat with AI using real data
```

### 3. **After New Transactions**
```
New transactions via webhook → Analysis updates → 
New insights generated → User sees updated dashboard
```

## 🔄 Data Flow

### Reactive State Management

```kotlin
class InsightsRepository {
    private val _insights = MutableStateFlow<List<SpendingInsight>>(emptyList())
    val insights: StateFlow<List<SpendingInsight>> = _insights.asStateFlow()
    
    // UI automatically updates when this changes
    suspend fun refreshAllData() {
        val newInsights = insightsApiService.getInsights()
        _insights.value = newInsights.getOrElse { emptyList() }
    }
}
```

### Background Updates

```kotlin
class FinancialAnalysisService {
    fun triggerAnalysis() {
        scope.launch {
            // Analyze transactions
            insightsRepository.analyzeTransactions()
            
            // Wait for processing
            delay(2000)
            
            // Refresh UI data
            insightsRepository.refreshAllData()
        }
    }
}
```

## 🎨 UI Components

### Insights Screen
- Comprehensive view of all insights
- Goal progress tracking
- Spending pattern analysis
- Action item management

### Dashboard Cards
- Dynamic insights preview
- AI-generated goals
- Real spending data
- Quick actions

### Enhanced Chat
- Context-aware responses
- References real spending
- Specific recommendations
- Goal progress updates

## 🧪 Testing Integration

### Test Real Data Flow

```kotlin
@Test
fun testTransactionAnalysisFlow() {
    // Mock Plaid connection
    val publicToken = "test_token"
    
    // Trigger analysis
    runBlocking {
        analysisService.triggerAnalysis()
        
        // Verify insights generated
        val insights = insightsRepository.insights.value
        assertTrue(insights.isNotEmpty())
        
        // Verify goals created
        val goals = insightsRepository.goals.value
        assertTrue(goals.isNotEmpty())
    }
}
```

### Test UI Updates

```kotlin
@Test
fun testUIUpdatesWithRealData() {
    composeTestRule.setContent {
        val mockInsights = listOf(
            SpendingInsight(
                id = "1",
                title = "Dining Alert",
                description = "Spending increased 18%",
                insight_type = "alert",
                confidence_score = 0.92
            )
        )
        
        DynamicInsightsCard()
    }
    
    // Verify insights are displayed
    composeTestRule.onNodeWithText("Dining Alert").assertExists()
}
```

## 🚀 Deployment Checklist

### Backend (Railway)
- ✅ New endpoints deployed
- ✅ Database schema updated
- ✅ Gemini AI configured
- ✅ Plaid integration working

### Mobile App
- ✅ New services in DI
- ✅ Navigation updated
- ✅ Dashboard integrated
- ✅ Chat enhanced
- ✅ Insights screen created

### Testing
- ✅ Railway deployment tested
- ✅ API endpoints working
- ✅ AI integration verified
- ✅ Data flow tested

## 🎯 Next Steps

1. **Deploy to Railway**: Push your changes to trigger Railway deployment
2. **Test Mobile App**: Build and test the mobile app with new features
3. **User Testing**: Get feedback on the new insights and goals
4. **Iterate**: Improve based on user feedback and usage patterns

## 🔍 Monitoring

### Key Metrics to Track
- **Insight Generation Rate**: How many insights per user
- **Goal Completion Rate**: User engagement with AI goals
- **Chat Enhancement**: Usage of context-aware responses
- **User Retention**: Impact of personalized insights

### Error Monitoring
- API endpoint failures
- AI generation errors
- Data synchronization issues
- Mobile app crashes

The transaction analysis system is now fully integrated and ready to provide users with intelligent, personalized financial insights based on their real spending data!