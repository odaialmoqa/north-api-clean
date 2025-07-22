package com.north.mobile.data.privacy

import com.north.mobile.data.repository.PrivacyRepository
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of DataExportManager for PIPEDA compliance
 */
class DataExportManagerImpl(
    private val repository: PrivacyRepository,
    private val auditLogger: AuditLogger,
    private val json: Json = Json { prettyPrint = true }
) : DataExportManager {
    
    override suspend fun requestDataExport(userId: String, format: ExportFormat): DataExportResult {
        return try {
            val exportId = generateExportId()
            val now = Clock.System.now()
            
            val exportRequest = DataExportRequest(
                id = exportId,
                userId = userId,
                format = format,
                requestedAt = now,
                status = ExportStatus.PENDING,
                expiresAt = now + 7.days // Download link expires in 7 days
            )
            
            repository.insertDataExportRequest(exportRequest)
            
            // Log the export request
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = userId,
                    eventType = AuditEventType.DATA_EXPORT_REQUESTED,
                    details = mapOf(
                        "export_id" to exportId,
                        "format" to format.name
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
            
            // Start async export processing
            processDataExport(exportRequest)
            
            DataExportResult.Success(exportId)
        } catch (e: Exception) {
            DataExportResult.Error("Failed to request data export", e)
        }
    }
    
    override suspend fun getExportStatus(exportId: String): ExportStatus {
        return repository.getDataExportRequest(exportId)?.status ?: ExportStatus.FAILED
    }
    
    override suspend fun downloadExport(exportId: String): ByteArray {
        val exportRequest = repository.getDataExportRequest(exportId)
            ?: throw IllegalArgumentException("Export request not found")
        
        if (exportRequest.status != ExportStatus.COMPLETED) {
            throw IllegalStateException("Export not completed")
        }
        
        if (exportRequest.expiresAt != null && exportRequest.expiresAt < Clock.System.now()) {
            throw IllegalStateException("Export download link has expired")
        }
        
        // Log the download
        auditLogger.logPrivacyEvent(
            PrivacyEvent(
                userId = exportRequest.userId,
                eventType = AuditEventType.DATA_EXPORT_DOWNLOADED,
                details = mapOf(
                    "export_id" to exportId,
                    "format" to exportRequest.format.name,
                    "file_size" to (exportRequest.fileSize ?: 0)
                ),
                ipAddress = null,
                userAgent = null,
                sessionId = null
            )
        )
        
        return repository.getExportData(exportId)
    }
    
    override suspend fun cancelExport(exportId: String): Boolean {
        return try {
            val exportRequest = repository.getDataExportRequest(exportId)
                ?: return false
            
            if (exportRequest.status in listOf(ExportStatus.PENDING, ExportStatus.PROCESSING)) {
                repository.updateExportStatus(exportId, ExportStatus.CANCELLED)
                
                auditLogger.logPrivacyEvent(
                    PrivacyEvent(
                        userId = exportRequest.userId,
                        eventType = AuditEventType.DATA_EXPORT_REQUESTED,
                        details = mapOf(
                            "export_id" to exportId,
                            "action" to "cancelled"
                        ),
                        ipAddress = null,
                        userAgent = null,
                        sessionId = null
                    )
                )
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getExportHistory(userId: String): List<DataExportRequest> {
        return repository.getDataExportHistory(userId)
    }
    
    private suspend fun processDataExport(exportRequest: DataExportRequest) {
        try {
            repository.updateExportStatus(exportRequest.id, ExportStatus.PROCESSING)
            
            // Gather all user data
            val userDataExport = gatherUserData(exportRequest.userId)
            
            // Convert to requested format
            val exportData = when (exportRequest.format) {
                ExportFormat.JSON -> json.encodeToString(userDataExport).toByteArray()
                ExportFormat.CSV -> convertToCSV(userDataExport)
                ExportFormat.PDF -> convertToPDF(userDataExport)
            }
            
            // Store the export data
            repository.storeExportData(exportRequest.id, exportData)
            
            // Update request status
            repository.updateExportRequest(
                exportRequest.copy(
                    status = ExportStatus.COMPLETED,
                    completedAt = Clock.System.now(),
                    fileSize = exportData.size.toLong()
                )
            )
            
        } catch (e: Exception) {
            repository.updateExportStatus(exportRequest.id, ExportStatus.FAILED)
            
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = exportRequest.userId,
                    eventType = AuditEventType.DATA_EXPORT_REQUESTED,
                    details = mapOf(
                        "export_id" to exportRequest.id,
                        "error" to e.message.orEmpty()
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
        }
    }
    
    private suspend fun gatherUserData(userId: String): UserDataExport {
        val now = Clock.System.now()
        
        return UserDataExport(
            userId = userId,
            exportedAt = now,
            privacyPolicyVersion = "2025.1.0",
            profile = repository.getUserProfile(userId),
            accounts = repository.getUserAccounts(userId),
            transactions = repository.getUserTransactions(userId),
            goals = repository.getUserGoals(userId),
            gamification = repository.getUserGamification(userId),
            consents = repository.getConsentHistory(userId),
            auditLog = auditLogger.getAuditLogs(userId)
        )
    }
    
    private fun convertToCSV(userDataExport: UserDataExport): ByteArray {
        val csv = StringBuilder()
        
        // Profile section
        csv.appendLine("PROFILE")
        csv.appendLine("ID,Email,Created At,Last Login")
        csv.appendLine("${userDataExport.profile.id},${userDataExport.profile.email},${userDataExport.profile.createdAt},${userDataExport.profile.lastLoginAt}")
        csv.appendLine()
        
        // Accounts section
        csv.appendLine("ACCOUNTS")
        csv.appendLine("ID,Institution,Type,Currency,Created At,Last Sync")
        userDataExport.accounts.forEach { account ->
            csv.appendLine("${account.id},${account.institutionName},${account.accountType},${account.currency},${account.createdAt},${account.lastSyncAt}")
        }
        csv.appendLine()
        
        // Transactions section
        csv.appendLine("TRANSACTIONS")
        csv.appendLine("ID,Account ID,Amount,Description,Category,Date,Created At")
        userDataExport.transactions.forEach { transaction ->
            csv.appendLine("${transaction.id},${transaction.accountId},${transaction.amount},\"${transaction.description}\",${transaction.category},${transaction.date},${transaction.createdAt}")
        }
        
        return csv.toString().toByteArray()
    }
    
    private fun convertToPDF(userDataExport: UserDataExport): ByteArray {
        // This would typically use a PDF library like iText or similar
        // For now, return a simple text representation
        val content = "NORTH MOBILE APP - USER DATA EXPORT\n\n" +
                "Export Date: ${userDataExport.exportedAt}\n" +
                "User ID: ${userDataExport.userId}\n" +
                "Privacy Policy Version: ${userDataExport.privacyPolicyVersion}\n\n" +
                "This export contains all your personal data stored in the North mobile app.\n" +
                "For detailed information, please request a JSON or CSV export."
        
        return content.toByteArray()
    }
    
    private fun generateExportId(): String {
        return "export_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}