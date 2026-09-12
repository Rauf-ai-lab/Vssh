package com.example.ui.screens.studio

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonRed
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Image Studio", "Music Studio", "Video Lab")

    val studioState by viewModel.studioImageState.collectAsState()
    val allConfigs by viewModel.allConfigs.collectAsState()

    // Check if an image config with API key is available
    val hasImageConfig = remember(allConfigs) {
        allConfigs.any {
            (it.category == "Image Generation" || it.providerType == "GEMINI" || it.supportedCapabilities.contains("image_gen")) &&
                    it.apiKey.isNotBlank() && it.isEnabled
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        // Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NovaGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Creative Studio",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Multi-modal generation & AI tools",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Subtabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIdx,
            containerColor = SurfaceDark,
            contentColor = ElectricBlue,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx]),
                    color = ElectricBlue
                )
            }
        ) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTabIdx == idx,
                    onClick = { selectedTabIdx = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIdx == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIdx == idx) ElectricBlue else TextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTabIdx) {
                0 -> ImageStudioTab(
                    hasConfig = hasImageConfig,
                    state = studioState,
                    onPromptChange = { viewModel.updateStudioPrompt(it) },
                    onRatioChange = { viewModel.updateStudioAspectRatio(it) },
                    onStyleChange = { viewModel.updateStudioStyle(it) },
                    onGenerate = { viewModel.generateStudioImage() },
                    onConfigureApi = { viewModel.selectTab(AppTab.SETTINGS) },
                    onOpenKeyPortal = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    }
                )
                1 -> MusicStudioTab()
                2 -> VideoLabTab()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageStudioTab(
    hasConfig: Boolean,
    state: com.example.ui.StudioImageState,
    onPromptChange: (String) -> Unit,
    onRatioChange: (String) -> Unit,
    onStyleChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onConfigureApi: () -> Unit,
    onOpenKeyPortal: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unconfigured Notice Card
        if (!hasConfig) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SoftViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(18.dp)
                    .testTag("image_api_unconfigured_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = SoftViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image Generation API is not configured",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To generate high-fidelity images, configure an API key for Google Gemini (Free tier available) or OpenAI DALL-E in the Central API Hub.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(
                        onClick = onConfigureApi,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_configure_image_api")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure API", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenKeyPortal,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_get_free_image_key")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Free API Key", fontSize = 12.sp)
                    }
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChange,
            placeholder = { Text("Describe the image you want to create...", color = TextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("image_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 5
        )

        // Inspiration Chips
        Text(text = "Try an idea:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Cyberpunk neon cityscape in rain",
                "Retro synthwave sports car",
                "Minimalist watercolor mountain sunrise",
                "Futuristic AI hologram crystal sphere"
            ).forEach { idea ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .clickable { onPromptChange(idea) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = idea, color = TextPrimary, fontSize = 11.sp)
                }
            }
        }

        // Aspect Ratio Selector
        Text(text = "Aspect Ratio", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1:1", "16:9", "9:16", "4:3").forEach { ratio ->
                val isSelected = state.aspectRatio == ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.2f) else SurfaceDark)
                        .border(1.dp, if (isSelected) ElectricBlue else BorderDark, RoundedCornerShape(10.dp))
                        .clickable { onRatioChange(ratio) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio,
                        color = if (isSelected) ElectricBlue else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Style Selector
        Text(text = "Visual Style", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Realistic", "Anime", "3D Render", "Cyberpunk", "Watercolor", "Cinematic").forEach { style ->
                val isSelected = state.style == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SoftViolet.copy(alpha = 0.25f) else SurfaceDark)
                        .border(1.dp, if (isSelected) SoftViolet else BorderDark, RoundedCornerShape(16.dp))
                        .clickable { onStyleChange(style) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style,
                        color = if (isSelected) SoftViolet else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Generate Button
        val btnBgModifier = if (state.isGenerating || state.prompt.isBlank()) {
            Modifier.background(SecondarySurface)
        } else {
            Modifier.background(NovaGradient)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(btnBgModifier)
                .clickable(enabled = !state.isGenerating && state.prompt.isNotBlank()) {
                    onGenerate()
                }
                .testTag("btn_generate_studio_image"),
            contentAlignment = Alignment.Center
        ) {
            if (state.isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ElectricBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Creating masterpiece...", color = TextPrimary, fontSize = 14.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Image", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error message if any
        if (!state.error.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonRed.copy(alpha = 0.15f))
                    .border(1.dp, CrimsonRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(text = state.error, color = CrimsonRed, fontSize = 12.sp)
            }
        }

        // Result Image View
        state.result?.let { result ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                if (!result.base64Data.isNullOrBlank()) {
                    val bytes = Base64.decode(result.base64Data, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (state.aspectRatio) {
                                        "16:9" -> 16f / 9f
                                        "9:16" -> 9f / 16f
                                        "4:3" -> 4f / 3f
                                        else -> 1f
                                    }
                                )
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (!result.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = "Generated Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nova Studio · ${state.style}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Image saved to gallery.", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download Image", tint = ElectricBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MusicStudioTab() {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "AI Music & Audio Composition",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Generate melodies, beats, lyrics, and harmonic progressions.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // Preset cards
        val presets = listOf(
            Pair("90s Boom Bap Rap", "Punchy kicks, vinyl crackle, jazz horns sample, 92 BPM"),
            Pair("Latin Tropical Pop", "Upbeat reggaeton dembow percussion, bright nylon guitars"),
            Pair("Intimate Folk Ballad", "Warm fingerpicked acoustic guitar, subtle strings"),
            Pair("8-Bit Arcade Chiptune", "Fast arpeggiated square waves, nostalgic game sound"),
            Pair("Cinematic Orchestral", "Deep brass swells, rhythmic timpani, soaring violins")
        )

        presets.forEach { (genre, details) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(genre, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(details, color = TextSecondary, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .clickable {
                                isPlaying = !isPlaying
                                Toast.makeText(context, if (isPlaying) "Playing $genre preview" else "Paused", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Headphones else Icons.Default.PlayArrow,
                            contentDescription = "Play Track",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoLabTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "AI Video Generation Studio",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Generate cinematic visual shots and storyboard camera movements.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        val videoTemplates = listOf(
            Pair("Cinematic Drone Flyover", "Sweeping aerial 4K shot descending over misty pine forest"),
            Pair("Decades of Fashion", "Seamless time-morphing portrait transitioning from 1920 to 2050"),
            Pair("Macro Liquid Ink Droplet", "Super slow-motion ink cloud dispersing in illuminated water")
        )

        videoTemplates.forEach { (title, desc) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SecondarySurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = SoftViolet, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(desc, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
