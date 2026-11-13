// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Clean neutral surfaces with a single blue accent: true-black background and soft grey cards in
// dark mode, light grey background and white cards in light mode.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF5B8CFF),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE9EBEF),
    onSurfaceVariant = Color(0xFF7A7F89),
    outlineVariant = Color(0xFFD5D8DE),
    error = Color(0xFFD93B3B),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF0F1F4),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE2E4E9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3D7BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF6C9BFF),
    background = Color.Black,
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF2A2A2D),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF48484B),
    error = Color(0xFFFF6B6B),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF151517),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = Color(0xFF2C2C2E),
)

/** Everything is rounded: no sharp corners anywhere (dialogs, menus, fields, chips, cards). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own look; flip to true to follow Material You wallpaper colours.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
