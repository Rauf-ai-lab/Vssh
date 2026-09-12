package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.network.providers.ConnectionTestResult
import com.example.data.network.providers.ProviderRegistry
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ApiHubRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.apiConfigDao()

    val allConfigs: Flow<List<ApiConfigEntity>> = dao.getAllConfigs()
    val enabledConfigs: Flow<List<ApiConfigEntity>> = dao.getEnabledConfigs()

    suspend fun initializeDefaultsIfNeeded() {
        val existing = dao.getDefaultConfig()
        if (existing == null) {
            // Check if BuildConfig has a GEMINI_API_KEY injected
            val buildKey = try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
            } catch (e: Exception) {
                ""
            }

            // Seed Google Gemini as default profile
            val geminiConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Google Gemini",
                category = "Chat / LLM",
                providerType = "GEMINI",
                apiKey = buildKey,
                baseUrl = "https://generativelanguage.googleapis.com",
                modelName = "gemini-2.5-flash",
                isEnabled = true,
                isDefault = true,
                status = if (buildKey.isNotBlank()) "CONNECTED" else "UNTESTED",
                supportedCapabilities = "chat,vision,streaming,image_gen"
            )
            dao.insertConfig(geminiConfig)

            // Seed Gemini Image Studio
            val imageStudioConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Gemini Image Studio",
                category = "Image Generation",
                providerType = "GEMINI",
                apiKey = buildKey,
                baseUrl = "https://generativelanguage.googleapis.com",
                modelName = "gemini-2.5-flash-image",
                isEnabled = true,
                isDefault = false,
                status = if (buildKey.isNotBlank()) "CONNECTED" else "UNTESTED",
                supportedCapabilities = "image_gen"
            )
            dao.insertConfig(imageStudioConfig)

            // Seed Groq Cloud template (famous for free tier)
            val groqConfig = ApiConfigEntity(
                id = UUID.randomUUID().toString(),
                name = "Groq Cloud",
                category = "Chat / LLM",
                providerType = "OPENAI_COMPATIBLE",
                apiKey = "",
                baseUrl = "https://api.groq.com/openai/v1",
                modelName = "llama-3.3-70b-versatile",
                isEnabled = false,
                isDefault = false,
                status = "UNTESTED",
                supportedCapabilities = "chat,streaming"
            )
            dao.insertConfig(groqConfig)
        }
    }

    suspend fun getConfigById(id: String): ApiConfigEntity? = dao.getConfigById(id)

    suspend fun getActiveChatConfig(): ApiConfigEntity? {
        return dao.getDefaultConfig()
    }

    suspend fun getImageConfig(): ApiConfigEntity? {
        val specific = dao.getConfigForCategory("Image Generation")
        if (specific != null && specific.apiKey.isNotBlank()) return specific

        // Fallback to active chat config if it supports image generation (e.g. Gemini)
        val defaultCfg = dao.getDefaultConfig()
        if (defaultCfg != null && (defaultCfg.providerType == "GEMINI" || defaultCfg.supportedCapabilities.contains("image_gen"))) {
            return defaultCfg
        }
        return null
    }

    suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> {
        val provider = ProviderRegistry.getProvider(config.providerType)
        return provider.testConnection(config)
    }

    suspend fun saveConfig(config: ApiConfigEntity, performTestFirst: Boolean = true): Result<ApiConfigEntity> {
        var updated = config

        if (performTestFirst && config.apiKey.isNotBlank()) {
            val testResult = testConnection(config)
            testResult.onSuccess { res ->
                updated = config.copy(
                    status = if (res.success) "CONNECTED" else "MODEL_UNAVAILABLE",
                    lastTestedTimestamp = System.currentTimeMillis(),
                    lastLatencyMs = res.latencyMs,
                    lastErrorMessage = if (res.success) null else res.message
                )
            }.onFailure { err ->
                updated = config.copy(
                    status = "ERROR",
                    lastTestedTimestamp = System.currentTimeMillis(),
                    lastErrorMessage = err.message
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
}
