package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlin.test.*

class CanadianTaxCalculatorTest {
    
    @Test
    fun `calculateCanadianTaxes should calculate correct federal tax for low income`() {
        // Given - Income below first federal bracket ($55,867)
        val grossIncome = Money.fromDollars(40000.0)
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then
        val expectedFederalTax = 40000.0 * 0.15 // 15% federal rate
        assertEquals(Money.fromDollars(expectedFederalTax), result.federalTax)
        assertTrue(result.totalTax.isPositive)
        assertTrue(result.afterTaxIncome.isPositive)
        assertTrue(result.marginalTaxRate > 0.0)
        assertTrue(result.averageTaxRate > 0.0)
    }
    
    @Test
    fun `calculateCanadianTaxes should calculate correct federal tax for middle income`() {
        // Given - Income in second federal bracket ($55,867 - $111,733)
        val grossIncome = Money.fromDollars(80000.0)
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then
        val expectedFederalTax = 8380.05 + (80000.0 - 55867) * 0.205
        assertEquals(Money.fromDollars(expectedFederalTax), result.federalTax)
        assertTrue(result.totalTax.isPositive)
        assertTrue(result.afterTaxIncome.isPositive)
    }
    
    @Test
    fun `calculateCanadianTaxes should calculate correct federal tax for high income`() {
        // Given - Income in highest federal bracket (>$246,752)
        val grossIncome = Money.fromDollars(300000.0)
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then
        val expectedFederalTax = 58170.09 + (300000.0 - 246752) * 0.33
        assertEquals(Money.fromDollars(expectedFederalTax), result.federalTax)
        assertTrue(result.totalTax.isPositive)
        assertTrue(result.afterTaxIncome.isPositive)
    }
    
    @Test
    fun `calculateCanadianTaxes should calculate correct CPP contributions`() {
        // Given - Income above CPP maximum
        val grossIncome = Money.fromDollars(80000.0)
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then - CPP should be capped at maximum
        val expectedCPP = kotlin.math.min(80000.0 * 0.0595, 3754.45)
        assertEquals(Money.fromDollars(expectedCPP), result.cpp)
    }
    
    @Test
    fun `calculateCanadianTaxes should calculate correct EI contributions`() {
        // Given - Income above EI maximum
        val grossIncome = Money.fromDollars(60000.0)
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then - EI should be capped at maximum
        val expectedEI = kotlin.math.min(60000.0 * 0.0229, 1049.12)
        assertEquals(Money.fromDollars(expectedEI), result.ei)
    }
    
