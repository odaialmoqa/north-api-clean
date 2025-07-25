import com.north.mobile.data.plaid.*
import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.domain.model.*
import com.north.mobile.ui.accounts.AccountLinkingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountLinkingTest {
    
    @Test
    fun testAccountLinkingFlow() = runTest {
        // Create mock dependencies
        val mockPlaidService = MockPlaidService()
        val mockAccountRepository = MockAccountRepository()
        val mockEncryptionManager = MockEncryptionManager()
        val mockPlaidLinkHandler = MockPlaidLinkHandler(shouldSucceed = true)
        
        // Create account linking manager
        val accountLinkingManager = AccountLinkingManagerImpl(
            plaidService = mockPlaidService,
            accountRepository = mockAccountRepository,
            secureStorage = mockEncryptionManager
        )
        
        // Create view model
        val viewModel = AccountLinkingViewModel(
            accountLinkingManager = accountLinkingManager,
            plaidLinkHandler = mockPlaidLinkHandler,
            userId = "test_user_123"
        )
        
        // Test initial state
        val initialState = viewModel.state.first()
        assertEquals(com.north.mobile.ui.accounts.model.AccountLinkingState.Step.INSTITUTION_SELECTION, initialState.currentStep)
        assertTrue(initialState.availableInstitutions.isNotEmpty())
        
        // Test institution selection
        val rbc = CanadianInstitutions.RBC
        viewModel.handleEvent(com.north.mobile.ui.accounts.model.AccountLinkingEvent.SelectInstitution(rbc))
        
        val afterSelection = viewModel.state.first()
        assertEquals(com.north.mobile.ui.accounts.model.AccountLinkingState.Step.SECURITY_EXPLANATION, afterSelection.currentStep)
        assertEquals(rbc, afterSelection.selectedInstitution)
        
        // Test starting secure connection
        viewModel.handleEvent(com.north.mobile.ui.accounts.model.AccountLinkingEvent.StartSecureConnection)
        
        // Wait for async operations to complete
        kotlinx.coroutines.delay(1500)
        
        val finalState = viewModel.state.first()
        assertEquals(com.north.mobile.ui.accounts.model.AccountLinkingState.Step.SUCCESS, finalState.currentStep)
        assertTrue(finalState.linkedAccounts.isNotEmpty())
    }
    
    @Test
    fun testAccountLinkingCancellation() = runTest {
        val mockPlaidService = MockPlaidService()
        val mockAccountRepository = MockAccountRepository()
        val mockEncryptionManager = MockEncryptionManager()
        val mockPlaidLinkHandler = MockPlaidLinkHandler(simulateUserCancellation = true)
        
        val accountLinkingManager = AccountLinkingManagerImpl(
            plaidService = mockPlaidService,
            accountRepository = mockAccountRepository,
            secureStorage = mockEncryptionManager
        )
        
        val viewModel = AccountLinkingViewModel(
            accountLinkingManager = accountLinkingManager,
            plaidLinkHandler = mockPlaidLinkHandler,
            userId = "test_user_123"
        )
        
        // Select institution and start connection
        viewModel.handleEvent(com.north.mobile.ui.accounts.model.AccountLinkingEvent.SelectInstitution(CanadianInstitutions.RBC))
        viewModel.handleEvent(com.north.mobile.ui.accounts.model.AccountLinkingEvent.StartSecureConnection)
        
        // Wait for async operations
        kotlinx.coroutines.delay(1500)
        
        val finalState = viewModel.state.first()
        assertEquals(com.north.mobile.ui.accounts.model.AccountLinkingState.Step.ERROR, finalState.currentStep)
        assertNotNull(finalState.error)
    }
}

// Mock implementations for testing
class MockPlaidService : PlaidService {
    override suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> {
        return Result.success(
            PlaidLinkToken(
                linkToken = "link-sandbox-test-token",
                expiration = kotlinx.datetime.Clock.System.now().plus(kotlinx.datetime.DateTimePeriod(hours = 1)),
                requestId = "mock_request_id"
            )
        )
    }
    
    override suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> {
        return Result.success(
            PlaidAccessToken(
                accessToken = "access-sandbox-test-token",
                itemId = "mock_item_id",
                institutionId = "ins_3",
                institutionName = "RBC Royal Bank"
            )
        )
    }
    
    override suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>> {
        return Result.success(
            listOf(
                PlaidAccount(
                    accountId = "mock_account_1",
                    itemId = "mock_item_id",
                    name = "Chequing Account",
                    officialName = "RBC Chequing Account",
                    type = PlaidAccountType.DEPOSITORY,
                    subtype = PlaidAccountSubtype.CHECKING,
                    mask = "0000",
                    balances = PlaidBalances(
                        available = 2450.0,
                        current = 2450.0,
                        limit = null,
                        isoCurrencyCode = "CAD"
                    ),
                    verificationStatus = PlaidVerificationStatus.DATABASE_MATCHED
                ),
                PlaidAccount(
                    accountId = "mock_account_2",
                    itemId = "mock_item_id",
                    name = "Savings Account",
                    officialName = "RBC Savings Account",
                    type = PlaidAccountType.DEPOSITORY,
                    subtype = PlaidAccountSubtype.SAVINGS,
                    mask = "1111",
                    balances = PlaidBalances(
                        available = 15800.0,
                        current = 15800.0,
                        limit = null,
                        isoCurrencyCode = "CAD"
                    ),
                    verificationStatus = PlaidVerificationStatus.DATABASE_MATCHED
                )
            )
        )
    }
    
