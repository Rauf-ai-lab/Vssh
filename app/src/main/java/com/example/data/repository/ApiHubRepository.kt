package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.data.network.providers.ConnectionTestResult
import com.example.data.network.providers.ModelVerificationResult
import com.example.data.network.providers.ProviderRegistry
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ApiHubRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.apiConfigDao()
    val modelRegistry = ModelRegistryRepository(context)

    val allConfigs: Flow<List<ApiConfigEntity>> = dao.getAllConfigs()
    val enabledConfigs: Flow<List<ApiConfigEntity>> = dao.getEnabledConfigs()

    suspend fun initializeDefaultsIfNeeded() {
        val existing = dao.getDefaultConfig()
        if (existing == null) {
            val buildKey = try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
            } catch (e: Exception) {
                ""
            }

            // Google Gemini
            val geminiConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Google Gemini",
                category = "Chat / LLM",
                providerType = "GEMINI",
                apiKey = buildKey,
                baseUrl = "https://generativelanguage.googleapis.com",
                modelName = "", // Will be dynamically discovered
                isEnabled = true,
                isDefault = true,
                status = if (buildKey.isNotBlank()) "UNTESTED" else "UNTESTED",
                supportedCapabilities = "chat,vision,streaming,image_gen"
            )
            dao.insertConfig(geminiConfig)

            // Seed Gemini Image Studio profile
            val imageStudioConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Gemini Image Studio",
                category = "Image Generation",
                providerType = "GEMINI",
                apiKey = buildKey,
                baseUrl = "https://generativelanguage.googleapis.com",
                modelName = "", // Will be dynamically discovered
                isEnabled = true,
                isDefault = false,
                status = "UNTESTED",
                supportedCapabilities = "image_gen"
            )
            dao.insertConfig(imageStudioConfig)

            // Seed Groq Cloud template (famous for free tier LPUs)
            val groqConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Groq Cloud",
                category = "Chat / LLM",
                providerType = "OPENAI_COMPATIBLE",
                apiKey = "",
                baseUrl = "https://api.groq.com/openai/v1",
                modelName = "", // Will be dynamically discovered
                isEnabled = false,
                isDefault = false,
                status = "UNTESTED",
                supportedCapabilities = "chat,streaming"
            )
            dao.insertConfig(groqConfig)

            // If API key is already present in build configuration, trigger initial dynamic discovery
            if (buildKey.isNotBlank()) {
                refreshAndAutoSelectModel(geminiConfig, capability = "chat")
                refreshAndAutoSelectModel(imageStudioConfig, capability = "image_gen")
            }
        }
    }

    suspend fun refreshAndAutoSelectModel(
        config: ApiConfigEntity,
        capability: String = "chat"
    ): Result<ApiConfigEntity> {
        val discoveryResult = modelRegistry.discoverAndRegisterModels(config)
        if (discoveryResult.isFailure) {
            val err = discoveryResult.exceptionOrNull()
            return Result.failure(err ?: Exception("Model discovery failed"))
        }

        // Auto-select latest eligible model if not set or if current is unavailable
        val best = modelRegistry.selectBestEligibleModel(config.providerType, capability, preferFreeTier = true)
        val selectedModelId = best?.modelId ?: config.modelName

        // Verify the chosen model
        var finalStatus = "CONNECTED"
        var lastErr: String? = null
        var latency = 0L

        if (selectedModelId.isNotBlank()) {
            val verifyRes = modelRegistry.verifySingleModel(config, selectedModelId, capability)
            if (verifyRes.isSuccess) {
                val v = verifyRes.getOrNull()!!
                latency = v.latencyMs
                if (!v.verified) {
                    finalStatus = if (v.is404) "MODEL_UNAVAILABLE" else "ERROR"
                    lastErr = v.message
                }
            } else {
                finalStatus = "ERROR"
                lastErr = verifyRes.exceptionOrNull()?.message
            }
        }

        val updated = config.copy(
            modelName = selectedModelId,
            status = finalStatus,
            lastTestedTimestamp = System.currentTimeMillis(),
            lastLatencyMs = latency,
            lastErrorMessage = lastErr
        )
        dao.insertConfig(updated)
        return Result.success(updated)
    }

    suspend fun verifyActiveModel(config: ApiConfigEntity): Result<ModelVerificationResult> {
        if (config.modelName.isBlank()) {
            return Result.failure(IllegalArgumentException("No model selected to verify."))
        }
        val capability = if (config.category.contains("Image")) "image_gen" else "chat"
        return modelRegistry.verifySingleModel(config, config.modelName, capability)
    }

    suspend fun getConfigById(id: String): ApiConfigEntity? = dao.getConfigById(id)

    suspend fun getActiveChatConfig(): ApiConfigEntity? {
        val defaultCfg = dao.getDefaultConfig() ?: return null
        if (defaultCfg.modelName.isBlank() && defaultCfg.apiKey.isNotBlank()) {
            // Dynamically discover and select model
            val updated = refreshAndAutoSelectModel(defaultCfg, "chat").getOrNull()
            if (updated != null) return updated
        }
        return defaultCfg
    }

    suspend fun getImageConfig(): ApiConfigEntity? {
        val specific = dao.getConfigForCategory("Image Generation")
        if (specific != null && specific.apiKey.isNotBlank()) {
            if (specific.modelName.isBlank()) {
                refreshAndAutoSelectModel(specific, "image_gen")
                return dao.getConfigById(specific.id)
            }
            return specific
        }

        val defaultCfg = dao.getDefaultConfig()
        if (defaultCfg != null && (defaultCfg.providerType == "GEMINI" || defaultCfg.supportedCapabilities.contains("image_gen"))) {
            return defaultCfg
        }
        return null
    }

    suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> {
        val provider = ProviderRegistry.getProvider(config.providerType)
        // 1. Discover models
        val testRes = provider.testConnection(config)

        testRes.onSuccess { res ->
            // Store discovered models into registry
            modelRegistry.discoverAndRegisterModels(config)
        }
        return testRes
    }

    suspend fun saveConfig(config: ApiConfigEntity, performTestFirst: Boolean = true): Result<ApiConfigEntity> {
        var updated = config

        if (performTestFirst && config.apiKey.isNotBlank()) {
            val refreshResult = refreshAndAutoSelectModel(
                config,
                capability = if (config.category.contains("Image")) "image_gen" else "chat"
            )
            if (refreshResult.isSuccess) {
                updated = refreshResult.getOrNull() ?: updated
            } else {
                updated = updated.copy(
                    status = "ERROR",
                    lastTestedTimestamp = System.currentTimeMillis(),
                    lastErrorMessage = refreshResult.exceptionOrNull()?.message
                )
            }
        }

        dao.insertConfig(updated)
        return Result.success(updated)
    }

    suspend fun setDefault(id: String) {
        dao.setDefault(id)
    }

    suspend fun toggleEnabled(id: String, isEnabled: Boolean) {
        val config = dao.getConfigById(id) ?: return
        dao.updateConfig(config.copy(isEnabled = isEnabled))
    }

    suspend fun deleteConfig(id: String) {
        dao.deleteConfig(id)
    }

    suspend fun markModelUnavailable(providerType: String, modelId: String) {
        modelRegistry.markModelUnavailable(providerType, modelId)
    }

    suspend fun getDiscoveredModels(providerType: String): Flow<List<DiscoveredModelEntity>> {
        return modelRegistry.getModelsForProvider(providerType)
    }
}
