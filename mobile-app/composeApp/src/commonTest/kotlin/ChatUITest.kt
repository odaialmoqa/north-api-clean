import com.north.mobile.ui.chat.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for chat UI functionality
 */
class ChatUITest {
    
    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            id = "test_1",
            content = "Hello, North!",
            type = MessageType.USER
        )
        
        assertEquals("test_1", message.id)
        assertEquals("Hello, North!", message.content)
        assertEquals(MessageType.USER, message.type)
        assertTrue(message.supportingData.isEmpty())
        assertTrue(message.recommendations.isEmpty())
    }
    
    @Test
    fun testConversationContextUpdate() {
        val initialContext = ConversationContext(
            userId = "user_123",
            lastTopics = emptyList(),
            recentQueries = emptyList()
        )
        
        val updatedContext = updateConversationContext(
            initialContext,
            "Can I afford a $500 vacation?"
        )
        
        assertTrue(updatedContext.lastTopics.contains("affordability"))
        assertTrue(updatedContext.recentQueries.contains("Can I afford a $500 vacation?"))
    }
    
    @Test
    fun testTopicExtraction() {
        val topics1 = extractTopicsFromMessage("Can I afford this expense?")
        assertTrue(topics1.contains("affordability"))
        
        val topics2 = extractTopicsFromMessage("How much did I spend on groceries?")
        assertTrue(topics2.contains("spending"))
        
        val topics3 = extractTopicsFromMessage("Am I on track with my savings goal?")
        assertTrue(topics3.contains("goals"))
        assertTrue(topics3.contains("savings"))
    }
    
    @Test
    fun testQuickQuestionsGeneration() {
        val defaultQuestions = getDefaultQuickQuestions()
        assertTrue(defaultQuestions.isNotEmpty())
        assertTrue(defaultQuestions.size <= 6)
        
        val context = ConversationContext(
            lastTopics = listOf("affordability", "goals")
        )
        
        val contextualQuestions = getContextualQuickQuestions(context, null)
        assertTrue(contextualQuestions.isNotEmpty())
        assertTrue(contextualQuestions.size <= 4)
    }
}