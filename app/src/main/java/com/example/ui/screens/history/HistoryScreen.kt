package com.example.ui.screens.history

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatSessionEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterPinnedOnly by remember { mutableStateOf(false) }

    var renamingSession by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var newTitleText by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    val filteredSessions = remember(sessions, searchQuery, filterPinnedOnly) {
        sessions.filter {
            (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)) &&
                    (!filterPinnedOnly || it.isPinned)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .testTag("history_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Chat History",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sessions.size} saved conversations",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            ElevatedButton(
                onClick = { viewModel.createNewChat() },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ElectricBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search conversations...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (!filterPinnedOnly) ElectricBlue.copy(alpha = 0.2f) else SurfaceDark)
                    .border(1.dp, if (!filterPinnedOnly) ElectricBlue else BorderDark, RoundedCornerShape(16.dp))
                    .clickable { filterPinnedOnly = false }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("All Chats", color = if (!filterPinnedOnly) ElectricBlue else TextSecondary, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (filterPinnedOnly) SoftViolet.copy(alpha = 0.25f) else SurfaceDark)
                    .border(1.dp, if (filterPinnedOnly) SoftViolet else BorderDark, RoundedCornerShape(16.dp))
                    .clickable { filterPinnedOnly = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pinned", color = if (filterPinnedOnly) SoftViolet else TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Session List
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No conversations match your search." else "No conversations yet. Start a new chat!",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) SurfaceVariantDark else SurfaceDark)
                            .border(
                                1.dp,
                                if (isCurrent) ElectricBlue.copy(alpha = 0.4f) else BorderDark,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.selectSession(session.id)
                                viewModel.selectTab(AppTab.CHAT)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (session.isPinned) SoftViolet.copy(alpha = 0.2f) else SecondarySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (session.isPinned) SoftViolet else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(session.updatedAt)),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("•", color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = session.modelUsed,
                                    color = ElectricBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Actions
                        IconButton(
                            onClick = { viewModel.togglePinSession(session.id, !session.isPinned) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin session",
                                tint = if (session.isPinned) SoftViolet else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                renamingSession = session
                                newTitleText = session.title
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { sessionToDelete = session.id },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = CrimsonRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Rename Dialog
    renamingSession?.let { session ->
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Rename Conversation", color = TextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitleText.isNotBlank()) {
                            viewModel.renameSession(session.id, newTitleText.trim())
                        }
                        renamingSession = null
                    }
                ) {
                    Text("Save", color = ElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete confirmation dialog
    sessionToDelete?.let { sId ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Conversation?", color = TextPrimary, fontSize = 16.sp) },
            text = { Text("This will permanently remove this chat and its messages.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sId)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
