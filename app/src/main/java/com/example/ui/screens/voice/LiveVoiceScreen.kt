package com.example.ui.screens.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiOutline
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GeminiRed
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.GeminiSurface
import com.example.ui.theme.GeminiSurfaceElevated
import com.example.ui.theme.GeminiSurfaceVariant
import com.example.ui.theme.GeminiTextPrimary
import com.example.ui.theme.GeminiTextSecondary

@Composable
fun LiveVoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.speechService.isListening.collectAsState()
    val isSpeaking by viewModel.speechService.isSpeaking.collectAsState()
    val partialText by viewModel.speechService.partialTranscript.collectAsState()
    val spokenText by viewModel.speechService.spokenTranscript.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    var isMicMuted by remember { mutableStateOf(false) }

    // Start speech recognition upon entry
    LaunchedEffect(Unit) {
        viewModel.speechService.startListening { text ->
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
            }
        }
    }

    // Concentric Waveform Animation for Gemini Live Orb
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking || isListening) 1.28f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
            .testTag("live_voice_fullscreen")
    ) {
        // Top Header with Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini Live",
                    color = GeminiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { viewModel.setLiveVoiceActive(false) },
                modifier = Modifier.testTag("btn_close_live_voice")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Live Voice",
                    tint = GeminiTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center Animated Waveform / Fluid Orb
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring 2 (Pink/Purple glow)
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(GeminiPink.copy(alpha = waveAlpha * 0.18f))
                )

                // Outer ring 1 (Blue/Violet glow)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale * 0.94f)
                        .clip(CircleShape)
                        .background(GeminiBlue.copy(alpha = waveAlpha * 0.3f))
                )

                // Core fluid glowing orb
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(GeminiSparkleGradient)
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            if (isSpeaking) {
                                viewModel.speechService.stopSpeaking()
                            } else if (!isListening) {
                                viewModel.speechService.startListening { text ->
                                    if (text.isNotBlank()) viewModel.sendMessage(text)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // State Badge
            val statusText = when {
                isSpeaking -> "Gemini is speaking"
                isSending -> "Gemini is thinking..."
                isListening -> "Listening..."
                else -> "Paused · Tap orb or speak"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(GeminiSurfaceElevated)
                    .border(1.dp, GeminiOutline, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusText,
                    color = GeminiTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcript Card
            val displayedText = if (partialText.isNotBlank()) partialText else spokenText
            if (displayedText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurfaceVariant)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\"$displayedText\"",
                        color = GeminiTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom Controls Pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute Mic
            IconButton(
                onClick = {
                    isMicMuted = !isMicMuted
                    if (isMicMuted) {
                        viewModel.speechService.stopListening()
                    } else {
                        viewModel.speechService.startListening { text ->
                            if (text.isNotBlank()) viewModel.sendMessage(text)
                        }
                    }
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GeminiSurfaceVariant)
                    .border(1.dp, GeminiOutline, CircleShape)
            ) {
                Icon(
                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mic",
                    tint = if (isMicMuted) GeminiRed else GeminiTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop / Interrupt Button
            IconButton(
                onClick = {
                    viewModel.speechService.stopSpeaking()
                    viewModel.speechService.stopListening()
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GeminiSurfaceVariant)
                    .border(1.dp, GeminiOutline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Interrupt Gemini",
                    tint = GeminiRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Exit Live Mode
            IconButton(
                onClick = { viewModel.setLiveVoiceActive(false) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GeminiSurfaceElevated)
                    .border(1.dp, GeminiOutline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Gemini Live",
                    tint = GeminiTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
