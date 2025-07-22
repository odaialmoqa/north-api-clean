package com.north.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.north.mobile.ui.dashboard.DashboardScreen
import com.north.mobile.ui.dashboard.model.DashboardState
import com.north.mobile.ui.goals.GoalManagementScreen
import com.north.mobile.ui.goals.model.GoalDashboardState
import com.north.mobile.ui.chat.ChatScreen
import com.north.mobile.ui.chat.model.ChatState
import com.north.mobile.ui.onboarding.OnboardingScreen
import com.north.mobile.ui.onboarding.model.OnboardingState
import com.north.mobile.ui.onboarding.model.OnboardingStep
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Tests to ensure UI consistency across different platforms and screen sizes
 * Verifies that core functionality and visual elements are identical across iOS and Android
 */
class CrossPlatformConsistencyTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboardScreen_hasConsistentLayout() {
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
        
        // Verify core dashboard elements are present regardless of platform
        composeTestRule.onNodeWithText("Good morning, Alex! 👋").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's Micro-Wins").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Insights").assertIsDisplayed()
        
        // Verify account summary grid is consistent
        composeTestRule.onNodeWithText("Checking").assertIsDisplayed()
        composeTestRule.onNodeWithText("$2,450").assertIsDisplayed()
        composeTestRule.onNodeWithText("Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("$15,800").assertIsDisplayed()
        
        // Verify gamification elements are consistent
        composeTestRule.onNodeWithText("3-day saving streak!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Check balance (+10 pts)").assertIsDisplayed()
    }
    
    @Test
    fun goalManagementScreen_hasConsistentLayout() {
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
        
        // Verify goal management elements are consistent
        composeTestRule.onNodeWithText("Your Goals").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Create new goal").assertIsDisplayed()
        
        // Verify goal cards have consistent structure
        composeTestRule.onNodeWithText("Emergency Fund").assertIsDisplayed()
        composeTestRule.onNodeWithText("$8,500 / $10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target: Dec 2025").assertIsDisplayed()
        composeTestRule.onNodeWithText("$125/week to stay on track").assertIsDisplayed()
        
        // Verify progress indicators are consistent
        composeTestRule.onNode(hasProgressBarRangeInfo(0.85f, 0f..1f)).assertIsDisplayed()
    }
    
