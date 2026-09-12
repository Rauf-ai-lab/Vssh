package com.example.ui.screens.apihub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.network.providers.ProviderRegistry
import com.example.data.network.providers.ProviderTemplate
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
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
import java.util.UUID

@Composable
fun ApiHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allConfigs by viewModel.allConfigs.collectAsState()
    val testingId by viewModel.testingConfigId.collectAsState()

    var editingConfig by remember { mutableStateOf<ApiConfigEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .testTag("api_hub_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NovaGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Central API Hub",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage multi-provider LLMs & credentials",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Add Provider Button
            ElevatedButton(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ElectricBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_add_api_provider")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List of Configured Providers
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Free Tier / Quick Links Ribbon
                QuickPresetsSection(
                    onSelectPreset = { template ->
                        editingConfig = ApiConfigEntity(
                            id = UUID.randomUUID().toString(),
                            name = template.name,
                            category = template.category,
                            providerType = template.providerType,
                            baseUrl = template.defaultBaseUrl,
                            modelName = template.defaultModel,
                            supportedCapabilities = "chat,vision,streaming"
                        )
                    }
                )
            }

            item {
                Text(
                    text = "Configured Providers (${allConfigs.size})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(allConfigs, key = { it.id }) { config ->
                ApiConfigCard(
                    config = config,
                    isTesting = testingId == config.id,
                    onTest = { viewModel.testConfig(config) },
                    onSetDefault = { viewModel.setSelectedConfig(config) },
                    onToggleEnabled = { enabled -> viewModel.toggleConfigEnabled(config.id, enabled) },
                    onEdit = { editingConfig = config },
                    onDelete = { viewModel.deleteConfig(config.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Add or Edit Dialog
    if (isAddingNew || editingConfig != null) {
        AddEditApiDialog(
            initialConfig = editingConfig,
            onDismiss = {
                isAddingNew = false
                editingConfig = null
            },
            onSave = { updated ->
                viewModel.saveConfig(updated, performTest = true)
                isAddingNew = false
                editingConfig = null
                Toast.makeText(context, "Saved ${updated.name}", Toast.LENGTH_SHORT).show()
            },
            onTestDirect = { cfg, onResult ->
                // Direct test connection in dialog
            }
        )
    }
}

@Composable
private fun QuickPresetsSection(
    onSelectPreset: (ProviderTemplate) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Official Developer Portals & Free Keys",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Get direct free API keys from official provider dashboards:",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProviderRegistry.templates.take(4).forEach { t ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(t.defaultModel, color = TextMuted, fontSize = 10.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(t.apiKeyUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = if (t.hasFreeTier) "Free Key" else "Get Key",
                            color = if (t.hasFreeTier) EmeraldGreen else ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = if (t.hasFreeTier) EmeraldGreen else ElectricBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = { onSelectPreset(t) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Use", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiConfigCard(
    config: ApiConfigEntity,
    isTesting: Boolean,
    onTest: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (config.isDefault) ElectricBlue.copy(alpha = 0.5f) else BorderDark,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
            .testTag("api_config_card_${config.name}")
    ) {
        // Top Row: Name, Category, Default Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = config.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (config.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Active Default", color = ElectricBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Enable / Disable switch
            Switch(
                checked = config.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ElectricBlue,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceVariantDark
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status & Model info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusBadge(status = config.status, latencyMs = config.lastLatencyMs)

            Text(
                text = "Model: ${config.modelName.ifBlank { "Not set" }}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // API Key Masked & Base URL
        Text(
            text = "Key: ${config.maskedApiKey}",
            color = TextMuted,
            fontSize = 11.sp
        )
        Text(
            text = "Endpoint: ${config.baseUrl.ifBlank { "Default provider host" }}",
            color = TextMuted,
            fontSize = 11.sp
        )

        // Error message if any
        if (!config.lastErrorMessage.isNullOrBlank() && config.status != "CONNECTED") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Note: ${config.lastErrorMessage}",
                color = CrimsonRed,
                fontSize = 11.sp
            )
        }

        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Test Connection Button
                OutlinedButton(
                    onClick = onTest,
                    enabled = !isTesting && config.apiKey.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = ElectricBlue)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 11.sp)
                }

                // Set as Default
                if (!config.isDefault && config.isEnabled) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Default", fontSize = 11.sp)
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditApiDialog(
    initialConfig: ApiConfigEntity?,
    onDismiss: () -> Unit,
    onSave: (ApiConfigEntity) -> Unit,
    onTestDirect: (ApiConfigEntity, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialConfig?.name ?: "Google Gemini") }
    var category by remember { mutableStateOf(initialConfig?.category ?: "Chat / LLM") }
    var providerType by remember { mutableStateOf(initialConfig?.providerType ?: "GEMINI") }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(initialConfig?.baseUrl ?: "https://generativelanguage.googleapis.com") }
    var modelName by remember { mutableStateOf(initialConfig?.modelName ?: "gemini-2.5-flash") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (initialConfig != null) "Edit API Provider" else "Add API Provider",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Provider Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Provider Name") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key with Show/Hide toggle
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle key visibility",
                                tint = TextSecondary
                            )
                        }
                    }
                )

                // Quick Link to get free key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val url = when (providerType) {
                                "GEMINI" -> "https://aistudio.google.com/app/apikey"
                                else -> "https://console.groq.com/keys"
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Text("Get Free API Key", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                    }
                }

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL Endpoint") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name (e.g. gemini-2.5-flash, llama-3.3-70b-versatile)") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ElevatedButton(
                        onClick = {
                            val entity = (initialConfig ?: ApiConfigEntity(
                                id = UUID.randomUUID().toString(),
                                name = name
                            )).copy(
                                name = name.trim(),
                                category = category,
                                providerType = if (baseUrl.contains("google")) "GEMINI" else "OPENAI_COMPATIBLE",
                                apiKey = apiKey.trim(),
                                baseUrl = baseUrl.trim(),
                                modelName = modelName.trim()
                            )
                            onSave(entity)
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save & Validate")
                    }
                }
            }
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceVariantDark,
    unfocusedContainerColor = SurfaceVariantDark,
    focusedBorderColor = ElectricBlue,
    unfocusedBorderColor = BorderDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
