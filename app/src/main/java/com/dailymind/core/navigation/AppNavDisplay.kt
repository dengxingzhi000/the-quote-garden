package com.dailymind.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dailymind.core.designsystem.BottomTab
import com.dailymind.core.designsystem.EditorialBottomBar
import com.dailymind.feature.home.HomeScreen
import com.dailymind.feature.me.MeScreen

private val Tabs = listOf(BottomTab("home", "Today"), BottomTab("me", "Me"))

private fun Route.tabId(): String = when (this) {
    is Route.Home -> "home"
    is Route.Me -> "me"
    else -> "home"
}

@Composable
fun AppNavDisplay() {
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
                    else -> NavEntry(route) { }
                }
            },
            modifier = Modifier.weight(1f)
        )
        EditorialBottomBar(
            tabs = Tabs,
            selectedId = backStack.lastOrNull()?.tabId() ?: "home",
            onSelect = { id -> select(if (id == "me") Route.Me else Route.Home) }
        )
    }
}
