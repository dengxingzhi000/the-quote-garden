package com.dailymind.core.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacingTest {
    @Test fun `spacing scale matches editorial spec`() {
        assertEquals(4.dp, EditorialSpacing.Xs)
        assertEquals(8.dp, EditorialSpacing.Sm)
        assertEquals(16.dp, EditorialSpacing.Md)
        assertEquals(24.dp, EditorialSpacing.Lg)
        assertEquals(32.dp, EditorialSpacing.Xl)
        assertEquals(48.dp, EditorialSpacing.Xxl)
        assertEquals(64.dp, EditorialSpacing.Hero)
    }
}
