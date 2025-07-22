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

/**
 * Comprehensive accessibility tests for the North mobile app
 * Tests screen reader compatibility, keyboard navigation, voice control, and other accessibility features
 */
class AccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboardScreen_isAccessibleToScreenReaders() {
        AccessibilityTestScenarios.run {
            composeTestRule.testScreenReaderCompatibility("Dashboard Screen") {
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
        
        // Verify specific dashboard elements are accessible
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
        
        // Test net worth card accessibility
        composeTestRule.onNodeWithContentDescription("Net worth: $47,250 CAD, up $1,200 this month")
            .assertIsDisplayed()
            .assertSupportsScreenReader()
        
        // Test account summary accessibility
        composeTestRule.onNodeWithContentDescription("Checking account: $2,450 at RBC")
            .assertIsDisplayed()
            .assertSupportsScreenReader()
        
        // Test gamification panel accessibility
        composeTestRule.onNodeWithContentDescription("Current streak: 12 days, Level 7 Money Master")
            .assertIsDisplayed()
            .assertSupportsScreenReader()
    }
    
    @Test
    fun dashboardScreen_supportsKeyboardNavigation() {
        AccessibilityTestScenarios.run {
            composeTestRule.testKeyboardNavigation("Dashboard Screen") {
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
        
        // Test that quick actions are keyboard accessible
        composeTestRule.onNodeWithContentDescription("Add transaction")
            .assertIsKeyboardAccessible()
        
        composeTestRule.onNodeWithContentDescription("View goals")
            .assertIsKeyboardAccessible()
        
        composeTestRule.onNodeWithContentDescription("Check insights")
            .assertIsKeyboardAccessible()
    }
    
    @Test
    fun dashboardScreen_supportsVoiceControl() {
        AccessibilityTestScenarios.run {
            composeTestRule.testVoiceControlCompatibility("Dashboard Screen") {
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
        
        // Test voice control labels
        composeTestRule.onNodeWithContentDescription("Refresh financial data")
            .assertSupportsVoiceControl()
        
        composeTestRule.onNodeWithContentDescription("Open account details")
            .assertSupportsVoiceControl()
    }
    
    @Test
    fun goalManagementScreen_isAccessibleToScreenReaders() {
        AccessibilityTestScenarios.run {
            composeTestRule.testScreenReaderCompatibility("Goal Management Screen") {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
        }
        
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
        
        // Test goal cards accessibility
        composeTestRule.onNodeWithContentDescription("Emergency Fund goal: $8,500 of $10,000, 85% complete, target December 2025")
            .assertSupportsScreenReader()
        
        // Test progress indicators accessibility
        composeTestRule.onNodeWithContentDescription("Goal progress: 85 percent complete")
            .assertIsAccessibleProgressIndicator()
    }
    
    @Test
    fun chatScreen_isAccessibleToScreenReaders() {
        AccessibilityTestScenarios.run {
            composeTestRule.testScreenReaderCompatibility("Chat Screen") {
                ChatScreen(
                    state = ChatState(),
                    onSendMessage = { },
                    onQuickQuestionClick = { },
                    onClearChat = { }
                )
            }
        }
        
        composeTestRule.setContent {
            ChatScreen(
                state = ChatState(),
                onSendMessage = { },
                onQuickQuestionClick = { },
                onClearChat = { }
            )
        }
        
        // Test chat messages accessibility
        composeTestRule.onNodeWithContentDescription("Message from North AI: Hi Alex! I'm your personal CFO.")
            .assertSupportsScreenReader()
        
        composeTestRule.onNodeWithContentDescription("Your message: Can I afford a $400 weekend trip?")
            .assertSupportsScreenReader()
        
        // Test input field accessibility
        composeTestRule.onNodeWithContentDescription("Type your question to North AI")
            .assertIsAccessibleFormField()
        
        // Test send button accessibility
        composeTestRule.onNodeWithContentDescription("Send message to North AI")
            .assertSupportsVoiceControl()
    }
    
    @Test
    fun onboardingScreen_isAccessibleToScreenReaders() {
        AccessibilityTestScenarios.run {
            composeTestRule.testScreenReaderCompatibility("Onboarding Screen") {
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
        }
        
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
        
        // Test onboarding content accessibility
        composeTestRule.onNodeWithContentDescription("Welcome to North, your intelligent finance partner")
            .assertSupportsScreenReader()
        
        composeTestRule.onNodeWithContentDescription("Get started with North onboarding")
            .assertSupportsVoiceControl()
    }
    
    @Test
    fun allScreens_supportHighContrastMode() {
        // Test dashboard in high contrast
        AccessibilityTestScenarios.run {
            composeTestRule.testHighContrastCompatibility("Dashboard High Contrast") {
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
        
        // Test goal management in high contrast
        AccessibilityTestScenarios.run {
            composeTestRule.testHighContrastCompatibility("Goals High Contrast") {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
        }
        
        // Test chat in high contrast
        AccessibilityTestScenarios.run {
            composeTestRule.testHighContrastCompatibility("Chat High Contrast") {
                ChatScreen(
                    state = ChatState(),
                    onSendMessage = { },
                    onQuickQuestionClick = { },
                    onClearChat = { }
                )
            }
        }
    }
    
    @Test
    fun allScreens_supportLargeTextSizes() {
        // Test dashboard with large text
        AccessibilityTestScenarios.run {
            composeTestRule.testLargeTextCompatibility("Dashboard Large Text") {
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
        
        // Test goal management with large text
        AccessibilityTestScenarios.run {
            composeTestRule.testLargeTextCompatibility("Goals Large Text") {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
        }
        
        // Test chat with large text
        AccessibilityTestScenarios.run {
            composeTestRule.testLargeTextCompatibility("Chat Large Text") {
                ChatScreen(
                    state = ChatState(),
                    onSendMessage = { },
                    onQuickQuestionClick = { },
                    onClearChat = { }
                )
            }
        }
    }
    
    @Test
    fun allScreens_supportReducedMotion() {
        // Test dashboard with reduced motion
        AccessibilityTestScenarios.run {
            composeTestRule.testReducedMotionCompatibility("Dashboard Reduced Motion") {
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
        
        // Test goal management with reduced motion
        AccessibilityTestScenarios.run {
            composeTestRule.testReducedMotionCompatibility("Goals Reduced Motion") {
                GoalManagementScreen(
                    state = GoalDashboardState(),
                    onCreateGoal = { },
                    onEditGoal = { },
                    onDeleteGoal = { },
                    onUpdateProgress = { _, _ -> },
                    onGoalClick = { }
                )
            }
        }
    }
    
    @Test
    fun errorStates_areAccessible() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(
                    error = "Failed to load account data. Please check your connection."
                ),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Test error message accessibility
        composeTestRule.onNodeWithText("Failed to load account data. Please check your connection.")
            .assertHasAccessibleErrorState()
        
        // Test retry button accessibility
        composeTestRule.onNodeWithContentDescription("Retry loading account data")
            .assertSupportsVoiceControl()
    }
    
    @Test
    fun loadingStates_areAccessible() {
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
        
        // Test loading indicator accessibility
        composeTestRule.onNodeWithContentDescription("Loading your financial data")
            .assertIsDisplayed()
            .assertSupportsScreenReader()
        
        // Test that loading states don't interfere with screen readers
        composeTestRule.onNodeWithContentDescription("Please wait while we load your information")
            .assertSupportsScreenReader()
    }
    
    @Test
    fun formValidation_isAccessible() {
        composeTestRule.setContent {
            com.north.mobile.ui.goals.components.GoalCreationModal(
                state = com.north.mobile.ui.goals.model.GoalCreationState(
                    titleError = "Goal title is required",
                    targetAmountError = "Amount must be greater than $0"
                ),
                onTitleChange = { },
                onDescriptionChange = { },
                onTargetAmountChange = { },
                onTargetDateChange = { },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { _, _, _, _, _, _ -> },
                onDismiss = { }
            )
        }
        
        // Test that validation errors are accessible
        composeTestRule.onNodeWithText("Goal title is required")
            .assertHasAccessibleErrorState()
        
        composeTestRule.onNodeWithText("Amount must be greater than $0")
            .assertHasAccessibleErrorState()
        
        // Test that error states are announced to screen readers
        composeTestRule.onNodeWithContentDescription("Error: Goal title is required")
            .assertSupportsScreenReader()
    }
}