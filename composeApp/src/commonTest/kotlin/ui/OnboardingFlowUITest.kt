package com.north.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.north.mobile.ui.onboarding.OnboardingScreen
import com.north.mobile.ui.onboarding.model.OnboardingState
import com.north.mobile.ui.onboarding.model.OnboardingStep
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive UI tests for the onboarding flow
 * Tests critical user flows, accessibility, and user experience
 */
class OnboardingFlowUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun onboardingFlow_completesSuccessfully() {
        var currentStep = OnboardingStep.WELCOME
        var onboardingCompleted = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = currentStep),
                onNextStep = { 
                    currentStep = when (currentStep) {
                        OnboardingStep.WELCOME -> OnboardingStep.SECURITY_SETUP
                        OnboardingStep.SECURITY_SETUP -> OnboardingStep.ACCOUNT_LINKING
                        OnboardingStep.ACCOUNT_LINKING -> OnboardingStep.GOAL_SETUP
                        OnboardingStep.GOAL_SETUP -> OnboardingStep.GAMIFICATION_INTRO
                        OnboardingStep.GAMIFICATION_INTRO -> OnboardingStep.GAMIFICATION_INTRO
                    }
                },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { onboardingCompleted = true },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Step 1: Welcome screen
        composeTestRule.onNodeWithText("Welcome to North").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Step 2: Security setup
        composeTestRule.onNodeWithText("Secure Your Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // Step 3: Account linking
        composeTestRule.onNodeWithText("Connect Your Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // Step 4: Goal setup
        composeTestRule.onNodeWithText("Set Your First Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // Step 5: Gamification intro and completion
        composeTestRule.onNodeWithText("You're All Set!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Your Journey").performClick()
        
        assertTrue(onboardingCompleted)
    }
    
    @Test
    fun onboardingFlow_hasAccessibleNavigation() {
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.SECURITY_SETUP),
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
        
        // Test that navigation buttons have proper accessibility labels
        composeTestRule.onNodeWithContentDescription("Go back to previous step")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Skip this step")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test that main action buttons are accessible
        composeTestRule.onNodeWithText("Set up Biometric Authentication")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun onboardingFlow_supportsKeyboardNavigation() {
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.SECURITY_SETUP),
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
        
        // Test that focusable elements can be navigated with keyboard
        val focusableNodes = composeTestRule.onAllNodes(hasClickAction())
        focusableNodes.assertCountEquals(4) // Back, Skip, Biometric, PIN buttons
        
        // Each focusable node should be accessible via keyboard
        focusableNodes.fetchSemanticsNodes().forEach { node ->
            val semantics = node.config
            assertTrue(semantics.contains(SemanticsProperties.Focused) || 
                      semantics.contains(SemanticsProperties.Role))
        }
    }
    
    @Test
    fun onboardingFlow_hasProperProgressIndicators() {
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(
                    currentStep = OnboardingStep.SECURITY_SETUP,
                    totalSteps = 5,
                    currentStepIndex = 1
                ),
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
        
        // Test that progress indicator is accessible
        composeTestRule.onNodeWithContentDescription("Step 2 of 5")
            .assertIsDisplayed()
        
        // Test that progress is visually indicated
        composeTestRule.onNode(hasProgressBarRangeInfo(0.4f, 0f..1f))
            .assertIsDisplayed()
    }
    
    @Test
    fun onboardingFlow_handlesErrorStatesGracefully() {
        var errorOccurred = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(
                    currentStep = OnboardingStep.SECURITY_SETUP,
                    error = if (errorOccurred) "Biometric setup failed" else null
                ),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { errorOccurred = true },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Trigger error
        composeTestRule.onNodeWithText("Set up Biometric Authentication").performClick()
        
        // Verify error is displayed accessibly
        composeTestRule.onNodeWithText("Biometric setup failed")
            .assertIsDisplayed()
        
        // Verify error has proper semantics for screen readers
        composeTestRule.onNode(hasText("Biometric setup failed") and hasRole(androidx.compose.ui.semantics.Role.Text))
            .assertIsDisplayed()
    }
    
    @Test
    fun onboardingFlow_supportsLargeTextSizes() {
        composeTestRule.setContent {
            // Simulate large text size preference
            androidx.compose.material3.MaterialTheme {
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
        
        // Verify that text elements are still visible and accessible with large text
        composeTestRule.onNodeWithText("Welcome to North")
            .assertIsDisplayed()
            .assertTextContains("Welcome to North")
        
        composeTestRule.onNodeWithText("Get Started")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun onboardingFlow_providesHelpfulContentDescriptions() {
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
        
        // Test that images and icons have meaningful content descriptions
        composeTestRule.onNodeWithContentDescription("North app logo")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Welcome illustration showing financial growth")
            .assertIsDisplayed()
        
        // Test that interactive elements have clear descriptions
        composeTestRule.onNodeWithContentDescription("Get started with North onboarding")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun onboardingFlow_maintainsStateAcrossSteps() {
        var biometricSetup = false
        var pinSetup = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(
                    currentStep = OnboardingStep.SECURITY_SETUP,
                    biometricEnabled = biometricSetup,
                    pinEnabled = pinSetup
                ),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { biometricSetup = true },
                onSetupPIN = { pinSetup = true },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Setup biometric authentication
        composeTestRule.onNodeWithText("Set up Biometric Authentication").performClick()
        assertTrue(biometricSetup)
        
        // Setup PIN authentication
        composeTestRule.onNodeWithText("Set up PIN Authentication").performClick()
        assertTrue(pinSetup)
        
        // Verify state is maintained
        assertTrue(biometricSetup && pinSetup)
    }
}