package com.dailymind.core.model

data class Quote(
    val id: String,
    val content: String,
    val translation: String?,
    val author: String?,
    val category: String?,
    val difficulty: Int?,
    val audioUrl: String?,
    val imageUrl: String?,
    val updatedAt: Long
)
