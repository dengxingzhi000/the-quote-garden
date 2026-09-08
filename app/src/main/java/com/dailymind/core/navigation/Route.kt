package com.dailymind.core.navigation

sealed interface Route {
    data object Home : Route
    data object Me : Route
    data object History : Route
    data object Profile : Route
}
