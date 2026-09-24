package com.btbaazlaunda.lull.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

val LightColors = lightColorScheme(
    primary = Color(0xFF4B45B8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3DFFF),
    onPrimaryContainer = Color(0xFF14105E),
    secondary = Color(0xFF5E5C71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E0F9),
    onSecondaryContainer = Color(0xFF1B1A2C),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFECF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFC5C0FF),
    onPrimary = Color(0xFF2A2387),
    primaryContainer = Color(0xFF3A329F),
    onPrimaryContainer = Color(0xFFE3DFFF),
    secondary = Color(0xFFC8C4DC),
    onSecondary = Color(0xFF302F42),
    secondaryContainer = Color(0xFF47465A),
    onSecondaryContainer = Color(0xFFE4E0F9),
    tertiary = Color(0xFFFFB871),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF12121C),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF12121C),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF0C0C16),
    surfaceContainerLow = Color(0xFF1A1A25),
    surfaceContainer = Color(0xFF1E1E29),
    surfaceContainerHigh = Color(0xFF292834),
    surfaceContainerHighest = Color(0xFF34333F),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
)

/** Colours for the sky behind the sleep button. */
@Immutable
data class SkyPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val content: Color,
    val button: Color,
    val icon: Color,
    val glow: Color,
)

val DaySky = SkyPalette(
    top = Color(0xFF9DBBFF),
    middle = Color(0xFFCFC6FF),
    bottom = Color(0xFFFFE2CC),
    content = Color(0xFF1B1A3A),
    button = Color(0xFFFFFFFF),
    icon = Color(0xFFD97706),
    glow = Color(0xFFFFC98A),
)

val DuskSky = SkyPalette(
    top = Color(0xFF141B45),
    middle = Color(0xFF34306E),
    bottom = Color(0xFF6E4A78),
    content = Color(0xFFF3EEFF),
    button = Color(0xFF2B2760),
    icon = Color(0xFFFFB547),
    glow = Color(0xFFFF9E6B),
)

val NightSky = SkyPalette(
    top = Color(0xFF050817),
    middle = Color(0xFF12173F),
    bottom = Color(0xFF2D2462),
    content = Color(0xFFEDE9FF),
    button = Color(0xFF221D57),
    icon = Color(0xFFFFE7B0),
    glow = Color(0xFF8F7CFF),
)

private val LullTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun LullTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LullTypography,
        content = content,
    )
}
