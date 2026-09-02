package com.dailymind.core.datastore

import org.junit.Assert.*
import org.junit.Test

class PreferencesTest {
    @Test fun `lastSync key exists`() {
        assertNotNull(PreferencesKeys.LAST_SYNC)
    }
}
