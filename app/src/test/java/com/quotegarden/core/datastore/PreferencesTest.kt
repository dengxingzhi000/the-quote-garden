package com.quotegarden.core.datastore

import org.junit.Assert.assertNotNull
import org.junit.Test

class PreferencesTest {
    @Test fun `lastSync key exists`() {
        assertNotNull(PreferencesKeys.LAST_SYNC)
    }

    @Test fun `daily pin keys exist`() {
        assertNotNull(PreferencesKeys.DAILY_QUOTE_DATE)
        assertNotNull(PreferencesKeys.DAILY_QUOTE_ID)
    }

    @Test fun `category preference key exists`() {
        assertNotNull(PreferencesKeys.CATEGORY)
    }
}
