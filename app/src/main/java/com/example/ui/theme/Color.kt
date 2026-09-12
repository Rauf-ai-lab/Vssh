package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val ElectricBlue = Color(0xFF4285F4)
val SoftViolet = Color(0xFF9B72CB)
val AccentPurple = Color(0xFF7C3AED)
val DeepCharcoal = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF141414)
val SurfaceVariantDark = Color(0xFF1E1E1E)
val SecondarySurface = Color(0xFF282828)
val BorderDark = Color(0xFF323232)
val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFF9E9E9E)
val TextMuted = Color(0xFF6E6E6E)
val EmeraldGreen = Color(0xFF34A853)
val CrimsonRed = Color(0xFFEA4335)
val AmberWarning = Color(0xFFFBBC04)

val NovaGradient = Brush.horizontalGradient(
    colors = listOf(ElectricBlue, SoftViolet)
)

val NovaGlowGradient = Brush.radialGradient(
    colors = listOf(SoftViolet.copy(alpha = 0.35f), Color.Transparent)
)
