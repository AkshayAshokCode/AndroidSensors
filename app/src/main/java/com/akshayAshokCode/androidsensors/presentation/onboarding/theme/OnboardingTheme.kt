package com.akshayAshokCode.androidsensors.presentation.onboarding.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// App colors matching the main theme
private val DarkPurpleBlue = Color(0xFF1A1A2E)
private val MediumPurpleBlue = Color(0xFF16213E)
private val DeepBlue = Color(0xFF0F3460)
private val CardOuter = Color(0xFF2A2A3E)
private val CardInner = Color(0xFF1E1E32)
private val ToolbarBackground = Color(0xFF1E1E32)
private val White = Color(0xFFFFFFFF)
private val GravityLow = Color(0xFF4CAF50)
private val GravityNormal = Color(0xFFFFC107)
private val GravityHigh = Color(0xFFFF5722)
private val Orange = Color(0xFFFF9800)

// Dark color scheme matching the app
private val DarkColorScheme = darkColorScheme(
    primary = GravityLow,
    onPrimary = White,
    primaryContainer = CardOuter,
    onPrimaryContainer = White,

    secondary = GravityNormal,
    onSecondary = DarkPurpleBlue,
    secondaryContainer = CardInner,
    onSecondaryContainer = White,

    tertiary = Orange,
    onTertiary = White,

    background = DarkPurpleBlue,
    onBackground = White,

    surface = CardOuter,
    onSurface = White,
    surfaceVariant = CardInner,
    onSurfaceVariant = White.copy(alpha = 0.7f),

    error = GravityHigh,
    onError = White,

    outline = White.copy(alpha = 0.2f),
    outlineVariant = White.copy(alpha = 0.1f)
)

/**
 * Custom theme for onboarding screens matching the app's dark theme
 */
@Composable
fun OnboardingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark theme to match the app
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}