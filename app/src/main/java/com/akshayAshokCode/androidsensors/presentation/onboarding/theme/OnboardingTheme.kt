package com.akshayAshokCode.androidsensors.presentation.onboarding.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Futuristic design system tokens
private val Void    = Color(0xFF050510)
private val Surface = Color(0xFF0D0D2B)
private val Cyan    = Color(0xFF00D4FF)
private val Purple  = Color(0xFF7B2FFF)
private val Green   = Color(0xFF00FF88)
private val Red     = Color(0xFFFF3355)
private val White   = Color(0xFFFFFFFF)
private val Grid    = Color(0xFF1A1A3E)

private val DarkColorScheme = darkColorScheme(
    primary             = Cyan,
    onPrimary           = Void,
    primaryContainer    = Surface,
    onPrimaryContainer  = Cyan,

    secondary           = Purple,
    onSecondary         = White,
    secondaryContainer  = Grid,
    onSecondaryContainer = White,

    tertiary            = Green,
    onTertiary          = Void,

    background          = Void,
    onBackground        = White,

    surface             = Surface,
    onSurface           = White,
    surfaceVariant      = Grid,
    onSurfaceVariant    = White.copy(alpha = 0.6f),

    error               = Red,
    onError             = White,

    outline             = Cyan.copy(alpha = 0.25f),
    outlineVariant      = Grid
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