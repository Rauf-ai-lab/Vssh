package com.example.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.OfflineBanner
import com.example.ui.theme.BorderDark
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnline by viewModel.isOnline.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val enabledConfigs by viewModel.enabledConfigs.collectAsState()
    val selectedConfig by viewModel.selectedModelConfig.collectAsState()
    val attachedB64 by viewModel.attachedImageBase64.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Activity result launcher for image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImageUri(it) }
    }

    // Audio permission launcher for voice
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.speechService.startListening { spoken ->
                inputText = spoken
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .imePadding()
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model Selector Pill Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                        .clickable { showModelMenu = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("model_selector_dropdown"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (selectedConfig?.isConnected == true) ElectricBlue else SoftViolet)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedConfig?.modelName?.ifBlank { "Select Model" } ?: "Select Model",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Model",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Configured AI Models",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(color = BorderDark)

                    enabledConfigs.forEach { cfg ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${cfg.name} (${cfg.modelName})",
                                        color = if (cfg.id == selectedConfig?.id) ElectricBlue else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (cfg.id == selectedConfig?.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = cfg.category,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setSelectedConfig(cfg)
                                showModelMenu = false
                            }
                        )
                    }

                    HorizontalDivider(color = BorderDark)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Manage in API Hub", color = ElectricBlue, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            showModelMenu = false
                            viewModel.selectTab(AppTab.SETTINGS)
                        }
                    )
                }
            }

            // Right Action: New Chat Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.createNewChat() },
                    modifier = Modifier.testTag("btn_new_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Conversation",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Offline Banner if disconnected
        if (!isOnline) {
            OfflineBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // CHAT MESSAGES LIST / EMPTY STATE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyStateView(
                    onSuggestionClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(messages, key = { it.id }) { msg ->
                        when (msg.role) {
                            "user" -> UserMessageItem(msg)
                            "assistant" -> AssistantMessageItem(
                                message = msg,
                                onSpeak = { text -> viewModel.speechService.speak(text) },
                                onCopy = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Nova AI", text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onShare = { text ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share with"))
                                }
                            )
                            "error" -> ErrorActionCard(
                                errorMessage = msg.content,
                                onConfigureApi = { viewModel.selectTab(AppTab.SETTINGS) },
                                onTestConnection = {
                                    selectedConfig?.let { viewModel.testConfig(it) }
                                }
                            )
                        }
                    }

                    if (isSending) {
                        item {
                            ThinkingIndicator()
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // ATTACHMENT PREVIEW CHIP
        AnimatedVisibility(visible = attachedB64 != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Image ready to analyze", color = TextPrimary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearAttachment() }
                        )
                    }
                }
            }
        }

        // FLOATING PILL INPUT BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "+" Attach Button
            IconButton(
                onClick = { showAttachSheet = true },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_attach_media")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach media or tools",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Input Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Nova anything...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ElectricBlue
                ),
                maxLines = 4
            )

            // Mic / Voice Recognition Button
            IconButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        viewModel.speechService.startListening { spoken ->
                            inputText = spoken
                        }
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_voice_input")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button OR Live Voice Waveform Button
            if (inputText.isNotBlank() || attachedB64 != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NovaGradient)
                        .clickable {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendMessage(textToSend)
                        }
                        .testTag("btn_send_message"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.setLiveVoiceActive(true)
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("btn_live_voice_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Live Voice Mode",
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // ATTACHMENT BOTTOM SHEET
    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = SurfaceDark,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add to conversation",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark)
                        .clickable {
                            showAttachSheet = false
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Upload Image / Vision", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Analyze diagrams, photos, or documents", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark)
                        .clickable {
                            showAttachSheet = false
                            viewModel.sendMessage("Turn my class notes into custom quizzes")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SoftViolet)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Interactive Quiz Generator", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Create multi-choice knowledge checks", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark)
                        .clickable {
                            showAttachSheet = false
                            viewModel.sendMessage("Deep dive: Roman aqueducts architecture and engineering")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Deep Dive & Architecture Diagrams", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Explore annotated architectural components", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyStateView(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SecondarySurface)
                .border(1.dp, BorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nova AI",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your intelligent companion for every task.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        val suggestions = listOf(
            "Turn class notes into custom quizzes",
            "Deep dive: Roman aqueducts architecture",
            "Generate 90s rap beats & lyrics",
            "Analyze and explain complex documents"
        )

        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = suggestion,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun UserMessageItem(message: ChatMessageEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_message_bubble"),
        horizontalAlignment = Alignment.End
    ) {
        // Thumbnail if image attached
        if (!message.mediaUri.isNullOrBlank() && message.mediaUri.contains("base64,")) {
            val base64Data = message.mediaUri.substringAfter("base64,")
            val decodedBytes = try {
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
            if (decodedBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .background(SurfaceVariantDark)
                .border(1.dp, BorderDark, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageItem(
    message: ChatMessageEntity,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assistant_message_bubble"),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Nova Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NovaGradient),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Assistant Text Body
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )

                // Interactive Cards if present
                if (message.cardType == "quiz" && !message.cardJson.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuizCard(cardJson = message.cardJson)
                } else if (message.cardType == "diagram" && !message.cardJson.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DiagramCard(cardJson = message.cardJson)
                }

                // Action Bar (Listen/TTS, Copy, Share)
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSpeak(message.spokenText ?: message.content) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen with Text-to-Speech",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onCopy(message.content) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { onShare(message.content) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share message",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(SecondarySurface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = ElectricBlue,
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Nova is thinking...",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
