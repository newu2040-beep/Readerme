package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val content: String,
    val category: String, // "Ebooks", "Imports", "Notes", "Articles"
    val format: String, // "TXT", "PDF", "EPUB", "CLIP"
    val dateAdded: Long = System.currentTimeMillis(),
    val lastReadPosition: Int = 0, // sentence index
    val isFavorite: Boolean = false,
    val rating: Float = 0f,
    val coverColorHex: String = "#FF6B6B" // Hex string for elegant gradient cover
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val sentenceIndex: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val sentenceIndex: Int,
    val text: String,
    val note: String = "",
    val colorHex: String = "#FFF59D", // yellow, pink, mint highlights
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_stats")
data class ListeningStat(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val secondsListened: Int,
    val wordsRead: Int
)
