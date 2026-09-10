package com.quotegarden.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.quotegarden.core.designsystem.BottomTab
import com.quotegarden.core.designsystem.EditorialBottomBar
import com.quotegarden.feature.browse.BrowseScreen
import com.quotegarden.feature.browse.CategoryDetailScreen
import com.quotegarden.feature.home.HomeScreen
import com.quotegarden.feature.me.MeScreen
import com.quotegarden.feature.onboarding.OnboardingScreen
import com.quotegarden.feature.onboarding.OnboardingViewModel

private val Tabs = listOf(
    BottomTab("home", "Today"),
    BottomTab("browse", "Browse"),
    BottomTab("me", "Me"),
)

private fun Route.tabId(): String = when (this) {
    is Route.Home -> "home"
    is Route.Me -> "me"
    is Route.Browse, is Route.CategoryDetail -> "browse"
    else -> "home"
}

@Composable
fun AppNavDisplay(
    onboardingVm: OnboardingViewModel = hiltViewModel(),
) {
    val seen by onboardingVm.seen.collectAsStateWithLifecycle()
    if (!seen) {
        OnboardingScreen(onDone = { onboardingVm.markSeen() })
        return
    }
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    fun select(route: Route) {
        backStack.remove(route)
        backStack.add(route)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { route ->
                when (route) {
                    is Route.Home -> NavEntry(route) { HomeScreen() }
                    is Route.Me -> NavEntry(route) { MeScreen(onExplore = { select(Route.Home) }) }
                    is Route.Browse -> NavEntry(route) {
                        BrowseScreen(
                            onSelectCategory = { backStack.add(Route.CategoryDetail(it)) },
                            onGoHome = { select(Route.Home) }
                        )
                    }
                    is Route.CategoryDetail -> NavEntry(route) {
                        CategoryDetailScreen(
                            category = route.category,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    else -> NavEntry(route) { }
                }
            },
            modifier = Modifier.weight(1f)
        )
        EditorialBottomBar(
            tabs = Tabs,
            selectedId = backStack.lastOrNull()?.tabId() ?: "home",
            onSelect = { id ->
                select(
                    when (id) {
                        "me" -> Route.Me
                        "browse" -> Route.Browse
                        else -> Route.Home
                    }
                )
            }
        )
    }
}
