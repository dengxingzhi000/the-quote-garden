package com.quotegarden.core.model

import com.quotegarden.core.common.Result
import org.junit.Test
import org.junit.Assert.*

class QuoteTest {
    @Test fun `quote mapping preserves fields`() {
        val q = Quote(id = "1", content = "Hello", translation = "你好", author = "Anon", category = "life", difficulty = 1, audioUrl = null, imageUrl = null, updatedAt = 1000L)
        assertEquals("Hello", q.content)
        val r: Result<Quote> = Result.Success(q)
        assertTrue(r is Result.Success)
    }
}
