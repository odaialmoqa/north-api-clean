package com.north.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FinancialInstitution(
    val id: String,
    val name: String,
    val displayName: String,
    val logo: String? = null,
    val primaryColor: String? = null,
    val url: String? = null,
    val country: String = "CA",
    val products: List<PlaidProduct> = emptyList(),
    val routingNumbers: List<String> = emptyList(),
    val oauth: Boolean = false,
    val status: InstitutionStatus = InstitutionStatus.SUPPORTED
)

@Serializable
enum class PlaidProduct {
    TRANSACTIONS,
    AUTH,
    IDENTITY,
    ASSETS,
    LIABILITIES,
    INVESTMENTS
}

@Serializable
enum class InstitutionStatus {
    SUPPORTED,
    BETA,
    DEGRADED,
    DOWN
}

/**
 * Major Canadian financial institutions supported by Plaid
 */
object CanadianInstitutions {
    val RBC = FinancialInstitution(
        id = "ins_3",
        name = "rbc",
        displayName = "RBC Royal Bank",
        primaryColor = "#005DAA",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val TD = FinancialInstitution(
        id = "ins_5",
        name = "td",
        displayName = "TD Canada Trust",
        primaryColor = "#00B04F",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val BMO = FinancialInstitution(
        id = "ins_4",
        name = "bmo",
        displayName = "Bank of Montreal",
        primaryColor = "#0066CC",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val SCOTIABANK = FinancialInstitution(
        id = "ins_6",
        name = "scotiabank",
        displayName = "Scotiabank",
        primaryColor = "#E31837",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val CIBC = FinancialInstitution(
        id = "ins_7",
        name = "cibc",
        displayName = "CIBC",
        primaryColor = "#ED1C24",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val NATIONAL_BANK = FinancialInstitution(
        id = "ins_8",
        name = "national_bank",
        displayName = "National Bank of Canada",
        primaryColor = "#005A8B",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val TANGERINE = FinancialInstitution(
        id = "ins_9",
        name = "tangerine",
        displayName = "Tangerine",
        primaryColor = "#FF6900",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val PC_FINANCIAL = FinancialInstitution(
        id = "ins_10",
        name = "pc_financial",
        displayName = "President's Choice Financial",
        primaryColor = "#D71921",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    val DESJARDINS = FinancialInstitution(
        id = "ins_11",
        name = "desjardins",
        displayName = "Desjardins",
        primaryColor = "#00A651",
        oauth = true,
        products = listOf(PlaidProduct.TRANSACTIONS, PlaidProduct.AUTH, PlaidProduct.IDENTITY)
    )
    
    /**
     * List of major Canadian banks (Big 6 + popular alternatives)
     */
    val MAJOR_CANADIAN_BANKS = listOf(
        RBC, TD, BMO, SCOTIABANK, CIBC, NATIONAL_BANK,
        TANGERINE, PC_FINANCIAL, DESJARDINS
    )
    
    /**
     * Get institution by ID
     */
    fun getById(id: String): FinancialInstitution? {
        return MAJOR_CANADIAN_BANKS.find { it.id == id }
    }
    
    /**
     * Search institutions by name
     */
    fun searchByName(query: String): List<FinancialInstitution> {
        val lowercaseQuery = query.lowercase()
        return MAJOR_CANADIAN_BANKS.filter { 
            it.displayName.lowercase().contains(lowercaseQuery) ||
            it.name.lowercase().contains(lowercaseQuery)
        }
    }
}