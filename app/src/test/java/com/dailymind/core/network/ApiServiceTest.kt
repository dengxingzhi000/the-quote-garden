package com.dailymind.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

class ApiServiceTest {
    @Test fun `get daily quote parses dto`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"id":"1","content":"Hello","translation":"你好","author":"Anon","category":"life","difficulty":1,"audioUrl":null,"imageUrl":null,"updatedAt":1000}"""
            ).setResponseCode(200)
        )
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ApiService::class.java)
        val dto = api.getDailyQuote()
        assertEquals("1", dto.id)
        server.shutdown()
    }
}
