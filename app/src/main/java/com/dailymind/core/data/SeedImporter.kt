package com.dailymind.core.data

import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.network.dto.SyncResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SeedImporter @Inject constructor(
    private val dao: QuoteDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importIfEmpty(jsonString: String): Int {
        if (dao.getAll().isNotEmpty()) return 0
        return import(jsonString)
    }

    suspend fun import(jsonString: String): Int {
        val response = json.decodeFromString(SyncResponse.serializer(), jsonString)
        if (response.items.isEmpty()) return 0
        dao.upsertAll(response.items.map { it.toEntity() })
        return response.items.size
    }

    private fun com.dailymind.core.network.dto.QuoteDto.toEntity() =
        QuoteEntity(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt, deletedAt)
}
