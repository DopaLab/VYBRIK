package com.dopalabs.vybrik.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dopalabs.vybrik.R

val Charcoal = Color(0xFF111315)
val Ink = Color(0xFF191C1F)
val Concrete = Color(0xFF292C2F)
val Bone = Color(0xFFF2EEE8)
val Muted = Color(0xFFA9A49E)
val Coral = Color(0xFFFF5E57)
val Amber = Color(0xFFFFB44A)
val Acid = Color(0xFFCBEF43)
val Sky = Color(0xFF64C8FF)
val Asphalt = Color(0xFF0C0E10)

val VybrikDisplay = FontFamily(Font(R.font.bebas_neue_regular, FontWeight.Normal))

private val VybrikTypography = Typography(
    displayLarge = TextStyle(fontFamily = VybrikDisplay, fontSize = 64.sp, lineHeight = 60.sp),
    headlineLarge = TextStyle(fontFamily = VybrikDisplay, fontSize = 38.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = VybrikDisplay, fontSize = 26.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontFamily = VybrikDisplay, fontSize = 18.sp, letterSpacing = 1.sp)
)

private val VybrikColors = darkColorScheme(
    primary = Coral,
    onPrimary = Charcoal,
    secondary = Acid,
    background = Charcoal,
    onBackground = Bone,
    surface = Ink,
    onSurface = Bone,
    surfaceVariant = Concrete,
    onSurfaceVariant = Muted,
    outline = Color(0xFF464A4D),
    error = Coral
)

@Composable
fun VybrikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VybrikColors, typography = VybrikTypography, content = content)
}
