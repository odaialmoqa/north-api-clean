import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.north.mobile.ui.dashboard.model.*
import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.TrendDirection
import kotlinx.datetime.Clock

class DashboardTest {
    
    @Test
    fun testNetWorthDisplayData() {
        val netWorthData = NetWorthDisplayData(
            currentNetWorth = Money.fromDollars(47250.0),
            monthlyChange = Money.fromDollars(1200.0),
            changePercentage = 2.6,
            trend = TrendDirection.INCREASING,
            totalAssets = Money.fromDollars(65000.0),
            totalLiabilities = Money.fromDollars(17750.0),
            progressToGoal = 0.68
        )
        
        assertEquals(47250_00L, netWorthData.currentNetWorth.amount)
        assertEquals(1200_00L, netWorthData.monthlyChange?.amount)
        assertEquals(2.6, netWorthData.changePercentage)
        assertEquals(TrendDirection.INCREASING, netWorthData.trend)
        assertTrue(netWorthData.currentNetWorth.isPositive)
    }
    
    @Test
    fun testAccountDisplayData() {
        val account = AccountDisplayData(
            id = "test_account",
            displayName = "Test Checking",
            institutionName = "Test Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(2450.0),
            trend = TrendDirection.STABLE,
            lastUpdated = Clock.System.now()
        )
        
        assertEquals("test_account", account.id)
        assertEquals("Test Checking", account.displayName)
        assertEquals(AccountType.CHECKING, account.accountType)
        assertEquals(2450_00L, account.balance.amount)
        assertTrue(account.isActive)
    }
    
    @Test
    fun testGamificationDisplayData() {
        val gamificationData = GamificationDisplayData(
            level = 7,
            totalPoints = 1250,
            pointsToNextLevel = 250,
            levelProgress = 0.83,
            activeStreaks = emptyList(),
            availableMicroWins = emptyList(),
            recentAchievements = emptyList()
        )
        
        assertEquals(7, gamificationData.level)
        assertEquals(1250, gamificationData.totalPoints)
        assertEquals(250, gamificationData.pointsToNextLevel)
        assertEquals(0.83, gamificationData.levelProgress)
    }
    
    @Test
    fun testDashboardState() {
        val dashboardState = DashboardState(
            userName = "Alex",
            isLoading = false,
            error = null,
            netWorthData = null,
            accounts = emptyList(),
            gamificationData = null,
            insights = emptyList(),
            lastUpdated = Clock.System.now()
        )
        
        assertEquals("Alex", dashboardState.userName)
        assertEquals(false, dashboardState.isLoading)
        assertEquals(null, dashboardState.error)
        assertNotNull(dashboardState.lastUpdated)
    }
    
    @Test
    fun testDashboardActions() {
        val refreshAction = DashboardAction.RefreshData
        val accountAction = DashboardAction.ViewAccountDetails("account_123")
        val microWinAction = DashboardAction.CompleteMicroWin("microwin_456")
        
        assertTrue(refreshAction is DashboardAction.RefreshData)
        assertTrue(accountAction is DashboardAction.ViewAccountDetails)
        assertTrue(microWinAction is DashboardAction.CompleteMicroWin)
        
        assertEquals("account_123", (accountAction as DashboardAction.ViewAccountDetails).accountId)
        assertEquals("microwin_456", (microWinAction as DashboardAction.CompleteMicroWin).microWinId)
    }
}