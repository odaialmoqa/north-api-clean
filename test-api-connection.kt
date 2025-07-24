import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HealthResponse(
    val status: String,
    val database: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

fun main() = runBlocking {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    val baseUrl = "https://north-api-clean-production.up.railway.app"
    
    println("🚀 Testing Railway API Connection: $baseUrl")
    println("=" * 50)
    
    try {
        // Test 1: Health Check
        println("\n1️⃣ Testing Health Check...")
        val healthResponse = client.get("$baseUrl/health").body<HealthResponse>()
        println("✅ Health Check Successful!")
        println("   Status: ${healthResponse.status}")
        println("   Database: ${healthResponse.database}")
        
        // Test 2: User Registration
        println("\n2️⃣ Testing User Registration...")
        val testEmail = "test+${System.currentTimeMillis()}@north.app"
        val registerRequest = RegisterRequest(
            email = testEmail,
            password = "TestPassword123!",
            firstName = "Test",
            lastName = "User"
        )
        
        val registerResponse = client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerRequest)
        }.body<AuthResponse>()
        
        println("✅ Registration Successful!")
        println("   User ID: ${registerResponse.user.id}")
        println("   Email: ${registerResponse.user.email}")
        println("   Name: ${registerResponse.user.firstName} ${registerResponse.user.lastName}")
        println("   Token: ${registerResponse.token.take(20)}...")
        
        // Test 3: User Login
        println("\n3️⃣ Testing User Login...")
        val loginResponse = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to testEmail,
                "password" to "TestPassword123!"
            ))
        }.body<AuthResponse>()
        
        println("✅ Login Successful!")
        println("   User ID: ${loginResponse.user.id}")
        println("   Token: ${loginResponse.token.take(20)}...")
        
        println("\n🎉 All API tests passed! Your Railway API is working perfectly.")
        
    } catch (e: Exception) {
        println("❌ API Test Failed: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}

operator fun String.times(n: Int): String = this.repeat(n)