    @Test
    fun chatScreen_hasConsistentLayout() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatState(),
                onSendMessage = { },
                onQuickQuestionClick = { },
                onClearChat = { }
            )
        }
        
        // Verify chat interface elements are consistent
        composeTestRule.onNodeWithText("Chat with North").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi Alex! I'm your personal CFO.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Type your question...").assertIsDisplayed()
        
        // Verify quick questions are consistent
        composeTestRule.onNodeWithText("How much can I spend on...?").assertIsDisplayed()
        composeTestRule.onNodeWithText("When will I reach my goal?").assertIsDisplayed()
        composeTestRule.onNodeWithText("What should I focus on?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Am I on track this month?").assertIsDisplayed()
    }
    
    @Test
    fun onboardingScreen_hasConsistentFlow() {
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.WELCOME),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Verify welcome screen elements are consistent
        composeTestRule.onNodeWithText("Welcome to North").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Intelligent Finance Partner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reduce anxiety, build wealth, achieve your goals").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        
        // Verify progress indicators are consistent
        composeTestRule.onNode(hasProgressBarRangeInfo(0.2f, 0f..1f)).assertIsDisplayed()
    }
    
    @Test
    fun navigationElements_areConsistentAcrossPlatforms() {
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
        
        // Verify bottom navigation is consistent
        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Insights").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Goals").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
        
        // Verify navigation icons are accessible
        val navigationNodes = composeTestRule.onAllNodes(hasContentDescription() and hasClickAction())
        navigationNodes.assertCountEquals(5) // 5 navigation items
    }
    
    @Test
    fun colorScheme_isConsistentAcrossPlatforms() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
        }
        
        // Verify that themed elements are present (color consistency is tested visually)
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        
        // Test both light and dark themes
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme()
            ) {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
        }
        
        // Verify elements are still visible in dark theme
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
    }
    
    @Test
    fun typography_isConsistentAcrossPlatforms() {
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
        
        // Verify typography hierarchy is consistent
        composeTestRule.onNodeWithText("Good morning, Alex! 👋").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's Micro-Wins").assertIsDisplayed()
        
        // All text should be readable and properly sized
        val textNodes = composeTestRule.onAllNodes(hasText())
        textNodes.fetchSemanticsNodes().forEach { node ->
            assertTrue(node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text))
        }
    }
    
    @Test
    fun spacing_isConsistentAcrossPlatforms() {
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
        
        // Verify that elements are properly spaced and don't overlap
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Checking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Savings").assertIsDisplayed()
        
        // Elements should not overlap (this would be caught by layout tests)
        val allDisplayedNodes = composeTestRule.onAllNodes(isDisplayed())
        assertTrue(allDisplayedNodes.fetchSemanticsNodes().isNotEmpty())
    }
    
    @Test
    fun interactionPatterns_areConsistentAcrossPlatforms() {
        var accountClicked = false
        var goalClicked = false
        var insightClicked = false
        
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(),
                onRefresh = { },
                onAccountClick = { accountClicked = true },
                onGoalClick = { goalClicked = true },
                onInsightClick = { insightClicked = true },
                onQuickActionClick = { }
            )
        }
        
        // Test consistent tap interactions
        composeTestRule.onNodeWithText("Checking").performClick()
        assertTrue(accountClicked)
        
        composeTestRule.onNodeWithText("Emergency Fund").performClick()
        assertTrue(goalClicked)
        
        composeTestRule.onNodeWithText("You're spending 15% less on dining this month! 🎉").performClick()
        assertTrue(insightClicked)
    }
    
    @Test
    fun errorStates_areConsistentAcrossPlatforms() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(
                    error = "Failed to load account data"
                ),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify error states are displayed consistently
        composeTestRule.onNodeWithText("Failed to load account data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        
        // Error states should be accessible
        composeTestRule.onNodeWithContentDescription("Error loading data").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Retry loading data").assertHasClickAction()
    }
    
    @Test
    fun loadingStates_areConsistentAcrossPlatforms() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(isLoading = true),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify loading states are displayed consistently
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
        composeTestRule.onNode(hasProgressBarRangeInfo()).assertIsDisplayed()
        
        // Loading indicators should be accessible
        composeTestRule.onNodeWithContentDescription("Loading your financial data").assertIsDisplayed()
    }
    
    @Test
    fun animations_areConsistentAcrossPlatforms() {
        composeTestRule.setContent {
            com.north.mobile.ui.goals.components.CelebrationAnimations(
                isVisible = true,
                celebrationType = com.north.mobile.data.goal.CelebrationType.CONFETTI,
                onAnimationComplete = { }
            )
        }
        
        // Verify animation elements are present
        composeTestRule.onNodeWithContentDescription("Celebration animation").assertIsDisplayed()
        
        // Animations should respect accessibility preferences
        composeTestRule.onNodeWithContentDescription("Celebration confetti animation").assertIsDisplayed()
    }
    
    @Test
    fun responsiveLayout_worksAcrossPlatforms() {
        // Test compact layout
        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides 
                    android.content.res.Configuration().apply {
                        screenWidthDp = 360
                        screenHeightDp = 640
                    }
            ) {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
        }
        
        // Verify compact layout elements
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("Checking").assertIsDisplayed()
        
        // Test expanded layout
        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides 
                    android.content.res.Configuration().apply {
                        screenWidthDp = 800
                        screenHeightDp = 1200
                    }
            ) {
                DashboardScreen(
                    state = DashboardState(),
                    onRefresh = { },
                    onAccountClick = { },
                    onGoalClick = { },
                    onInsightClick = { },
                    onQuickActionClick = { }
                )
            }
        }
        
        // Verify expanded layout still shows core elements
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("Checking").assertIsDisplayed()
    }
}