package com.quotegarden.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuoteDto(
    val id: String,
    val content: String,
    val translation: String? = null,
    val author: String? = null,
    val category: String? = null,
    val difficulty: Int? = null,
    val audioUrl: String? = null,
    val imageUrl: String? = null,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class SyncResponse(
    val items: List<QuoteDto>,
    val nextCursor: String? = null
)
