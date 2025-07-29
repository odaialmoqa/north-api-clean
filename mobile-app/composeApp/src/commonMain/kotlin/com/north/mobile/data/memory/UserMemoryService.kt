package com.north.mobile.data.memory

import com.north.mobile.ui.chat.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Serializable
data class UserMemoryProfile(
    val userId: String,
    val personalInfo: PersonalInfo = PersonalInfo(),
    val financialProfile: FinancialMemoryProfile = FinancialMemoryProfile(),
    val conversationHistory: List<ConversationSession> = emptyList(),
    val knowledgeGraph: UserKnowledgeGraph = UserKnowledgeGraph(),
    val preferences: UserMemoryPreferences = UserMemoryPreferences(),
    val lastUpdated: String = Clock.System.now().toString()
)

@Serializable
data class PersonalInfo(
    val name: String = "Odai",
    val email: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val occupation: String? = null,
    val relationshipStatus: String? = null
)

@Serializable
data class FinancialMemoryProfile(
    val monthlyIncome: Double? = null,
    val monthlyExpenses: Double? = null,
    val currentSavings: Double? = null,
    val goals: List<FinancialGoal> = listOf(
        FinancialGoal("Emergency Fund", 10000.0, 8500.0, "2024-12-31"),
        FinancialGoal("Europe Trip", 5000.0, 2100.0, "2024-12-31")
    ),
    val accounts: List<BankAccount> = emptyList(),
    val spendingPatterns: Map<String, SpendingPattern> = mapOf(
        "dining" to SpendingPattern("dining", 450.0, 380.0, "increased 18% this month"),
        "groceries" to SpendingPattern("groceries", 300.0, 295.0, "within budget range")
    ),
    val budgetCategories: Map<String, BudgetCategory> = emptyMap(),
    val creditScore: Int? = null,
    val debts: List<Debt> = emptyList()
)

@Serializable
data class FinancialGoal(
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val priority: String = "medium",
    val strategy: String? = null,
    val monthlyContribution: Double? = null
)

@Serializable
data class BankAccount(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val institution: String
)

@Serializable
data class SpendingPattern(
    val category: String,
    val budgetAmount: Double,
    val actualAmount: Double,
    val trend: String,
    val insights: List<String> = emptyList()
)

@Serializable
data class BudgetCategory(
    val name: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double
)

@Serializable
data class Debt(
    val name: String,
    val balance: Double,
    val interestRate: Double,
    val minimumPayment: Double,
    val type: String
)

@Serializable
data class ConversationSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String? = null,
    val messages: List<StoredChatMessage> = emptyList(),
    val topics: List<String> = emptyList(),
    val insights: List<String> = emptyList(),
    val actionItems: List<String> = emptyList()
)

@Serializable
data class StoredChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: String,
    val topics: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val sentiment: String? = null
)

@Serializable
data class UserKnowledgeGraph(
    val concepts: Map<String, FinancialConcept> = emptyMap(),
    val relationships: List<ConceptRelationship> = emptyList(),
    val userBehaviors: List<UserBehavior> = emptyList(),
    val insights: List<UserInsight> = emptyList()
)

@Serializable
data class FinancialConcept(
    val id: String,
    val name: String,
    val category: String,
    val userUnderstanding: String = "unknown", // basic, intermediate, advanced
    val interactions: Int = 0,
    val lastDiscussed: String? = null
)

@Serializable
data class ConceptRelationship(
    val fromConcept: String,
    val toConcept: String,
    val relationshipType: String, // "affects", "requires", "enables", etc.
    val strength: Double = 1.0
)

@Serializable
data class UserBehavior(
    val behaviorType: String,
    val frequency: String,
    val context: String,
    val impact: String,
    val firstObserved: String,
    val lastObserved: String
)

@Serializable
data class UserInsight(
    val insight: String,
    val category: String,
    val confidence: Double,
    val evidence: List<String>,
    val actionable: Boolean,
    val createdAt: String
)

@Serializable
data class UserMemoryPreferences(
    val communicationStyle: String = "friendly and encouraging",
    val currency: String = "CAD",
    val reminderFrequency: String = "weekly",
    val preferredTopics: List<String> = emptyList(),
    val avoidedTopics: List<String> = emptyList(),
    val responseLength: String = "medium", // short, medium, detailed
    val personalityTraits: List<String> = emptyList()
)

class UserMemoryService {
    private var currentUserMemory: UserMemoryProfile? = null
    private val apiClient = com.north.mobile.data.api.ApiClient()
    
    suspend fun loadUserMemory(userId: String): UserMemoryProfile {
        // For now, create default profile (backend integration will be added later)
        currentUserMemory = createDefaultMemoryProfile(userId)
        return currentUserMemory!!
    }
    
    suspend fun saveUserMemory(memory: UserMemoryProfile) {
        // Store in memory for now (backend sync will be added later)
        currentUserMemory = memory
        println("💾 User memory saved: ${memory.conversationHistory.size} sessions, ${memory.knowledgeGraph.concepts.size} concepts")
    }
    
    private fun createDefaultMemoryProfile(userId: String): UserMemoryProfile {
        val defaultMemory = UserMemoryProfile(userId = userId)
        currentUserMemory = defaultMemory
        return defaultMemory
    }
    
