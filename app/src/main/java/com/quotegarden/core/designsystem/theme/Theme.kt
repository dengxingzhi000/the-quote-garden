package com.quotegarden.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.quotegarden.core.designsystem.LocalSpacing
import com.quotegarden.core.designsystem.Spacing

@Composable
fun QuoteGardenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkQuietLuxury else LightQuietLuxury,
        typography = EditorialTypography,
        content = {
            CompositionLocalProvider(LocalSpacing provides Spacing()) {
                content()
            }
        }
    )
}