    @Test
    fun `calculateCanadianTaxes should handle different provinces correctly`() {
        val grossIncome = Money.fromDollars(60000.0)
        
        // Test different provinces
        val ontarioResult = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, CanadianProvince.ON)
        val bcResult = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, CanadianProvince.BC)
        val albertaResult = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, CanadianProvince.AB)
        val quebecResult = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, CanadianProvince.QC)
        
        // Federal tax should be the same across provinces
        assertEquals(ontarioResult.federalTax, bcResult.federalTax)
        assertEquals(ontarioResult.federalTax, albertaResult.federalTax)
        assertEquals(ontarioResult.federalTax, quebecResult.federalTax)
        
        // Provincial tax should be different
        assertNotEquals(ontarioResult.provincialTax, bcResult.provincialTax)
        assertNotEquals(ontarioResult.provincialTax, albertaResult.provincialTax)
        assertNotEquals(ontarioResult.provincialTax, quebecResult.provincialTax)
        
        // Total tax should be different
        assertNotEquals(ontarioResult.totalTax, bcResult.totalTax)
        assertNotEquals(ontarioResult.totalTax, albertaResult.totalTax)
        assertNotEquals(ontarioResult.totalTax, quebecResult.totalTax)
    }
    
    @Test
    fun `calculateMarginalTaxRate should return correct rates for Ontario`() {
        // Test various income levels in Ontario
        assertEquals(20.05, CanadianTaxCalculator.calculateMarginalTaxRate(30000.0, CanadianProvince.ON))
        assertEquals(24.15, CanadianTaxCalculator.calculateMarginalTaxRate(55000.0, CanadianProvince.ON))
        assertEquals(29.65, CanadianTaxCalculator.calculateMarginalTaxRate(80000.0, CanadianProvince.ON))
        assertEquals(31.16, CanadianTaxCalculator.calculateMarginalTaxRate(120000.0, CanadianProvince.ON))
        assertEquals(37.16, CanadianTaxCalculator.calculateMarginalTaxRate(160000.0, CanadianProvince.ON))
        assertEquals(41.16, CanadianTaxCalculator.calculateMarginalTaxRate(200000.0, CanadianProvince.ON))
        assertEquals(43.41, CanadianTaxCalculator.calculateMarginalTaxRate(230000.0, CanadianProvince.ON))
        assertEquals(46.16, CanadianTaxCalculator.calculateMarginalTaxRate(300000.0, CanadianProvince.ON))
    }
    
    @Test
    fun `calculateMarginalTaxRate should return correct rates for different provinces`() {
        val income = 80000.0
        
        val ontarioRate = CanadianTaxCalculator.calculateMarginalTaxRate(income, CanadianProvince.ON)
        val bcRate = CanadianTaxCalculator.calculateMarginalTaxRate(income, CanadianProvince.BC)
        val albertaRate = CanadianTaxCalculator.calculateMarginalTaxRate(income, CanadianProvince.AB)
        val quebecRate = CanadianTaxCalculator.calculateMarginalTaxRate(income, CanadianProvince.QC)
        
        // All rates should be positive and different
        assertTrue(ontarioRate > 0)
        assertTrue(bcRate > 0)
        assertTrue(albertaRate > 0)
        assertTrue(quebecRate > 0)
        
        // Quebec should have the highest marginal rate
        assertTrue(quebecRate > ontarioRate)
        assertTrue(quebecRate > bcRate)
        assertTrue(quebecRate > albertaRate)
    }
    
    @Test
    fun `calculateRRSPAnalysis should calculate correct contribution limits`() {
        // Given
        val grossIncome = Money.fromDollars(100000.0)
        
        // When
        val result = CanadianTaxCalculator.calculateRRSPAnalysis(grossIncome)
        
        // Then
        val expectedMaxContribution = kotlin.math.min(100000.0 * 0.18, 31560.0)
        assertEquals(Money.fromDollars(expectedMaxContribution), result.maxContribution)
        assertTrue(result.contributionRoom.isPositive)
        assertTrue(result.recommendedContribution.isPositive)
        assertTrue(result.taxSavings.isPositive)
        assertTrue(result.carryForwardRoom.isPositive)
    }
    
    @Test
    fun `calculateRRSPAnalysis should cap contribution at annual limit`() {
        // Given - Very high income that would exceed RRSP limit
        val grossIncome = Money.fromDollars(500000.0)
        
        // When
        val result = CanadianTaxCalculator.calculateRRSPAnalysis(grossIncome)
        
        // Then - Should be capped at 2024 limit
        assertEquals(Money.fromDollars(31560.0), result.maxContribution)
    }
    
    @Test
    fun `calculateTFSAAnalysis should return correct analysis`() {
        // When
        val result = CanadianTaxCalculator.calculateTFSAAnalysis()
        
        // Then
        assertEquals(Money.fromDollars(7000.0), result.maxContribution)
        assertTrue(result.contributionRoom.isPositive)
        assertTrue(result.recommendedContribution.isPositive)
        assertTrue(result.withdrawalRoom.isPositive)
        assertTrue(result.currentContributions.isPositive)
    }
    
    @Test
    fun `generateTaxRecommendations should create RRSP recommendation when room available`() {
        // Given
        val grossIncome = Money.fromDollars(80000.0)
        val rrspAnalysis = RRSPAnalysis(
            currentContributions = Money.fromDollars(5000.0),
            contributionRoom = Money.fromDollars(10000.0),
            maxContribution = Money.fromDollars(15000.0),
            taxSavings = Money.fromDollars(3000.0),
            recommendedContribution = Money.fromDollars(8000.0),
            carryForwardRoom = Money.fromDollars(2000.0)
        )
        val tfsaAnalysis = TFSAAnalysis(
            currentContributions = Money.fromDollars(3000.0),
            contributionRoom = Money.fromDollars(4000.0),
            maxContribution = Money.fromDollars(7000.0),
            recommendedContribution = Money.fromDollars(2000.0),
            withdrawalRoom = Money.fromDollars(10000.0)
        )
        
        // When
        val recommendations = CanadianTaxCalculator.generateTaxRecommendations(
            grossIncome, rrspAnalysis, tfsaAnalysis
        )
        
        // Then
        assertTrue(recommendations.isNotEmpty())
        val rrspRecommendation = recommendations.find { it.type == TaxRecommendationType.RRSP_CONTRIBUTION }
        assertNotNull(rrspRecommendation)
        assertEquals(Priority.HIGH, rrspRecommendation.priority)
        assertTrue(rrspRecommendation.potentialSavings.isPositive)
    }
    
    @Test
    fun `generateTaxRecommendations should create TFSA recommendation when room available`() {
        // Given
        val grossIncome = Money.fromDollars(60000.0)
        val rrspAnalysis = RRSPAnalysis(
            currentContributions = Money.fromDollars(10000.0),
            contributionRoom = Money.fromDollars(500.0), // Small room
            maxContribution = Money.fromDollars(10500.0),
            taxSavings = Money.fromDollars(150.0),
            recommendedContribution = Money.fromDollars(500.0),
            carryForwardRoom = Money.fromDollars(0.0)
        )
        val tfsaAnalysis = TFSAAnalysis(
            currentContributions = Money.fromDollars(2000.0),
            contributionRoom = Money.fromDollars(5000.0), // Good room
            maxContribution = Money.fromDollars(7000.0),
            recommendedContribution = Money.fromDollars(2000.0),
            withdrawalRoom = Money.fromDollars(10000.0)
        )
        
        // When
        val recommendations = CanadianTaxCalculator.generateTaxRecommendations(
            grossIncome, rrspAnalysis, tfsaAnalysis
        )
        
        // Then
        val tfsaRecommendation = recommendations.find { it.type == TaxRecommendationType.TFSA_CONTRIBUTION }
        assertNotNull(tfsaRecommendation)
        assertEquals(Priority.MEDIUM, tfsaRecommendation.priority)
        assertTrue(tfsaRecommendation.potentialSavings.isPositive)
    }
    
    @Test
    fun `generateTaxRecommendations should create income splitting recommendation for high earners`() {
        // Given - High income
        val grossIncome = Money.fromDollars(150000.0)
        val rrspAnalysis = RRSPAnalysis(
            currentContributions = Money.fromDollars(20000.0),
            contributionRoom = Money.fromDollars(500.0),
            maxContribution = Money.fromDollars(20500.0),
            taxSavings = Money.fromDollars(150.0),
            recommendedContribution = Money.fromDollars(500.0),
            carryForwardRoom = Money.fromDollars(0.0)
        )
        val tfsaAnalysis = TFSAAnalysis(
            currentContributions = Money.fromDollars(7000.0),
            contributionRoom = Money.fromDollars(0.0),
            maxContribution = Money.fromDollars(7000.0),
            recommendedContribution = Money.fromDollars(0.0),
            withdrawalRoom = Money.fromDollars(10000.0)
        )
        
        // When
        val recommendations = CanadianTaxCalculator.generateTaxRecommendations(
            grossIncome, rrspAnalysis, tfsaAnalysis
        )
        
        // Then
        val incomeSplittingRecommendation = recommendations.find { it.type == TaxRecommendationType.INCOME_SPLITTING }
        assertNotNull(incomeSplittingRecommendation)
        assertEquals(Priority.MEDIUM, incomeSplittingRecommendation.priority)
        assertTrue(incomeSplittingRecommendation.potentialSavings.isPositive)
    }
    
    @Test
    fun `generateTaxRecommendations should not create recommendations when no room available`() {
        // Given - No contribution room
        val grossIncome = Money.fromDollars(50000.0)
        val rrspAnalysis = RRSPAnalysis(
            currentContributions = Money.fromDollars(9000.0),
            contributionRoom = Money.fromDollars(0.0),
            maxContribution = Money.fromDollars(9000.0),
            taxSavings = Money.fromDollars(0.0),
            recommendedContribution = Money.fromDollars(0.0),
            carryForwardRoom = Money.fromDollars(0.0)
        )
        val tfsaAnalysis = TFSAAnalysis(
            currentContributions = Money.fromDollars(7000.0),
            contributionRoom = Money.fromDollars(0.0),
            maxContribution = Money.fromDollars(7000.0),
            recommendedContribution = Money.fromDollars(0.0),
            withdrawalRoom = Money.fromDollars(0.0)
        )
        
        // When
        val recommendations = CanadianTaxCalculator.generateTaxRecommendations(
            grossIncome, rrspAnalysis, tfsaAnalysis
        )
        
        // Then - Should have no RRSP or TFSA recommendations
        val rrspRecommendation = recommendations.find { it.type == TaxRecommendationType.RRSP_CONTRIBUTION }
        val tfsaRecommendation = recommendations.find { it.type == TaxRecommendationType.TFSA_CONTRIBUTION }
        assertNull(rrspRecommendation)
        assertNull(tfsaRecommendation)
    }
    
    @Test
    fun `tax calculations should be consistent across income ranges`() {
        val incomes = listOf(30000.0, 60000.0, 90000.0, 120000.0, 200000.0)
        val province = CanadianProvince.ON
        
        for (i in 0 until incomes.size - 1) {
            val lowerIncome = Money.fromDollars(incomes[i])
            val higherIncome = Money.fromDollars(incomes[i + 1])
            
            val lowerResult = CanadianTaxCalculator.calculateCanadianTaxes(lowerIncome, province)
            val higherResult = CanadianTaxCalculator.calculateCanadianTaxes(higherIncome, province)
            
            // Higher income should result in higher total tax
            assertTrue(
                higherResult.totalTax > lowerResult.totalTax,
                "Higher income (${higherIncome.format()}) should have higher total tax than lower income (${lowerIncome.format()})"
            )
            
            // After-tax income should still be higher for higher gross income
            assertTrue(
                higherResult.afterTaxIncome > lowerResult.afterTaxIncome,
                "Higher income should result in higher after-tax income"
            )
            
            // Average tax rate should generally increase (progressive taxation)
            assertTrue(
                higherResult.averageTaxRate >= lowerResult.averageTaxRate,
                "Average tax rate should increase or stay same with higher income"
            )
        }
    }
    
    @Test
    fun `edge case - zero income should result in zero taxes`() {
        // Given
        val grossIncome = Money.zero()
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then
        assertEquals(Money.zero(), result.federalTax)
        assertEquals(Money.zero(), result.provincialTax)
        assertEquals(Money.zero(), result.cpp)
        assertEquals(Money.zero(), result.ei)
        assertEquals(Money.zero(), result.totalTax)
        assertEquals(Money.zero(), result.afterTaxIncome)
        assertEquals(0.0, result.averageTaxRate)
    }
    
    @Test
    fun `edge case - very high income should not cause overflow`() {
        // Given - Extremely high income
        val grossIncome = Money.fromDollars(10000000.0) // 10 million
        val province = CanadianProvince.ON
        
        // When
        val result = CanadianTaxCalculator.calculateCanadianTaxes(grossIncome, province)
        
        // Then - Should not crash and should produce reasonable results
        assertTrue(result.totalTax.isPositive)
        assertTrue(result.afterTaxIncome.isPositive)
        assertTrue(result.marginalTaxRate > 0.0)
        assertTrue(result.averageTaxRate > 0.0)
        assertTrue(result.afterTaxIncome < grossIncome) // Should pay some tax
    }
}