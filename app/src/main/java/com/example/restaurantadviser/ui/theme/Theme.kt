package com.example.restaurantadviser.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = ForegroundBlue80,
    secondary = Red80,
    onSecondary = ForegroundRed80,
    background = Background80,
    onBackground = OnBackGround80,
    onSurface = OnPrimary80
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = ForegroundGreen40,
    secondary = Yellow40,
    onSecondary = ForegroundYellow40,
    background = Background40,
    onBackground = OnBackGround40,
    onSurface = OnPrimary40
)

@Composable
fun RestaurantAdviserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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