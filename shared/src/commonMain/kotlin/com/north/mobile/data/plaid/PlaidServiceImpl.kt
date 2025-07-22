package com.north.mobile.data.plaid

import com.north.mobile.domain.model.Account
import com.north.mobile.domain.model.FinancialInstitution
import com.north.mobile.domain.model.Transaction
import com.north.mobile.domain.model.CanadianInstitutions
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlinx.datetime.LocalDate

class PlaidServiceImpl(
    private val httpClient: HttpClient,
    private val clientId: String,
    private val secret: String,
    private val environment: PlaidEnvironment = PlaidEnvironment.SANDBOX
) : PlaidService {
    
    private val baseUrl = when (environment) {
        PlaidEnvironment.SANDBOX -> "https://sandbox.plaid.com"
        PlaidEnvironment.DEVELOPMENT -> "https://development.plaid.com"
        PlaidEnvironment.PRODUCTION -> "https://production.plaid.com"
    }
    
    override suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("client_name", "North - Personal Finance")
                put("country_codes", JsonArray(listOf(JsonPrimitive("CA"))))
                put("language", "en")
                putJsonObject("user") {
                    put("client_user_id", userId)
                }
                put("products", JsonArray(listOf(
                    JsonPrimitive("transactions"),
                    JsonPrimitive("auth"),
                    JsonPrimitive("identity")
                )))
                putJsonObject("account_filters") {
                    putJsonObject("depository") {
                        put("account_subtypes", JsonArray(listOf(
                            JsonPrimitive("checking"),
                            JsonPrimitive("savings")
                        )))
                    }
                    putJsonObject("credit") {
                        put("account_subtypes", JsonArray(listOf(
                            JsonPrimitive("credit card")
                        )))
                    }
                }
            }
            
            val response = httpClient.post("$baseUrl/link/token/create") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val linkToken = responseBody["link_token"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing link_token in response", "MISSING_LINK_TOKEN")
                
                val expiration = responseBody["expiration"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing expiration in response", "MISSING_EXPIRATION")
                
                val requestId = responseBody["request_id"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing request_id in response", "MISSING_REQUEST_ID")
                
                Result.success(PlaidLinkToken(
                    linkToken = linkToken,
                    expiration = kotlinx.datetime.Instant.parse(expiration),
                    requestId = requestId
                ))
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to create link token", e))
        }
    }
    
    override suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("public_token", publicToken)
            }
            
            val response = httpClient.post("$baseUrl/item/public_token/exchange") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val accessToken = responseBody["access_token"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing access_token in response", "MISSING_ACCESS_TOKEN")
                
                val itemId = responseBody["item_id"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing item_id in response", "MISSING_ITEM_ID")
                
                // Get institution info
                val itemResult = getItem(accessToken)
                val institutionId = itemResult.getOrNull()?.institutionId ?: "unknown"
                val institutionName = CanadianInstitutions.getById(institutionId)?.displayName ?: "Unknown Bank"
                
                Result.success(PlaidAccessToken(
                    accessToken = accessToken,
                    itemId = itemId,
                    institutionId = institutionId,
                    institutionName = institutionName
                ))
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to exchange public token", e))
        }
    }
    
    override suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("access_token", accessToken)
            }
            
            val response = httpClient.post("$baseUrl/accounts/get") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val accountsJson = responseBody["accounts"]?.jsonArray
                    ?: throw PlaidServiceError.ApiError("Missing accounts in response", "MISSING_ACCOUNTS")
                
                val accounts = accountsJson.map { accountJson ->
                    parseAccount(accountJson.jsonObject)
                }
                
                Result.success(accounts)
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to get accounts", e))
        }
    }
    
    override suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>> {
        return getAccounts(accessToken) // Same endpoint provides balance information
    }
    
    override suspend fun getTransactions(
        accessToken: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountIds: List<String>?
    ): Result<List<Transaction>> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("access_token", accessToken)
                put("start_date", startDate.toString())
                put("end_date", endDate.toString())
                put("count", 500) // Maximum transactions per request
                put("offset", 0)
                accountIds?.let { ids ->
                    put("account_ids", JsonArray(ids.map { JsonPrimitive(it) }))
                }
            }
            
            val response = httpClient.post("$baseUrl/transactions/get") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val transactionsJson = responseBody["transactions"]?.jsonArray
                    ?: throw PlaidServiceError.ApiError("Missing transactions in response", "MISSING_TRANSACTIONS")
                
                val transactions = transactionsJson.map { transactionJson ->
                    parseTransaction(transactionJson.jsonObject)
                }
                
                Result.success(transactions)
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to get transactions", e))
        }
    }
    
    override suspend fun getItem(accessToken: String): Result<PlaidItem> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("access_token", accessToken)
            }
            
            val response = httpClient.post("$baseUrl/item/get") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val itemJson = responseBody["item"]?.jsonObject
                    ?: throw PlaidServiceError.ApiError("Missing item in response", "MISSING_ITEM")
                
                val item = parseItem(itemJson)
                Result.success(item)
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to get item", e))
        }
    }
    
    override suspend fun removeItem(accessToken: String): Result<Unit> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("access_token", accessToken)
            }
            
            val response = httpClient.post("$baseUrl/item/remove") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to remove item", e))
        }
    }
    
    override suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken> {
        return try {
            val requestBody = buildJsonObject {
                put("client_id", clientId)
                put("secret", secret)
                put("access_token", accessToken)
                put("client_name", "North - Personal Finance")
                put("country_codes", JsonArray(listOf(JsonPrimitive("CA"))))
                put("language", "en")
                putJsonObject("update") {
                    put("account_selection_enabled", true)
                }
            }
            
            val response = httpClient.post("$baseUrl/link/token/create") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                val linkToken = responseBody["link_token"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing link_token in response", "MISSING_LINK_TOKEN")
                
                val expiration = responseBody["expiration"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing expiration in response", "MISSING_EXPIRATION")
                
                val requestId = responseBody["request_id"]?.jsonPrimitive?.content
                    ?: throw PlaidServiceError.ApiError("Missing request_id in response", "MISSING_REQUEST_ID")
                
                Result.success(PlaidLinkToken(
                    linkToken = linkToken,
                    expiration = kotlinx.datetime.Instant.parse(expiration),
                    requestId = requestId
                ))
            } else {
                val errorBody = response.body<JsonObject>()
                Result.failure(parseError(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(PlaidServiceError.NetworkError("Failed to create update link token", e))
        }
    }
    
    override suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>> {
        // For now, return our predefined list of Canadian institutions
        // In a real implementation, you might want to fetch this from Plaid's institutions endpoint
        return Result.success(CanadianInstitutions.MAJOR_CANADIAN_BANKS)
    }
    
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> {
        return Result.success(CanadianInstitutions.searchByName(query))
    }
    
    private fun parseAccount(accountJson: JsonObject): PlaidAccount {
        val accountId = accountJson["account_id"]?.jsonPrimitive?.content ?: ""
        val name = accountJson["name"]?.jsonPrimitive?.content ?: ""
        val officialName = accountJson["official_name"]?.jsonPrimitive?.contentOrNull
        val mask = accountJson["mask"]?.jsonPrimitive?.contentOrNull
        val type = accountJson["type"]?.jsonPrimitive?.content?.let { 
            PlaidAccountType.valueOf(it.uppercase()) 
        } ?: PlaidAccountType.OTHER
        val subtype = accountJson["subtype"]?.jsonPrimitive?.contentOrNull?.let {
            PlaidAccountSubtype.valueOf(it.uppercase().replace(" ", "_"))
        }
        
        val balancesJson = accountJson["balances"]?.jsonObject ?: JsonObject(emptyMap())
        val balances = PlaidBalances(
            available = balancesJson["available"]?.jsonPrimitive?.doubleOrNull,
            current = balancesJson["current"]?.jsonPrimitive?.doubleOrNull,
            limit = balancesJson["limit"]?.jsonPrimitive?.doubleOrNull,
            isoCurrencyCode = balancesJson["iso_currency_code"]?.jsonPrimitive?.contentOrNull
        )
        
        return PlaidAccount(
            accountId = accountId,
            itemId = "", // Will be set by caller
            name = name,
            officialName = officialName,
            type = type,
            subtype = subtype,
            mask = mask,
            balances = balances,
            verificationStatus = null
        )
    }
    
    private fun parseTransaction(transactionJson: JsonObject): Transaction {
        // This is a simplified implementation - you'd need to map Plaid transaction format
        // to your Transaction model properly
        val transactionId = transactionJson["transaction_id"]?.jsonPrimitive?.content ?: ""
        val accountId = transactionJson["account_id"]?.jsonPrimitive?.content ?: ""
        val amount = transactionJson["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val name = transactionJson["name"]?.jsonPrimitive?.content ?: ""
        val date = transactionJson["date"]?.jsonPrimitive?.content ?: ""
        
        return Transaction(
            id = transactionId,
            accountId = accountId,
            amount = com.north.mobile.domain.model.Money.fromDollars(-amount), // Plaid uses positive for outflows
            description = name,
            category = com.north.mobile.domain.model.Category.UNCATEGORIZED, // Would need proper mapping
            date = kotlinx.datetime.LocalDate.parse(date),
            isRecurring = false
        )
    }
    
    private fun parseItem(itemJson: JsonObject): PlaidItem {
        val itemId = itemJson["item_id"]?.jsonPrimitive?.content ?: ""
        val institutionId = itemJson["institution_id"]?.jsonPrimitive?.contentOrNull
        val webhook = itemJson["webhook"]?.jsonPrimitive?.contentOrNull
        val availableProducts = itemJson["available_products"]?.jsonArray?.map { 
            it.jsonPrimitive.content 
        } ?: emptyList()
        val billedProducts = itemJson["billed_products"]?.jsonArray?.map { 
            it.jsonPrimitive.content 
        } ?: emptyList()
        
        return PlaidItem(
            itemId = itemId,
            institutionId = institutionId ?: "",
            webhook = webhook,
            error = null, // Would parse error if present
            availableProducts = availableProducts,
            billedProducts = billedProducts,
            consentExpirationTime = null,
            updateType = null
        )
    }
    
    private fun parseError(errorJson: JsonObject): PlaidServiceError {
        val errorType = errorJson["error_type"]?.jsonPrimitive?.content ?: "UNKNOWN"
        val errorCode = errorJson["error_code"]?.jsonPrimitive?.content ?: "UNKNOWN"
        val errorMessage = errorJson["error_message"]?.jsonPrimitive?.content ?: "Unknown error"
        val displayMessage = errorJson["display_message"]?.jsonPrimitive?.contentOrNull
        val requestId = errorJson["request_id"]?.jsonPrimitive?.contentOrNull
        
        val plaidError = PlaidError(
            errorType = errorType,
            errorCode = errorCode,
            errorMessage = errorMessage,
            displayMessage = displayMessage,
            requestId = requestId
        )
        
        return when (errorType) {
            "INVALID_REQUEST" -> PlaidServiceError.InvalidRequestError(errorMessage)
            "INVALID_INPUT" -> PlaidServiceError.InvalidRequestError(errorMessage)
            "INSTITUTION_ERROR" -> PlaidServiceError.ItemError(plaidError)
            "RATE_LIMIT_EXCEEDED" -> PlaidServiceError.RateLimitError(errorMessage)
            "API_ERROR" -> PlaidServiceError.ApiError(errorMessage, errorCode)
            "ITEM_ERROR" -> PlaidServiceError.ItemError(plaidError)
            else -> PlaidServiceError.UnknownError(errorMessage)
        }
    }
}

enum class PlaidEnvironment {
    SANDBOX,
    DEVELOPMENT,
    PRODUCTION
}