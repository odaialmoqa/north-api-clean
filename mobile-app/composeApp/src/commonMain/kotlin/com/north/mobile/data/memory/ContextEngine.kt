package com.north.mobile.data.memory

import com.north.mobile.ui.chat.ChatMessage
import kotlinx.datetime.Clock

class ContextEngine(private val memoryService: UserMemoryService) {
    
    suspend fun generateContextualResponse(
        userMessage: String,
        allMessages: List<ChatMessage>
    ): String {
        // Load user memory
        val memory = memoryService.getCurrentMemory() ?: return generateFallbackResponse(userMessage)
        
        // Add current message to memory
        val currentMessage = ChatMessage(userMessage, true)
        val topics = extractTopics(userMessage)
        memoryService.addConversationMessage(currentMessage, topics)
        
        // Get relevant context
        val relevantContext = memoryService.getRelevantContext(userMessage)
        
        // Generate response based on context and message analysis
        return generateIntelligentResponse(userMessage, memory, relevantContext, allMessages)
    }
    
    private fun extractTopics(message: String): List<String> {
        val topics = mutableListOf<String>()
        
        // Financial topics
        if (message.contains("Europe", ignoreCase = true) || message.contains("trip", ignoreCase = true)) {
            topics.add("Travel Planning")
        }
        if (message.contains("emergency", ignoreCase = true)) {
            topics.add("Emergency Fund")
        }
        if (message.contains("budget", ignoreCase = true)) {
            topics.add("Budgeting")
        }
        if (message.contains("save", ignoreCase = true) || message.contains("saving", ignoreCase = true)) {
            topics.add("Savings Strategy")
        }
        if (message.contains("goal", ignoreCase = true)) {
            topics.add("Goal Planning")
        }
        if (message.contains("afford", ignoreCase = true)) {
            topics.add("Affordability Analysis")
        }
        if (message.contains("spend", ignoreCase = true) || message.contains("spending", ignoreCase = true)) {
            topics.add("Spending Analysis")
        }
        
        return topics
    }
    
    private suspend fun generateIntelligentResponse(
        userMessage: String,
        memory: UserMemoryProfile,
        context: List<String>,
        allMessages: List<ChatMessage>
    ): String {
        return when {
            // Europe trip specific responses with memory
            userMessage.contains("Europe", ignoreCase = true) && userMessage.contains("time", ignoreCase = true) -> {
                generateEuropeTimingResponse(memory, context)
            }
            
            userMessage.contains("Europe", ignoreCase = true) && (userMessage.contains("end of year", ignoreCase = true) || userMessage.contains("financially", ignoreCase = true)) -> {
                generateEuropeFinancialResponse(memory, context)
            }
            
            // Goal-related responses with memory
            userMessage.contains("goal", ignoreCase = true) && userMessage.contains("doing", ignoreCase = true) -> {
                generateGoalProgressResponse(memory, context)
            }
            
            userMessage.contains("goal", ignoreCase = true) -> {
                generateGoalResponse(memory, context, userMessage)
            }
            
            // Budget responses with memory
            userMessage.contains("budget", ignoreCase = true) -> {
                generateBudgetResponse(memory, context)
            }
            
            // Affordability with memory
            userMessage.contains("afford", ignoreCase = true) -> {
                generateAffordabilityResponse(memory, context, userMessage)
            }
            
            // Savings with memory
            userMessage.contains("save", ignoreCase = true) || userMessage.contains("saving", ignoreCase = true) -> {
                generateSavingsResponse(memory, context)
            }
            
            // Spending with memory
            userMessage.contains("spend", ignoreCase = true) || userMessage.contains("spending", ignoreCase = true) -> {
                generateSpendingResponse(memory, context)
            }
            
            else -> {
                generateGeneralResponseWithMemory(memory, context, userMessage, allMessages)
            }
        }
    }
    
    private suspend fun generateEuropeTimingResponse(memory: UserMemoryProfile, context: List<String>): String {
        val europeGoal = memory.financialProfile.goals.find { it.name.contains("Europe", ignoreCase = true) }
        val currentAmount = europeGoal?.currentAmount ?: 2100.0
        val targetAmount = europeGoal?.targetAmount ?: 5000.0
        val progress = (currentAmount / targetAmount * 100).toInt()
        
        // Add insight to memory
        memoryService.addInsight(
            "User is planning Europe trip timing for end of year",
            "Travel Planning",
            listOf("Asked about optimal timing", "Target: end of year", "Current progress: $progress%")
        )
        
        return """Great question about timing your Europe trip! Based on your current progress ($progress% towards your $${targetAmount.toInt()} goal), here's my recommendation:

**Best Financial Timing for End-of-Year:**
• **October**: Perfect sweet spot - 25-30% cheaper than summer, great weather
• **November**: Even better deals, but weather can be unpredictable

**Your Financial Position:**
• Current savings: $${currentAmount.toInt()} of $${targetAmount.toInt()}
• You need: $${(targetAmount - currentAmount).toInt()} more
• Monthly target: $${((targetAmount - currentAmount) / 3).toInt()} to reach by October

**Money-Saving Tips:**
• Book flights 6-8 weeks ahead for October travel
• Consider Eastern Europe (Prague, Budapest) - 40% cheaper
• Travel Tuesday-Thursday for better rates

Would you like me to create a specific savings plan to hit your October target?"""
    }
    
