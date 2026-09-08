package com.dailymind.core.datastore

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
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryPreferenceStoreTest {
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

    @Test fun `selectedCategory defaults to null when key absent`() = runTest {
        val (store, _) = makeStore()
        val s = CategoryPreferenceStore(store)
        assertNull(s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory(value) writes the value`() = runTest {
        val (store, _) = makeStore()
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory("\u52b1\u5fd7")
        assertEquals("\u52b1\u5fd7", s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory(null) clears the value`() = runTest {
        val (store, backing) = makeStore()
        backing.set(PreferencesKeys.CATEGORY, "\u52b1\u5fd7")
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory(null)
        assertNull(s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory is safe to call twice`() = runTest {
        val (store, _) = makeStore()
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory("\u52b1\u5fd7")
        s.setSelectedCategory("\u7231\u60c5")
        assertEquals("\u7231\u60c5", s.selectedCategory.first())
    }
}
