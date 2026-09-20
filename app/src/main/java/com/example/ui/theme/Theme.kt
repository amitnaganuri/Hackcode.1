package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val FocusGuardColorScheme = darkColorScheme(
    primary = FocusPrimary,
    onPrimary = FocusOnPrimary,
    primaryContainer = FocusPrimaryContainer,
    onPrimaryContainer = FocusOnPrimaryContainer,
    inversePrimary = FocusPrimaryFixed,
    secondary = FocusSecondary,
    onSecondary = FocusOnSecondary,
    secondaryContainer = FocusSecondaryContainer,
    onSecondaryContainer = FocusOnSecondaryContainer,
    tertiary = FocusTertiary,
    onTertiary = FocusOnTertiary,
    tertiaryContainer = FocusTertiaryContainer,
    onTertiaryContainer = FocusOnTertiaryContainer,
    background = FocusSurface,
    onBackground = FocusOnSurface,
    surface = FocusSurface,
    onSurface = FocusOnSurface,
    surfaceVariant = FocusSurfaceVariant,
    onSurfaceVariant = FocusOnSurfaceVariant,
    surfaceContainerLowest = FocusSurfaceContainerLowest,
    surfaceContainerLow = FocusSurfaceContainerLow,
    surfaceContainer = FocusSurfaceContainer,
    surfaceContainerHigh = FocusSurfaceContainerHigh,
    surfaceContainerHighest = FocusSurfaceContainerHighest,
    outline = FocusOutline,
    outlineVariant = FocusOutlineVariant,
    error = FocusError,
    onError = FocusOnError,
    errorContainer = FocusErrorContainer,
    onErrorContainer = FocusOnErrorContainer,
    inverseSurface = FocusInverseSurface,
    inverseOnSurface = FocusInverseOnSurface
)

@Composable
fun FocusGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocusGuardColorScheme,
        typography = Typography,
        content = content
    )
}
