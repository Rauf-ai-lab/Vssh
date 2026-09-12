package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.screens.apihub.ApiHubScreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showApiHubSubscreen by remember { mutableStateOf(false) }

    val activeConfig by viewModel.selectedModelConfig.collectAsState()
    val isSpeechCleanerOn by viewModel.isSpeechCleanerEnabled.collectAsState()
    val isMemoryOn by viewModel.isMemoryEnabled.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val testingId by viewModel.testingConfigId.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var newMemoryFact by remember { mutableStateOf("") }
    var newMemoryCategory by remember { mutableStateOf("Preference") }

    if (showApiHubSubscreen) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showApiHubSubscreen = false }) {
                    Text("← Back to Settings", color = ElectricBlue)
                }
            }
            ApiHubScreen(viewModel = viewModel)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(NovaGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Settings & Diagnostics", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Preferences, API connection, and memory", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        item {
            // CENTRAL API HUB NAVIGATION SHORTCUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { showApiHubSubscreen = true }
                    .padding(16.dp)
                    .testTag("nav_to_api_hub"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Central API Hub", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Active: ${activeConfig?.name ?: "None"} (${activeConfig?.modelName ?: "No model"})",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        item {
            // SYSTEM & API DIAGNOSTICS CARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(16.dp)
                    .testTag("diagnostics_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Diagnostics", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    activeConfig?.let {
                        StatusBadge(status = it.status, latencyMs = it.lastLatencyMs)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DiagnosticRow("Active Provider", activeConfig?.name ?: "None configured")
                DiagnosticRow("Active Model", activeConfig?.modelName ?: "None")
                DiagnosticRow("Endpoint URL", activeConfig?.baseUrl ?: "None")
                DiagnosticRow("Masked Key", activeConfig?.maskedApiKey ?: "Not set")
                DiagnosticRow("Capabilities", activeConfig?.supportedCapabilities ?: "chat")

                Spacer(modifier = Modifier.height(12.dp))

                // Ping / Test Button
                OutlinedButton(
                    onClick = {
                        activeConfig?.let { viewModel.testConfig(it) }
                    },
                    enabled = activeConfig != null && testingId == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (testingId != null) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying endpoint...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Diagnostic Ping & Model Verification", fontSize = 12.sp)
                    }
                }

                testResult?.let { res ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (res.success) EmeraldGreen.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = res.message,
                            color = if (res.success) EmeraldGreen else CrimsonRed,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            // VOICE OUTPUT & SPEECH CLEANER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SoftViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speech Cleaning Pipeline", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Switch(
                        checked = isSpeechCleanerOn,
                        onCheckedChange = { viewModel.toggleSpeechCleaner(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SoftViolet
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Intelligently filters out emojis, markdown symbols (#, **), code snippets, URLs, JSON, and citations before voice playback for a smooth human reading voice.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        item {
            // MEMORY & PERSONALIZATION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Assistant Memory", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Switch(
                        checked = isMemoryOn,
                        onCheckedChange = { viewModel.toggleMemory(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When active, Nova remembers key details and preferences across chats to personalize answers.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Memories list
                memories.forEach { mem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${mem.category}] ${mem.fact}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.deleteMemory(mem.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = CrimsonRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showAddMemoryDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Memory Fact", fontSize = 11.sp)
                    }

                    if (memories.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAllMemories() }) {
                            Text("Clear All", color = CrimsonRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            // ABOUT SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("About Nova AI", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                DiagnosticRow("Assistant", "Nova")
                DiagnosticRow("Tagline", "Your intelligent companion for every task.")
                DiagnosticRow("Version", "1.0.0 (Production)")
                DiagnosticRow("Developer", "Rauf")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Add Memory Fact", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newMemoryFact,
                        onValueChange = { newMemoryFact = it },
                        placeholder = { Text("e.g., Prefers Kotlin over Java", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMemoryFact.isNotBlank()) {
                            viewModel.addMemory(newMemoryFact.trim(), newMemoryCategory)
                            newMemoryFact = ""
                        }
                        showAddMemoryDialog = false
                    }
                ) {
                    Text("Save Fact", color = ElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
