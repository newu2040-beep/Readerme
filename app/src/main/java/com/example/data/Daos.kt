package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): Book?

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookByIdFlow(id: Int): Flow<Book?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Int)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    // Bookmarks Section
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY sentenceIndex ASC")
    fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    // Highlights Section
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY sentenceIndex ASC")
    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Int)

    // Stats Section
    @Query("SELECT * FROM listening_stats ORDER BY dateString DESC")
    fun getListeningStats(): Flow<List<ListeningStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListeningStat(stat: ListeningStat)

    @Query("SELECT * FROM listening_stats WHERE dateString = :dateString")
    suspend fun getListeningStatByDate(dateString: String): ListeningStat?
}
