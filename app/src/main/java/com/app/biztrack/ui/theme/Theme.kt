package com.app.biztrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF004C43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F3EC),
    onPrimaryContainer = Color(0xFF00332D),
    secondary = Color(0xFF0E7768),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2EC),
    onSecondaryContainer = Color(0xFF093C35),
    tertiary = Color(0xFFD9A24A),
    onTertiary = Color(0xFF271706),
    tertiaryContainer = Color(0xFFFFE7BB),
    onTertiaryContainer = Color(0xFF51320A),
    background = Color(0xFFFFF8EF),
    onBackground = Color(0xFF081E1C),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF081E1C),
    surfaceVariant = Color(0xFFF4EDE2),
    onSurfaceVariant = Color(0xFF65706B),
    outline = Color(0xFFE9D8BD),
    error = Color(0xFFC94E28),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7BE70),
    onPrimary = Color(0xFF06201D),
    primaryContainer = Color(0xFF00483F),
    onPrimaryContainer = Color(0xFFFFF1D0),
    secondary = Color(0xFF6EE0C8),
    onSecondary = Color(0xFF052F2B),
    secondaryContainer = Color(0xFF053F38),
    onSecondaryContainer = Color(0xFFD8F8F0),
    tertiary = Color(0xFFE7BE70),
    onTertiary = Color(0xFF342006),
    tertiaryContainer = Color(0xFF5A3B12),
    onTertiaryContainer = Color(0xFFFFE1B2),
    background = Color(0xFF061715),
    onBackground = Color(0xFFF6EFE5),
    surface = Color(0xFF0B221F),
    onSurface = Color(0xFFF6EFE5),
    surfaceVariant = Color(0xFF172E2A),
    onSurfaceVariant = Color(0xFFC9D3CC),
    outline = Color(0xFF6E5937),
    error = Color(0xFFFFB099),
)

private val BizTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun BizTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BizTypography,
        content = content,
    )
}
