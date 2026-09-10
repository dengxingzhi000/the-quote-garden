package com.quotegarden.core.datastore

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {
    @Test fun `LIGHT always resolves to false`() {
        assertFalse(ThemeMode.LIGHT.resolveDark(true))
        assertFalse(ThemeMode.LIGHT.resolveDark(false))
    }

    @Test fun `DARK always resolves to true`() {
        assertTrue(ThemeMode.DARK.resolveDark(true))
        assertTrue(ThemeMode.DARK.resolveDark(false))
    }

    @Test fun `SYSTEM mirrors system flag`() {
        assertTrue(ThemeMode.SYSTEM.resolveDark(true))
        assertFalse(ThemeMode.SYSTEM.resolveDark(false))
    }
}
