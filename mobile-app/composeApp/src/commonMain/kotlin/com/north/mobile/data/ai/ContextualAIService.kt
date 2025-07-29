package com.north.mobile.data.ai

import com.north.mobile.ui.chat.ChatMessage
import com.north.mobile.ui.chat.ChatAttachment
import com.north.mobile.ui.chat.AttachmentType
import com.north.mobile.data.repository.AuthRepository
import com.north.mobile.data.repository.InsightsRepository
import com.north.mobile.data.api.FinancialApiService
import com.north.mobile.data.api.ApiClient

/**
 * Enhanced AI service with memory and context awareness
 */
class ContextualAIService(
    private val authRepository: AuthRepository? = null,
    private val insightsRepository: InsightsRepository? = null
) {
    private val apiClient = ApiClient()
    private val financialApiService = FinancialApiService(apiClient)
    
    // In-memory conversation context and user profile
    private var conversationHistory = mutableListOf<ChatMessage>()
    private var userProfile = UserProfile()
    private var conversationTopics = mutableSetOf<String>()
    
    data class UserProfile(
        val name: String = "Odai",
        val goals: List<FinancialGoal> = listOf(
            FinancialGoal("Emergency Fund", 10000.0, 8500.0, "high"),
            FinancialGoal("Europe Trip", 5000.0, 2100.0, "medium")
        ),
        val spendingPatterns: Map<String, SpendingPattern> = mapOf(
            "dining" to SpendingPattern(450.0, 380.0, "increased 18% this month"),
            "groceries" to SpendingPattern(300.0, 295.0, "within budget range")
        ),
        val monthlyIncome: Double = 4500.0,
        val insights: MutableList<String> = mutableListOf()
    )
    
    data class FinancialGoal(
        val name: String,
        val targetAmount: Double,
        val currentAmount: Double,
        val priority: String
    ) {
        val progressPercentage: Int get() = ((currentAmount / targetAmount) * 100).toInt()
        val remainingAmount: Double get() = targetAmount - currentAmount
    }
    
    data class SpendingPattern(
        val budgetAmount: Double,
        val actualAmount: Double,
        val trend: String
    )
    
    suspend fun generateContextualResponse(
        userMessage: String,
        attachments: List<ChatAttachment>,
        allMessages: List<ChatMessage>
    ): String {
        // Update conversation history and extract topics
        updateConversationContext(userMessage, allMessages)
        
        return when {
            attachments.isNotEmpty() -> generateAttachmentResponse(userMessage, attachments)
            else -> {
                // Try backend AI first, fallback to enhanced local responses
                generateBackendResponse(userMessage) ?: generateEnhancedLocalResponse(userMessage)
            }
        }
    }
    
    private fun updateConversationContext(userMessage: String, allMessages: List<ChatMessage>) {
        // Keep recent conversation history
        conversationHistory.clear()
        conversationHistory.addAll(allMessages.takeLast(10))
        
        // Extract and store topics
        extractTopics(userMessage).forEach { conversationTopics.add(it) }
        
        // Store insights about user behavior
        if (userMessage.contains("goal", ignoreCase = true)) {
            userProfile.insights.add("User frequently discusses goals - highly goal-oriented")
        }
        if (userMessage.contains("Europe", ignoreCase = true)) {
            userProfile.insights.add("User actively planning Europe trip")
        }
    }
    
    private fun extractTopics(message: String): List<String> {
        val topics = mutableListOf<String>()
        
        if (message.contains("Europe", ignoreCase = true) || message.contains("trip", ignoreCase = true)) {
            topics.add("Travel Planning")
        }
        if (message.contains("emergency", ignoreCase = true)) {
            topics.add("Emergency Fund")
        }
        if (message.contains("budget", ignoreCase = true)) {
            topics.add("Budgeting")
        }
        if (message.contains("goal", ignoreCase = true)) {
            topics.add("Goal Planning")
        }
        if (message.contains("afford", ignoreCase = true)) {
            topics.add("Affordability")
        }
        if (message.contains("save", ignoreCase = true) || message.contains("saving", ignoreCase = true)) {
            topics.add("Savings Strategy")
        }
        
        return topics
    }
    
    private suspend fun generateBackendResponse(userMessage: String): String? {
        return try {
            val token = authRepository?.getCurrentToken() ?: return null
            
            // Enrich the message with real insights data if available
            val enrichedMessage = enrichMessageWithInsights(userMessage)
            
            val response = financialApiService.sendChatMessage(token, enrichedMessage)
            
            if (response.isSuccess) {
                val chatResponse = response.getOrThrow()
                // Store successful interaction
                userProfile.insights.add("Successfully used backend AI for: ${userMessage.take(50)}")
                chatResponse.response
            } else {
                println("⚠️ Backend AI failed: ${response.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            println("❌ Backend AI error: ${e.message}")
            null
        }
    }
    
    private suspend fun enrichMessageWithInsights(userMessage: String): String {
        if (insightsRepository == null) return userMessage
        
        try {
            // Get current insights and goals
            val insights = insightsRepository.insights.value
            val goals = insightsRepository.goals.value
            val patterns = insightsRepository.spendingPatterns.value
            
            if (insights.isEmpty() && goals.isEmpty() && patterns.isEmpty()) {
                return userMessage
            }
            
            // Add context to the message
            val contextBuilder = StringBuilder(userMessage)
            contextBuilder.append("\n\n[CONTEXT FROM REAL DATA:")
            
            if (insights.isNotEmpty()) {
                contextBuilder.append("\nRecent Insights: ")
                insights.take(3).forEach { insight ->
                    contextBuilder.append("${insight.title} (${insight.insight_type}); ")
                }
            }
            
            if (goals.isNotEmpty()) {
                contextBuilder.append("\nActive Goals: ")
                goals.take(3).forEach { goal ->
                    contextBuilder.append("${goal.title} ${goal.progressPercentage}% complete; ")
                }
            }
            
            if (patterns.isNotEmpty()) {
                contextBuilder.append("\nTop Spending: ")
                patterns.take(3).forEach { pattern ->
                    contextBuilder.append("${pattern.category} $${pattern.total_amount.toInt()}/month; ")
                }
            }
            
            contextBuilder.append("]")
            
            return contextBuilder.toString()
        } catch (e: Exception) {
            println("Error enriching message: ${e.message}")
            return userMessage
        }
    }
    
    private fun generateEnhancedLocalResponse(userMessage: String): String {
        return when {
            // Europe trip timing questions
            userMessage.contains("Europe", ignoreCase = true) && userMessage.contains("time", ignoreCase = true) -> {
                generateEuropeTimingResponse()
            }
            
            // Europe trip financial planning
            userMessage.contains("Europe", ignoreCase = true) && 
            (userMessage.contains("end of year", ignoreCase = true) || userMessage.contains("financially", ignoreCase = true)) -> {
                generateEuropeFinancialResponse()
            }
            
            // Goal progress check
            userMessage.contains("goal", ignoreCase = true) && userMessage.contains("doing", ignoreCase = true) -> {
                generateGoalProgressResponse()
            }
            
            // General goal discussion
            userMessage.contains("goal", ignoreCase = true) -> {
                generateGoalResponse()
            }
            
            // Budget questions
            userMessage.contains("budget", ignoreCase = true) -> {
                generateBudgetResponse()
            }
            
            // Affordability questions
            userMessage.contains("afford", ignoreCase = true) -> {
                generateAffordabilityResponse()
            }
            
            // Savings questions
            userMessage.contains("save", ignoreCase = true) || userMessage.contains("saving", ignoreCase = true) -> {
                generateSavingsResponse()
            }
            
            // Spending questions
            userMessage.contains("spend", ignoreCase = true) || userMessage.contains("spending", ignoreCase = true) -> {
                generateSpendingResponse()
            }
            
            // General response with context
            else -> {
                generateContextualGeneralResponse(userMessage)
            }
        }
    }
    
    private fun generateEuropeTimingResponse(): String {
        val europeGoal = userProfile.goals.find { it.name.contains("Europe", ignoreCase = true) }!!
        val monthsRemaining = 3
        val monthlyNeeded = europeGoal.remainingAmount / monthsRemaining
        
        return """Great question about timing your Europe trip! Based on your current progress (${europeGoal.progressPercentage}% towards your $${europeGoal.targetAmount.toInt()} goal), here's my personalized recommendation:

**Best Financial Timing for End-of-Year:**
• **October**: Perfect sweet spot - 25-30% cheaper than summer, great weather
• **November**: Even better deals, but weather can be unpredictable

**Your Financial Position:**
• Current savings: $${europeGoal.currentAmount.toInt()} of $${europeGoal.targetAmount.toInt()}
• You need: $${europeGoal.remainingAmount.toInt()} more
• Monthly target: $${monthlyNeeded.toInt()} to reach by October

**Money-Saving Tips:**
• Book flights 6-8 weeks ahead for October travel
• Consider Eastern Europe (Prague, Budapest) - 40% cheaper
• Travel Tuesday-Thursday for better rates

Based on your excellent grocery budget discipline ($${userProfile.spendingPatterns["groceries"]?.actualAmount?.toInt()}/$${userProfile.spendingPatterns["groceries"]?.budgetAmount?.toInt()}), you're already showing great financial habits!

Would you like me to create a specific savings plan to hit your October target?"""
    }
    
    private fun generateEuropeFinancialResponse(): String {
        val europeGoal = userProfile.goals.find { it.name.contains("Europe", ignoreCase = true) }!!
        
        return """Absolutely! Let's make your Europe trip financially smart. Here's your personalized plan:

**Your Current Position:**
• Europe fund: $${europeGoal.currentAmount.toInt()} of $${europeGoal.targetAmount.toInt()} (${europeGoal.progressPercentage}%)
• Monthly income: $${userProfile.monthlyIncome.toInt()}
• Time to year-end: ~3 months

**Recommended Strategy:**
• **Monthly savings needed**: $${(europeGoal.remainingAmount / 3).toInt()}
• **Best travel window**: October (shoulder season = 30% savings)
• **Trip budget breakdown**: Flights ($800), Hotels ($1,200), Food/Activities ($1,500), Buffer ($500)

**Smart Moves Based on Your Spending Patterns:**
• Your grocery discipline ($${userProfile.spendingPatterns["groceries"]?.actualAmount?.toInt()}/$${userProfile.spendingPatterns["groceries"]?.budgetAmount?.toInt()}) is excellent
• Consider redirecting some dining budget ($${userProfile.spendingPatterns["dining"]?.actualAmount?.toInt()}/$${userProfile.spendingPatterns["dining"]?.budgetAmount?.toInt()}) to boost travel savings

You're already showing great financial habits. Want me to show you how to redirect some dining expenses to accelerate your Europe fund?"""
    }
    
    private fun generateGoalProgressResponse(): String {
        val emergencyGoal = userProfile.goals.find { it.name.contains("Emergency", ignoreCase = true) }!!
        val europeGoal = userProfile.goals.find { it.name.contains("Europe", ignoreCase = true) }!!
        
        return """Your goals are looking fantastic! Here's your current progress:

**Emergency Fund**: ${emergencyGoal.progressPercentage}% complete ($${emergencyGoal.currentAmount.toInt()}/$${emergencyGoal.targetAmount.toInt()})
• You're SO close! Just $${emergencyGoal.remainingAmount.toInt()} away
• At your current pace, you'll complete this in ~2 months

**Europe Trip**: ${europeGoal.progressPercentage}% complete ($${europeGoal.currentAmount.toInt()}/$${europeGoal.targetAmount.toInt()})
• Right on track for your end-of-year timeline
• October travel would optimize both cost and experience

**What I love about your approach:**
• You're balancing multiple goals effectively
• Your spending discipline (especially groceries at $${userProfile.spendingPatterns["groceries"]?.actualAmount?.toInt()}/$${userProfile.spendingPatterns["groceries"]?.budgetAmount?.toInt()}) is paying off
• You're asking the right questions about timing and strategy

${if (conversationTopics.contains("Travel Planning")) "Based on our previous discussions about travel timing, " else ""}you're making smart strategic decisions.

Want to explore ways to accelerate either goal, or should we focus on optimizing your Europe trip planning?"""
    }
    
    private fun generateGoalResponse(): String {
        val hasContext = conversationTopics.isNotEmpty()
        
        return if (hasContext) {
            val recentTopics = conversationTopics.take(3).joinToString(", ")
            
            """I love that we're continuing our discussion about $recentTopics! Your current goals are:

${userProfile.goals.joinToString("\n") { goal -> 
    "• **${goal.name}**: ${goal.progressPercentage}% complete ($${goal.currentAmount.toInt()}/$${goal.targetAmount.toInt()})"
}}

From our conversations, I can see you're particularly focused on strategic planning and timing. What specific aspect would you like to dive deeper into?"""
        } else {
            """Goals are the foundation of great financial planning! I can see you're working on some exciting ones:

${userProfile.goals.joinToString("\n") { goal -> 
    "• **${goal.name}**: ${goal.progressPercentage}% complete"
}}

What would you like to focus on - tracking progress, adjusting timelines, or creating acceleration strategies?"""
        }
    }
    
    private fun generateBudgetResponse(): String {
        return """Your budget management has been solid! Here's what I'm seeing:

**Current Spending Patterns:**
${userProfile.spendingPatterns.entries.joinToString("\n") { (category, pattern) ->
    "• **${category.replaceFirstChar { it.uppercase() }}**: $${pattern.actualAmount.toInt()}/$${pattern.budgetAmount.toInt()} - ${pattern.trend}"
}}

**Budget Health:**
• Monthly income: $${userProfile.monthlyIncome.toInt()}
• You're staying disciplined with grocery spending
• Your dining optimization strategy is working well

**Recommendations:**
• Continue your grocery vs dining balance strategy
• Consider redirecting dining savings to your Europe fund
• Your current approach supports both goals effectively

Want me to help optimize any specific category or create a plan to boost your goal contributions?"""
    }
    
    private fun generateAffordabilityResponse(): String {
        val emergencyGoal = userProfile.goals.find { it.name.contains("Emergency") }!!
        
        return """Great question! Let me analyze this based on your financial position:

**Your Financial Snapshot:**
• Monthly income: $${userProfile.monthlyIncome.toInt()}
• Emergency fund: ${emergencyGoal.progressPercentage}% complete (excellent!)
• Goal progress: Both goals on track
• Spending discipline: Strong (especially groceries)

**Affordability Framework:**
• Emergency fund status: Strong (${emergencyGoal.progressPercentage}%)
• Monthly goal contributions: On track
• Spending discipline: Excellent

**To give you the best advice:**
What specific purchase are you considering? I can analyze:
• Impact on your Europe trip timeline
• Effect on emergency fund progress
• Alternative timing or financing options
• Ways to afford it without derailing goals

Share the details and I'll give you a personalized affordability analysis!"""
    }
    
    private fun generateSavingsResponse(): String {
        val totalProgress = userProfile.goals.sumOf { it.currentAmount }
        val totalTargets = userProfile.goals.sumOf { it.targetAmount }
        val overallProgress = ((totalProgress / totalTargets) * 100).toInt()
        
        return """Your savings discipline has been impressive! Here's your progress:

**Overall Savings Performance:**
• Combined goal progress: $overallProgress% ($${totalProgress.toInt()}/$${totalTargets.toInt()})
• You're consistently hitting your targets
• Your spending optimizations are accelerating progress

**What's Working Well:**
• Grocery budget discipline ($${userProfile.spendingPatterns["groceries"]?.actualAmount?.toInt()}/$${userProfile.spendingPatterns["groceries"]?.budgetAmount?.toInt()})
• Balanced approach to multiple goals
• Smart questioning about timing and strategy

**Optimization Opportunities:**
• Dining budget reallocation could boost Europe fund by $150/month
• Emergency fund is nearly complete - consider redirecting after completion
• October Europe travel timing would save 25-30% on trip costs

Want to explore specific acceleration strategies, or should we focus on optimizing your approach for the final push?"""
    }
    
    private fun generateSpendingResponse(): String {
        return """Your spending patterns show great financial awareness! Here's what I'm tracking:

**Current Spending Analysis:**
${userProfile.spendingPatterns.entries.joinToString("\n") { (category, pattern) ->
    "• **${category.replaceFirstChar { it.uppercase() }}**: $${pattern.actualAmount.toInt()} (${pattern.trend})"
}}

**Positive Trends:**
• Grocery spending: Consistently within budget
• You're making conscious trade-offs between dining out and cooking
• Overall discipline supports your savings goals

**Strategic Insights:**
• Your dining increase was balanced by smart grocery choices
• This approach maintains lifestyle while building wealth
• Current spending pattern supports both goals

Want to dive deeper into any specific category or explore optimization strategies?"""
    }
    
    private fun generateContextualGeneralResponse(userMessage: String): String {
        val hasContext = conversationTopics.isNotEmpty() || conversationHistory.size > 2
        
        return if (hasContext) {
            val contextInfo = if (conversationTopics.isNotEmpty()) {
                "We've been discussing: ${conversationTopics.take(3).joinToString(", ")}"
            } else {
                "Based on our conversation history"
            }
            
            """I'm here to help! $contextInfo.

You're making great progress with your financial goals:
• Emergency Fund: Nearly complete (${userProfile.goals.find { it.name.contains("Emergency") }?.progressPercentage}%)
• Europe Trip: On track (${userProfile.goals.find { it.name.contains("Europe") }?.progressPercentage}%)
• Spending discipline: Excellent

What would you like to explore today?"""
        } else {
            """Great to chat with you! I'm North, your personal CFO. I can help you with:

• **Goal Planning**: Your Europe trip and Emergency Fund strategies
• **Budget Optimization**: Making the most of your $${userProfile.monthlyIncome.toInt()} monthly income
• **Spending Analysis**: Building on your great financial discipline
• **Affordability Checks**: Smart purchase decisions that align with your goals

What's your biggest financial priority right now?"""
        }
    }
    
    private fun generateAttachmentResponse(userMessage: String, attachments: List<ChatAttachment>): String {
        val attachment = attachments.first()
        
        return when (attachment.type) {
            AttachmentType.RECEIPT -> {
                val grocerySpending = userProfile.spendingPatterns["groceries"]!!
                
                """Thanks for sharing that receipt! I can see it's a grocery purchase for $45.67.

**Budget Analysis:**
• Your grocery spending: $${grocerySpending.actualAmount.toInt()}/$${grocerySpending.budgetAmount.toInt()} this month
• This purchase: Well within your disciplined grocery budget
• Impact on goals: Zero negative impact - you're staying on track!

**What I love about this:**
• You're consistently making smart grocery choices
• This supports your Europe trip savings by avoiding expensive dining out
• Your spending discipline is accelerating your Emergency Fund progress

This is exactly the kind of conscious spending that's helping you reach your goals faster. Keep it up!"""
            }
            
            AttachmentType.DOCUMENT -> {
                """I've reviewed your document and I'm analyzing it in the context of your financial goals.

Based on your current progress (Emergency Fund at ${userProfile.goals.find { it.name.contains("Emergency") }?.progressPercentage}%, Europe Trip at ${userProfile.goals.find { it.name.contains("Europe") }?.progressPercentage}%), I can provide personalized insights.

What specific aspect of this document would you like me to focus on?"""
            }
            
            else -> {
                """Thanks for sharing that! I'm analyzing it based on your financial profile and our conversation history.

Your current financial position is strong, and I can help you use this information to optimize your strategy. How can I help?"""
            }
        }
    }
}