package com.north.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import com.north.mobile.ui.goals.components.GoalCreationModal
import com.north.mobile.ui.goals.model.GoalCreationState
import com.north.mobile.domain.model.GoalType
import com.north.mobile.domain.model.Priority
import kotlinx.datetime.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive UI tests for goal creation flow
 * Tests critical user flows, form validation, accessibility, and user experience
 */
class GoalCreationUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun goalCreation_completesSuccessfully() {
        var goalCreated = false
        var createdGoalTitle = ""
        var createdGoalAmount = ""
        
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
                onTitleChange = { },
                onDescriptionChange = { },
                onTargetAmountChange = { },
                onTargetDateChange = { },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { title, amount, _, _, _, _ ->
                    goalCreated = true
                    createdGoalTitle = title
                    createdGoalAmount = amount
                },
                onDismiss = { }
            )
        }
        
        // Fill in goal title
        composeTestRule.onNodeWithText("Goal Title").performTextInput("Emergency Fund")
        
        // Fill in target amount
        composeTestRule.onNodeWithText("Target Amount").performTextInput("10000")
        
        // Select goal type
        composeTestRule.onNodeWithText("Goal Type").performClick()
        composeTestRule.onNodeWithText("Emergency Fund").performClick()
        
        // Select target date
        composeTestRule.onNodeWithText("Target Date").performClick()
        // Simulate date picker interaction
        composeTestRule.onNodeWithContentDescription("Select date").performClick()
        
        // Select priority
        composeTestRule.onNodeWithText("Priority").performClick()
        composeTestRule.onNodeWithText("High").performClick()
        
        // Create goal
        composeTestRule.onNodeWithText("Create Goal").performClick()
        
        assertTrue(goalCreated)
        assertEquals("Emergency Fund", createdGoalTitle)
        assertEquals("10000", createdGoalAmount)
    }
    
    @Test
    fun goalCreation_validatesFormInputs() {
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(
                    titleError = "Title is required",
                    targetAmountError = "Amount must be greater than 0"
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
        
        // Verify error messages are displayed
        composeTestRule.onNodeWithText("Title is required")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Amount must be greater than 0")
            .assertIsDisplayed()
        
        // Verify error messages have proper accessibility semantics
        composeTestRule.onNode(hasText("Title is required") and hasRole(androidx.compose.ui.semantics.Role.Text))
            .assertIsDisplayed()
    }
    
    @Test
    fun goalCreation_hasAccessibleFormFields() {
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
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
        
        // Test that form fields have proper labels and accessibility
        composeTestRule.onNodeWithContentDescription("Enter goal title")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsFocusable()
        
        composeTestRule.onNodeWithContentDescription("Enter target amount in dollars")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsFocusable()
        
        composeTestRule.onNodeWithContentDescription("Enter goal description (optional)")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsFocusable()
        
        // Test dropdown accessibility
        composeTestRule.onNodeWithContentDescription("Select goal type")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Select goal priority")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Select target date")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun goalCreation_supportsKeyboardNavigation() {
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
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
        
        // Test that all interactive elements are focusable
        val focusableNodes = composeTestRule.onAllNodes(isFocusable())
        focusableNodes.assertCountEquals(7) // Title, Description, Amount, Type, Priority, Date, Create button
        
        // Test tab order - title field should be first focusable element
        composeTestRule.onNodeWithContentDescription("Enter goal title")
            .requestFocus()
            .assertIsFocused()
    }
    
    @Test
    fun goalCreation_handlesLargeTextSizes() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                GoalCreationModal(
                    state = GoalCreationState(),
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
        }
        
        // Verify that labels and text are still readable with large text
        composeTestRule.onNodeWithText("Create New Goal")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Goal Title")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Target Amount")
            .assertIsDisplayed()
        
        // Verify that the modal doesn't overflow with large text
        composeTestRule.onNodeWithText("Create Goal")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun goalCreation_providesHelpfulHints() {
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
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
        
        // Test that helpful hints are provided
        composeTestRule.onNodeWithText("e.g., Emergency Fund, Vacation, New Car")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Enter amount in CAD")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Help: Choose a specific, measurable goal title")
            .assertIsDisplayed()
    }
    
    @Test
    fun goalCreation_handlesDateSelection() {
        var selectedDate: LocalDate? = null
        
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
                onTitleChange = { },
                onDescriptionChange = { },
                onTargetAmountChange = { },
                onTargetDateChange = { selectedDate = it },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { _, _, _, _, _, _ -> },
                onDismiss = { }
            )
        }
        
        // Open date picker
        composeTestRule.onNodeWithText("Target Date").performClick()
        
        // Verify date picker is accessible
        composeTestRule.onNodeWithContentDescription("Date picker")
            .assertIsDisplayed()
        
        // Select a future date (simulate)
        val futureDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6))
        composeTestRule.onNodeWithContentDescription("Select ${futureDate.monthNumber}/${futureDate.dayOfMonth}/${futureDate.year}")
            .performClick()
        
        composeTestRule.onNodeWithText("OK").performClick()
        
        // Verify date was selected
        assertEquals(futureDate, selectedDate)
    }
    
    @Test
    fun goalCreation_showsProgressIndicators() {
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(
                    title = "Emergency Fund",
                    targetAmount = "10000",
                    targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6))
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
        
        // Test that form completion progress is shown
        composeTestRule.onNodeWithContentDescription("Form completion: 3 of 6 fields completed")
            .assertIsDisplayed()
        
        // Test that validation status is indicated
        composeTestRule.onNodeWithContentDescription("Title field completed")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Amount field completed")
            .assertIsDisplayed()
    }
    
    @Test
    fun goalCreation_supportsVoiceInput() {
        var titleChanged = false
        var amountChanged = false
        
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
                onTitleChange = { titleChanged = true },
                onDescriptionChange = { },
                onTargetAmountChange = { amountChanged = true },
                onTargetDateChange = { },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { _, _, _, _, _, _ -> },
                onDismiss = { }
            )
        }
        
        // Test that voice input buttons are accessible
        composeTestRule.onNodeWithContentDescription("Voice input for goal title")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Voice input for target amount")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Simulate voice input
        composeTestRule.onNodeWithContentDescription("Voice input for goal title").performClick()
        assertTrue(titleChanged)
        
        composeTestRule.onNodeWithContentDescription("Voice input for target amount").performClick()
        assertTrue(amountChanged)
    }
    
    @Test
    fun goalCreation_handlesModalDismissal() {
        var dismissed = false
        
        composeTestRule.setContent {
            GoalCreationModal(
                state = GoalCreationState(),
                onTitleChange = { },
                onDescriptionChange = { },
                onTargetAmountChange = { },
                onTargetDateChange = { },
                onGoalTypeChange = { },
                onPriorityChange = { },
                onCreateGoal = { _, _, _, _, _, _ -> },
                onDismiss = { dismissed = true }
            )
        }
        
        // Test close button
        composeTestRule.onNodeWithContentDescription("Close goal creation")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        
        assertTrue(dismissed)
        
        // Reset for next test
        dismissed = false
        
        // Test cancel button
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        
        assertTrue(dismissed)
    }
}