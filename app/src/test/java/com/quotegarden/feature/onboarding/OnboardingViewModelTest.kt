package com.quotegarden.feature.onboarding

import com.quotegarden.MainDispatcherRule
import com.quotegarden.core.datastore.OnboardingStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `markSeen delegates to store`() = runTest {
        val store = mockk<OnboardingStore>(relaxed = true)
        every { store.seen } returns flowOf(false)
        val vm = OnboardingViewModel(store)
        vm.markSeen()
        coVerify { store.setSeen() }
    }
}