    private suspend fun generateEuropeFinancialResponse(memory: UserMemoryProfile, context: List<String>): String {
        val europeGoal = memory.financialProfile.goals.find { it.name.contains("Europe", ignoreCase = true) }
        val currentAmount = europeGoal?.currentAmount ?: 2100.0
        val targetAmount = europeGoal?.targetAmount ?: 5000.0
        val monthlyIncome = memory.financialProfile.monthlyIncome ?: 4500.0
        
        return """Absolutely! Let's make your Europe trip financially smart. Here's your personalized plan:

**Your Current Position:**
• Europe fund: $${currentAmount.toInt()} of $${targetAmount.toInt()} (${((currentAmount/targetAmount)*100).toInt()}%)
• Monthly income: $${monthlyIncome.toInt()}
• Time to year-end: ~3 months

**Recommended Strategy:**
• **Monthly savings needed**: $${((targetAmount - currentAmount) / 3).toInt()}
• **Best travel window**: October (shoulder season = 30% savings)
• **Trip budget breakdown**: Flights ($800), Hotels ($1,200), Food/Activities ($1,500), Buffer ($500)

**Smart Moves:**
• Book now for October travel - prices increase closer to date
• Consider 8-10 day trip vs 14 days to optimize cost/experience
• Use your dining budget optimization to boost travel savings

Based on your spending patterns, you're already great at managing your grocery budget. Want me to show you how to redirect some dining expenses to accelerate your Europe fund?"""
    }
    
    private suspend fun generateGoalProgressResponse(memory: UserMemoryProfile, context: List<String>): String {
        val goals = memory.financialProfile.goals
        val emergencyGoal = goals.find { it.name.contains("Emergency", ignoreCase = true) }
        val europeGoal = goals.find { it.name.contains("Europe", ignoreCase = true) }
        
        return """Your goals are looking fantastic! Here's your current progress:

**Emergency Fund**: ${emergencyGoal?.let { "${((it.currentAmount/it.targetAmount)*100).toInt()}% complete ($${it.currentAmount.toInt()}/$${it.targetAmount.toInt()})" } ?: "Not tracked"}
• You're SO close! Just $${emergencyGoal?.let { (it.targetAmount - it.currentAmount).toInt() } ?: "1,500"} away
• At your current pace, you'll complete this in ~2 months

**Europe Trip**: ${europeGoal?.let { "${((it.currentAmount/it.targetAmount)*100).toInt()}% complete ($${it.currentAmount.toInt()}/$${it.targetAmount.toInt()})" } ?: "Not tracked"}
• Right on track for your end-of-year timeline
• October travel would optimize both cost and experience

**What I love about your approach:**
• You're balancing multiple goals effectively
• Your spending discipline (especially groceries) is paying off
• You're asking the right questions about timing and strategy

Want to explore ways to accelerate either goal, or should we focus on optimizing your Europe trip planning?"""
    }
    
    private fun generateGoalResponse(memory: UserMemoryProfile, context: List<String>, userMessage: String): String {
        val hasContext = context.isNotEmpty()
        val goals = memory.financialProfile.goals
        
        return if (hasContext) {
            """I love that we're continuing our goal discussion! Based on what we've talked about:

${context.take(2).joinToString("\n") { "• $it" }}

Your current goals are:
${goals.joinToString("\n") { goal -> 
    val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt()
    "• **${goal.name}**: $progress% complete ($${goal.currentAmount.toInt()}/$${goal.targetAmount.toInt()})"
}}

What specific aspect would you like to dive deeper into? I can help with strategy, timeline adjustments, or acceleration plans."""
        } else {
            """Goals are the foundation of great financial planning! I can see you're working on some exciting ones:

${goals.joinToString("\n") { goal -> 
    val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt()
    "• **${goal.name}**: $progress% complete"
}}

What would you like to focus on - tracking progress, adjusting timelines, or creating new goals?"""
        }
    }
    
    private fun generateBudgetResponse(memory: UserMemoryProfile, context: List<String>): String {
        val spendingPatterns = memory.financialProfile.spendingPatterns
        val monthlyIncome = memory.financialProfile.monthlyIncome ?: 4500.0
        
        return """Your budget management has been solid! Here's what I'm seeing:

**Current Spending Patterns:**
${spendingPatterns.entries.joinToString("\n") { (category, pattern) ->
    "• **${category.capitalize()}**: $${pattern.actualAmount.toInt()}/$${pattern.budgetAmount.toInt()} - ${pattern.trend}"
}}

**Budget Health:**
• Monthly income: $${monthlyIncome.toInt()}
• You're staying disciplined with grocery spending
• Dining optimization is working well

**Recommendations:**
• Continue your grocery vs dining balance strategy
• Consider redirecting dining savings to your Europe fund
• Your current approach supports both goals effectively

Want me to help optimize any specific category or create a plan to boost your goal contributions?"""
    }
    