    override suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>> = getAccounts(accessToken)
    
    override suspend fun getTransactions(
        accessToken: String,
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate,
        accountIds: List<String>?
    ): Result<List<Transaction>> = Result.success(emptyList())
    
    override suspend fun getItem(accessToken: String): Result<PlaidItem> {
        return Result.success(
            PlaidItem(
                itemId = "mock_item_id",
                institutionId = "ins_3",
                webhook = null,
                error = null,
                availableProducts = listOf("transactions", "auth"),
                billedProducts = listOf("transactions"),
                consentExpirationTime = null,
                updateType = null
            )
        )
    }
    
    override suspend fun removeItem(accessToken: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken> = createLinkToken("test_user")
    
    override suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>> {
        return Result.success(CanadianInstitutions.MAJOR_CANADIAN_BANKS)
    }
    
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> {
        return Result.success(CanadianInstitutions.searchByName(query))
    }
}

class MockAccountRepository : AccountRepository {
    private val accounts = mutableListOf<Account>()
    
    override suspend fun insert(entity: Account): Result<Account> {
        accounts.add(entity)
        return Result.success(entity)
    }
    
    override suspend fun update(entity: Account): Result<Account> {
        val index = accounts.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            accounts[index] = entity
            return Result.success(entity)
        }
        return Result.failure(Exception("Account not found"))
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        accounts.removeAll { it.id == id }
        return Result.success(Unit)
    }
    
    override suspend fun findById(id: String): Result<Account?> {
        return Result.success(accounts.find { it.id == id })
    }
    
    override suspend fun findAll(): Result<List<Account>> = Result.success(accounts.toList())
    
    override suspend fun findByUserId(userId: String): Result<List<Account>> = Result.success(accounts.toList())
    
    override suspend fun findByInstitution(userId: String, institutionId: String): Result<List<Account>> {
        return Result.success(accounts.filter { it.institutionId == institutionId })
    }
    
    override suspend fun updateBalance(accountId: String, balance: Money): Result<Unit> {
        val account = accounts.find { it.id == accountId }
        if (account != null) {
            val updated = account.copy(balance = balance)
            return update(updated).map { Unit }
        }
        return Result.failure(Exception("Account not found"))
    }
    
    override suspend fun deactivateAccount(accountId: String): Result<Unit> {
        val account = accounts.find { it.id == accountId }
        if (account != null) {
            val updated = account.copy(isActive = false)
            return update(updated).map { Unit }
        }
        return Result.failure(Exception("Account not found"))
    }
    
    override suspend fun saveAccount(account: Account): Result<Account> = insert(account)
    
    override suspend fun updateAccount(account: Account): Result<Account> = update(account)
    
    override suspend fun getAllAccounts(): List<Account> = accounts.toList()
    
    override suspend fun getAccountById(accountId: String): Account? = accounts.find { it.id == accountId }
    
    override suspend fun createAccount(account: Account): Result<Account> = insert(account)
    
    override suspend fun getAccount(accountId: String): Result<Account?> = findById(accountId)
    
    override suspend fun getUserAccounts(userId: String): Result<List<Account>> = findByUserId(userId)
    
    override suspend fun deleteAccount(accountId: String): Result<Unit> = delete(accountId)
    
    override suspend fun getAccountsByInstitution(institutionId: String): Result<List<Account>> {
        return Result.success(accounts.filter { it.institutionId == institutionId })
    }
    
    override suspend fun updateAccountBalance(accountId: String, balance: Money): Result<Unit> = updateBalance(accountId, balance)
    
    override suspend fun getActiveAccounts(userId: String): Result<List<Account>> {
        return Result.success(accounts.filter { it.isActive })
    }
    
    override suspend fun syncAccountData(accountId: String): Result<Account> = findById(accountId).mapCatching { 
        it ?: throw Exception("Account not found") 
    }
}

class MockEncryptionManager : EncryptionManager {
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun generateDatabaseKey(): Result<String> = Result.success("mock_db_key")
    
    override suspend fun getDatabaseKey(): Result<String> = Result.success("mock_db_key")
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<com.north.mobile.data.security.EncryptedData> {
        return Result.success(
            com.north.mobile.data.security.EncryptedData(
                encryptedContent = data.toByteArray(), // Not actually encrypted in mock
                iv = ByteArray(16),
                keyAlias = keyAlias
            )
        )
    }
    
    override suspend fun decrypt(encryptedData: com.north.mobile.data.security.EncryptedData, keyAlias: String): Result<String> {
        return Result.success(String(encryptedData.encryptedContent)) // Not actually decrypted in mock
    }
    
    override fun isEncryptionAvailable(): Boolean = true
    
    override suspend fun clearKeys(): Result<Unit> = Result.success(Unit)
}