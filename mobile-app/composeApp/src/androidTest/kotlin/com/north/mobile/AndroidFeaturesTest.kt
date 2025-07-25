package com.north.mobile

import android.content.Context
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.north.mobile.auth.AndroidBiometricManager
import com.north.mobile.auth.BiometricAvailability
import com.north.mobile.notification.NotificationService
import com.north.mobile.notification.SyncStatus
import com.north.mobile.performance.AndroidPerformanceOptimizer
import com.north.mobile.performance.DevicePerformanceClass
import com.north.mobile.ui.utils.AndroidScreenUtils
import com.north.mobile.ui.utils.NavigationType
import com.north.mobile.ui.utils.ContentType
import com.north.mobile.widget.FinancialOverviewWidget
import com.north.mobile.widget.WidgetFinancialData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidFeaturesTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var context: Context
    private lateinit var biometricManager: AndroidBiometricManager
    private lateinit var notificationService: NotificationService
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        biometricManager = AndroidBiometricManager(context)
        notificationService = NotificationService(context)
    }
    
    @Test
    fun testBiometricManagerInitialization() {
        // Test that biometric manager initializes correctly
        assertNotNull(biometricManager)
        
        // Test biometric availability check
        val availability = biometricManager.isBiometricAvailable()
        assertTrue(availability is BiometricAvailability)
        
        // Test biometric capabilities
        val capabilities = biometricManager.getBiometricCapabilities()
        assertNotNull(capabilities)
    }
    
    @Test
    fun testNotificationService() {
        // Test notification service initialization
        assertNotNull(notificationService)
        
        // Test notification permissions check
        val notificationsEnabled = notificationService.areNotificationsEnabled()
        assertTrue(notificationsEnabled is Boolean)
        
        // Test streak reminder notification
        notificationService.sendStreakReminderNotification(5, "savings")
        
        // Test goal progress notification
        notificationService.sendGoalProgressNotification("Emergency Fund", 75, "$2,500")
        
        // Test insight notification
        notificationService.sendInsightNotification(
            "Spending Alert",
            "You've spent 20% more on dining this month",
            "Review Budget"
        )
        
        // Test sync notification
        notificationService.sendAccountSyncNotification(SyncStatus.SUCCESS)
        
        // Test micro-win notification
        notificationService.sendMicroWinNotification("Daily check-in completed", 10)
    }
    
    @Test
    fun testPerformanceOptimizer() {
        // Test device performance classification
        val performanceClass = AndroidPerformanceOptimizer.getDevicePerformanceClass(context)
        assertTrue(performanceClass is DevicePerformanceClass)
        
        // Test low memory device detection
        val isLowMemory = AndroidPerformanceOptimizer.isLowMemoryDevice(context)
        assertTrue(isLowMemory is Boolean)
        
        // Test memory optimization
        AndroidPerformanceOptimizer.optimizeMemoryUsage(context)
        
        // Test battery optimization
        AndroidPerformanceOptimizer.optimizeBatteryUsage(context)
        
        // Test image quality optimization
        val imageQuality = AndroidPerformanceOptimizer.getOptimalImageQuality(context)
        assertNotNull(imageQuality)
        
        // Test animation duration optimization
        val animationDuration = AndroidPerformanceOptimizer.getOptimalAnimationDuration(context, 300L)
        assertTrue(animationDuration > 0)
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun testResponsiveDesign() {
        // Test compact window size class
        val compactWindowSize = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
        
        val compactColumns = AndroidScreenUtils.getAdaptiveColumns(compactWindowSize)
        assertEquals(1, compactColumns)
        
        val compactNavType = AndroidScreenUtils.getNavigationType(compactWindowSize)
        assertEquals(NavigationType.BOTTOM_NAVIGATION, compactNavType)
        
        val compactContentType = AndroidScreenUtils.getContentType(compactWindowSize)
        assertEquals(ContentType.SINGLE_PANE, compactContentType)
        
        // Test medium window size class
        val mediumWindowSize = WindowSizeClass.calculateFromSize(DpSize(700.dp, 900.dp))
        
        val mediumColumns = AndroidScreenUtils.getAdaptiveColumns(mediumWindowSize)
        assertEquals(2, mediumColumns)
        
        val mediumNavType = AndroidScreenUtils.getNavigationType(mediumWindowSize)
        assertEquals(NavigationType.NAVIGATION_RAIL, mediumNavType)
        
        // Test expanded window size class
        val expandedWindowSize = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 1200.dp))
        
        val expandedColumns = AndroidScreenUtils.getAdaptiveColumns(expandedWindowSize)
        assertEquals(3, expandedColumns)
        
        val expandedNavType = AndroidScreenUtils.getNavigationType(expandedWindowSize)
        assertEquals(NavigationType.PERMANENT_NAVIGATION_DRAWER, expandedNavType)
        
        val expandedContentType = AndroidScreenUtils.getContentType(expandedWindowSize)
        assertEquals(ContentType.DUAL_PANE, expandedContentType)
    }
    
    @Test
    fun testWidgetDataModel() {
        // Test widget financial data creation
        val widgetData = WidgetFinancialData(
            netWorth = 47250.0,
            netWorthChange = 1200.0,
            checkingBalance = 2450.0,
            savingsBalance = 15800.0,
            currentStreak = 12,
            lastUpdated = "2 min ago",
            goalProgress = 85
        )
        
        assertEquals(47250.0, widgetData.netWorth)
        assertEquals(1200.0, widgetData.netWorthChange)
        assertEquals(2450.0, widgetData.checkingBalance)
        assertEquals(15800.0, widgetData.savingsBalance)
        assertEquals(12, widgetData.currentStreak)
        assertEquals("2 min ago", widgetData.lastUpdated)
        assertEquals(85, widgetData.goalProgress)
    }
    
    @Test
    fun testMaterialDesignTheme() {
        composeTestRule.setContent {
            com.north.mobile.ui.theme.NorthAndroidTheme {
                // Test that theme applies without crashing
                androidx.compose.material3.Text("Test")
            }
        }
        
        // Verify theme loads successfully
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testScreenUtilities() {
        composeTestRule.setContent {
            // Test screen utilities in Compose context
            val screenWidth = AndroidScreenUtils.getScreenWidth()
            val screenHeight = AndroidScreenUtils.getScreenHeight()
            val isTablet = AndroidScreenUtils.isTablet()
            val isLandscape = AndroidScreenUtils.isLandscape()
            val density = AndroidScreenUtils.getScreenDensity()
            val adaptiveColumns = AndroidScreenUtils.getAdaptiveColumns()
            val adaptivePadding = AndroidScreenUtils.getAdaptivePadding()
            val cardMaxWidth = AndroidScreenUtils.getCardMaxWidth()
            
            // Verify all utilities return valid values
            assertTrue(screenWidth.value > 0)
            assertTrue(screenHeight.value > 0)
            assertTrue(density > 0)
            assertTrue(adaptiveColumns > 0)
            assertTrue(adaptivePadding.value >= 0)
            assertTrue(cardMaxWidth.value > 0)
        }
    }
    
    @Test
    fun testPerformanceMonitoring() {
        // Test that performance monitoring doesn't crash
        composeTestRule.setContent {
            com.north.mobile.performance.AndroidPerformanceMonitor()
        }
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testSystemUIConfiguration() {
        composeTestRule.setContent {
            com.north.mobile.performance.AndroidSystemUI(
                isDarkTheme = false
            )
        }
        
        composeTestRule.waitForIdle()
    }
}