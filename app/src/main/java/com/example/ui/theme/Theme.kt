package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MvaRedPrimaryDark,
    onPrimary = MvaWhite,
    primaryContainer = MvaRedContainerDark,
    onPrimaryContainer = MvaRedOnContainerDark,
    secondary = MvaRedSecondaryDark,
    onSecondary = MvaWhite,
    secondaryContainer = MvaRedContainerDark,
    onSecondaryContainer = MvaRedOnContainerDark,
    tertiary = MvaTextSecondaryDark,
    background = MvaBackgroundDark,
    onBackground = MvaTextPrimaryDark,
    surface = MvaSurfaceDark,
    onSurface = MvaTextPrimaryDark,
    surfaceVariant = MvaSurfaceVariantDark,
    onSurfaceVariant = MvaTextSecondaryDark,
    outline = MvaOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MvaRedPrimary,
    onPrimary = MvaWhite,
    primaryContainer = MvaRedLightContainer,
    onPrimaryContainer = MvaRedOnLightContainer,
    secondary = MvaRedSecondary,
    onSecondary = MvaWhite,
    secondaryContainer = MvaRedLightContainer,
    onSecondaryContainer = MvaRedOnLightContainer,
    tertiary = MvaTextSecondaryLight,
    background = MvaBackgroundLight,
    onBackground = MvaTextPrimaryLight,
    surface = MvaSurfaceLight,
    onSurface = MvaTextPrimaryLight,
    surfaceVariant = MvaSurfaceVariantLight,
    onSurfaceVariant = MvaTextSecondaryLight,
    outline = MvaOutlineLight
)

@Composable
fun MVABusinessChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
