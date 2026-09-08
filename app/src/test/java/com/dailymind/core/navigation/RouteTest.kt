package com.dailymind.core.navigation

import org.junit.Assert.*
import org.junit.Test

class RouteTest {
    @Test fun `routes are distinct`() {
        assertTrue(Route.Home != Route.Favorite)
    }
}
