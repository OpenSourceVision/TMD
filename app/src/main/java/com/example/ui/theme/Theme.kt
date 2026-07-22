package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF60A5FA),       // Vibrant Sky Blue
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF94A3B8),     // Modern Slate Gray
    onSecondary = Color(0xFF1E293B),
    background = Color(0xFF181A20),    // Softer modern dark slate background (NOT pitch black #0D1117)
    onBackground = Color(0xFFF1F5F9),  // Crisp soft white text
    surface = Color(0xFF222630),       // Modern elevated dark card surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2C3240),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3B4454)        // Softer border
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF2563EB),       // Modern Royal Blue
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF64748B),     // Slate Gray
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),    // Modern soft off-white background
    onBackground = Color(0xFF0F172A),  // Deep dark text
    surface = Color(0xFFFFFFFF),       // Card surface
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)        // Subtle light border
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
