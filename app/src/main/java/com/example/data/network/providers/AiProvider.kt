package com.example.data.network.providers

import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
    val availableModels: List<String> = emptyList()
)

data class ChatResponse(
    val text: String,
    val spokenText: String,
    val cardType: String? = null,
    val cardJson: String? = null
)

data class ImageResult(
    val imageUrl: String? = null,
    val base64Data: String? = null,
    val mimeType: String = "image/png"
)

interface AiProvider {
    suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult>
    suspend fun verifyModel(config: ApiConfigEntity): Result<Boolean>
    suspend fun generateChat(
        config: ApiConfigEntity,
        messages: List<ChatMessageEntity>,
        memoryPrompt: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Result<ChatResponse>
    suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult>
}
