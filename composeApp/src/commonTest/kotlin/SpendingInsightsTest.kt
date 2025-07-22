import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.north.mobile.ui.insights.model.*
import com.north.mobile.data.analytics.*
import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class SpendingInsightsTest {
    
    @Test
    fun testSpendingInsightsStateInitialization() {
        val state = SpendingInsightsState()
        
        assertEquals(TimePeriod.THIS_MONTH, state.selectedPeriod)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertEquals(emptyList(), state.categorySpending)
        assertEquals(emptyList(), state.spendingTrends)
        assertEquals(emptyList(), state.spendingAlerts)
        assertEquals(emptyList(), state.recommendations)
    }
    
    @Test
    fun testTimePeriodDisplayNames() {
        assertEquals("This Month", TimePeriod.THIS_MONTH.displayName)
        assertEquals("Last Month", TimePeriod.LAST_MONTH.displayName)
        assertEquals("Last 3 Months", TimePeriod.LAST_3_MONTHS.displayName)
        assertEquals("Last 6 Months", TimePeriod.LAST_6_MONTHS.displayName)
        assertEquals("This Year", TimePeriod.THIS_YEAR.displayName)
    }
    
    @Test
    fun testPeriodSummaryDisplay() {
        val totalSpent = Money(1500.0, Currency.CAD)
        val totalIncome = Money(3000.0, Currency.CAD)
        val netCashFlow = Money(1500.0, Currency.CAD)
        
        val comparison = PeriodComparisonDisplay(
            changeAmount = Money(200.0, Currency.CAD),
            changePercentage = 15.0,
            isIncrease = true
        )
        
        val summary = PeriodSummaryDisplay(
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netCashFlow = netCashFlow,
            comparisonToPrevious = comparison
        )
        
        assertEquals(totalSpent, summary.totalSpent)
        assertEquals(totalIncome, summary.totalIncome)
        assertEquals(netCashFlow, summary.netCashFlow)
        assertNotNull(summary.comparisonToPrevious)
        assertTrue(summary.comparisonToPrevious!!.isIncrease)
        assertEquals(15.0, summary.comparisonToPrevious!!.changePercentage)
    }
    
    @Test
    fun testCategorySpendingDisplay() {
        val category = Category(
            id = "food",
            name = "Food & Dining",
            displayName = "Food & Dining",
            icon = "🍽️",
            color = "#FF9800",
            parentId = null
        )
        
        val categorySpending = CategorySpendingDisplay(
            category = category,
            totalAmount = Money(500.0, Currency.CAD),
            transactionCount = 25,
            averageAmount = Money(20.0, Currency.CAD),
            percentageOfTotal = 33.3,
            trend = TrendDirection.INCREASING,
            comparedToPrevious = Money(50.0, Currency.CAD)
        )
        
        assertEquals(category, categorySpending.category)
        assertEquals(500.0, categorySpending.totalAmount.amount)
        assertEquals(25, categorySpending.transactionCount)
        assertEquals(20.0, categorySpending.averageAmount.amount)
        assertEquals(33.3, categorySpending.percentageOfTotal)
        assertEquals(TrendDirection.INCREASING, categorySpending.trend)
        assertNotNull(categorySpending.comparedToPrevious)
        assertEquals(50.0, categorySpending.comparedToPrevious!!.amount)
    }
    
    @Test
    fun testBudgetComparisonDisplay() {
        val totalBudget = Money(2000.0, Currency.CAD)
        val totalSpent = Money(1800.0, Currency.CAD)
        val remainingBudget = Money(200.0, Currency.CAD)
        
        val budgetComparison = BudgetComparisonDisplay(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            overallPerformance = BudgetPerformance.ON_TRACK,
            categoryComparisons = emptyList()
        )
        
        assertEquals(totalBudget, budgetComparison.totalBudget)
        assertEquals(totalSpent, budgetComparison.totalSpent)
        assertEquals(remainingBudget, budgetComparison.remainingBudget)
        assertEquals(BudgetPerformance.ON_TRACK, budgetComparison.overallPerformance)
        assertTrue(budgetComparison.categoryComparisons.isEmpty())
    }
    
    @Test
    fun testSpendingTrendDisplay() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateRange = DateRange(
            startDate = today.minus(30, kotlinx.datetime.DateTimeUnit.DAY),
            endDate = today
        )
        
        val trend = SpendingTrendDisplay(
            id = "food_spending_trend",
            category = null,
            trendType = TrendType.SPENDING,
            direction = TrendDirection.INCREASING,
            magnitude = 15.5,
            confidence = 0.85f,
            description = "Your spending has increased by 15.5% this month",
            timeframe = dateRange
        )
        
        assertEquals("food_spending_trend", trend.id)
        assertEquals(TrendType.SPENDING, trend.trendType)
        assertEquals(TrendDirection.INCREASING, trend.direction)
        assertEquals(15.5, trend.magnitude)
        assertEquals(0.85f, trend.confidence)
        assertTrue(trend.description.contains("15.5%"))
    }
    
    @Test
    fun testSpendingAlertDisplay() {
        val category = Category(
            id = "entertainment",
            name = "Entertainment",
            displayName = "Entertainment",
            icon = "🎬",
            color = "#9C27B0",
            parentId = null
        )
        
        val alert = SpendingAlertDisplay(
            id = "alert_1",
            type = SpendingAlertType.UNUSUAL_SPENDING,
            title = "Unusual Entertainment Spending",
            description = "You've spent 50% more on entertainment this week",
            severity = AlertSeverity.MEDIUM,
            category = category,
            potentialSavings = Money(100.0, Currency.CAD)
        )
        
        assertEquals("alert_1", alert.id)
        assertEquals(SpendingAlertType.UNUSUAL_SPENDING, alert.type)
        assertEquals("Unusual Entertainment Spending", alert.title)
        assertEquals(AlertSeverity.MEDIUM, alert.severity)
        assertEquals(category, alert.category)
        assertNotNull(alert.potentialSavings)
        assertEquals(100.0, alert.potentialSavings!!.amount)
    }
    
    @Test
    fun testRecommendationDisplay() {
        val recommendation = RecommendationDisplay(
            id = "rec_1",
            type = RecommendationType.SPENDING_REDUCTION,
            title = "Reduce Dining Out",
            description = "Consider cooking more meals at home to save money",
            reasoning = "You're spending 40% more on dining out than similar users",
            priority = Priority.MEDIUM,
            category = null,
            potentialImpact = Money(200.0, Currency.CAD),
            confidence = 0.75f,
            actionSteps = listOf(
                "Plan weekly meals",
                "Buy groceries in bulk",
                "Limit dining out to weekends"
            )
        )
        
        assertEquals("rec_1", recommendation.id)
        assertEquals(RecommendationType.SPENDING_REDUCTION, recommendation.type)
        assertEquals("Reduce Dining Out", recommendation.title)
        assertEquals(Priority.MEDIUM, recommendation.priority)
        assertEquals(0.75f, recommendation.confidence)
        assertEquals(3, recommendation.actionSteps.size)
        assertTrue(recommendation.actionSteps.contains("Plan weekly meals"))
        assertNotNull(recommendation.potentialImpact)
        assertEquals(200.0, recommendation.potentialImpact!!.amount)
    }
    
    @Test
    fun testSpendingInsightsActions() {
        val refreshAction = SpendingInsightsAction.RefreshData
        val changePeriodAction = SpendingInsightsAction.ChangePeriod(TimePeriod.LAST_MONTH)
        val viewCategoryAction = SpendingInsightsAction.ViewCategoryDetails("food")
        val dismissAlertAction = SpendingInsightsAction.DismissAlert("alert_1")
        
        assertTrue(refreshAction is SpendingInsightsAction.RefreshData)
        assertTrue(changePeriodAction is SpendingInsightsAction.ChangePeriod)
        assertTrue(viewCategoryAction is SpendingInsightsAction.ViewCategoryDetails)
        assertTrue(dismissAlertAction is SpendingInsightsAction.DismissAlert)
        
        assertEquals(TimePeriod.LAST_MONTH, (changePeriodAction as SpendingInsightsAction.ChangePeriod).period)
        assertEquals("food", (viewCategoryAction as SpendingInsightsAction.ViewCategoryDetails).category)
        assertEquals("alert_1", (dismissAlertAction as SpendingInsightsAction.DismissAlert).alertId)
    }
}