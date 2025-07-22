package com.north.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.north.mobile.ui.dashboard.DashboardScreen
import com.north.mobile.ui.dashboard.model.DashboardState
import com.north.mobile.ui.goals.GoalManagementScreen
import com.north.mobile.ui.goals.model.GoalDashboardState
import com.north.mobile.ui.chat.ChatScreen
import com.north.mobile.ui.chat.model.ChatState
import com.north.mobile.App
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance tests for app launch, data sync, and UI responsiveness
 * Tests critical performance metrics and ensures smooth user experience
 */
class PerformanceTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun appLaunch_completesWithinTargetTime() {
        val launchTime = measureTimeMillis {
            composeTestRule.setContent {
                App()
            }
            
            // Wait for initial composition to complete
            composeTestRule.waitForIdle()
            
            // Verify app has launched successfully
            composeTestRule.onNodeWithText("Welcome to North").assertIsDisplayed()
        }
        
        // Target: App should launch within 2 seconds (2000ms)
        assertTrue(launchTime < 2000, "App launch took ${launchTime}ms, should be under 2000ms")
    }
    
    @Test
    fun dashboardScreen_rendersQuickly() {
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Verify key elements are rendered
            composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
            composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        }
        
        // Target: Dashboard should render within 500ms
        assertTrue(renderTime < 500, "Dashboard render took ${renderTime}ms, should be under 500ms")
    }
    
    @Test
    fun goalManagementScreen_rendersQuickly() {
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Verify key elements are rendered
            composeTestRule.onNodeWithText("Your Goals").assertIsDisplayed()
        }
        
        // Target: Goals screen should render within 500ms
        assertTrue(renderTime < 500, "Goals screen render took ${renderTime}ms, should be under 500ms")
    }
    
    @Test
    fun chatScreen_rendersQuickly() {
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                ChatScreen(
                    state = ChatState(),
                    onSendMessage = { },
                    onQuickQuestionClick = { },
                    onClearChat = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Verify key elements are rendered
            composeTestRule.onNodeWithText("Chat with North").assertIsDisplayed()
        }
        
        // Target: Chat screen should render within 500ms
        assertTrue(renderTime < 500, "Chat screen render took ${renderTime}ms, should be under 500ms")
    }
    
    @Test
    fun dataSync_completesWithinTargetTime() = runTest {
        var syncCompleted = false
        var syncStartTime = 0L
        var syncEndTime = 0L
        
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(isLoading = true),
                onRefresh = { 
                    syncStartTime = System.currentTimeMillis()
                    // Simulate data sync
                    launch {
                        delay(800) // Simulate network request
                        syncEndTime = System.currentTimeMillis()
                        syncCompleted = true
                    }
                },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Trigger refresh
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        // Wait for sync to complete
        composeTestRule.waitUntil(timeoutMillis = 2000) { syncCompleted }
        
        val syncDuration = syncEndTime - syncStartTime
        
        // Target: Data sync should complete within 1.5 seconds (1500ms)
        assertTrue(syncDuration < 1500, "Data sync took ${syncDuration}ms, should be under 1500ms")
    }
    
    @Test
    fun scrollPerformance_isSmoothWithLargeDatasets() {
        // Create a large dataset to test scroll performance
        val largeGoalsList = (1..100).map { index ->
            com.north.mobile.domain.model.FinancialGoal(
                id = "goal-$index",
                userId = "user-1",
                title = "Goal $index",
                targetAmount = com.north.mobile.domain.model.Money.fromDollars(1000.0 * index),
                targetDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()),
                priority = com.north.mobile.domain.model.Priority.MEDIUM,
                goalType = com.north.mobile.domain.model.GoalType.GENERAL_SAVINGS,
                createdAt = kotlinx.datetime.Clock.System.now()
            )
        }
        
        val scrollTime = measureTimeMillis {
            composeTestRule.setContent {
                GoalManagementScreen(
                    state = GoalDashboardState(goals = largeGoalsList),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Perform scroll operations
            composeTestRule.onNodeWithText("Goal 1").assertIsDisplayed()
            
            // Scroll down multiple times
            repeat(10) {
                composeTestRule.onRoot().performTouchInput {
                    swipeUp()
                }
                composeTestRule.waitForIdle()
            }
        }
        
        // Target: Scrolling through large dataset should complete within 2 seconds
        assertTrue(scrollTime < 2000, "Scroll performance took ${scrollTime}ms, should be under 2000ms")
    }
    
    @Test
    fun animationPerformance_isSmoothAndResponsive() {
        var animationStarted = false
        var animationCompleted = false
        
        val animationTime = measureTimeMillis {
            composeTestRule.setContent {
                com.north.mobile.ui.goals.components.CelebrationAnimations(
                    isVisible = true,
                    celebrationType = com.north.mobile.data.goal.CelebrationType.CONFETTI,
                    onAnimationStart = { animationStarted = true },
                    onAnimationComplete = { animationCompleted = true }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Wait for animation to complete
            composeTestRule.waitUntil(timeoutMillis = 3000) { animationCompleted }
        }
        
        assertTrue(animationStarted, "Animation should have started")
        assertTrue(animationCompleted, "Animation should have completed")
        
        // Target: Celebration animation should complete within 2.5 seconds
        assertTrue(animationTime < 2500, "Animation took ${animationTime}ms, should be under 2500ms")
    }
    
    @Test
    fun memoryUsage_staysWithinLimits() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create multiple screens to test memory usage
        repeat(5) {
            composeTestRule.setContent {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
            composeTestRule.waitForIdle()
            
            composeTestRule.setContent {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
            composeTestRule.waitForIdle()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Target: Memory increase should be less than 50MB (52,428,800 bytes)
        assertTrue(memoryIncrease < 52_428_800, "Memory increased by ${memoryIncrease} bytes, should be under 50MB")
    }
    
    @Test
    fun userInteraction_respondsQuickly() {
        var buttonClicked = false
        var clickTime = 0L
        
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(),
                onRefresh = { },
                onAccountClick = { 
                    clickTime = System.currentTimeMillis()
                    buttonClicked = true 
                },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        val interactionStartTime = System.currentTimeMillis()
        
        // Perform user interaction
        composeTestRule.onNodeWithText("Checking").performClick()
        
        // Wait for response
        composeTestRule.waitUntil(timeoutMillis = 1000) { buttonClicked }
        
        val responseTime = clickTime - interactionStartTime
        
        // Target: User interactions should respond within 100ms
        assertTrue(responseTime < 100, "User interaction took ${responseTime}ms, should be under 100ms")
    }
    
    @Test
    fun formInput_respondsQuicklyToTextChanges() {
        var textChanged = false
        var lastChangeTime = 0L
        
        composeTestRule.setContent {
            com.north.mobile.ui.goals.components.GoalCreationModal(
                state = com.north.mobile.ui.goals.model.GoalCreationState(),
                onTitleChange = { 
                    lastChangeTime = System.currentTimeMillis()
                    textChanged = true 
                },
                onDescriptionChange = { },
                onTargetAmountChange = { },
                onTargetDateChange = { },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { _, _, _, _, _, _ -> },
                onDismiss = { }
            )
        }
        
        val inputStartTime = System.currentTimeMillis()
        
        // Perform text input
        composeTestRule.onNodeWithText("Goal Title").performTextInput("Emergency Fund")
        
        // Wait for text change to be processed
        composeTestRule.waitUntil(timeoutMillis = 500) { textChanged }
        
        val inputResponseTime = lastChangeTime - inputStartTime
        
        // Target: Text input should respond within 50ms
        assertTrue(inputResponseTime < 50, "Text input took ${inputResponseTime}ms, should be under 50ms")
    }
    
    @Test
    fun networkError_handlesGracefully() {
        var errorHandled = false
        var errorTime = 0L
        
        val errorHandlingTime = measureTimeMillis {
            composeTestRule.setContent {
                DashboardScreen(
                    state = DashboardState(
                        error = "Network connection failed"
                    ),
                    onRefresh = { 
                        errorTime = System.currentTimeMillis()
                        errorHandled = true 
                    },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Verify error is displayed
            composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
            
            // Test retry functionality
            composeTestRule.onNodeWithText("Retry").performClick()
            
            composeTestRule.waitUntil(timeoutMillis = 1000) { errorHandled }
        }
        
        // Target: Error handling should complete within 500ms
        assertTrue(errorHandlingTime < 500, "Error handling took ${errorHandlingTime}ms, should be under 500ms")
    }
    
    @Test
    fun backgroundSync_doesNotBlockUI() = runTest {
        var syncInProgress = false
        var uiResponsive = false
        
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(isSyncing = true),
                onRefresh = { 
                    syncInProgress = true
                    // Simulate background sync
                    launch {
                        delay(1000) // Long-running background task
                        syncInProgress = false
                    }
                },
                onAccountClick = { uiResponsive = true },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Start background sync
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        // Verify UI is still responsive during sync
        composeTestRule.onNodeWithText("Checking").performClick()
        
        assertTrue(syncInProgress, "Background sync should be in progress")
        assertTrue(uiResponsive, "UI should remain responsive during background sync")
    }
    
    @Test
    fun largeDataset_rendersEfficiently() {
        // Create a large transaction dataset
        val largeTransactionList = (1..1000).map { index ->
            com.north.mobile.domain.model.Transaction(
                id = "transaction-$index",
                accountId = "account-1",
                amount = com.north.mobile.domain.model.Money.fromDollars(index.toDouble()),
                description = "Transaction $index",
                category = com.north.mobile.data.analytics.Category(
                    id = "category-${index % 10}",
                    name = "Category ${index % 10}",
                    icon = "🛒"
                ),
                date = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()),
                createdAt = kotlinx.datetime.Clock.System.now()
            )
        }
        
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                com.north.mobile.ui.insights.SpendingInsightsScreen(
                    state = com.north.mobile.ui.insights.model.SpendingInsightsState(
                        transactions = largeTransactionList
                    ),
                    onPeriodChange = { },
                    onCategoryClick = { },
                    onTransactionClick = { },
                    onRecommendationClick = { }
                )
            }
            
            composeTestRule.waitForIdle()
            
            // Verify screen renders with large dataset
            composeTestRule.onNodeWithText("Spending Insights").assertIsDisplayed()
        }
        
        // Target: Large dataset should render within 1 second
        assertTrue(renderTime < 1000, "Large dataset render took ${renderTime}ms, should be under 1000ms")
    }
}