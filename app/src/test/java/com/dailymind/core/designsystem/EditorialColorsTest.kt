package com.dailymind.core.designsystem

import com.dailymind.core.designsystem.theme.LightQuietLuxury
import com.dailymind.core.designsystem.theme.DarkQuietLuxury
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialColorsTest {
    @Test fun `light scheme matches spec hex`() {
        assertEquals(0xFFF7F5F0u, LightQuietLuxury.background.value)
        assertEquals(0xFF171717u, LightQuietLuxury.onBackground.value)
        assertEquals(0xFF77736Bu, LightQuietLuxury.onSurfaceVariant.value)
        assertEquals(0xFFDDD9D0u, LightQuietLuxury.outlineVariant.value)
        assertEquals(0xFF8A5A44u, LightQuietLuxury.primary.value)
    }

    @Test fun `dark scheme matches spec hex`() {
        assertEquals(0xFF151515u, DarkQuietLuxury.background.value)
        assertEquals(0xFFF2EFE8u, DarkQuietLuxury.onBackground.value)
        assertEquals(0xFFA7A39Bu, DarkQuietLuxury.onSurfaceVariant.value)
        assertEquals(0xFF383838u, DarkQuietLuxury.outlineVariant.value)
        assertEquals(0xFFC49A7Au, DarkQuietLuxury.primary.value)
    }
}
