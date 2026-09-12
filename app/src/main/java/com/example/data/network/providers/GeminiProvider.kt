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

class GeminiProvider : AiProvider {

    private val client = HttpClientFactory.okHttpClient
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty. Please enter your API key."))
            }

            val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
            val url = "$baseUrl/v1beta/models?key=$apiKey"

            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val body = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val errMsg = parseError(body, response.code)
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val json = JSONObject(body)
                    val modelsArray = json.optJSONArray("models") ?: JSONArray()
                    val available = mutableListOf<String>()
                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val name = m.optString("name").removePrefix("models/")
                        available.add(name)
                    }

                    // Check if selected model is available
                    val requestedModel = config.modelName.trim().removePrefix("models/")
                    val modelFound = requestedModel.isBlank() || available.any { it.equals(requestedModel, ignoreCase = true) }

                    if (!modelFound) {
                        return@withContext Result.success(
                            ConnectionTestResult(
                                success = false,
                                latencyMs = latency,
                                message = "Connected to Google AI Studio, but model '$requestedModel' was not found in your available models list.",
                                availableModels = available
                            )
                        )
                    }

                    Result.success(
                        ConnectionTestResult(
                            success = true,
                            latencyMs = latency,
                            message = "Connected successfully. ${available.size} models verified.",
                            availableModels = available
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error while connecting to Gemini: ${e.localizedMessage ?: e.message}"))
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
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing. Please configure it in API Hub."))
        }

        val rawModel = if (config.modelName.isNotBlank()) config.modelName.trim() else "gemini-2.5-flash"
        val model = rawModel.removePrefix("models/")
        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
        val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val payload = JSONObject()

            // System instructions (persona & user memory preferences)
            val systemParts = mutableListOf<String>()
            systemParts.add("You are Nova, an intelligent, modern, versatile AI assistant. Be helpful, concise, thoughtful, and highly capable.")
            if (!memoryPrompt.isNullOrBlank()) {
                systemParts.add("User's Long-Term Memory & Preferences:\n$memoryPrompt")
            }
            val sysInstructionObj = JSONObject()
            val sysPartsArr = JSONArray()
            sysPartsArr.put(JSONObject().put("text", systemParts.joinToString("\n\n")))
            sysInstructionObj.put("parts", sysPartsArr)
            payload.put("systemInstruction", sysInstructionObj)

            // Build contents history
            val contentsArr = JSONArray()
            val relevantMessages = messages.takeLast(12) // sliding context window
            for (msg in relevantMessages) {
                if (msg.role == "error" || msg.role == "system") continue
                val role = if (msg.role == "user") "user" else "model"
                val contentObj = JSONObject().put("role", role)
                val partsArr = JSONArray()

                // If media attached (image Base64)
                if (msg.role == "user" && !msg.mediaUri.isNullOrBlank() && msg.mediaType?.startsWith("image") == true) {
                    val base64Data = msg.mediaUri.substringAfter("base64,", msg.mediaUri)
                    val mime = if (msg.mediaUri.contains("image/png")) "image/png" else "image/jpeg"
                    val inlineDataObj = JSONObject()
                        .put("mimeType", mime)
                        .put("data", base64Data)
                    partsArr.put(JSONObject().put("inlineData", inlineDataObj))
                }

                partsArr.put(JSONObject().put("text", msg.content))
                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
            }
            payload.put("contents", contentsArr)

            // Generation config
            val genConfig = JSONObject()
                .put("temperature", 0.7)
                .put("topP", 0.95)
            payload.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No response generated by model. The content may have triggered safety filters."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotEmpty()) {
                            textBuilder.append(text)
                            onChunk?.invoke(text)
                        }
                    }
                }

                val fullText = textBuilder.toString().ifBlank { "I received your message." }
                val cleanSpeech = SpeechCleaner.cleanForSpeech(fullText)

                Result.success(
                    ChatResponse(
                        text = fullText,
                        spokenText = cleanSpeech
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gemini generation failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is not configured for image generation."))
        }

        val rawModel = if (config.modelName.isNotBlank() && config.modelName.contains("image")) {
            config.modelName.trim()
        } else {
            "gemini-2.5-flash-image"
        }
        val model = rawModel.removePrefix("models/")
        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
        val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val payload = JSONObject()
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray().put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            payload.put("contents", contentsArr)

            val modalities = JSONArray().put("IMAGE").put("TEXT")
            val imageConfig = JSONObject()
                .put("aspectRatio", if (aspectRatio.isNotBlank()) aspectRatio else "1:1")
                .put("imageSize", if (size.isNotBlank()) size else "1K")

            val genConfig = JSONObject()
                .put("responseModalities", modalities)
                .put("imageConfig", imageConfig)
            payload.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates") ?: JSONArray()
                if (candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Image generation returned no candidates."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: JSONArray()

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val mime = inlineData.optString("mimeType", "image/png")
                        val data = inlineData.optString("data")
                        if (data.isNotBlank()) {
                            return@withContext Result.success(
                                ImageResult(
                                    base64Data = data,
                                    mimeType = mime
                                )
                            )
                        }
                    }
                }

                Result.failure(Exception("Model did not return image data in the response."))
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
            val status = errorObj?.optString("status") ?: ""
            when (statusCode) {
                400 -> "Bad Request: $message"
                401, 403 -> "Authentication failed ($statusCode): Invalid or unauthorized API key. Check your key in API Hub."
                404 -> "Model not found (404): The requested model does not exist or is deprecated."
                429 -> "Rate limit reached (429): Too many requests or quota exhausted."
                500, 503 -> "Server error ($statusCode): Provider service temporarily unavailable."
                else -> "Error ($statusCode): $message"
            }
        } catch (e: Exception) {
            "API error ($statusCode): ${body.take(200)}"
        }
    }
}