    private fun generateAffordabilityResponse(memory: UserMemoryProfile, context: List<String>, userMessage: String): String {
        val monthlyIncome = memory.financialProfile.monthlyIncome ?: 4500.0
        val currentSavings = memory.financialProfile.currentSavings ?: 10600.0
        val goals = memory.financialProfile.goals
        
        return """Great question! Let me analyze this based on your financial position:

**Your Financial Snapshot:**
• Monthly income: $${monthlyIncome.toInt()}
• Current savings: $${currentSavings.toInt()}
• Goal progress: ${goals.map { "${it.name} (${((it.currentAmount/it.targetAmount)*100).toInt()}%)" }.joinToString(", ")}

**Affordability Framework:**
• Emergency fund status: ${goals.find { it.name.contains("Emergency") }?.let { "Strong (${((it.currentAmount/it.targetAmount)*100).toInt()}%)" } ?: "Building"}
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
    
    private fun generateSavingsResponse(memory: UserMemoryProfile, context: List<String>): String {
        val goals = memory.financialProfile.goals
        val totalGoalProgress = goals.sumOf { it.currentAmount }
        val totalGoalTargets = goals.sumOf { it.targetAmount }
        val overallProgress = ((totalGoalProgress / totalGoalTargets) * 100).toInt()
        
        return """Your savings discipline has been impressive! Here's your progress:

**Overall Savings Performance:**
• Combined goal progress: $overallProgress% ($${totalGoalProgress.toInt()}/$${totalGoalTargets.toInt()})
• You're consistently hitting your targets
• Your spending optimizations are accelerating progress

**What's Working Well:**
• Grocery budget discipline
• Balanced approach to multiple goals
• Smart questioning about timing and strategy

**Optimization Opportunities:**
• Dining budget reallocation could boost Europe fund by $150/month
• Emergency fund is nearly complete - consider redirecting after completion
• October Europe travel timing would save 25-30% on trip costs

**Next Steps:**
Want to explore specific acceleration strategies, or should we focus on optimizing your approach for the final push to your goals?"""
    }
    
    private fun generateSpendingResponse(memory: UserMemoryProfile, context: List<String>): String {
        val spendingPatterns = memory.financialProfile.spendingPatterns
        
        return """Your spending patterns show great financial awareness! Here's what I'm tracking:

**Current Spending Analysis:**
${spendingPatterns.entries.joinToString("\n") { (category, pattern) ->
    "• **${category.capitalize()}**: $${pattern.actualAmount.toInt()} (${pattern.trend})"
}}

**Positive Trends:**
• Grocery spending: Consistently within budget
• You're making conscious trade-offs between dining out and cooking
• Overall discipline supports your savings goals

**Strategic Insights:**
• Your 18% dining increase last month was balanced by smart grocery choices
• This approach maintains lifestyle while building wealth
• Current spending pattern supports both emergency fund and Europe trip progress

**Recommendations:**
• Continue your balanced grocery/dining approach
• Consider seasonal adjustments (more dining savings in fall = more Europe fund)
• Your spending consciousness is already accelerating goal achievement

Want to dive deeper into any specific category or explore optimization strategies?"""
    }
    
    private fun generateGeneralResponseWithMemory(
        memory: UserMemoryProfile, 
        context: List<String>, 
        userMessage: String,
        allMessages: List<ChatMessage>
    ): String {
        val isEarlyConversation = allMessages.size < 3
        val hasRelevantContext = context.isNotEmpty()
        
        return when {
            hasRelevantContext -> {
                """I'm here to help! Based on our previous discussions:

${context.take(3).joinToString("\n") { "• $it" }}

You're making great progress with your financial goals. What would you like to explore today?"""
            }
            
            isEarlyConversation -> {
                """Great to chat with you! I'm North, your personal CFO. I can help you with:

• **Goal Planning**: Your Europe trip and Emergency Fund strategies
• **Budget Optimization**: Making the most of your $${memory.financialProfile.monthlyIncome?.toInt() ?: 4500} monthly income
• **Spending Analysis**: Building on your great grocery budget discipline
• **Affordability Checks**: Smart purchase decisions that align with your goals

What's your biggest financial priority right now?"""
            }
            
            else -> {
                """I'm here to help with whatever's on your mind! You've been doing great with your financial discipline and goal progress. 

Your current focus areas:
• Emergency Fund: Nearly complete!
• Europe Trip: On track for end-of-year
• Spending optimization: Working well

What would you like to explore today?"""
            }
        }
    }
    
    private fun generateFallbackResponse(userMessage: String): String {
        return "I'm here to help you with your financial goals! Let me learn more about your situation so I can provide better personalized advice. What's on your mind today?"
    }
}