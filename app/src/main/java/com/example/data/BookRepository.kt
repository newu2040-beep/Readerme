package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookRepository(private val bookDao: BookDao) {

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    val allHighlights: Flow<List<Highlight>> = bookDao.getAllHighlights()

    val listeningStats: Flow<List<ListeningStat>> = bookDao.getListeningStats()

    suspend fun getBookById(id: Int): Book? = bookDao.getBookById(id)

    fun getBookByIdFlow(id: Int): Flow<Book?> = bookDao.getBookByIdFlow(id)

    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: Book) = bookDao.updateBook(book)

    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    suspend fun deleteBookById(id: Int) = bookDao.deleteBookById(id)

    suspend fun getBookCount(): Int = bookDao.getBookCount()

    // Bookmarks API
    fun getBookmarksForBook(bookId: Int): Flow<List<Bookmark>> = bookDao.getBookmarksForBook(bookId)

    suspend fun insertBookmark(bookmark: Bookmark) = bookDao.insertBookmark(bookmark)

    suspend fun deleteBookmarkById(id: Int) = bookDao.deleteBookmarkById(id)

    // Highlights API
    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>> = bookDao.getHighlightsForBook(bookId)

    suspend fun insertHighlight(highlight: Highlight) = bookDao.insertHighlight(highlight)

    suspend fun deleteHighlightById(id: Int) = bookDao.deleteHighlightById(id)

    // Stats and Streaks API
    suspend fun recordReadingEvent(seconds: Int, words: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val existing = bookDao.getListeningStatByDate(todayStr)
        if (existing != null) {
            val updated = existing.copy(
                secondsListened = existing.secondsListened + seconds,
                wordsRead = existing.wordsRead + words
            )
            bookDao.insertListeningStat(updated)
        } else {
            val newStat = ListeningStat(
                dateString = todayStr,
                secondsListened = seconds,
                wordsRead = words
            )
            bookDao.insertListeningStat(newStat)
        }
    }
}
