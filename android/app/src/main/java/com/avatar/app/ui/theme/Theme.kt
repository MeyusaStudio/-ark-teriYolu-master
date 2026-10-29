package com.avatar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGreen,
    error = ErrorRed,
    background = BackgroundGray,
    surface = SurfaceWhite,
)

@Composable
fun AvatarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
