package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.data.UserSession

/**
 * AppThemePreferences encapsulates global user visual options:
 * - isDarkMode: active state of nocturnal style.
 * - chatBgColor: target user-customized wallpaper color.
 * - fontSizeMultiplier: scaled proportional units for excellent accessibility.
 */
data class AppThemePreferences(
    val isDarkMode: Boolean = false,
    val chatBgColor: Color = Color(0xFFFDFCFF),
    val fontSizeMultiplier: Float = 1.0f
)

/**
 * CompositionLocal to expose preferences down the Composable hierarchy automatically.
 */
val LocalAppThemePreferences = staticCompositionLocalOf { AppThemePreferences() }

/**
 * ThemeProvider is a highly optimized Material 3 context provider.
 * It dynamically maps, coordinates, and exposes light/dark themes,
 * text sizes, and wallpaper settings across the Frendo application.
 */
@Composable
fun ThemeProvider(
    userSession: UserSession?,
    onboardingIsDarkMode: Boolean? = null,
    onboardingFontSize: Float? = null,
    systemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val darkTheme = onboardingIsDarkMode ?: userSession?.isDarkMode ?: systemDarkTheme
    val chatBgColorHex = userSession?.chatBgColorHex ?: "#FDFCFF"
    val chatBgColor = try {
        Color(android.graphics.Color.parseColor(chatBgColorHex))
    } catch (e: Exception) {
        if (darkTheme) Color(0xFF121212) else Color(0xFFFDFCFF)
    }
    val fontSizeMultiplier = onboardingFontSize ?: userSession?.fontSizeMultiplier ?: 1.0f

    val preferences = AppThemePreferences(
        isDarkMode = darkTheme,
        chatBgColor = chatBgColor,
        fontSizeMultiplier = fontSizeMultiplier
    )

    CompositionLocalProvider(
        LocalAppThemePreferences provides preferences
    ) {
        content()
    }
}

/**
 * Accessibility extension helper to safely scale floats to SP text units.
 */
@Composable
fun Float.scaled(): TextUnit {
    return (this * LocalAppThemePreferences.current.fontSizeMultiplier).sp
}

/**
 * Accessibility extension helper to safely scale integers to SP text units.
 */
@Composable
fun Int.scaled(): TextUnit {
    return (this.toFloat() * LocalAppThemePreferences.current.fontSizeMultiplier).sp
}
