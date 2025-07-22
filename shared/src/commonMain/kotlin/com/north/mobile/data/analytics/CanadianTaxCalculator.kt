package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.math.min

/**
 * Canadian tax calculation utilities
 */
object CanadianTaxCalculator {
    
    /**
     * Calculate Canadian federal and provincial taxes for 2024
     */
    fun calculateCanadianTaxes(grossIncome: Money, province: CanadianProvince): TaxBreakdown {
        val income = grossIncome.dollars
        
        // Federal tax brackets (2024)
        val federalTax = when {
            income <= 55867 -> income * 0.15
            income <= 111733 -> 8380.05 + (income - 55867) * 0.205
            income <= 173205 -> 20849.58 + (income - 111733) * 0.26
            income <= 246752 -> 36838.46 + (income - 173205) * 0.29
            else -> 58170.09 + (income - 246752) * 0.33
        }
        
        // Provincial tax calculation
        val provincialTax = calculateProvincialTax(income, province)
        
        // CPP and EI calculations (2024 rates)
        val cpp = min(income * 0.0595, 3754.45) // 2024 max CPP
        val ei = min(income * 0.0229, 1049.12) // 2024 max EI
        
        val totalTax = Money.fromDollars(federalTax + provincialTax + cpp + ei)
        val afterTaxIncome = grossIncome - totalTax
        
        val marginalTaxRate = calculateMarginalTaxRate(income, province)
        val averageTaxRate = (totalTax.dollars / grossIncome.dollars) * 100
        
        return TaxBreakdown(
            federalTax = Money.fromDollars(federalTax),
            provincialTax = Money.fromDollars(provincialTax),
            cpp = Money.fromDollars(cpp),
            ei = Money.fromDollars(ei),
            totalTax = totalTax,
            afterTaxIncome = afterTaxIncome,
            marginalTaxRate = marginalTaxRate,
            averageTaxRate = averageTaxRate
        )
    }
    
    /**
     * Calculate provincial tax based on province
     */
    private fun calculateProvincialTax(income: Double, province: CanadianProvince): Double {
        return when (province) {
            CanadianProvince.ON -> calculateOntarioTax(income)
            CanadianProvince.BC -> calculateBCTax(income)
            CanadianProvince.AB -> calculateAlbertaTax(income)
            CanadianProvince.QC -> calculateQuebecTax(income)
            else -> calculateOntarioTax(income) // Default to Ontario
        }
    }
    
    /**
     * Ontario provincial tax calculation (2024)
     */
    private fun calculateOntarioTax(income: Double): Double {
        return when {
            income <= 51446 -> income * 0.0505
            income <= 102894 -> 2598.02 + (income - 51446) * 0.0915
            income <= 150000 -> 7300.41 + (income - 102894) * 0.1116
            income <= 220000 -> 12556.77 + (income - 150000) * 0.1216
            else -> 21068.77 + (income - 220000) * 0.1316
        }
    }
    
    /**
     * British Columbia provincial tax calculation (2024)
     */
    private fun calculateBCTax(income: Double): Double {
        return when {
            income <= 47937 -> income * 0.0506
            income <= 95875 -> 2425.61 + (income - 47937) * 0.077
            income <= 110076 -> 6117.53 + (income - 95875) * 0.105
            income <= 133664 -> 7608.64 + (income - 110076) * 0.1229
            income <= 181232 -> 10509.35 + (income - 133664) * 0.147
            else -> 17500.34 + (income - 181232) * 0.168
        }
    }
    
    /**
     * Alberta provincial tax calculation (2024)
     */
    private fun calculateAlbertaTax(income: Double): Double {
        return when {
            income <= 148269 -> income * 0.10
            income <= 177922 -> 14826.90 + (income - 148269) * 0.12
            income <= 237675 -> 18385.26 + (income - 177922) * 0.13
            income <= 355649 -> 26152.15 + (income - 237675) * 0.14
            else -> 42648.51 + (income - 355649) * 0.15
        }
    }
    
