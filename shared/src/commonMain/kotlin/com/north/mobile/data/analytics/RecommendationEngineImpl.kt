package com.north.mobile.data.analytics

import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Implementation of the recommendation engine for automated financial planning
 */
class RecommendationEngineImpl(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val analyticsService: FinancialAnalyticsService
) : RecommendationEngine {
    
    override suspend fun generateFinancialPlanningRecommendations(
        userId: String,
        userProfile: UserFinancialProfile
    ): Result<List<FinancialPlanningRecommendation>> {
        return try {
            val recommendations = mutableListOf<FinancialPlanningRecommendation>()
            
            // Generate tax optimization recommendations
            val taxRecommendations = generateTaxOptimizationRecommendations(userProfile)
            recommendations.addAll(taxRecommendations)
            
            // Generate debt reduction recommendations
            val debtRecommendations = generateDebtReductionRecommendations(userProfile)
            recommendations.addAll(debtRecommendations)
            
            // Generate savings optimization recommendations
            val savingsRecommendations = generateSavingsOptimizationRecommendations(userProfile)
            recommendations.addAll(savingsRecommendations)
            
            // Generate emergency fund recommendations
            val emergencyFundRecommendations = generateEmergencyFundRecommendations(userProfile)
            recommendations.addAll(emergencyFundRecommendations)
            
            // Generate goal acceleration recommendations
            val goalRecommendations = generateGoalAccelerationRecommendations(userProfile)
            recommendations.addAll(goalRecommendations)
            
            // Generate cash flow optimization recommendations
            val cashFlowRecommendations = generateCashFlowOptimizationRecommendations(userProfile)
            recommendations.addAll(cashFlowRecommendations)
            
            // Sort by priority and expected impact
            val sortedRecommendations = recommendations.sortedWith(
                compareByDescending<FinancialPlanningRecommendation> { it.priority.ordinal }
                    .thenByDescending { it.expectedImpact.financialImpact.amount }
            )
            
            Result.success(sortedRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun optimizeRRSPContributions(
        userProfile: UserFinancialProfile
    ): Result<RRSPOptimizationRecommendation> {
        return try {
            val currentContribution = userProfile.currentRRSPContributions
            val availableRoom = userProfile.rrspRoom
            val grossIncome = userProfile.grossAnnualIncome
            val marginalTaxRate = userProfile.marginalTaxRate
            
            // Calculate optimal contribution based on tax savings and cash flow
            val maxAffordableContribution = calculateMaxAffordableRRSPContribution(userProfile)
            val recommendedContribution = minOf(availableRoom, maxAffordableContribution)
            
            val taxSavings = recommendedContribution * marginalTaxRate
            
            // Determine optimal timing
            val optimalTiming = determineOptimalRRSPTiming(userProfile)
            
            // Calculate impact on goals
            val goalImpacts = calculateRRSPGoalImpacts(userProfile, recommendedContribution)
            
            // Generate alternative strategies
            val alternatives = generateRRSPAlternatives(userProfile, recommendedContribution)
            
            val reasoning = buildRRSPReasoning(
                userProfile, recommendedContribution, taxSavings, optimalTiming
            )
            
            val recommendation = RRSPOptimizationRecommendation(
                recommendedContribution = recommendedContribution,
                currentContribution = currentContribution,
                availableRoom = availableRoom,
                taxSavings = taxSavings,
                optimalTiming = optimalTiming,
                reasoning = reasoning,
                impactOnGoals = goalImpacts,
                alternativeStrategies = alternatives
            )
            
            Result.success(recommendation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    } 
   
    override suspend fun optimizeTFSAContributions(
        userProfile: UserFinancialProfile
    ): Result<TFSAOptimizationRecommendation> {
        return try {
            val currentContribution = userProfile.currentTFSAContributions
            val availableRoom = userProfile.tfsaRoom
            
            // Calculate optimal contribution based on goals and cash flow
            val maxAffordableContribution = calculateMaxAffordableTFSAContribution(userProfile)
            val recommendedContribution = minOf(availableRoom, maxAffordableContribution)
            
            // Determine optimal allocation within TFSA
            val optimalAllocation = determineOptimalTFSAAllocation(userProfile)
            
            // Calculate impact on goals
            val goalImpacts = calculateTFSAGoalImpacts(userProfile, recommendedContribution)
            
            // Generate alternative strategies
            val alternatives = generateTFSAAlternatives(userProfile, recommendedContribution)
            
            val reasoning = buildTFSAReasoning(
                userProfile, recommendedContribution, optimalAllocation
            )
            
            val recommendation = TFSAOptimizationRecommendation(
                recommendedContribution = recommendedContribution,
                currentContribution = currentContribution,
                availableRoom = availableRoom,
                optimalAllocation = optimalAllocation,
                reasoning = reasoning,
                impactOnGoals = goalImpacts,
                alternativeStrategies = alternatives
            )
            
            Result.success(recommendation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun optimizeDebtPayoff(
        userProfile: UserFinancialProfile
    ): Result<DebtPayoffStrategy> {
        return try {
            val debtAccounts = userProfile.accounts.filter { it.isDebt && it.balance.amount < 0 }
            
            if (debtAccounts.isEmpty()) {
                return Result.success(
                    DebtPayoffStrategy(
                        strategy = DebtPayoffMethod.MINIMUM_ONLY,
                        payoffOrder = emptyList(),
                        totalInterestSaved = Money.zero(),
                        payoffTimeframe = 0,
                        monthlyPaymentPlan = Money.zero(),
                        reasoning = "No debt to optimize",
                        alternativeStrategies = emptyList()
                    )
                )
            }
            
            // Calculate available funds for debt payoff
            val availableFunds = calculateAvailableDebtPayoffFunds(userProfile)
            
            // Analyze different debt payoff strategies
            val avalancheStrategy = calculateAvalancheStrategy(debtAccounts, availableFunds)
            val snowballStrategy = calculateSnowballStrategy(debtAccounts, availableFunds)
            val hybridStrategy = calculateHybridStrategy(debtAccounts, availableFunds)
            
            // Choose optimal strategy based on user profile
            val optimalStrategy = chooseOptimalDebtStrategy(
                userProfile, avalancheStrategy, snowballStrategy, hybridStrategy
            )
            
            // Generate alternative strategies
            val alternatives = listOf(avalancheStrategy, snowballStrategy, hybridStrategy)
                .filter { it.strategy != optimalStrategy.strategy }
                .map { strategy ->
                    AlternativeDebtStrategy(
                        method = strategy.strategy,
                        description = getDebtStrategyDescription(strategy.strategy),
                        totalInterestSaved = strategy.totalInterestSaved,
                        payoffTimeframe = strategy.payoffTimeframe,
                        pros = getDebtStrategyPros(strategy.strategy),
                        cons = getDebtStrategyCons(strategy.strategy)
                    )
                }
            
            Result.success(optimalStrategy.copy(alternativeStrategies = alternatives))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun optimizeSavingsStrategy(
        userProfile: UserFinancialProfile
    ): Result<SavingsOptimizationRecommendation> {
        return try {
            val currentSavingsRate = calculateCurrentSavingsRate(userProfile)
            val recommendedSavingsRate = calculateOptimalSavingsRate(userProfile)
            
            // Calculate emergency fund requirements
            val monthlyExpenses = calculateMonthlyExpenses(userProfile)
            val emergencyFundTarget = monthlyExpenses * 6.0 // 6 months of expenses
            val currentEmergencyFund = calculateCurrentEmergencyFund(userProfile)
            
            // Determine optimal savings allocation
            val optimalAllocation = determineOptimalSavingsAllocation(userProfile)
            
            // Calculate impact on goals
            val goalImpacts = calculateSavingsGoalImpacts(userProfile, recommendedSavingsRate)
            
            val reasoning = buildSavingsReasoning(
                userProfile, recommendedSavingsRate, emergencyFundTarget, optimalAllocation
            )
            
            val recommendation = SavingsOptimizationRecommendation(
                recommendedSavingsRate = recommendedSavingsRate,
                currentSavingsRate = currentSavingsRate,
                optimalAllocation = optimalAllocation,
                emergencyFundTarget = emergencyFundTarget,
                currentEmergencyFund = currentEmergencyFund,
                reasoning = reasoning,
                impactOnGoals = goalImpacts
            )
            
            Result.success(recommendation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    } 
   
    override suspend fun trackRecommendationEffectiveness(
        recommendationId: String,
        outcome: RecommendationOutcome
    ): Result<Unit> {
        return try {
            // In a real implementation, this would store the outcome in a database
            // and update recommendation algorithms based on effectiveness
            
            // Update recommendation completion status
            updateRecommendationStatus(recommendationId, outcome.action)
            
            // Track metrics for algorithm improvement
            trackRecommendationMetrics(outcome)
            
            // Update user's financial profile if needed
            if (outcome.actualImpact != null) {
                updateUserFinancialImpact(outcome)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecommendationExplanation(
        recommendationId: String
    ): Result<RecommendationExplanation> {
        return try {
            // In a real implementation, this would retrieve the stored explanation
            // For now, we'll generate a sample explanation
            
            val explanation = RecommendationExplanation(
                recommendationId = recommendationId,
                summary = "This recommendation is based on your current financial situation and Canadian tax optimization opportunities.",
                detailedReasoning = "Based on your income level, tax bracket, and available contribution room, this recommendation maximizes your tax savings while maintaining adequate cash flow.",
                assumptions = listOf(
                    "Your income remains stable",
                    "Tax rates remain unchanged",
                    "You maintain current spending patterns"
                ),
                calculations = listOf(
                    CalculationStep(
                        step = 1,
                        description = "Calculate tax savings",
                        formula = "Contribution × Marginal Tax Rate",
                        inputs = mapOf("Contribution" to "$5,000", "Tax Rate" to "30%"),
                        result = "$1,500 tax savings"
                    )
                ),
                sources = listOf(
                    "Canada Revenue Agency contribution limits",
                    "Your transaction history",
                    "Current tax brackets"
                ),
                riskFactors = listOf(
                    "Tax law changes",
                    "Income fluctuations",
                    "Market volatility"
                ),
                alternativeApproaches = listOf(
                    "Contribute to TFSA instead",
                    "Increase emergency fund first",
                    "Pay down high-interest debt"
                )
            )
            
            Result.success(explanation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Private helper methods for generating specific recommendation types
    
    private fun generateTaxOptimizationRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val recommendations = mutableListOf<FinancialPlanningRecommendation>()
        
        // RRSP contribution recommendation
        if (userProfile.rrspRoom.amount > 0) {
            val rrspRecommendation = createRRSPContributionRecommendation(userProfile)
            recommendations.add(rrspRecommendation)
        }
        
        // TFSA contribution recommendation
        if (userProfile.tfsaRoom.amount > 0) {
            val tfsaRecommendation = createTFSAContributionRecommendation(userProfile)
            recommendations.add(tfsaRecommendation)
        }
        
        return recommendations
    }
    
    private fun generateDebtReductionRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val debtAccounts = userProfile.accounts.filter { it.isDebt && it.balance.amount < 0 }
        
        if (debtAccounts.isEmpty()) return emptyList()
        
        val recommendations = mutableListOf<FinancialPlanningRecommendation>()
        
        // High-interest debt payoff recommendation (assume credit cards are high interest)
        val highInterestDebts = debtAccounts.filter { it.accountType == AccountType.CREDIT_CARD }
        if (highInterestDebts.isNotEmpty()) {
            val debtRecommendation = createHighInterestDebtRecommendation(userProfile, highInterestDebts)
            recommendations.add(debtRecommendation)
        }
        
        // Debt consolidation recommendation
        if (debtAccounts.size > 2) {
            val consolidationRecommendation = createDebtConsolidationRecommendation(userProfile, debtAccounts)
            recommendations.add(consolidationRecommendation)
        }
        
        return recommendations
    }
    
    private fun generateSavingsOptimizationRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val recommendations = mutableListOf<FinancialPlanningRecommendation>()
        
        // Savings rate optimization
        val currentSavingsRate = calculateCurrentSavingsRate(userProfile)
        val optimalSavingsRate = calculateOptimalSavingsRate(userProfile)
        
        if (optimalSavingsRate > currentSavingsRate + 0.02) { // 2% improvement threshold
            val savingsRecommendation = createSavingsRateRecommendation(userProfile, optimalSavingsRate)
            recommendations.add(savingsRecommendation)
        }
        
        return recommendations
    }
    
    private fun generateEmergencyFundRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val monthlyExpenses = calculateMonthlyExpenses(userProfile)
        val emergencyFundTarget = monthlyExpenses * 6.0
        val currentEmergencyFund = calculateCurrentEmergencyFund(userProfile)
        
        if (currentEmergencyFund < emergencyFundTarget) {
            val emergencyRecommendation = createEmergencyFundRecommendation(
                userProfile, emergencyFundTarget, currentEmergencyFund
            )
            return listOf(emergencyRecommendation)
        }
        
        return emptyList()
    }
    
    private fun generateGoalAccelerationRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val recommendations = mutableListOf<FinancialPlanningRecommendation>()
        
        for (goal in userProfile.goals) {
            if (goal.isOffTrack()) {
                val accelerationRecommendation = createGoalAccelerationRecommendation(userProfile, goal)
                recommendations.add(accelerationRecommendation)
            }
        }
        
        return recommendations
    }
    
    private fun generateCashFlowOptimizationRecommendations(
        userProfile: UserFinancialProfile
    ): List<FinancialPlanningRecommendation> {
        val recommendations = mutableListOf<FinancialPlanningRecommendation>()
        
        // Analyze spending patterns for optimization opportunities
        val highSpendingCategories = userProfile.spendingAnalysis.categoryBreakdown
            .filter { it.percentageOfTotal > 15.0 && it.trend == TrendDirection.INCREASING }
        
        for (category in highSpendingCategories) {
            val optimizationRecommendation = createSpendingOptimizationRecommendation(userProfile, category)
            recommendations.add(optimizationRecommendation)
        }
        
        return recommendations
    }    

    // Helper methods for calculations
    
    private fun calculateMaxAffordableRRSPContribution(userProfile: UserFinancialProfile): Money {
        val monthlyIncome = userProfile.grossAnnualIncome / 12.0
        val monthlyExpenses = calculateMonthlyExpenses(userProfile)
        val availableCashFlow = monthlyIncome - monthlyExpenses
        
        // Use 20% of available cash flow for RRSP contributions
        val monthlyContribution = availableCashFlow * 0.20
        return monthlyContribution * 12.0
    }
    
    private fun calculateMaxAffordableTFSAContribution(userProfile: UserFinancialProfile): Money {
        val monthlyIncome = userProfile.grossAnnualIncome / 12.0
        val monthlyExpenses = calculateMonthlyExpenses(userProfile)
        val availableCashFlow = monthlyIncome - monthlyExpenses
        
        // Use 15% of available cash flow for TFSA contributions
        val monthlyContribution = availableCashFlow * 0.15
        return monthlyContribution * 12.0
    }
    
    private fun calculateMonthlyExpenses(userProfile: UserFinancialProfile): Money {
        val spentTransactions = userProfile.transactions.filter { it.isDebit }
        val totalSpent = spentTransactions.fold(Money.zero()) { acc, transaction -> 
            acc + transaction.absoluteAmount 
        }
        
        // Assume transactions cover the analysis period, convert to monthly
        val analysisMonths = userProfile.spendingAnalysis.period.durationInMonths
        return totalSpent / analysisMonths
    }
    
    private fun calculateCurrentSavingsRate(userProfile: UserFinancialProfile): Double {
        val monthlyIncome = userProfile.grossAnnualIncome / 12.0
        val monthlyExpenses = calculateMonthlyExpenses(userProfile)
        val monthlySavings = monthlyIncome - monthlyExpenses
        
        return if (monthlyIncome.amount > 0) {
            monthlySavings.amount / monthlyIncome.amount
        } else 0.0
    }
    
    private fun calculateOptimalSavingsRate(userProfile: UserFinancialProfile): Double {
        // Base recommendation: 20% savings rate
        var optimalRate = 0.20
        
        // Adjust based on age (younger = higher savings rate)
        when {
            userProfile.age < 30 -> optimalRate += 0.05
            userProfile.age > 50 -> optimalRate -= 0.05
        }
        
        // Adjust based on debt levels
        val debtToIncomeRatio = calculateDebtToIncomeRatio(userProfile)
        if (debtToIncomeRatio > 0.3) {
            optimalRate -= 0.05 // Focus on debt reduction first
        }
        
        return maxOf(0.10, minOf(0.30, optimalRate)) // Cap between 10% and 30%
    }
    
    private fun calculateDebtToIncomeRatio(userProfile: UserFinancialProfile): Double {
        val totalDebt = userProfile.accounts
            .filter { it.isDebt }
            .fold(Money.zero()) { acc, account -> acc + account.balance.absoluteValue }
        
        return totalDebt.amount / userProfile.grossAnnualIncome.amount
    }
    
    private fun calculateCurrentEmergencyFund(userProfile: UserFinancialProfile): Money {
        return userProfile.accounts
            .filter { it.accountType == AccountType.SAVINGS && it.name.contains("emergency", ignoreCase = true) }
            .fold(Money.zero()) { acc, account -> acc + account.balance }
    }
    
    private fun determineOptimalRRSPTiming(userProfile: UserFinancialProfile): ContributionTiming {
        return ContributionTiming(
            frequency = ContributionFrequency.MONTHLY,
            optimalMonths = listOf(1, 2), // January and February for tax benefits
            reasoning = "Monthly contributions provide dollar-cost averaging benefits"
        )
    }
    
    private fun calculateRRSPGoalImpacts(userProfile: UserFinancialProfile, contribution: Money): List<GoalImpact> {
        return userProfile.goals.map { goal ->
            GoalImpact(
                goalId = goal.id,
                goalName = goal.title,
                impactType = GoalImpactType.NEUTRAL,
                timeImpact = 0,
                amountImpact = Money.zero()
            )
        }
    }
    
    private fun generateRRSPAlternatives(userProfile: UserFinancialProfile, recommendedContribution: Money): List<String> {
        return listOf(
            "Contribute to TFSA instead for tax-free growth",
            "Split contributions between RRSP and TFSA",
            "Delay contributions until higher income year"
        )
    }
    
    private fun buildRRSPReasoning(
        userProfile: UserFinancialProfile,
        recommendedContribution: Money,
        taxSavings: Money,
        timing: ContributionTiming
    ): String {
        return "Based on your ${userProfile.marginalTaxRate * 100}% marginal tax rate, contributing $${recommendedContribution.format()} to your RRSP will save you $${taxSavings.format()} in taxes this year."
    }
    
    private fun determineOptimalTFSAAllocation(userProfile: UserFinancialProfile): List<AllocationRecommendation> {
        return when (userProfile.riskTolerance) {
            RiskTolerance.CONSERVATIVE -> listOf(
                AllocationRecommendation(AssetClass.BONDS, 60.0, "Stable income", RiskLevel.LOW),
                AllocationRecommendation(AssetClass.CANADIAN_EQUITY, 40.0, "Growth potential", RiskLevel.MEDIUM)
            )
            RiskTolerance.MODERATE -> listOf(
                AllocationRecommendation(AssetClass.CANADIAN_EQUITY, 50.0, "Balanced growth", RiskLevel.MEDIUM),
                AllocationRecommendation(AssetClass.BONDS, 30.0, "Stability", RiskLevel.LOW),
                AllocationRecommendation(AssetClass.US_EQUITY, 20.0, "Diversification", RiskLevel.MEDIUM)
            )
            RiskTolerance.AGGRESSIVE -> listOf(
                AllocationRecommendation(AssetClass.CANADIAN_EQUITY, 40.0, "Growth", RiskLevel.HIGH),
                AllocationRecommendation(AssetClass.US_EQUITY, 30.0, "Growth", RiskLevel.HIGH),
                AllocationRecommendation(AssetClass.INTERNATIONAL_EQUITY, 20.0, "Diversification", RiskLevel.HIGH),
                AllocationRecommendation(AssetClass.BONDS, 10.0, "Stability", RiskLevel.LOW)
            )
        }
    }
    
    private fun calculateTFSAGoalImpacts(userProfile: UserFinancialProfile, contribution: Money): List<GoalImpact> {
        return userProfile.goals.map { goal ->
            GoalImpact(
                goalId = goal.id,
                goalName = goal.title,
                impactType = GoalImpactType.ACCELERATES,
                timeImpact = -30, // 30 days faster
                amountImpact = contribution * 0.05 // 5% growth assumption
            )
        }
    }
    
    private fun generateTFSAAlternatives(userProfile: UserFinancialProfile, recommendedContribution: Money): List<String> {
        return listOf(
            "Contribute to RRSP for immediate tax deduction",
            "Build emergency fund first",
            "Pay down high-interest debt"
        )
    }
    
    private fun buildTFSAReasoning(
        userProfile: UserFinancialProfile,
        recommendedContribution: Money,
        allocation: List<AllocationRecommendation>
    ): String {
        return "Contributing $${recommendedContribution.format()} to your TFSA provides tax-free growth and flexibility for your financial goals."
    }    
 
   // Debt optimization methods
    private fun calculateAvailableDebtPayoffFunds(userProfile: UserFinancialProfile): Money {
        val monthlyIncome = userProfile.grossAnnualIncome / 12.0
        val monthlyExpenses = calculateMonthlyExpenses(userProfile)
        val availableCashFlow = monthlyIncome - monthlyExpenses
        
        // Use 30% of available cash flow for additional debt payments
        return availableCashFlow * 0.30
    }
    
    private fun calculateAvalancheStrategy(debtAccounts: List<Account>, availableFunds: Money): DebtPayoffStrategy {
        // Sort by assumed interest rate (credit cards = 20%, loans = 8%, mortgages = 4%)
        val sortedDebts = debtAccounts.sortedByDescending { getAssumedInterestRate(it.accountType) }
        
        return DebtPayoffStrategy(
            strategy = DebtPayoffMethod.AVALANCHE,
            payoffOrder = sortedDebts.mapIndexed { index, account ->
                DebtPayoffPlan(
                    accountId = account.id,
                    accountName = account.displayName,
                    currentBalance = account.balance.absoluteValue,
                    interestRate = getAssumedInterestRate(account.accountType),
                    minimumPayment = getAssumedMinimumPayment(account),
                    recommendedPayment = if (index == 0) availableFunds else Money.zero(),
                    payoffOrder = index + 1,
                    estimatedPayoffMonths = calculatePayoffMonths(account, availableFunds)
                )
            },
            totalInterestSaved = calculateInterestSaved(sortedDebts, availableFunds),
            payoffTimeframe = calculateTotalPayoffTime(sortedDebts, availableFunds),
            monthlyPaymentPlan = availableFunds,
            reasoning = "Avalanche method saves the most money by paying off highest interest debts first",
            alternativeStrategies = emptyList()
        )
    }
    
    private fun calculateSnowballStrategy(debtAccounts: List<Account>, availableFunds: Money): DebtPayoffStrategy {
        val sortedDebts = debtAccounts.sortedBy { it.balance.amount }
        
        return DebtPayoffStrategy(
            strategy = DebtPayoffMethod.SNOWBALL,
            payoffOrder = sortedDebts.mapIndexed { index, account ->
                DebtPayoffPlan(
                    accountId = account.id,
                    accountName = account.displayName,
                    currentBalance = account.balance.absoluteValue,
                    interestRate = getAssumedInterestRate(account.accountType),
                    minimumPayment = getAssumedMinimumPayment(account),
                    recommendedPayment = if (index == 0) availableFunds else Money.zero(),
                    payoffOrder = index + 1,
                    estimatedPayoffMonths = calculatePayoffMonths(account, availableFunds)
                )
            },
            totalInterestSaved = calculateInterestSaved(sortedDebts, availableFunds),
            payoffTimeframe = calculateTotalPayoffTime(sortedDebts, availableFunds),
            monthlyPaymentPlan = availableFunds,
            reasoning = "Snowball method provides psychological wins by paying off smallest debts first",
            alternativeStrategies = emptyList()
        )
    }
    
    private fun calculateHybridStrategy(debtAccounts: List<Account>, availableFunds: Money): DebtPayoffStrategy {
        // Hybrid approach: pay off small debts under $1000 first, then switch to avalanche
        val smallDebts = debtAccounts.filter { it.balance.amount > -1000.0 }.sortedBy { it.balance.amount }
        val largeDebts = debtAccounts.filter { it.balance.amount <= -1000.0 }.sortedByDescending { getAssumedInterestRate(it.accountType) }
        val sortedDebts = smallDebts + largeDebts
        
        return DebtPayoffStrategy(
            strategy = DebtPayoffMethod.HYBRID,
            payoffOrder = sortedDebts.mapIndexed { index, account ->
                DebtPayoffPlan(
                    accountId = account.id,
                    accountName = account.displayName,
                    currentBalance = account.balance.absoluteValue,
    
    private fun calculateSnowballStrategy(debtAccounts: List<Account>, availableFunds: Money): DebtPayoffStrategy {
        val sortedDebts = debtAccounts.sortedBy { it.balance.amount }
        
        return DebtPayoffStrategy(
            strategy = DebtPayoffMethod.SNOWBALL,
            payoffOrder = sortedDebts.mapIndexed { index, account ->
                DebtPayoffPlan(
                    accountId = account.id,
                    accountName = account.name,
                    currentBalance = account.balance.absoluteValue,
                    interestRate = account.interestRate,
                    minimumPayment = account.minimumPayment ?: Money.fromDollars(50.0),
                    recommendedPayment = if (index == 0) availableFunds else Money.zero(),
                    payoffOrder = index + 1,
                    estimatedPayoffMonths = calculatePayoffMonths(account, availableFunds)
                )
            },
            totalInterestSaved = calculateInterestSaved(sortedDebts, availableFunds),
            payoffTimeframe = calculateTotalPayoffTime(sortedDebts, availableFunds),
            monthlyPaymentPlan = availableFunds,
            reasoning = "Snowball method provides psychological wins by paying off smallest debts first",
            alternativeStrategies = emptyList()
        )
    }
    
    private fun calculateHybridStrategy(debtAccounts: List<Account>, availableFunds: Money): DebtPayoffStrategy {
        // Hybrid approach: pay off small debts under $1000 first, then switch to avalanche
        val smallDebts = debtAccounts.filter { it.balance.amount < 1000.0 }.sortedBy { it.balance.amount }
        val largeDebts = debtAccounts.filter { it.balance.amount >= 1000.0 }.sortedByDescending { it.interestRate }
        val sortedDebts = smallDebts + largeDebts
        
        return DebtPayoffStrategy(
            strategy = DebtPayoffMethod.HYBRID,
            payoffOrder = sortedDebts.mapIndexed { index, account ->
                DebtPayoffPlan(
                    accountId = account.id,
                    accountName = account.name,
                    currentBalance = account.balance.absoluteValue,
                    interestRate = account.interestRate,
                    minimumPayment = account.minimumPayment ?: Money.fromDollars(50.0),
                    recommendedPayment = if (index == 0) availableFunds else Money.zero(),
                    payoffOrder = index + 1,
                    estimatedPayoffMonths = calculatePayoffMonths(account, availableFunds)
                )
            },
            totalInterestSaved = calculateInterestSaved(sortedDebts, availableFunds),
            payoffTimeframe = calculateTotalPayoffTime(sortedDebts, availableFunds),
            monthlyPaymentPlan = availableFunds,
            reasoning = "Hybrid method balances psychological wins with interest savings",
            alternativeStrategies = emptyList()
        )
    }
    
    private fun chooseOptimalDebtStrategy(
        userProfile: UserFinancialProfile,
        avalanche: DebtPayoffStrategy,
        snowball: DebtPayoffStrategy,
        hybrid: DebtPayoffStrategy
    ): DebtPayoffStrategy {
        // Choose based on user profile and debt characteristics
        val totalDebt = userProfile.accounts.filter { it.isDebt }.fold(Money.zero()) { acc, account -> 
            acc + account.balance.absoluteValue 
        }
        
        return when {
            totalDebt.amount > 50000.0 -> avalanche // High debt = focus on interest savings
            userProfile.age < 30 -> hybrid // Young = balance motivation and savings
            else -> snowball // Default to psychological wins
        }
    }
    
    private fun calculatePayoffMonths(account: Account, extraPayment: Money): Int {
        val balance = account.balance.absoluteValue.amount
        val rate = account.interestRate / 12.0 / 100.0 // Monthly rate
        val payment = (account.minimumPayment?.amount ?: 50.0) + extraPayment.amount
        
        if (rate == 0.0) return (balance / payment).toInt()
        
        return (-ln(1 - (balance * rate) / payment) / ln(1 + rate)).toInt()
    }
    
    private fun calculateInterestSaved(debts: List<Account>, extraPayment: Money): Money {
        // Simplified calculation - in reality would be more complex
        val totalInterest = debts.fold(0.0) { acc, debt ->
            acc + (debt.balance.amount * debt.interestRate / 100.0 * 2.0) // Assume 2 years average
        }
        
        val interestWithExtra = totalInterest * 0.7 // Assume 30% savings with extra payments
        return Money.fromDollars(totalInterest - interestWithExtra)
    }
    
    private fun calculateTotalPayoffTime(debts: List<Account>, extraPayment: Money): Int {
        return debts.maxOfOrNull { calculatePayoffMonths(it, extraPayment) } ?: 0
    }
    
    private fun getDebtStrategyDescription(method: DebtPayoffMethod): String {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> "Pay minimum on all debts, put extra money toward highest interest rate debt"
            DebtPayoffMethod.SNOWBALL -> "Pay minimum on all debts, put extra money toward smallest balance debt"
            DebtPayoffMethod.HYBRID -> "Pay off small debts first for quick wins, then focus on highest interest rates"
            DebtPayoffMethod.MINIMUM_ONLY -> "Pay only minimum payments on all debts"
        }
    }
    
    private fun getDebtStrategyPros(method: DebtPayoffMethod): List<String> {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> listOf("Saves the most money", "Mathematically optimal")
            DebtPayoffMethod.SNOWBALL -> listOf("Provides quick psychological wins", "Builds momentum")
            DebtPayoffMethod.HYBRID -> listOf("Balances savings and motivation", "Flexible approach")
            DebtPayoffMethod.MINIMUM_ONLY -> listOf("Lowest monthly commitment", "Preserves cash flow")
        }
    }
    
    private fun getDebtStrategyCons(method: DebtPayoffMethod): List<String> {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> listOf("May take longer to see progress", "Requires discipline")
            DebtPayoffMethod.SNOWBALL -> listOf("Costs more in interest", "Not mathematically optimal")
            DebtPayoffMethod.HYBRID -> listOf("More complex to manage", "Compromise approach")
            DebtPayoffMethod.MINIMUM_ONLY -> listOf("Costs the most in interest", "Debts never decrease")
        }
    }  
  
    // Savings optimization methods
    private fun determineOptimalSavingsAllocation(userProfile: UserFinancialProfile): List<SavingsAllocation> {
        val allocations = mutableListOf<SavingsAllocation>()
        
        // Emergency fund first
        val emergencyFundNeeded = calculateMonthlyExpenses(userProfile) * 6.0
        val currentEmergencyFund = calculateCurrentEmergencyFund(userProfile)
        
        if (currentEmergencyFund < emergencyFundNeeded) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.EMERGENCY_FUND,
                    percentage = 40.0,
                    amount = (emergencyFundNeeded - currentEmergencyFund) * 0.4,
                    reasoning = "Build emergency fund to 6 months of expenses",
                    expectedReturn = 2.5
                )
            )
        }
        
        // RRSP allocation
        if (userProfile.rrspRoom.amount > 0) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.RRSP,
                    percentage = 30.0,
                    amount = userProfile.rrspRoom * 0.3,
                    reasoning = "Tax-deferred growth and immediate tax deduction",
                    expectedReturn = 6.0
                )
            )
        }
        
        // TFSA allocation
        if (userProfile.tfsaRoom.amount > 0) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.TFSA,
                    percentage = 30.0,
                    amount = userProfile.tfsaRoom * 0.3,
                    reasoning = "Tax-free growth and withdrawal flexibility",
                    expectedReturn = 5.5
                )
            )
        }
        
        return allocations
    }
    
    private fun calculateSavingsGoalImpacts(userProfile: UserFinancialProfile, savingsRate: Double): List<GoalImpact> {
        return userProfile.goals.map { goal ->
            val monthlyIncome = userProfile.grossAnnualIncome / 12.0
            val additionalSavings = monthlyIncome * (savingsRate - calculateCurrentSavingsRate(userProfile))
            
            GoalImpact(
                goalId = goal.id,
                goalName = goal.title,
                impactType = if (additionalSavings.amount > 0) GoalImpactType.ACCELERATES else GoalImpactType.NEUTRAL,
                timeImpact = if (additionalSavings.amount > 0) -60 else 0, // 60 days faster
                amountImpact = additionalSavings * 12.0 // Annual impact
            )
        }
    }
    
    private fun buildSavingsReasoning(
        userProfile: UserFinancialProfile,
        recommendedRate: Double,
        emergencyTarget: Money,
        allocation: List<SavingsAllocation>
    ): String {
        return "Increasing your savings rate to ${(recommendedRate * 100).toInt()}% will help you build a $${emergencyTarget.format()} emergency fund and accelerate your financial goals."
    }
    
    // Methods for creating specific recommendations
    
    private fun createRRSPContributionRecommendation(userProfile: UserFinancialProfile): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "rrsp_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.TAX_OPTIMIZATION,
            priority = Priority.HIGH,
            title = "Optimize RRSP Contributions",
            description = "Maximize your tax savings by contributing to your RRSP",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Tax savings", "Available contribution room"),
                dataPoints = listOf(),
                assumptions = listOf("Income remains stable"),
                confidence = 0.9f,
                methodology = "Canadian tax optimization analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review available RRSP contribution room", 5, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.rrspRoom * userProfile.marginalTaxRate,
                timeToRealize = 12,
                confidence = 0.9f,
                impactType = ImpactType.TAX_SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createTFSAContributionRecommendation(userProfile: UserFinancialProfile): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "tfsa_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.TAX_OPTIMIZATION,
            priority = Priority.MEDIUM,
            title = "Maximize TFSA Contributions",
            description = "Contribute to your TFSA for tax-free growth",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Tax-free growth", "Withdrawal flexibility"),
                dataPoints = listOf(),
                assumptions = listOf("Market returns average 5-7%"),
                confidence = 0.85f,
                methodology = "Tax-free savings analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Check TFSA contribution room", 5, Difficulty.EASY, listOf()),
                ActionStep(2, "Set up automatic contributions", 15, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.tfsaRoom * 0.06, // 6% growth assumption
                timeToRealize = 12,
                confidence = 0.8f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf("Market volatility"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }    

    private fun createHighInterestDebtRecommendation(
        userProfile: UserFinancialProfile,
        highInterestDebts: List<Account>
    ): FinancialPlanningRecommendation {
        val totalHighInterestDebt = highInterestDebts.fold(Money.zero()) { acc, account -> 
            acc + account.balance.absoluteValue 
        }
        
        return FinancialPlanningRecommendation(
            id = "debt_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.DEBT_REDUCTION,
            priority = Priority.CRITICAL,
            title = "Pay Off High-Interest Debt",
            description = "Focus on eliminating high-interest debt to save money",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("High interest rates", "Guaranteed savings"),
                dataPoints = listOf(),
                assumptions = listOf("Interest rates remain stable"),
                confidence = 0.95f,
                methodology = "Debt avalanche analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "List all high-interest debts", 10, Difficulty.EASY, listOf()),
                ActionStep(2, "Allocate extra payments to highest rate debt", 5, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = totalHighInterestDebt * 0.15, // 15% interest savings
                timeToRealize = 24,
                confidence = 0.9f,
                impactType = ImpactType.DEBT_REDUCTION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.LONG_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createDebtConsolidationRecommendation(
        userProfile: UserFinancialProfile,
        debtAccounts: List<Account>
    ): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "consolidation_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.DEBT_REDUCTION,
            priority = Priority.MEDIUM,
            title = "Consider Debt Consolidation",
            description = "Simplify payments and potentially reduce interest rates",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Multiple debts", "Potential rate reduction"),
                dataPoints = listOf(),
                assumptions = listOf("Qualify for consolidation loan"),
                confidence = 0.7f,
                methodology = "Debt consolidation analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Research consolidation options", 30, Difficulty.MODERATE, listOf()),
                ActionStep(2, "Compare interest rates", 15, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = Money.fromDollars(200.0), // Estimated monthly savings
                timeToRealize = 3,
                confidence = 0.6f,
                impactType = ImpactType.DEBT_REDUCTION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.SHORT_TERM,
            prerequisites = listOf("Good credit score"),
            risks = listOf("May extend repayment period"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createSavingsRateRecommendation(
        userProfile: UserFinancialProfile,
        optimalRate: Double
    ): FinancialPlanningRecommendation {
        val currentRate = calculateCurrentSavingsRate(userProfile)
        val improvement = optimalRate - currentRate
        
        return FinancialPlanningRecommendation(
            id = "savings_rate_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.SAVINGS_OPTIMIZATION,
            priority = Priority.MEDIUM,
            title = "Increase Savings Rate",
            description = "Boost your savings rate to ${(optimalRate * 100).toInt()}% for better financial security",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Financial security", "Goal achievement"),
                dataPoints = listOf(),
                assumptions = listOf("Income remains stable"),
                confidence = 0.8f,
                methodology = "Optimal savings rate analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review current spending", 20, Difficulty.EASY, listOf()),
                ActionStep(2, "Identify areas to reduce expenses", 30, Difficulty.MODERATE, listOf()),
                ActionStep(3, "Set up automatic savings", 10, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.grossAnnualIncome * improvement,
                timeToRealize = 6,
                confidence = 0.75f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf("Lifestyle changes required"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createEmergencyFundRecommendation(
        userProfile: UserFinancialProfile,
        target: Money,
        current: Money
    ): FinancialPlanningRecommendation {
        val needed = target - current
        
        return FinancialPlanningRecommendation(
            id = "emergency_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.EMERGENCY_FUND,
            priority = Priority.HIGH,
            title = "Build Emergency Fund",
            description = "Build a $${target.format()} emergency fund for financial security",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Financial security", "Unexpected expenses"),
                dataPoints = listOf(),
                assumptions = listOf("6 months of expenses needed"),
                confidence = 0.9f,
                methodology = "Emergency fund best practices"
            ),
            actionSteps = listOf(
                ActionStep(1, "Open high-yield savings account", 15, Difficulty.EASY, listOf()),
                ActionStep(2, "Set up automatic transfers", 10, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = needed,
                timeToRealize = 12,
                confidence = 0.85f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.LONG_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createGoalAccelerationRecommendation(
        userProfile: UserFinancialProfile,
        goal: FinancialGoal
    ): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "goal_accel_${goal.id}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.GOAL_ACCELERATION,
            priority = Priority.MEDIUM,
            title = "Accelerate ${goal.title}",
            description = "Get back on track with your ${goal.title} goal",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Goal timeline", "Current progress"),
                dataPoints = listOf(),
                assumptions = listOf("Increased contributions possible"),
                confidence = 0.7f,
                methodology = "Goal progress analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review goal timeline", 10, Difficulty.EASY, listOf()),
                ActionStep(2, "Increase monthly contributions", 5, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = goal.targetAmount - goal.currentAmount,
                timeToRealize = 6,
                confidence = 0.7f,
                impactType = ImpactType.GOAL_ACCELERATION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createSpendingOptimizationRecommendation(
        userProfile: UserFinancialProfile,
        category: CategorySpending
    ): FinancialPlanningRecommendation {
        val potentialSavings = category.totalAmount * 0.15 // 15% reduction potential
        
        return FinancialPlanningRecommendation(
            id = "spending_opt_${category.category.id}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.CASH_FLOW_OPTIMIZATION,
            priority = Priority.LOW,
            title = "Optimize ${category.category.name} Spending",
            description = "Reduce spending in ${category.category.name} to free up cash for savings",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("High spending category", "Increasing trend"),
                dataPoints = listOf(),
                assumptions = listOf("15% reduction achievable"),
                confidence = 0.6f,
                methodology = "Spending pattern analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review ${category.category.name} transactions", 15, Difficulty.EASY, listOf()),
                ActionStep(2, "Identify reduction opportunities", 20, Difficulty.MODERATE, listOf()),
                ActionStep(3, "Set spending alerts", 5, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = potentialSavings,
                timeToRealize = 3,
                confidence = 0.6f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.SHORT_TERM,
            prerequisites = listOf(),
            risks = listOf("Lifestyle changes required"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    // Helper methods for tracking and status updates
    
    private fun updateRecommendationStatus(recommendationId: String, action: RecommendationAction) {
        // Implementation would update database
    }
    
    private fun trackRecommendationMetrics(outcome: RecommendationOutcome) {
        // Implementation would track metrics for algorithm improvement
    }
    
    private fun updateUserFinancialImpact(outcome: RecommendationOutcome) {
        // Implementation would update user's financial profile
    }
}

// Extension function to check if a goal is off track
private fun FinancialGoal.isOffTrack(): Boolean {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val totalDays = targetDate.toEpochDays() - today.toEpochDays()
    val elapsedDays = today.toEpochDays() - createdAt.toEpochDays()
    
    if (totalDays <= 0) return currentAmount < targetAmount
    
    val expectedProgress = elapsedDays.toDouble() / (elapsedDays + totalDays).toDouble()
    val actualProgress = currentAmount.amount / targetAmount.amount
    
    return actualProgress < expectedProgress * 0.9 // 10% tolerance
}   
                 interestRate = getAssumedInterestRate(account.accountType),
                    minimumPayment = getAssumedMinimumPayment(account),
                    recommendedPayment = if (index == 0) availableFunds else Money.zero(),
                    payoffOrder = index + 1,
                    estimatedPayoffMonths = calculatePayoffMonths(account, availableFunds)
                )
            },
            totalInterestSaved = calculateInterestSaved(sortedDebts, availableFunds),
            payoffTimeframe = calculateTotalPayoffTime(sortedDebts, availableFunds),
            monthlyPaymentPlan = availableFunds,
            reasoning = "Hybrid method balances psychological wins with interest savings",
            alternativeStrategies = emptyList()
        )
    }
    
    // Helper methods for debt calculations
    private fun getAssumedInterestRate(accountType: AccountType): Double {
        return when (accountType) {
            AccountType.CREDIT_CARD -> 20.0
            AccountType.LOAN -> 8.0
            AccountType.MORTGAGE -> 4.0
            else -> 10.0
        }
    }
    
    private fun getAssumedMinimumPayment(account: Account): Money {
        return when (account.accountType) {
            AccountType.CREDIT_CARD -> account.balance.absoluteValue * 0.03 // 3% minimum
            AccountType.LOAN -> account.balance.absoluteValue * 0.02 // 2% minimum
            AccountType.MORTGAGE -> account.balance.absoluteValue * 0.01 // 1% minimum
            else -> Money.fromDollars(50.0)
        }
    }
    
    private fun chooseOptimalDebtStrategy(
        userProfile: UserFinancialProfile,
        avalanche: DebtPayoffStrategy,
        snowball: DebtPayoffStrategy,
        hybrid: DebtPayoffStrategy
    ): DebtPayoffStrategy {
        // Choose based on user profile and debt characteristics
        val totalDebt = userProfile.accounts.filter { it.isDebt }.fold(Money.zero()) { acc, account -> 
            acc + account.balance.absoluteValue 
        }
        
        return when {
            totalDebt.amount > 50000.0 -> avalanche // High debt = focus on interest savings
            userProfile.age < 30 -> hybrid // Young = balance motivation and savings
            else -> snowball // Default to psychological wins
        }
    }
    
    private fun calculatePayoffMonths(account: Account, extraPayment: Money): Int {
        val balance = account.balance.absoluteValue.amount
        val rate = getAssumedInterestRate(account.accountType) / 12.0 / 100.0 // Monthly rate
        val payment = getAssumedMinimumPayment(account).amount + extraPayment.amount
        
        if (rate == 0.0) return (balance / payment).toInt()
        
        return (-ln(1 - (balance * rate) / payment) / ln(1 + rate)).toInt()
    }
    
    private fun calculateInterestSaved(debts: List<Account>, extraPayment: Money): Money {
        // Simplified calculation - in reality would be more complex
        val totalInterest = debts.fold(0.0) { acc, debt ->
            acc + (debt.balance.amount * getAssumedInterestRate(debt.accountType) / 100.0 * 2.0) // Assume 2 years average
        }
        
        val interestWithExtra = totalInterest * 0.7 // Assume 30% savings with extra payments
        return Money.fromDollars(totalInterest - interestWithExtra)
    }
    
    private fun calculateTotalPayoffTime(debts: List<Account>, extraPayment: Money): Int {
        return debts.maxOfOrNull { calculatePayoffMonths(it, extraPayment) } ?: 0
    }
    
    private fun getDebtStrategyDescription(method: DebtPayoffMethod): String {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> "Pay minimum on all debts, put extra money toward highest interest rate debt"
            DebtPayoffMethod.SNOWBALL -> "Pay minimum on all debts, put extra money toward smallest balance debt"
            DebtPayoffMethod.HYBRID -> "Pay off small debts first for quick wins, then focus on highest interest rates"
            DebtPayoffMethod.MINIMUM_ONLY -> "Pay only minimum payments on all debts"
        }
    }
    
    private fun getDebtStrategyPros(method: DebtPayoffMethod): List<String> {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> listOf("Saves the most money", "Mathematically optimal")
            DebtPayoffMethod.SNOWBALL -> listOf("Provides quick psychological wins", "Builds momentum")
            DebtPayoffMethod.HYBRID -> listOf("Balances savings and motivation", "Flexible approach")
            DebtPayoffMethod.MINIMUM_ONLY -> listOf("Lowest monthly commitment", "Preserves cash flow")
        }
    }
    
    private fun getDebtStrategyCons(method: DebtPayoffMethod): List<String> {
        return when (method) {
            DebtPayoffMethod.AVALANCHE -> listOf("May take longer to see progress", "Requires discipline")
            DebtPayoffMethod.SNOWBALL -> listOf("Costs more in interest", "Not mathematically optimal")
            DebtPayoffMethod.HYBRID -> listOf("More complex to manage", "Compromise approach")
            DebtPayoffMethod.MINIMUM_ONLY -> listOf("Costs the most in interest", "Debts never decrease")
        }
    }
    
    // Savings optimization methods
    private fun determineOptimalSavingsAllocation(userProfile: UserFinancialProfile): List<SavingsAllocation> {
        val allocations = mutableListOf<SavingsAllocation>()
        
        // Emergency fund first
        val emergencyFundNeeded = calculateMonthlyExpenses(userProfile) * 6.0
        val currentEmergencyFund = calculateCurrentEmergencyFund(userProfile)
        
        if (currentEmergencyFund < emergencyFundNeeded) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.EMERGENCY_FUND,
                    percentage = 40.0,
                    amount = (emergencyFundNeeded - currentEmergencyFund) * 0.4,
                    reasoning = "Build emergency fund to 6 months of expenses",
                    expectedReturn = 2.5
                )
            )
        }
        
        // RRSP allocation
        if (userProfile.rrspRoom.amount > 0) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.RRSP,
                    percentage = 30.0,
                    amount = userProfile.rrspRoom * 0.3,
                    reasoning = "Tax-deferred growth and immediate tax deduction",
                    expectedReturn = 6.0
                )
            )
        }
        
        // TFSA allocation
        if (userProfile.tfsaRoom.amount > 0) {
            allocations.add(
                SavingsAllocation(
                    accountType = SavingsAccountType.TFSA,
                    percentage = 30.0,
                    amount = userProfile.tfsaRoom * 0.3,
                    reasoning = "Tax-free growth and withdrawal flexibility",
                    expectedReturn = 5.5
                )
            )
        }
        
        return allocations
    }
    
    private fun calculateSavingsGoalImpacts(userProfile: UserFinancialProfile, savingsRate: Double): List<GoalImpact> {
        return userProfile.goals.map { goal ->
            val monthlyIncome = userProfile.grossAnnualIncome / 12.0
            val additionalSavings = monthlyIncome * (savingsRate - calculateCurrentSavingsRate(userProfile))
            
            GoalImpact(
                goalId = goal.id,
                goalName = goal.title,
                impactType = if (additionalSavings.amount > 0) GoalImpactType.ACCELERATES else GoalImpactType.NEUTRAL,
                timeImpact = if (additionalSavings.amount > 0) -60 else 0, // 60 days faster
                amountImpact = additionalSavings * 12.0 // Annual impact
            )
        }
    }
    
    private fun buildSavingsReasoning(
        userProfile: UserFinancialProfile,
        recommendedRate: Double,
        emergencyTarget: Money,
        allocation: List<SavingsAllocation>
    ): String {
        return "Increasing your savings rate to ${(recommendedRate * 100).toInt()}% will help you build a ${emergencyTarget.format()} emergency fund and accelerate your financial goals."
    }
    
    // Methods for creating specific recommendations
    
    private fun createRRSPContributionRecommendation(userProfile: UserFinancialProfile): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "rrsp_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.TAX_OPTIMIZATION,
            priority = Priority.HIGH,
            title = "Optimize RRSP Contributions",
            description = "Maximize your tax savings by contributing to your RRSP",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Tax savings", "Available contribution room"),
                dataPoints = listOf(),
                assumptions = listOf("Income remains stable"),
                confidence = 0.9f,
                methodology = "Canadian tax optimization analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review available RRSP contribution room", 5, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.rrspRoom * userProfile.marginalTaxRate,
                timeToRealize = 12,
                confidence = 0.9f,
                impactType = ImpactType.TAX_SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createTFSAContributionRecommendation(userProfile: UserFinancialProfile): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "tfsa_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.TAX_OPTIMIZATION,
            priority = Priority.MEDIUM,
            title = "Maximize TFSA Contributions",
            description = "Contribute to your TFSA for tax-free growth",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Tax-free growth", "Withdrawal flexibility"),
                dataPoints = listOf(),
                assumptions = listOf("Market returns average 5-7%"),
                confidence = 0.85f,
                methodology = "Tax-free savings analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Check TFSA contribution room", 5, Difficulty.EASY, listOf()),
                ActionStep(2, "Set up automatic contributions", 15, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.tfsaRoom * 0.06, // 6% growth assumption
                timeToRealize = 12,
                confidence = 0.8f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf("Market volatility"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createHighInterestDebtRecommendation(
        userProfile: UserFinancialProfile,
        highInterestDebts: List<Account>
    ): FinancialPlanningRecommendation {
        val totalHighInterestDebt = highInterestDebts.fold(Money.zero()) { acc, account -> 
            acc + account.balance.absoluteValue 
        }
        
        return FinancialPlanningRecommendation(
            id = "debt_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.DEBT_REDUCTION,
            priority = Priority.CRITICAL,
            title = "Pay Off High-Interest Debt",
            description = "Focus on eliminating high-interest debt to save money",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("High interest rates", "Guaranteed savings"),
                dataPoints = listOf(),
                assumptions = listOf("Interest rates remain stable"),
                confidence = 0.95f,
                methodology = "Debt avalanche analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "List all high-interest debts", 10, Difficulty.EASY, listOf()),
                ActionStep(2, "Allocate extra payments to highest rate debt", 5, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = totalHighInterestDebt * 0.15, // 15% interest savings
                timeToRealize = 24,
                confidence = 0.9f,
                impactType = ImpactType.DEBT_REDUCTION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.LONG_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createDebtConsolidationRecommendation(
        userProfile: UserFinancialProfile,
        debtAccounts: List<Account>
    ): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "consolidation_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.DEBT_REDUCTION,
            priority = Priority.MEDIUM,
            title = "Consider Debt Consolidation",
            description = "Simplify payments and potentially reduce interest rates",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Multiple debts", "Potential rate reduction"),
                dataPoints = listOf(),
                assumptions = listOf("Qualify for consolidation loan"),
                confidence = 0.7f,
                methodology = "Debt consolidation analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Research consolidation options", 30, Difficulty.MODERATE, listOf()),
                ActionStep(2, "Compare interest rates", 15, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = Money.fromDollars(200.0), // Estimated monthly savings
                timeToRealize = 3,
                confidence = 0.6f,
                impactType = ImpactType.DEBT_REDUCTION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.SHORT_TERM,
            prerequisites = listOf("Good credit score"),
            risks = listOf("May extend repayment period"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createSavingsRateRecommendation(
        userProfile: UserFinancialProfile,
        optimalRate: Double
    ): FinancialPlanningRecommendation {
        val currentRate = calculateCurrentSavingsRate(userProfile)
        val improvement = optimalRate - currentRate
        
        return FinancialPlanningRecommendation(
            id = "savings_rate_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.SAVINGS_OPTIMIZATION,
            priority = Priority.MEDIUM,
            title = "Increase Savings Rate",
            description = "Boost your savings rate to ${(optimalRate * 100).toInt()}% for better financial security",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Financial security", "Goal achievement"),
                dataPoints = listOf(),
                assumptions = listOf("Income remains stable"),
                confidence = 0.8f,
                methodology = "Optimal savings rate analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review current spending", 20, Difficulty.EASY, listOf()),
                ActionStep(2, "Identify areas to reduce expenses", 30, Difficulty.MODERATE, listOf()),
                ActionStep(3, "Set up automatic savings", 10, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = userProfile.grossAnnualIncome * improvement,
                timeToRealize = 6,
                confidence = 0.75f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf("Lifestyle changes required"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createEmergencyFundRecommendation(
        userProfile: UserFinancialProfile,
        target: Money,
        current: Money
    ): FinancialPlanningRecommendation {
        val needed = target - current
        
        return FinancialPlanningRecommendation(
            id = "emergency_${userProfile.userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.EMERGENCY_FUND,
            priority = Priority.HIGH,
            title = "Build Emergency Fund",
            description = "Build a ${target.format()} emergency fund for financial security",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Financial security", "Unexpected expenses"),
                dataPoints = listOf(),
                assumptions = listOf("6 months of expenses needed"),
                confidence = 0.9f,
                methodology = "Emergency fund best practices"
            ),
            actionSteps = listOf(
                ActionStep(1, "Open high-yield savings account", 15, Difficulty.EASY, listOf()),
                ActionStep(2, "Set up automatic transfers", 10, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = needed,
                timeToRealize = 12,
                confidence = 0.85f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.LONG_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createGoalAccelerationRecommendation(
        userProfile: UserFinancialProfile,
        goal: FinancialGoal
    ): FinancialPlanningRecommendation {
        return FinancialPlanningRecommendation(
            id = "goal_accel_${goal.id}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.GOAL_ACCELERATION,
            priority = Priority.MEDIUM,
            title = "Accelerate ${goal.title}",
            description = "Get back on track with your ${goal.title} goal",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("Goal timeline", "Current progress"),
                dataPoints = listOf(),
                assumptions = listOf("Increased contributions possible"),
                confidence = 0.7f,
                methodology = "Goal progress analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review goal timeline", 10, Difficulty.EASY, listOf()),
                ActionStep(2, "Increase monthly contributions", 5, Difficulty.MODERATE, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = goal.targetAmount - goal.currentAmount,
                timeToRealize = 6,
                confidence = 0.7f,
                impactType = ImpactType.GOAL_ACCELERATION,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.MEDIUM_TERM,
            prerequisites = listOf(),
            risks = listOf(),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    private fun createSpendingOptimizationRecommendation(
        userProfile: UserFinancialProfile,
        category: CategorySpending
    ): FinancialPlanningRecommendation {
        val potentialSavings = category.totalAmount * 0.15 // 15% reduction potential
        
        return FinancialPlanningRecommendation(
            id = "spending_opt_${category.category.id}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userProfile.userId,
            type = FinancialPlanningType.CASH_FLOW_OPTIMIZATION,
            priority = Priority.LOW,
            title = "Optimize ${category.category.name} Spending",
            description = "Reduce spending in ${category.category.name} to free up cash for savings",
            reasoning = RecommendationReasoning(
                primaryFactors = listOf("High spending category", "Increasing trend"),
                dataPoints = listOf(),
                assumptions = listOf("15% reduction achievable"),
                confidence = 0.6f,
                methodology = "Spending pattern analysis"
            ),
            actionSteps = listOf(
                ActionStep(1, "Review ${category.category.name} transactions", 15, Difficulty.EASY, listOf()),
                ActionStep(2, "Identify reduction opportunities", 20, Difficulty.MODERATE, listOf()),
                ActionStep(3, "Set spending alerts", 5, Difficulty.EASY, listOf())
            ),
            expectedImpact = ExpectedImpact(
                financialImpact = potentialSavings,
                timeToRealize = 3,
                confidence = 0.6f,
                impactType = ImpactType.SAVINGS,
                goalProgress = listOf()
            ),
            timeframe = RecommendationTimeframe.SHORT_TERM,
            prerequisites = listOf(),
            risks = listOf("Lifestyle changes required"),
            alternatives = listOf(),
            trackingMetrics = listOf(),
            createdAt = Clock.System.now(),
            expiresAt = null
        )
    }
    
    // Helper methods for tracking and status updates
    
    private fun updateRecommendationStatus(recommendationId: String, action: RecommendationAction) {
        // Implementation would update database
    }
    
    private fun trackRecommendationMetrics(outcome: RecommendationOutcome) {
        // Implementation would track metrics for algorithm improvement
    }
    
    private fun updateUserFinancialImpact(outcome: RecommendationOutcome) {
        // Implementation would update user's financial profile
    }
}

// Extension function to check if a goal is off track
private fun FinancialGoal.isOffTrack(): Boolean {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val totalDays = targetDate.toEpochDays() - today.toEpochDays()
    val elapsedDays = today.toEpochDays() - createdAt.toEpochDays()
    
    if (totalDays <= 0) return currentAmount < targetAmount
    
    val expectedProgress = elapsedDays.toDouble() / (elapsedDays + totalDays).toDouble()
    val actualProgress = currentAmount.amount / targetAmount.amount
    
    return actualProgress < expectedProgress * 0.9 // 10% tolerance
}