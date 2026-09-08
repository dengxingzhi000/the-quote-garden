package com.dailymind.core.navigation

import org.junit.Assert.*
import org.junit.Test

class RouteTest {
    @Test fun `routes are distinct`() {
        assertNotEquals(Route.Home, Route.Me)
        assertNotEquals(Route.Home, Route.Browse)
        assertNotEquals(Route.Me, Route.Browse)
        assertNotEquals(Route.Browse, Route.CategoryDetail("love"))
    }

    @Test fun `CategoryDetail carries the category string`() {
        assertEquals("love", Route.CategoryDetail("love").category)
    }
}
