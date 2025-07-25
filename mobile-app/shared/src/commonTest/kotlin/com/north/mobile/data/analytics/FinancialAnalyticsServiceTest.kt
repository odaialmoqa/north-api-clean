package com.north.mobile.data.analytics

import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class FinancialAnalyticsServiceTest {
    
    private lateinit var analyticsService: FinancialAnalyticsService
    private lateinit var mockTransactionRepository: MockTransactionRepository
    private lateinit var mockAccountRepository: MockAccountRepository
    private lateinit var mockUserRepository: MockUserRepository
    
    @BeforeTest
    fun setup() {
        mockTransactionRepository = MockTransactionRepository()
        mockAccountRepository = MockAccountRepository()
        mockUserRepository = MockUserRepository()
        
        analyticsService = FinancialAnalyticsServiceImpl(
            mockTransactionRepository,
            mockAccountRepository,
            mockUserRepository
        )
    }
    
    @Test
    fun `generateSpendingAnalysis should calculate correct totals`() = runTest {
        // Given
        val userId = "user123"
        val period = DateRange(
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(-100.0), Category.GROCERIES),
            createTransaction("2", Money.fromDollars(-50.0), Category.GAS),
            createTransaction("3", Money.fromDollars(2000.0), Category.SALARY)
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        // When
        val result = analyticsService.generateSpendingAnalysis(userId, period)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        
        assertEquals(Money.fromDollars(150.0), analysis.totalSpent)
        assertEquals(Money.fromDollars(2000.0), analysis.totalIncome)
        assertEquals(Money.fromDollars(1850.0), analysis.netCashFlow)
        assertEquals(2, analysis.categoryBreakdown.size)
    }
    
    @Test
    fun `calculateNetWorth should handle assets and liabilities correctly`() = runTest {
        // Given
        val userId = "user123"
        val accounts = listOf(
            createAccount("1", AccountType.CHECKING, Money.fromDollars(1000.0)),
            createAccount("2", AccountType.SAVINGS, Money.fromDollars(5000.0)),
            createAccount("3", AccountType.CREDIT_CARD, Money.fromDollars(-2000.0))
        )
        
        mockAccountRepository.setAccounts(accounts)
        
        // When
        val result = analyticsService.calculateNetWorth(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val netWorth = result.getOrThrow()
        
        assertEquals(Money.fromDollars(6000.0), netWorth.totalAssets)
        assertEquals(Money.fromDollars(2000.0), netWorth.totalLiabilities)
        assertEquals(Money.fromDollars(4000.0), netWorth.netWorth)
    }
    
    @Test
    fun `analyzeBudgetPerformance should identify overspending`() = runTest {
        // Given
        val userId = "user123"
        val period = DateRange(
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(-500.0), Category.GROCERIES),
            createTransaction("2", Money.fromDollars(-300.0), Category.RESTAURANTS)
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        // When
        val result = analyticsService.analyzeBudgetPerformance(userId, period)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        
        assertEquals(Money.fromDollars(800.0), analysis.totalSpent)
        assertTrue(analysis.categoryComparisons.isNotEmpty())
    }
    
    @Test
    fun `analyzeCanadianTaxes should calculate correct tax breakdown`() = runTest {
        // Given
        val userId = "user123"
        val user = createUser(userId)
        mockUserRepository.setUser(user)
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(5000.0), Category.SALARY),
            createTransaction("2", Money.fromDollars(5000.0), Category.SALARY)
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        // When
        val result = analyticsService.analyzeCanadianTaxes(userId, 2024)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        
        assertEquals(Money.fromDollars(10000.0), analysis.grossIncome)
        assertTrue(analysis.estimatedTaxes.totalTax.isPositive)
        assertTrue(analysis.rrspContributions.contributionRoom.isPositive)
    }
    
    @Test
    fun `generatePersonalizedRecommendations should create relevant recommendations`() = runTest {
        // Given
        val userId = "user123"
        val user = createUser(userId)
        mockUserRepository.setUser(user)
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(-500.0), Category.RESTAURANTS), // High restaurant spending
            createTransaction("2", Money.fromDollars(3000.0), Category.SALARY)
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        val accounts = listOf(
            createAccount("1", AccountType.CHECKING, Money.fromDollars(1000.0))
        )
        
        mockAccountRepository.setAccounts(accounts)
        
        // When
        val result = analyticsService.generatePersonalizedRecommendations(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.type == RecommendationType.SPENDING_REDUCTION })
    }
    
    @Test
    fun `calculateRRSPRecommendations should provide accurate analysis`() = runTest {
        // Given
        val userId = "user123"
        val user = createUser(userId)
        mockUserRepository.setUser(user)
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(5000.0), Category.SALARY)
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        // When
        val result = analyticsService.calculateRRSPRecommendations(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        
        assertTrue(analysis.maxContribution.isPositive)
        assertTrue(analysis.contributionRoom.isPositive)
        assertTrue(analysis.recommendedContribution.isPositive)
    }
    
    @Test
    fun `generateSpendingInsights should identify high spending categories`() = runTest {
        // Given
        val userId = "user123"
        val period = DateRange(
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        
        val transactions = listOf(
            createTransaction("1", Money.fromDollars(-800.0), Category.RESTAURANTS), // 80% of spending
            createTransaction("2", Money.fromDollars(-200.0), Category.GROCERIES)    // 20% of spending
        )
        
        mockTransactionRepository.setTransactions(transactions)
        
        // When
        val result = analyticsService.generateSpendingAnalysis(userId, period)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        
        assertTrue(analysis.insights.isNotEmpty())
        assertTrue(analysis.insights.any { it.type == InsightType.SPENDING_PATTERN })
        
        val topCategoryInsight = analysis.insights.find { 
            it.title.contains("High spending") && it.category?.id == Category.RESTAURANTS.id 
        }
        assertNotNull(topCategoryInsight)
    }
    
    // Helper methods
    
    private fun createTransaction(
        id: String,
        amount: Money,
        category: Category,
        date: LocalDate = LocalDate(2024, 1, 15)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Test transaction",
            category = category,
            date = date,
            merchantName = "Test Merchant"
        )
    }
    
    private fun createAccount(
        id: String,
        type: AccountType,
        balance: Money
    ): Account {
        return Account(
            id = id,
            institutionId = "inst1",
            institutionName = "Test Bank",
            accountType = type,
            balance = balance,
            lastUpdated = Clock.System.now()
        )
    }
    
    private fun createUser(id: String): User {
        return User(
            id = id,
            email = "test@example.com",
            profile = UserProfile(
                firstName = "Test",
                lastName = "User"
            ),
            preferences = UserPreferences(),
            gamificationData = GamificationProfile(
                level = 1,
                totalPoints = 0,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = Clock.System.now()
            )
        )
    }
}

