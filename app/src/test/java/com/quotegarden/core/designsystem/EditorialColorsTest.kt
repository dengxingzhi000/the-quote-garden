package com.quotegarden.core.designsystem

import androidx.compose.ui.graphics.Color
import com.quotegarden.core.designsystem.theme.LightQuietLuxury
import com.quotegarden.core.designsystem.theme.DarkQuietLuxury
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialColorsTest {
    @Test fun `light scheme matches spec hex`() {
        assertEquals(Color(0xFF8A5A44), LightQuietLuxury.primary)
        assertEquals(Color(0xFFF7F5F0), LightQuietLuxury.onPrimary)
        assertEquals(Color(0xFFF7F5F0), LightQuietLuxury.background)
        assertEquals(Color(0xFF171717), LightQuietLuxury.onBackground)
        assertEquals(Color(0xFFF7F5F0), LightQuietLuxury.surface)
        assertEquals(Color(0xFF171717), LightQuietLuxury.onSurface)
        assertEquals(Color(0xFFF7F5F0), LightQuietLuxury.surfaceVariant)
        assertEquals(Color(0xFF77736B), LightQuietLuxury.onSurfaceVariant)
        assertEquals(Color(0xFFDDD9D0), LightQuietLuxury.outline)
        assertEquals(Color(0xFFDDD9D0), LightQuietLuxury.outlineVariant)
        assertEquals(Color(0xFF9C3B2E), LightQuietLuxury.error)
        assertEquals(Color(0xFFF7F5F0), LightQuietLuxury.onError)
    }

    @Test fun `dark scheme matches spec hex`() {
        assertEquals(Color(0xFFC49A7A), DarkQuietLuxury.primary)
        assertEquals(Color(0xFF151515), DarkQuietLuxury.onPrimary)
        assertEquals(Color(0xFF151515), DarkQuietLuxury.background)
        assertEquals(Color(0xFFF2EFE8), DarkQuietLuxury.onBackground)
        assertEquals(Color(0xFF151515), DarkQuietLuxury.surface)
        assertEquals(Color(0xFFF2EFE8), DarkQuietLuxury.onSurface)
        assertEquals(Color(0xFF151515), DarkQuietLuxury.surfaceVariant)
        assertEquals(Color(0xFFA7A39B), DarkQuietLuxury.onSurfaceVariant)
        assertEquals(Color(0xFF383838), DarkQuietLuxury.outline)
        assertEquals(Color(0xFF383838), DarkQuietLuxury.outlineVariant)
        assertEquals(Color(0xFFD18A7A), DarkQuietLuxury.error)
        assertEquals(Color(0xFF151515), DarkQuietLuxury.onError)
    }
}