    /**
     * Quebec provincial tax calculation (2024)
     */
    private fun calculateQuebecTax(income: Double): Double {
        return when {
            income <= 51780 -> income * 0.14
            income <= 103545 -> 7249.20 + (income - 51780) * 0.19
            income <= 126000 -> 17084.55 + (income - 103545) * 0.24
            else -> 22473.75 + (income - 126000) * 0.2575
        }
    }
    
    /**
     * Calculate marginal tax rate
     */
    fun calculateMarginalTaxRate(income: Double, province: CanadianProvince): Double {
        return when (province) {
            CanadianProvince.ON -> calculateOntarioMarginalRate(income)
            CanadianProvince.BC -> calculateBCMarginalRate(income)
            CanadianProvince.AB -> calculateAlbertaMarginalRate(income)
            CanadianProvince.QC -> calculateQuebecMarginalRate(income)
            else -> calculateOntarioMarginalRate(income)
        }
    }
    
    private fun calculateOntarioMarginalRate(income: Double): Double {
        return when {
            income <= 51446 -> 20.05 // 15% federal + 5.05% provincial
            income <= 55867 -> 24.15 // 15% federal + 9.15% provincial
            income <= 102894 -> 29.65 // 20.5% federal + 9.15% provincial
            income <= 111733 -> 31.16 // 20.5% federal + 11.16% provincial
            income <= 150000 -> 37.16 // 26% federal + 11.16% provincial
            income <= 173205 -> 41.16 // 26% federal + 12.16% provincial
            income <= 220000 -> 43.41 // 29% federal + 12.16% provincial
            else -> 46.16 // 33% federal + 13.16% provincial
        }
    }
    
    private fun calculateBCMarginalRate(income: Double): Double {
        return when {
            income <= 47937 -> 20.06 // 15% federal + 5.06% provincial
            income <= 55867 -> 22.70 // 15% federal + 7.70% provincial
            income <= 95875 -> 28.20 // 20.5% federal + 7.70% provincial
            income <= 110076 -> 30.50 // 20.5% federal + 10.50% provincial
            income <= 111733 -> 32.79 // 20.5% federal + 12.29% provincial
            income <= 133664 -> 38.29 // 26% federal + 12.29% provincial
            income <= 173205 -> 40.70 // 26% federal + 14.70% provincial
            income <= 181232 -> 43.70 // 29% federal + 14.70% provincial
            else -> 49.80 // 33% federal + 16.80% provincial
        }
    }
    
    private fun calculateAlbertaMarginalRate(income: Double): Double {
        return when {
            income <= 55867 -> 25.00 // 15% federal + 10% provincial
            income <= 111733 -> 30.50 // 20.5% federal + 10% provincial
            income <= 148269 -> 36.00 // 26% federal + 10% provincial
            income <= 173205 -> 38.00 // 26% federal + 12% provincial
            income <= 177922 -> 41.00 // 29% federal + 12% provincial
            income <= 237675 -> 42.00 // 29% federal + 13% provincial
            income <= 246752 -> 43.00 // 29% federal + 14% provincial
            income <= 355649 -> 47.00 // 33% federal + 14% provincial
            else -> 48.00 // 33% federal + 15% provincial
        }
    }
    
    private fun calculateQuebecMarginalRate(income: Double): Double {
        return when {
            income <= 51780 -> 29.00 // 15% federal + 14% provincial
            income <= 55867 -> 33.50 // 15% federal + 19% provincial (approx)
            income <= 103545 -> 39.00 // 20.5% federal + 19% provincial (approx)
            income <= 111733 -> 44.00 // 20.5% federal + 24% provincial (approx)
            income <= 126000 -> 50.00 // 26% federal + 24% provincial
            income <= 173205 -> 51.75 // 26% federal + 25.75% provincial
            else -> 54.75 // 29% federal + 25.75% provincial
        }
    }
    
