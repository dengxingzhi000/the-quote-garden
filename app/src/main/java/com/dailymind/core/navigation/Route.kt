package com.dailymind.core.navigation

sealed interface Route {
    data object Home : Route
    data object Favorite : Route
    data object History : Route
    data object Profile : Route
}
