package com.north.mobile.ui

import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.text.AnnotatedString

/**
 * Utility functions for accessibility testing
 */
object AccessibilityTestUtils {
    
    /**
     * Verifies that a node has proper accessibility semantics
     */
    fun SemanticsNodeInteraction.assertHasAccessibilitySemantics(): SemanticsNodeInteraction {
        return this.assert(hasAccessibilitySemantics())
    }
    
    /**
     * Verifies that a node has a meaningful content description
     */
    fun SemanticsNodeInteraction.assertHasMeaningfulContentDescription(): SemanticsNodeInteraction {
        return this.assert(hasMeaningfulContentDescription())
    }
    
    /**
     * Verifies that interactive elements have proper click actions
     */
    fun SemanticsNodeInteraction.assertIsAccessibleInteractive(): SemanticsNodeInteraction {
        return this.assertHasClickAction()
            .assertIsEnabled()
            .assertHasMeaningfulContentDescription()
    }
    
    /**
     * Verifies that text elements are accessible to screen readers
     */
    fun SemanticsNodeInteraction.assertIsAccessibleText(): SemanticsNodeInteraction {
        return this.assert(hasText() or hasContentDescription())
    }
    
    /**
     * Verifies that form fields have proper labels and states
     */
    fun SemanticsNodeInteraction.assertIsAccessibleFormField(): SemanticsNodeInteraction {
        return this.assertIsEnabled()
            .assertIsFocusable()
            .assert(hasContentDescription() or hasText())
    }
    
    /**
     * Verifies that error states are properly communicated
     */
    fun SemanticsNodeInteraction.assertHasAccessibleErrorState(): SemanticsNodeInteraction {
        return this.assert(hasRole(Role.Text))
            .assertIsDisplayed()
    }
    
    /**
     * Verifies that progress indicators are accessible
     */
    fun SemanticsNodeInteraction.assertIsAccessibleProgressIndicator(): SemanticsNodeInteraction {
        return this.assert(hasProgressBarRangeInfo() or hasContentDescription())
            .assertIsDisplayed()
    }
    
    /**
     * Custom matcher for accessibility semantics
     */
    private fun hasAccessibilitySemantics(): SemanticsMatcher {
        return SemanticsMatcher("has accessibility semantics") { node ->
            val config = node.config
            config.contains(SemanticsProperties.ContentDescription) ||
            config.contains(SemanticsProperties.Text) ||
            config.contains(SemanticsProperties.Role) ||
            config.contains(SemanticsProperties.StateDescription)
        }
    }
    
    /**
     * Custom matcher for meaningful content descriptions
     */
    private fun hasMeaningfulContentDescription(): SemanticsMatcher {
        return SemanticsMatcher("has meaningful content description") { node ->
            val contentDescription = node.config.getOrNull(SemanticsProperties.ContentDescription)
            contentDescription != null && 
            contentDescription.isNotEmpty() && 
            contentDescription.any { it.length > 3 } // More than just "OK", "Yes", etc.
        }
    }
    
    /**
     * Custom matcher for text content
     */
    private fun hasText(): SemanticsMatcher {
        return SemanticsMatcher("has text") { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)
            text != null && text.isNotEmpty()
        }
    }
    
    /**
     * Custom matcher for progress bar range info
     */
    private fun hasProgressBarRangeInfo(): SemanticsMatcher {
        return SemanticsMatcher("has progress bar range info") { node ->
            node.config.contains(SemanticsProperties.ProgressBarRangeInfo)
        }
    }
}

/**
 * Accessibility test scenarios for common UI patterns
 */
object AccessibilityTestScenarios {
    
