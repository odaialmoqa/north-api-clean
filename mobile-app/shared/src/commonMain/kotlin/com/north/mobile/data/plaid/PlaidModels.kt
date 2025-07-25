package com.north.mobile.data.plaid

import com.north.mobile.domain.model.AccountType
import com.north.mobile.domain.model.Currency
import com.north.mobile.domain.model.Money
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class PlaidLinkToken(
    val linkToken: String,
    val expiration: Instant,
    val requestId: String
)

@Serializable
data class PlaidPublicToken(
    val publicToken: String,
    val institutionId: String,
    val institutionName: String,
    val accounts: List<PlaidAccountMetadata>
)

@Serializable
data class PlaidAccessToken(
    val accessToken: String,
    val itemId: String,
    val institutionId: String,
    val institutionName: String
)

@Serializable
data class PlaidAccountMetadata(
    val accountId: String,
    val name: String,
    val mask: String?,
    val type: String,
    val subtype: String?,
    val verificationStatus: String?
)

@Serializable
data class PlaidAccount(
    val accountId: String,
    val itemId: String,
    val name: String,
    val officialName: String?,
    val type: PlaidAccountType,
    val subtype: PlaidAccountSubtype?,
    val mask: String?,
    val balances: PlaidBalances,
    val verificationStatus: PlaidVerificationStatus?
) {
    fun toAccount(institutionId: String, institutionName: String): com.north.mobile.domain.model.Account {
        return com.north.mobile.domain.model.Account(
            id = accountId,
            institutionId = institutionId,
            institutionName = institutionName,
            accountType = type.toAccountType(),
            balance = Money.fromDollars(balances.current ?: 0.0),
            availableBalance = balances.available?.let { Money.fromDollars(it) },
            currency = Currency.CAD,
            lastUpdated = kotlinx.datetime.Clock.System.now(),
            accountNumber = mask,
            nickname = if (name != officialName) name else null
        )
    }
}

@Serializable
data class PlaidBalances(
    val available: Double?,
    val current: Double?,
    val limit: Double?,
    val isoCurrencyCode: String?
)

@Serializable
enum class PlaidAccountType {
    DEPOSITORY,
    CREDIT,
    LOAN,
    INVESTMENT,
    OTHER;
    
    fun toAccountType(): AccountType {
        return when (this) {
            DEPOSITORY -> AccountType.CHECKING // Will be refined by subtype
            CREDIT -> AccountType.CREDIT_CARD
            LOAN -> AccountType.LOAN
            INVESTMENT -> AccountType.INVESTMENT
            OTHER -> AccountType.CHECKING
        }
    }
}

@Serializable
enum class PlaidAccountSubtype {
    CHECKING,
    SAVINGS,
    MONEY_MARKET,
    CD,
    CREDIT_CARD,
    PAYPAL,
    PREPAID,
    AUTO,
    BUSINESS,
    COMMERCIAL,
    CONSTRUCTION,
    CONSUMER,
    HOME_EQUITY,
    LINE_OF_CREDIT,
    LOAN,
    MORTGAGE,
    OVERDRAFT,
    STUDENT;
    
    fun toAccountType(): AccountType {
        return when (this) {
            CHECKING -> AccountType.CHECKING
            SAVINGS, MONEY_MARKET, CD -> AccountType.SAVINGS
            CREDIT_CARD, PAYPAL, PREPAID -> AccountType.CREDIT_CARD
            AUTO, BUSINESS, COMMERCIAL, CONSTRUCTION, CONSUMER, 
            HOME_EQUITY, LINE_OF_CREDIT, LOAN, OVERDRAFT, STUDENT -> AccountType.LOAN
            MORTGAGE -> AccountType.MORTGAGE
        }
    }
}

@Serializable
enum class PlaidVerificationStatus {
    PENDING_AUTOMATIC_VERIFICATION,
    PENDING_MANUAL_VERIFICATION,
    MANUALLY_VERIFIED,
    VERIFICATION_EXPIRED,
    VERIFICATION_FAILED,
    DATABASE_MATCHED,
    DATABASE_INSIGHTS_PASS,
    DATABASE_INSIGHTS_PASS_WITH_CAUTION,
    DATABASE_INSIGHTS_FAIL
}

@Serializable
data class PlaidError(
    val errorType: String,
    val errorCode: String,
    val errorMessage: String,
    val displayMessage: String?,
    val requestId: String?
)

@Serializable
data class PlaidItem(
    val itemId: String,
    val institutionId: String,
    val webhook: String?,
    val error: PlaidError?,
    val availableProducts: List<String>,
    val billedProducts: List<String>,
    val consentExpirationTime: Instant?,
    val updateType: String?
)

sealed class PlaidLinkResult {
    data class Success(
        val publicToken: String,
        val metadata: PlaidLinkMetadata
    ) : PlaidLinkResult()
    
    data class Exit(
        val error: PlaidError?,
        val metadata: PlaidLinkMetadata?
    ) : PlaidLinkResult()
}

@Serializable
data class PlaidLinkMetadata(
    val institution: PlaidInstitutionMetadata?,
    val accounts: List<PlaidAccountMetadata>,
    val linkSessionId: String,
    val metadataJson: String?
)

@Serializable
data class PlaidInstitutionMetadata(
    val institutionId: String,
    val name: String
)

sealed class AccountLinkingStatus {
    object NotStarted : AccountLinkingStatus()
    object InProgress : AccountLinkingStatus()
    data class Connected(val itemId: String, val accountCount: Int) : AccountLinkingStatus()
    data class Failed(val error: PlaidError) : AccountLinkingStatus()
    data class RequiresReauth(val itemId: String) : AccountLinkingStatus()
    object Disconnected : AccountLinkingStatus()
}

interface PlaidLinkHandler {
    suspend fun openLink(linkToken: String): Result<PlaidLinkResult>
    fun destroy()
}