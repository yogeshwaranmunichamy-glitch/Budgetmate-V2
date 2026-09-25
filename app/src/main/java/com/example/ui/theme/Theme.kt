package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldLight,
    onSecondary = Color.Black,
    background = SlateDarkBackground,
    onBackground = Color(0xFFE2EBE6),
    surface = SlateDarkSurface,
    onSurface = Color(0xFFE2EBE6),
    surfaceVariant = SlateDarkCard,
    onSurfaceVariant = Color(0xFFBAC7C0)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF003831),
    secondary = TealAccent,
    onSecondary = Color.Black,
    background = SlateLightBackground,
    onBackground = Color(0xFF191C1B),
    surface = SlateLightSurface,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFE6EBE8),
    onSurfaceVariant = Color(0xFF3F4945)
)

@Composable
fun BudgetMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false to keep BudgetMate signature emerald brand
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
