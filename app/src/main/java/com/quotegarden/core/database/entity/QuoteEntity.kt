package com.quotegarden.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quote")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val content: String,
    val translation: String?,
    val author: String?,
    val category: String?,
    val difficulty: Int?,
    val audioUrl: String?,
    val imageUrl: String?,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
