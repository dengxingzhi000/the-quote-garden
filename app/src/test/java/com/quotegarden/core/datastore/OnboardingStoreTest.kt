package com.quotegarden.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStoreTest {
    private fun makeStore(): Pair<DataStore<Preferences>, MutablePreferences> {
        val backing = mutablePreferencesOf()
        val flow = MutableStateFlow<Preferences>(backing)
        val store = mockk<DataStore<Preferences>>(relaxed = false)
        every { store.data } returns flow
        coEvery { store.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val current = flow.value
            val mutable = current.toMutablePreferences()
            val result = transform(mutable)
            flow.value = result
            result
        }
        return store to backing
    }

    @Test fun `seen defaults to false when key absent`() = runTest {
        val (store, _) = makeStore()
        assertEquals(false, OnboardingStore(store).seen.first())
    }

    @Test fun `setSeen persists true`() = runTest {
        val (store, _) = makeStore()
        val s = OnboardingStore(store)
        s.setSeen()
        assertEquals(true, s.seen.first())
    }
}