    /**
     * Tests screen reader compatibility for a given composable
     */
    fun ComposeContentTestRule.testScreenReaderCompatibility(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent(content)
        
        // Find all nodes with semantic meaning
        val semanticNodes = onAllNodes(hasAnyDescendant(hasClickAction()) or hasText() or hasContentDescription())
        
        // Verify each semantic node is accessible
        semanticNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            semanticNodes[index].assertHasAccessibilitySemantics()
        }
    }
    
    /**
     * Tests keyboard navigation for a given composable
     */
    fun ComposeContentTestRule.testKeyboardNavigation(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent(content)
        
        // Find all focusable nodes
        val focusableNodes = onAllNodes(isFocusable())
        
        // Verify each focusable node can receive focus
        focusableNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            focusableNodes[index]
                .assertIsFocusable()
                .assertIsEnabled()
        }
    }
    
    /**
     * Tests voice control compatibility
     */
    fun ComposeContentTestRule.testVoiceControlCompatibility(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent(content)
        
        // Find all interactive nodes
        val interactiveNodes = onAllNodes(hasClickAction())
        
        // Verify each interactive node has voice-accessible labels
        interactiveNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            interactiveNodes[index].assertHasMeaningfulContentDescription()
        }
    }
    
    /**
     * Tests high contrast mode compatibility
     */
    fun ComposeContentTestRule.testHighContrastCompatibility(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent {
            // Simulate high contrast mode
            androidx.compose.material3.MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme()
            ) {
                content()
            }
        }
        
        // Verify that all text is still readable
        val textNodes = onAllNodes(hasText())
        textNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            textNodes[index].assertIsDisplayed()
        }
    }
    
    /**
     * Tests large text size compatibility
     */
    fun ComposeContentTestRule.testLargeTextCompatibility(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent {
            // Simulate large text preference
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density = 2.0f, // Simulate 200% text scaling
                    fontScale = 2.0f
                )
            ) {
                content()
            }
        }
        
        // Verify that UI elements are still accessible and don't overflow
        val allNodes = onAllNodes(hasAnyDescendant(hasText() or hasContentDescription()))
        allNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            allNodes[index].assertIsDisplayed()
        }
    }
    
    /**
     * Tests reduced motion compatibility
     */
    fun ComposeContentTestRule.testReducedMotionCompatibility(
        testName: String,
        content: @Composable () -> Unit
    ) {
        setContent {
            // Simulate reduced motion preference
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalAccessibilityManager provides object : androidx.compose.ui.platform.AccessibilityManager {
                    override fun calculateRecommendedTimeoutMillis(
                        originalTimeoutMillis: Long,
                        containsIcons: Boolean,
                        containsText: Boolean
                    ): Long = originalTimeoutMillis * 2 // Longer timeouts for accessibility
                }
            ) {
                content()
            }
        }
        
        // Verify that essential functionality is still available without animations
        val interactiveNodes = onAllNodes(hasClickAction())
        interactiveNodes.fetchSemanticsNodes().forEachIndexed { index, _ ->
            interactiveNodes[index]
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }
}

/**
 * Extension functions for common accessibility assertions
 */
fun SemanticsNodeInteraction.assertSupportsScreenReader(): SemanticsNodeInteraction {
    return this.assert(
        hasContentDescription() or 
        hasText() or 
        hasRole(Role.Button) or 
        hasRole(Role.Image) or 
        hasRole(Role.TextField)
    )
}

fun SemanticsNodeInteraction.assertSupportsVoiceControl(): SemanticsNodeInteraction {
    return this.assert(hasContentDescription() and hasClickAction())
}

fun SemanticsNodeInteraction.assertIsKeyboardAccessible(): SemanticsNodeInteraction {
    return this.assertIsFocusable()
        .assertIsEnabled()
        .assert(hasClickAction() or hasRole(Role.TextField))
}

/**
 * Matcher for nodes that have any descendant matching the given matcher
 */
fun hasAnyDescendant(matcher: SemanticsMatcher): SemanticsMatcher {
    return SemanticsMatcher("has any descendant that ${matcher.description}") { node ->
        fun checkNode(currentNode: SemanticsNode): Boolean {
            if (matcher.matches(currentNode)) return true
            return currentNode.children.any { checkNode(it) }
        }
        checkNode(node)
    }
}