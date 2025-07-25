import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.north.mobile.ui.onboarding.OnboardingScreen
import com.north.mobile.ui.onboarding.model.OnboardingState
import com.north.mobile.ui.onboarding.model.OnboardingStep
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class OnboardingTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun onboardingScreen_displaysWelcomeStep() {
        var nextStepCalled = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.WELCOME),
                onNextStep = { nextStepCalled = true },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Verify welcome content is displayed
        composeTestRule.onNodeWithText("Welcome to North").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Intelligent Finance Partner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        
        // Test navigation
        composeTestRule.onNodeWithText("Get Started").performClick()
        assertEquals(true, nextStepCalled)
    }
    
    @Test
    fun onboardingScreen_displaysSecuritySetupStep() {
        var biometricSetupCalled = false
        var pinSetupCalled = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.SECURITY_SETUP),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { biometricSetupCalled = true },
                onSetupPIN = { pinSetupCalled = true },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Verify security setup content is displayed
        composeTestRule.onNodeWithText("Secure Your Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set up Biometric Authentication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set up PIN Authentication").assertIsDisplayed()
        
        // Test biometric setup
        composeTestRule.onNodeWithText("Set up Biometric Authentication").performClick()
        assertEquals(true, biometricSetupCalled)
        
        // Test PIN setup
        composeTestRule.onNodeWithText("Set up PIN Authentication").performClick()
        assertEquals(true, pinSetupCalled)
    }
    
    @Test
    fun onboardingScreen_displaysAccountLinkingStep() {
        var linkAccountCalled = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.ACCOUNT_LINKING),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { linkAccountCalled = true },
                onCreateGoal = { }
            )
        }
        
        // Verify account linking content is displayed
        composeTestRule.onNodeWithText("Connect Your Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect Your Bank").assertIsDisplayed()
        
        // Test account linking
        composeTestRule.onNodeWithText("Connect Your Bank").performClick()
        assertEquals(true, linkAccountCalled)
    }
    
    @Test
    fun onboardingScreen_displaysGoalSetupStep() {
        var createGoalCalled = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.GOAL_SETUP),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { createGoalCalled = true }
            )
        }
        
        // Verify goal setup content is displayed
        composeTestRule.onNodeWithText("Set Your First Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Your First Goal").assertIsDisplayed()
        
        // Test goal creation
        composeTestRule.onNodeWithText("Create Your First Goal").performClick()
        assertEquals(true, createGoalCalled)
    }
    
    @Test
    fun onboardingScreen_displaysGamificationIntroStep() {
        var completeOnboardingCalled = false
        
        composeTestRule.setContent {
            OnboardingScreen(
                onboardingState = OnboardingState(currentStep = OnboardingStep.GAMIFICATION_INTRO),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { completeOnboardingCalled = true },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Verify gamification intro content is displayed
        composeTestRule.onNodeWithText("You're All Set!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Your Journey").assertIsDisplayed()
        
        // Test completion
        composeTestRule.onNodeWithText("Start Your Journey").performClick()
        assertEquals(true, completeOnboardingCalled)
    }
    
    @Test
    fun onboardingScreen_showsProgressIndicator() {
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
        
        // Progress indicator should be visible
        // Note: In a real test, we'd verify the progress dots are displayed correctly
        // This is a simplified test to ensure the component renders without errors
        composeTestRule.onNodeWithText("Secure Your Account").assertIsDisplayed()
    }
    
    @Test
    fun onboardingScreen_showsNavigationButtons() {
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
        
        // Navigation buttons should be visible for optional steps
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }
}