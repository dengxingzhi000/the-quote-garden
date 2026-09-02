package com.dailymind.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dailymind.feature.favorite.FavoriteScreen
import com.dailymind.feature.home.HomeScreen

@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { route ->
            when (route) {
                is Route.Home -> NavEntry(route) { HomeScreen(onNavigateToFavorite = { backStack.add(Route.Favorite) }) }
                is Route.Favorite -> NavEntry(route) { FavoriteScreen() }
                else -> NavEntry(route) { }
            }
        }
    )
}