    private fun parseMemoryFromJson(jsonData: String): UserMemoryProfile {
        // TODO: Implement proper JSON parsing
        // For now, return default profile
        return UserMemoryProfile(userId = "user_current")
    }
    
    fun getCurrentMemory(): UserMemoryProfile? = currentUserMemory
    
    suspend fun addConversationMessage(message: ChatMessage, topics: List<String> = emptyList()) {
        currentUserMemory?.let { memory ->
            val currentSession = getCurrentOrCreateSession()
            val storedMessage = StoredChatMessage(
                message = message.message,
                isFromUser = message.isFromUser,
                timestamp = Clock.System.now().toString(),
                topics = topics,
                entities = extractEntities(message.message)
            )
            
            val updatedSession = currentSession.copy(
                messages = currentSession.messages + storedMessage,
                topics = (currentSession.topics + topics).distinct()
            )
            
            val updatedHistory = memory.conversationHistory.dropLast(1) + updatedSession
            val updatedMemory = memory.copy(
                conversationHistory = updatedHistory,
                lastUpdated = Clock.System.now().toString()
            )
            
            saveUserMemory(updatedMemory)
        }
    }
    
    private fun getCurrentOrCreateSession(): ConversationSession {
        val memory = currentUserMemory ?: return createNewSession()
        
        return if (memory.conversationHistory.isEmpty() || 
                   memory.conversationHistory.last().endTime != null) {
            createNewSession()
        } else {
            memory.conversationHistory.last()
        }
    }
    
    private fun createNewSession(): ConversationSession {
        return ConversationSession(
            sessionId = "session_${Clock.System.now().toEpochMilliseconds()}",
            startTime = Clock.System.now().toString()
        )
    }
    
    private fun extractEntities(message: String): List<String> {
        val entities = mutableListOf<String>()
        
        // Extract financial entities
        if (message.contains("Europe", ignoreCase = true)) entities.add("Europe Trip")
        if (message.contains("emergency", ignoreCase = true)) entities.add("Emergency Fund")
        if (message.contains("budget", ignoreCase = true)) entities.add("Budget")
        if (message.contains("save", ignoreCase = true) || message.contains("saving", ignoreCase = true)) entities.add("Savings")
        if (message.contains("spend", ignoreCase = true) || message.contains("spending", ignoreCase = true)) entities.add("Spending")
        if (message.contains("goal", ignoreCase = true)) entities.add("Goals")
        
        // Extract amounts (simple regex)
        val amountRegex = Regex("\\$[0-9,]+")
        amountRegex.findAll(message).forEach { entities.add("Amount: ${it.value}") }
        
        return entities
    }
    
    suspend fun updateFinancialGoal(goalName: String, currentAmount: Double? = null, targetAmount: Double? = null) {
        currentUserMemory?.let { memory ->
            val updatedGoals = memory.financialProfile.goals.map { goal ->
                if (goal.name == goalName) {
                    goal.copy(
                        currentAmount = currentAmount ?: goal.currentAmount,
                        targetAmount = targetAmount ?: goal.targetAmount
                    )
                } else goal
            }
            
            val updatedProfile = memory.financialProfile.copy(goals = updatedGoals)
            val updatedMemory = memory.copy(
                financialProfile = updatedProfile,
                lastUpdated = Clock.System.now().toString()
            )
            
            saveUserMemory(updatedMemory)
        }
    }
    
    suspend fun addInsight(insight: String, category: String, evidence: List<String>) {
        currentUserMemory?.let { memory ->
            val newInsight = UserInsight(
                insight = insight,
                category = category,
                confidence = 0.8,
                evidence = evidence,
                actionable = true,
                createdAt = Clock.System.now().toString()
            )
            
            val updatedGraph = memory.knowledgeGraph.copy(
                insights = memory.knowledgeGraph.insights + newInsight
            )
            
            val updatedMemory = memory.copy(
                knowledgeGraph = updatedGraph,
                lastUpdated = Clock.System.now().toString()
            )
            
            saveUserMemory(updatedMemory)
        }
    }
    
    fun getRelevantContext(query: String, limit: Int = 5): List<String> {
        val memory = currentUserMemory ?: return emptyList()
        val context = mutableListOf<String>()
        
        // Add recent conversation context
        memory.conversationHistory.takeLast(2).forEach { session ->
            session.messages.takeLast(4).forEach { message ->
                if (message.topics.any { topic -> 
                    query.contains(topic, ignoreCase = true) 
                }) {
                    context.add("Previous discussion: ${message.message}")
                }
            }
        }
        
        // Add relevant insights
        memory.knowledgeGraph.insights.filter { insight ->
            query.contains(insight.category, ignoreCase = true) ||
            insight.insight.contains(query.split(" ").first(), ignoreCase = true)
        }.take(2).forEach { insight ->
            context.add("Insight: ${insight.insight}")
        }
        
        // Add relevant financial data
        memory.financialProfile.goals.forEach { goal ->
            if (query.contains(goal.name, ignoreCase = true)) {
                val progress = (goal.currentAmount / goal.targetAmount * 100).toInt()
                context.add("Goal status: ${goal.name} is at $progress% (${goal.currentAmount}/${goal.targetAmount})")
            }
        }
        
        return context.take(limit)
    }
}