package com.example.fossrsvp.ui.theme

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
    primary = NordicBlue80,
    onPrimary = PolarNight0,
    primaryContainer = NordicBlue40,
    onPrimaryContainer = SnowStorm2,
    secondary = NordicTeal80,
    onSecondary = PolarNight0,
    tertiary = NordicRed80,
    background = PolarNight0,
    surface = PolarNight1,
    onBackground = SnowStorm2,
    onSurface = SnowStorm2
)

private val LightColorScheme = lightColorScheme(
    primary = NordicBlue40,
    onPrimary = Color.White,
    primaryContainer = NordicBlue80,
    onPrimaryContainer = PolarNight0,
    secondary = NordicTeal40,
    onSecondary = Color.White,
    tertiary = NordicRed40,
    background = SnowStorm2,
    surface = Color.White,
    onBackground = PolarNight0,
    onSurface = PolarNight0
)

@Composable
fun FOSSRSVPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // We disable dynamic color by default to enforce our premium theme, 
    // but the user can re-enable it if they wish (logic below still supports it)
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
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
