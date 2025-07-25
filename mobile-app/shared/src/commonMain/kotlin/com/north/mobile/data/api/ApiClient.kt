package com.north.mobile.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP client configuration for North API
 */
class ApiClient {
    
    companion object {
        const val BASE_URL = "https://north-api-clean-production.up.railway.app"
        const val LOCAL_URL = "http://10.0.2.2:3000" // Android emulator localhost for testing
    }
    
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(DefaultRequest) {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
    }
    
    /**
     * Add authorization header to requests
     */
    fun HttpRequestBuilder.authorize(token: String) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
    
    /**
     * Make authenticated GET request
     */
    suspend inline fun <reified T> get(endpoint: String, token: String? = null): T {
        return httpClient.get(endpoint) {
            token?.let { authorize(it) }
        }.body()
    }
    
    /**
     * Make authenticated POST request
     */
    suspend inline fun <reified T> post(endpoint: String, body: Any, token: String? = null): T {
        return httpClient.post(endpoint) {
            token?.let { authorize(it) }
            setBody(body)
        }.body()
    }
}