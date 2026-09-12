package com.example.data.network.providers

data class ProviderTemplate(
    val name: String,
    val category: String, // "Chat / LLM", "Image Generation", "Voice", "Vision"
    val providerType: String, // "GEMINI", "OPENAI_COMPATIBLE"
    val defaultBaseUrl: String,
    val defaultModel: String,
    val supportedModels: List<String>,
    val apiKeyUrl: String,
    val hasFreeTier: Boolean,
    val freeTierLabel: String,
    val description: String
)

object ProviderRegistry {

    private val geminiProvider = GeminiProvider()
    private val openAiProvider = OpenAiCompatibleProvider()

    fun getProvider(providerType: String): AiProvider {
        return when (providerType.uppercase()) {
            "GEMINI" -> geminiProvider
            else -> openAiProvider
        }
    }

    val templates: List<ProviderTemplate> = listOf(
        ProviderTemplate(
            name = "Google Gemini",
            category = "Chat / LLM",
            providerType = "GEMINI",
            defaultBaseUrl = "https://generativelanguage.googleapis.com",
            defaultModel = "gemini-2.5-flash",
            supportedModels = listOf(
                "gemini-2.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-2.5-flash-image"
            ),
            apiKeyUrl = "https://aistudio.google.com/app/apikey",
            hasFreeTier = true,
            freeTierLabel = "Get Free Gemini API Key",
            description = "Google AI Studio with high rate limits, multimodal vision, and generous free tier."
        ),
        ProviderTemplate(
            name = "Groq Cloud",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            supportedModels = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768",
                "gemma2-9b-it"
            ),
            apiKeyUrl = "https://console.groq.com/keys",
            hasFreeTier = true,
            freeTierLabel = "Get Free Groq API Key",
            description = "Ultra-fast inference on LPUs with open-source Meta Llama & Mistral."
        ),
        ProviderTemplate(
            name = "OpenAI",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            supportedModels = listOf(
                "gpt-4o-mini",
                "gpt-4o",
                "o3-mini",
                "dall-e-3"
            ),
            apiKeyUrl = "https://platform.openai.com/api-keys",
            hasFreeTier = false,
            freeTierLabel = "Get OpenAI API Key",
            description = "General reasoning, coding, and DALL-E image generation."
        ),
        ProviderTemplate(
            name = "DeepSeek",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-chat",
            supportedModels = listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            ),
            apiKeyUrl = "https://platform.deepseek.com/api_keys",
            hasFreeTier = true,
            freeTierLabel = "Get DeepSeek API Key",
            description = "Competitive open-weights reasoning model with low-cost API."
        ),
        ProviderTemplate(
            name = "Local Ollama / Custom",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "http://localhost:11434/v1",
            defaultModel = "llama3.2",
            supportedModels = listOf(
                "llama3.2",
                "mistral",
                "phi3",
                "qwen2.5"
            ),
            apiKeyUrl = "https://ollama.com",
            hasFreeTier = true,
            freeTierLabel = "Ollama Setup Guide",
            description = "Run open-source models completely locally or through custom reverse proxies."
        ),
        ProviderTemplate(
            name = "Gemini Image Studio",
            category = "Image Generation",
            providerType = "GEMINI",
            defaultBaseUrl = "https://generativelanguage.googleapis.com",
            defaultModel = "gemini-2.5-flash-image",
            supportedModels = listOf(
                "gemini-2.5-flash-image",
                "gemini-3.1-flash-image-preview"
            ),
            apiKeyUrl = "https://aistudio.google.com/app/apikey",
            hasFreeTier = true,
            freeTierLabel = "Get Free Image API Key",
            description = "Native image generation with aspect ratio control."
        )
    )
}