// Mock repositories for testing

class MockTransactionRepository : TransactionRepository {
    private var transactions = emptyList<Transaction>()
    
    fun setTransactions(transactions: List<Transaction>) {
        this.transactions = transactions
    }
    
    override suspend fun findByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> {
        return Result.success(transactions.filter { it.date >= startDate && it.date <= endDate })
    }
    
    override suspend fun insert(entity: Transaction): Result<Transaction> = Result.success(entity)
    override suspend fun update(entity: Transaction): Result<Transaction> = Result.success(entity)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun findById(id: String): Result<Transaction?> = Result.success(transactions.find { it.id == id })
    override suspend fun findAll(): Result<List<Transaction>> = Result.success(transactions)
    override suspend fun findByAccountId(accountId: String): Result<List<Transaction>> = Result.success(transactions.filter { it.accountId == accountId })
    override suspend fun findByAccountAndDateRange(accountId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> = Result.success(transactions.filter { it.accountId == accountId && it.date >= startDate && it.date <= endDate })
    override suspend fun findByCategory(categoryId: String): Result<List<Transaction>> = Result.success(transactions.filter { it.category.id == categoryId })
    override suspend fun updateCategory(transactionId: String, category: Category): Result<Unit> = Result.success(Unit)
    override suspend fun markAsRecurring(transactionId: String, isRecurring: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun findDuplicates(transaction: Transaction): Result<List<Transaction>> = Result.success(emptyList())
}

class MockAccountRepository : AccountRepository {
    private var accounts = emptyList<Account>()
    
    fun setAccounts(accounts: List<Account>) {
        this.accounts = accounts
    }
    
    override suspend fun findByUserId(userId: String): Result<List<Account>> = Result.success(accounts)
    
    override suspend fun insert(entity: Account): Result<Account> = Result.success(entity)
    override suspend fun update(entity: Account): Result<Account> = Result.success(entity)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun findById(id: String): Result<Account?> = Result.success(accounts.find { it.id == id })
    override suspend fun findAll(): Result<List<Account>> = Result.success(accounts)
    override suspend fun findByInstitution(userId: String, institutionId: String): Result<List<Account>> = Result.success(accounts.filter { it.institutionId == institutionId })
    override suspend fun updateBalance(accountId: String, balance: Money): Result<Unit> = Result.success(Unit)
    override suspend fun deactivateAccount(accountId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveAccount(account: Account): Result<Account> = Result.success(account)
    override suspend fun updateAccount(account: Account): Result<Account> = Result.success(account)
    override suspend fun getAllAccounts(): List<Account> = accounts
    override suspend fun getAccountById(accountId: String): Account? = accounts.find { it.id == accountId }
}

class MockUserRepository : UserRepository {
    private var user: User? = null
    
    fun setUser(user: User) {
        this.user = user
    }
    
    override suspend fun findById(id: String): Result<User?> = Result.success(user)
    
    override suspend fun insert(entity: User): Result<User> = Result.success(entity)
    override suspend fun update(entity: User): Result<User> = Result.success(entity)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun findAll(): Result<List<User>> = Result.success(listOfNotNull(user))
}