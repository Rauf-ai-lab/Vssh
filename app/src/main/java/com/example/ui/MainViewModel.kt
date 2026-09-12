package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.data.local.entity.MemoryEntity
import com.example.service.NetworkMonitor
import com.example.service.SpeechService
import com.example.data.network.providers.ConnectionTestResult
import com.example.data.network.providers.ImageResult
import com.example.data.network.providers.ModelVerificationResult
import com.example.data.network.providers.ProviderRegistry
import com.example.data.repository.ApiHubRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

enum class AppTab {
    CHAT,
    STUDIO,
    HISTORY,
    SETTINGS,
    API_HUB,
    LIVE_VOICE
}

data class StudioImageState(
    val prompt: String = "",
    val aspectRatio: String = "1:1",
    val style: String = "Photorealistic",
    val isGenerating: Boolean = false,
    val result: ImageResult? = null,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val apiHubRepository = ApiHubRepository(application)
    val memoryRepository = MemoryRepository(application)
    val chatRepository = ChatRepository(application, apiHubRepository, memoryRepository)
    val speechService = SpeechService(application)
    private val networkMonitor = NetworkMonitor(application)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allConfigs: StateFlow<List<ApiConfigEntity>> = apiHubRepository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledConfigs: StateFlow<List<ApiConfigEntity>> = apiHubRepository.enabledConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = memoryRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiscoveredModels: StateFlow<List<DiscoveredModelEntity>> = apiHubRepository.modelRegistry.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentMessages.asStateFlow()

    private val _selectedModelConfig = MutableStateFlow<ApiConfigEntity?>(null)
    val selectedModelConfig: StateFlow<ApiConfigEntity?> = _selectedModelConfig.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isLiveVoiceActive = MutableStateFlow(false)
    val isLiveVoiceActive: StateFlow<Boolean> = _isLiveVoiceActive.asStateFlow()

    private val _isSpeechCleanerEnabled = MutableStateFlow(true)
    val isSpeechCleanerEnabled: StateFlow<Boolean> = _isSpeechCleanerEnabled.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isDiscoveringModels = MutableStateFlow(false)
    val isDiscoveringModels: StateFlow<Boolean> = _isDiscoveringModels.asStateFlow()

    // Studio Image Generation State
    private val _studioImageState = MutableStateFlow(StudioImageState())
    val studioImageState: StateFlow<StudioImageState> = _studioImageState.asStateFlow()

    // Testing / Diagnostics State
    private val _testingConfigId = MutableStateFlow<String?>(null)
    val testingConfigId: StateFlow<String?> = _testingConfigId.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _modelVerificationResult = MutableStateFlow<ModelVerificationResult?>(null)
    val modelVerificationResult: StateFlow<ModelVerificationResult?> = _modelVerificationResult.asStateFlow()

    // Attachment State
    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName: StateFlow<String?> = _attachedFileName.asStateFlow()

    init {
        viewModelScope.launch {
            apiHubRepository.initializeDefaultsIfNeeded()
            val defaultCfg = apiHubRepository.getActiveChatConfig()
            _selectedModelConfig.value = defaultCfg

            // Load latest session or create clean new chat
            val existingSessions = chatRepository.sessions.firstOrNull()
            if (!existingSessions.isNullOrEmpty()) {
                selectSession(existingSessions.first().id)
            } else {
                createNewChat()
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            chatRepository.getMessages(sessionId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val modelName = _selectedModelConfig.value?.modelName.orEmpty()
            val newId = chatRepository.createNewSession("New Conversation", modelName)
            selectSession(newId)
            _currentTab.value = AppTab.CHAT
        }
    }

    fun renameSession(sessionId: String, title: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, title)
        }
    }

    fun togglePinSession(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePinSession(sessionId, isPinned)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun setSelectedConfig(config: ApiConfigEntity) {
        _selectedModelConfig.value = config
        viewModelScope.launch {
            apiHubRepository.setDefault(config.id)
            // If model is blank, dynamically discover and select
            if (config.modelName.isBlank() && config.apiKey.isNotBlank()) {
                refreshModelsForConfig(config)
            }
        }
    }

    fun selectModelForActiveConfig(modelId: String) {
        val current = _selectedModelConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                modelName = modelId,
                status = "CONNECTED"
            )
            apiHubRepository.saveConfig(updated, performTestFirst = false)
            _selectedModelConfig.value = updated

            // Verify the newly chosen model in background
            apiHubRepository.modelRegistry.verifySingleModel(updated, modelId, "chat")
        }
    }

    fun refreshModelsForConfig(config: ApiConfigEntity) {
        _isDiscoveringModels.value = true
        viewModelScope.launch {
            val res = apiHubRepository.refreshAndAutoSelectModel(config, capability = if (config.category.contains("Image")) "image_gen" else "chat")
            res.onSuccess { updated ->
                if (_selectedModelConfig.value?.id == config.id) {
                    _selectedModelConfig.value = updated
                }
            }
            _isDiscoveringModels.value = false
        }
    }

    fun verifySpecificModel(config: ApiConfigEntity, modelId: String) {
        viewModelScope.launch {
            val res = apiHubRepository.modelRegistry.verifySingleModel(config, modelId, capability = "chat")
            _modelVerificationResult.value = res.getOrNull()
        }
    }

    fun attachImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        if (ratio > 1) {
                            Bitmap.createScaledBitmap(bitmap, 1024, (1024 / ratio).toInt(), true)
                        } else {
                            Bitmap.createScaledBitmap(bitmap, (1024 * ratio).toInt(), 1024, true)
                        }
                    } else bitmap

                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val bytes = outputStream.toByteArray()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    _attachedImageBase64.value = b64
                    _attachedFileName.value = "Image attached"
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    fun clearAttachment() {
        _attachedImageBase64.value = null
        _attachedFileName.value = null
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val mediaB64 = _attachedImageBase64.value
        if (trimmed.isBlank() && mediaB64 == null) return

        val sId = _currentSessionId.value ?: return
        val currentCfg = _selectedModelConfig.value

        _isSending.value = true
        val mediaUri = if (mediaB64 != null) "data:image/jpeg;base64,$mediaB64" else null
        val mediaType = if (mediaB64 != null) "image/jpeg" else null

        clearAttachment()

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                sessionId = sId,
                userText = if (trimmed.isNotBlank()) trimmed else "Please analyze this attached image.",
                mediaUri = mediaUri,
                mediaType = mediaType,
                specificConfigId = currentCfg?.id,
                memoryEnabled = _isMemoryEnabled.value
            )

            _isSending.value = false

            result.onSuccess { message ->
                if (_isLiveVoiceActive.value && !message.spokenText.isNullOrBlank()) {
                    speechService.speak(message.spokenText)
                }
            }
        }
    }

    fun toggleSpeechCleaner(enabled: Boolean) {
        _isSpeechCleanerEnabled.value = enabled
    }

    fun toggleMemory(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun addMemory(fact: String, category: String) {
        viewModelScope.launch {
            memoryRepository.addMemory(fact, category)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAll()
        }
    }

    fun setLiveVoiceActive(active: Boolean) {
        _isLiveVoiceActive.value = active
        if (!active) {
            speechService.stopListening()
            speechService.stopSpeaking()
        }
    }

    // Studio Generation
    fun updateStudioPrompt(prompt: String) {
        _studioImageState.value = _studioImageState.value.copy(prompt = prompt, error = null)
    }

    fun updateStudioAspectRatio(ratio: String) {
        _studioImageState.value = _studioImageState.value.copy(aspectRatio = ratio)
    }

    fun updateStudioStyle(style: String) {
        _studioImageState.value = _studioImageState.value.copy(style = style)
    }

    fun generateStudioImage() {
        val state = _studioImageState.value
        if (state.prompt.isBlank()) return

        _studioImageState.value = state.copy(isGenerating = true, error = null)

        viewModelScope.launch {
            val imgConfig = apiHubRepository.getImageConfig()
            if (imgConfig == null || imgConfig.apiKey.isBlank()) {
                _studioImageState.value = _studioImageState.value.copy(
                    isGenerating = false,
                    error = "Image Generation API is not configured. Please configure an API key in Central API Hub."
                )
                return@launch
            }

            val styledPrompt = "${state.prompt}, in ${state.style} style, high resolution, detailed."
            val provider = ProviderRegistry.getProvider(imgConfig.providerType)
            val result = provider.generateImage(imgConfig, styledPrompt, state.aspectRatio, "1K")

            result.fold(
                onSuccess = { imgRes ->
                    _studioImageState.value = _studioImageState.value.copy(
                        isGenerating = false,
                        result = imgRes,
                        error = null
                    )
                },
                onFailure = { err ->
                    _studioImageState.value = _studioImageState.value.copy(
                        isGenerating = false,
                        error = err.message ?: "Failed to generate image."
                    )
                }
            )
        }
    }

    // API Hub Actions
    fun testConfig(config: ApiConfigEntity) {
        _testingConfigId.value = config.id
        viewModelScope.launch {
            val res = apiHubRepository.testConnection(config)
            res.fold(
                onSuccess = { testRes ->
                    _testResult.value = testRes
                    apiHubRepository.saveConfig(
                        config.copy(
                            status = if (testRes.success) "CONNECTED" else "MODEL_UNAVAILABLE",
                            lastTestedTimestamp = System.currentTimeMillis(),
                            lastLatencyMs = testRes.latencyMs,
                            lastErrorMessage = if (testRes.success) null else testRes.message
                        ),
                        performTestFirst = false
                    )
                },
                onFailure = { err ->
                    _testResult.value = ConnectionTestResult(
                        success = false,
                        latencyMs = 0,
                        message = err.message ?: "Unknown test failure"
                    )
                    apiHubRepository.saveConfig(
                        config.copy(
                            status = "ERROR",
                            lastTestedTimestamp = System.currentTimeMillis(),
                            lastErrorMessage = err.message
                        ),
                        performTestFirst = false
                    )
                }
            )
            _testingConfigId.value = null
        }
    }

    fun saveConfig(config: ApiConfigEntity, performTest: Boolean = true) {
        viewModelScope.launch {
            val res = apiHubRepository.saveConfig(config, performTest)
            if (config.isDefault) {
                _selectedModelConfig.value = res.getOrNull() ?: config
            }
        }
    }

    fun deleteConfig(id: String) {
        viewModelScope.launch {
            apiHubRepository.deleteConfig(id)
            if (_selectedModelConfig.value?.id == id) {
                _selectedModelConfig.value = apiHubRepository.getActiveChatConfig()
            }
        }
    }

    fun toggleConfigEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            apiHubRepository.toggleEnabled(id, enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechService.destroy()
    }
}
