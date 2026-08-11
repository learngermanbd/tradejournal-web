package com.tradejournal.app.integration

import com.tradejournal.app.data.Trade

/** Runtime configuration is injected from a secure build/deployment environment. */
data class BackendConfig(
    val baseUrl: String,
    val publicKey: String,
    val enabled: Boolean = baseUrl.isNotBlank() && publicKey.isNotBlank(),
)

data class AuthSession(
    val userId: String,
    val accessToken: String,
    val isAdmin: Boolean,
    val expiresAtEpochSeconds: Long,
)

interface AuthGateway {
    suspend fun signIn(email: String, password: String): Result<AuthSession>
    suspend fun signOut(): Result<Unit>
    suspend fun currentSession(): AuthSession?
}

enum class SyncState { DISABLED, READY, SYNCING, SYNCED, CONFLICT, ERROR }

data class VaultSyncStatus(
    val state: SyncState = SyncState.DISABLED,
    val lastSyncedAt: Long? = null,
    val remoteVersion: String? = null,
    val message: String? = null,
)

interface DriveVaultGateway {
    suspend fun connect(): Result<Unit>
    suspend fun uploadEncryptedVault(payload: ByteArray, version: String): Result<VaultSyncStatus>
    suspend fun downloadEncryptedVault(): Result<ByteArray?>
    suspend fun disconnect(): Result<Unit>
}

data class BrokerConnection(
    val id: String,
    val provider: String,
    val displayName: String,
    val readOnly: Boolean = true,
)

data class BrokerImportPreview(
    val provider: String,
    val trades: List<Trade>,
    val duplicateCount: Int,
    val warnings: List<String> = emptyList(),
)

interface BrokerGateway {
    suspend fun listConnections(): Result<List<BrokerConnection>>
    suspend fun previewReadOnlyImport(connectionId: String): Result<BrokerImportPreview>
    suspend fun disconnect(connectionId: String): Result<Unit>
}

data class AiConsent(
    val enabled: Boolean = false,
    val allowedFields: Set<String> = emptySet(),
    val expiresAt: Long? = null,
)

data class AiInsight(
    val title: String,
    val summary: String,
    val isFinancialAdvice: Boolean = false,
)

interface AiGateway {
    suspend fun analyzeLocally(trades: List<Trade>): List<AiInsight>
    suspend fun analyzeInCloud(trades: List<Trade>, consent: AiConsent): Result<List<AiInsight>>
}

interface CrashReporter {
    fun setUserReference(anonymousId: String?)
    fun recordNonSensitiveEvent(name: String, attributes: Map<String, String> = emptyMap())
    fun recordException(operation: String, throwable: Throwable)
}

enum class Plan { FREE, PREMIUM }

data class Entitlement(
    val plan: Plan = Plan.FREE,
    val active: Boolean = false,
    val expiresAt: Long? = null,
)

interface BillingGateway {
    suspend fun currentEntitlement(): Result<Entitlement>
    suspend fun restorePurchases(): Result<Entitlement>
}
