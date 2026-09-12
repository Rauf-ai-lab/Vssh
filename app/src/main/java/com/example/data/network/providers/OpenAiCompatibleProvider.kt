package com.example.data.network.providers

import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.network.HttpClientFactory
import com.example.data.network.SpeechCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleProvider : AiProvider {

    private val client = HttpClientFactory.okHttpClient
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = config.apiKey.trim()
            val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
            val baseUrl = rawBase.trimEnd('/')

            try {
                val reqBuilder = Request.Builder()
                    .url("$baseUrl/models")
                    .get()

                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
                if (!config.organizationId.isNullOrBlank()) {
                    reqBuilder.addHeader("OpenAI-Organization", config.organizationId)
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val body = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val errMsg = parseError(body, response.code)
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val json = JSONObject(body)
                    val dataArr = json.optJSONArray("data") ?: JSONArray()
                    val modelsList = mutableListOf<String>()
                    for (i in 0 until dataArr.length()) {
                        val m = dataArr.getJSONObject(i)
                        modelsList.add(m.optString("id"))
                    }

                    val requestedModel = config.modelName.trim()
                    val modelFound = requestedModel.isBlank() || modelsList.any { it.equals(requestedModel, ignoreCase = true) }

                    if (!modelFound && modelsList.isNotEmpty()) {
                        return@withContext Result.success(
                            ConnectionTestResult(
                                success = false,
                                latencyMs = latency,
                                message = "Connected to endpoint, but model '$requestedModel' is unavailable. Choose an available model from the list.",
                                availableModels = modelsList
                            )
                        )
                    }

                    Result.success(
                        ConnectionTestResult(
                            success = true,
                            latencyMs = latency,
                            message = "Connected successfully. ${modelsList.size} models verified.",
                            availableModels = modelsList
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(Exception("Connection failed to $baseUrl: ${e.localizedMessage ?: e.message}"))
            }
        }

    override suspend fun verifyModel(config: ApiConfigEntity): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val test = testConnection(config)
            test.map { it.success }
        }

    override suspend fun generateChat(
        config: ApiConfigEntity,
        messages: List<ChatMessageEntity>,
        memoryPrompt: String?,
        onChunk: ((String) -> Unit)?
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
        val baseUrl = rawBase.trimEnd('/')
        val model = config.modelName.trim().ifEmpty { "gpt-4o-mini" }

        try {
            val payload = JSONObject()
            payload.put("model", model)

            val messagesArr = JSONArray()

            // System prompt
            val systemContent = StringBuilder("You are Nova, an intelligent personal AI assistant. Be helpful, concise, thoughtful, and highly capable.")
            if (!memoryPrompt.isNullOrBlank()) {
                systemContent.append("\n\nUser's Long-Term Memory & Preferences:\n").append(memoryPrompt)
            }
            messagesArr.put(JSONObject().put("role", "system").put("content", systemContent.toString()))

            // User & Assistant history
            val relevant = messages.takeLast(12)
            for (msg in relevant) {
                if (msg.role == "error") continue
                val role = if (msg.role == "assistant") "assistant" else "user"

                if (msg.role == "user" && !msg.mediaUri.isNullOrBlank() && msg.mediaType?.startsWith("image") == true) {
                    // Vision multimodal message format
                    val contentParts = JSONArray()
                    contentParts.put(JSONObject().put("type", "text").put("text", msg.content))
                    val imgObj = JSONObject().put("url", "data:image/jpeg;base64,${msg.mediaUri.substringAfter("base64,")}")
                    contentParts.put(JSONObject().put("type", "image_url").put("image_url", imgObj))
                    messagesArr.put(JSONObject().put("role", role).put("content", contentParts))
                } else {
                    messagesArr.put(JSONObject().put("role", role).put("content", msg.content))
                }
            }

            payload.put("messages", messagesArr)
            payload.put("temperature", 0.7)

            val reqBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            if (!config.organizationId.isNullOrBlank()) {
                reqBuilder.addHeader("OpenAI-Organization", config.organizationId)
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: JSONArray()
                if (choices.length() == 0) {
                    return@withContext Result.failure(Exception("Empty response from AI provider."))
                }

                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                val text = messageObj?.optString("content").orEmpty().ifBlank { "I received your message." }
                val cleanSpeech = SpeechCleaner.cleanForSpeech(text)

                onChunk?.invoke(text)

                Result.success(
                    ChatResponse(
                        text = text,
                        spokenText = cleanSpeech
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Chat request failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
        val baseUrl = rawBase.trimEnd('/')

        try {
            val payload = JSONObject()
            payload.put("prompt", prompt)
            payload.put("model", config.modelName.ifBlank { "dall-e-3" })
            payload.put("n", 1)
            payload.put("size", "1024x1024")
            payload.put("response_format", "b64_json")

            val reqBuilder = Request.Builder()
                .url("$baseUrl/images/generations")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: JSONArray()
                if (data.length() == 0) {
                    return@withContext Result.failure(Exception("No image returned by provider."))
                }

                val first = data.getJSONObject(0)
                val b64 = first.optString("b64_json")
                val url = first.optString("url")

                if (b64.isNotBlank()) {
                    Result.success(ImageResult(base64Data = b64, mimeType = "image/png"))
                } else if (url.isNotBlank()) {
                    Result.success(ImageResult(imageUrl = url, mimeType = "image/png"))
                } else {
                    Result.failure(Exception("Image data was missing from provider response."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Image generation failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseError(body: String, statusCode: Int): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: body
            when (statusCode) {
                401 -> "Invalid or expired API key. Please check your credentials in the API Hub."
                404 -> "Model unavailable or endpoint not found ($statusCode): $message"
                429 -> "Rate limit reached (429): Quota exceeded or too many requests."
                500, 502, 503 -> "Server error ($statusCode): The provider service encountered an error."
                else -> "Provider error ($statusCode): $message"
            }
        } catch (e: Exception) {
            "API error ($statusCode): ${body.take(200)}"
        }
    }
}
