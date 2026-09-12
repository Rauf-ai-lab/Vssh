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
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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

    // Concentric Waveform Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking || isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
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
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaking) SoftViolet else ElectricBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nova Live Voice",
                    color = TextPrimary,
                    fontSize = 15.sp,
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
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center Animated Waveform / Sphere
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring 2
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(SoftViolet.copy(alpha = waveAlpha * 0.2f))
                )

                // Outer ring 1
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale * 0.95f)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = waveAlpha * 0.35f))
                )

                // Core glowing orb
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(NovaGradient)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
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
                    Text(
                        text = if (isSpeaking) "Speaking" else if (isListening) "Listening" else "Tap to Speak",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // State Badge
            val statusText = when {
                isSpeaking -> "Nova is speaking"
                isSending -> "Nova is thinking..."
                isListening -> "Listening to you..."
                else -> "Paused · Tap orb or speak"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SecondarySurface)
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusText,
                    color = TextPrimary,
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondarySurface.copy(alpha = 0.6f))
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "\"$displayedText\"",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute Mic
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isMicMuted) CrimsonRed.copy(alpha = 0.2f) else SecondarySurface)
                    .border(1.dp, if (isMicMuted) CrimsonRed else BorderDark, CircleShape)
                    .clickable {
                        isMicMuted = !isMicMuted
                        if (isMicMuted) {
                            viewModel.speechService.stopListening()
                        } else {
                            viewModel.speechService.startListening { text ->
                                if (text.isNotBlank()) viewModel.sendMessage(text)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mute",
                    tint = if (isMicMuted) CrimsonRed else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop / Interrupt Button (Mandatory voice feature)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CrimsonRed)
                    .clickable {
                        viewModel.speechService.stopSpeaking()
                        viewModel.speechService.stopListening()
                    }
                    .testTag("btn_voice_interrupt"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Speech",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
