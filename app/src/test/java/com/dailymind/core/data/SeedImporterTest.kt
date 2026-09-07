package com.dailymind.core.data

import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SeedImporterTest {
    private val sampleJson = """
        {"items":[
            {"id":"a","content":"Hello","translation":null,"author":null,"category":"life","difficulty":null,"audioUrl":null,"imageUrl":null,"updatedAt":1000,"deletedAt":null},
            {"id":"b","content":"World","translation":"你好","author":"Anon","category":"love","difficulty":2,"audioUrl":null,"imageUrl":null,"updatedAt":2000,"deletedAt":null}
        ]}
    """.trimIndent()

    @Test fun `import parses JSON and inserts all quotes`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val captured = slot<List<QuoteEntity>>()
        val count = SeedImporter(dao).import(sampleJson)
        coVerify { dao.upsertAll(capture(captured)) }
        assertEquals(2, count)
        assertEquals("Hello", captured.captured[0].content)
        assertEquals("你好", captured.captured[1].translation)
    }

    @Test fun `import returns count of inserted items`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val count = SeedImporter(dao).import(sampleJson)
        assertEquals(2, count)
    }

    @Test fun `import returns 0 for empty items list`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val count = SeedImporter(dao).import("""{"items":[]}""")
        assertEquals(0, count)
    }

    @Test fun `importIfEmpty skips when DB already has quotes`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        io.mockk.coEvery { dao.getAll() } returns listOf(
            com.dailymind.core.database.entity.QuoteEntity("existing", "x", null, null, null, null, null, null, 1L, null)
        )
        val count = SeedImporter(dao).importIfEmpty(sampleJson)
        assertEquals(0, count)
        io.mockk.coVerify(exactly = 0) { dao.upsertAll(any()) }
    }

    @Test fun `importIfEmpty imports when DB is empty`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        io.mockk.coEvery { dao.getAll() } returns emptyList()
        val count = SeedImporter(dao).importIfEmpty(sampleJson)
        assertEquals(2, count)
        io.mockk.coVerify { dao.upsertAll(any()) }
    }
}