    /**
     * Calculate RRSP contribution analysis
     */
    fun calculateRRSPAnalysis(grossIncome: Money): RRSPAnalysis {
        val income = grossIncome.dollars
        val maxContribution = min(income * 0.18, 31560.0) // 2024 limits
        
        // Assume user has contributed 50% of max so far
        val currentContributions = Money.fromDollars(maxContribution * 0.5)
        val contributionRoom = Money.fromDollars(maxContribution) - currentContributions
        
        // Tax savings calculation - use average marginal rate
        val marginalTaxRate = 0.3116 // Assume middle tax bracket
        val taxSavings = contributionRoom * marginalTaxRate
        
        val recommendedContribution = Money.fromDollars(min(contributionRoom.dollars, income * 0.1))
        
        return RRSPAnalysis(
            currentContributions = currentContributions,
            contributionRoom = contributionRoom,
            maxContribution = Money.fromDollars(maxContribution),
            taxSavings = taxSavings,
            recommendedContribution = recommendedContribution,
            carryForwardRoom = Money.fromDollars(5000.0) // Mock carry-forward room
        )
    }
    
    /**
     * Calculate TFSA contribution analysis
     */
    fun calculateTFSAAnalysis(): TFSAAnalysis {
        val maxContribution = 7000.0 // 2024 limit
        val currentContributions = Money.fromDollars(3500.0) // Assume 50% contributed
        val contributionRoom = Money.fromDollars(maxContribution) - currentContributions
        
        val recommendedContribution = Money.fromDollars(min(contributionRoom.dollars, 2000.0))
        
        return TFSAAnalysis(
            currentContributions = currentContributions,
            contributionRoom = contributionRoom,
            maxContribution = Money.fromDollars(maxContribution),
            recommendedContribution = recommendedContribution,
            withdrawalRoom = Money.fromDollars(10000.0) // Mock withdrawal room
        )
    }
    
    /**
     * Generate tax optimization recommendations
     */
    fun generateTaxRecommendations(
        grossIncome: Money,
        rrspAnalysis: RRSPAnalysis,
        tfsaAnalysis: TFSAAnalysis
    ): List<TaxRecommendation> {
        val recommendations = mutableListOf<TaxRecommendation>()
        
        if (rrspAnalysis.contributionRoom.dollars > 1000) {
            recommendations.add(
                TaxRecommendation(
                    id = "rrsp_contribution",
                    type = TaxRecommendationType.RRSP_CONTRIBUTION,
                    title = "Maximize RRSP contributions",
                    description = "You have ${rrspAnalysis.contributionRoom.format()} in RRSP contribution room. Contributing could save you ${rrspAnalysis.taxSavings.format()} in taxes.",
                    potentialSavings = rrspAnalysis.taxSavings,
                    priority = Priority.HIGH,
                    deadline = LocalDate(2025, 3, 1) // RRSP deadline
                )
            )
        }
        
        if (tfsaAnalysis.contributionRoom.dollars > 500) {
            recommendations.add(
                TaxRecommendation(
                    id = "tfsa_contribution",
                    type = TaxRecommendationType.TFSA_CONTRIBUTION,
                    title = "Maximize TFSA contributions",
                    description = "You have ${tfsaAnalysis.contributionRoom.format()} in TFSA contribution room. TFSA growth is tax-free.",
                    potentialSavings = Money.fromDollars(tfsaAnalysis.contributionRoom.dollars * 0.05), // Assume 5% growth
                    priority = Priority.MEDIUM,
                    deadline = LocalDate(2024, 12, 31)
                )
            )
        }
        
        // Income splitting recommendation for high earners
        if (grossIncome.dollars > 100000) {
            recommendations.add(
                TaxRecommendation(
                    id = "income_splitting",
                    type = TaxRecommendationType.INCOME_SPLITTING,
                    title = "Consider income splitting strategies",
                    description = "With your income level, you may benefit from income splitting strategies with your spouse or family members.",
                    potentialSavings = Money.fromDollars(grossIncome.dollars * 0.02), // Estimate 2% savings
                    priority = Priority.MEDIUM,
                    deadline = LocalDate(2024, 12, 31)
                )
            )
        }
        
        return recommendations
    }
}