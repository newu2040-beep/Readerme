package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.player.TtsEngine
import com.example.ui.theme.ReaderModeStyle
import com.example.ui.theme.ReaderThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = BookRepository(database.bookDao())
    val ttsEngine = TtsEngine(application)

    // Data lists
    val allBooks: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHighlights: StateFlow<List<Highlight>> = repository.allHighlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningStats: StateFlow<List<ListeningStat>> = repository.listeningStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active reading selections
    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    // Preferences and styles
    private val _currentPalette = MutableStateFlow(ReaderThemePalette.LAVENDER)
    val currentPalette: StateFlow<ReaderThemePalette> = _currentPalette.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _readerMode = MutableStateFlow(ReaderModeStyle.STANDARD)
    val readerMode: StateFlow<ReaderModeStyle> = _readerMode.asStateFlow()

    private val _fontSizeMultiplier = MutableStateFlow(1.0f) // 0.8f to 2.0f
    val fontSizeMultiplier: StateFlow<Float> = _fontSizeMultiplier.asStateFlow()

    // Navigation and screen route index
    private val _activeTab = MutableStateFlow(0) // 0 = Library, 1 = Player, 2 = Stats, 3 = Highlights Hub
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Reading statistics summary
    val streakCount = listeningStats.map { stats ->
        calculateStreak(stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayWords = listeningStats.map { stats ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        stats.firstOrNull { it.dateString == today }?.wordsRead ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayMinutes = listeningStats.map { stats ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val seconds = stats.firstOrNull { it.dateString == today }?.secondsListened ?: 0
        seconds / 60
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isPlaying = ttsEngine.isPlaying
    val currentSentenceIndex = ttsEngine.currentSentenceIndex
    val currentWordRange = ttsEngine.currentWordRange
    val sleepTimerSeconds = ttsEngine.sleepTimerSeconds

    init {
        // Automatically check last active book on start
        viewModelScope.launch {
            allBooks.collectLatest { list ->
                if (list.isNotEmpty() && _selectedBook.value == null) {
                    val lastBook = list.first() // Select the most recently active
                    _selectedBook.value = lastBook
                    loadBookToPlayer(lastBook)
                }
            }
        }
    }

    // Load active book into player
    fun selectBook(book: Book) {
        _selectedBook.value = book
        loadBookToPlayer(book)
    }

    private fun loadBookToPlayer(book: Book) {
        ttsEngine.loadBook(
            bookId = book.id,
            content = book.content,
            lastIndex = book.lastReadPosition,
            progressListener = { sentenceIndex, progressPercent ->
                viewModelScope.launch(Dispatchers.IO) {
                    // Save reading position reactively in the background
                    val updatedBook = book.copy(lastReadPosition = sentenceIndex)
                    repository.updateBook(updatedBook)
                }
            },
            readingTracker = { seconds, words ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.recordReadingEvent(seconds, words)
                }
            }
        )
    }

    // Import Clipboard & custom notes
    fun importBook(title: String, author: String, content: String, category: String = "Imports", format: String = "TXT") {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultColorHexes = listOf("#FF6B6B", "#4ECDC4", "#FFE66D", "#95A5A6", "#9B59B6", "#1ABC9C", "#F1C40F")
            val chosenColor = defaultColorHexes[Random().nextInt(defaultColorHexes.size)]
            val newBook = Book(
                title = title.ifBlank { "Untitled Clipboard note" },
                author = author.ifBlank { "Unknown Author" },
                content = content,
                category = category,
                format = format,
                coverColorHex = chosenColor
            )
            val newId = repository.insertBook(newBook)
            val fullBook = newBook.copy(id = newId.toInt())
            viewModelScope.launch(Dispatchers.Main) {
                selectBook(fullBook)
            }
        }
    }

    // Toggle Favorite
    fun toggleFavorite(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBook(book.copy(isFavorite = !book.isFavorite))
        }
    }

    // Delete Book
    fun deleteBook(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookById(bookId)
            if (_selectedBook.value?.id == bookId) {
                _selectedBook.value = null
            }
        }
    }

    // Bookmarks and highlights action
    fun addBookmark() {
        val book = _selectedBook.value ?: return
        val sentenceIdx = currentSentenceIndex.value
        val sentences = ttsEngine.getSentences()
        if (sentenceIdx in sentences.indices) {
            viewModelScope.launch(Dispatchers.IO) {
                val sentenceText = sentences[sentenceIdx]
                val shortNote = if (sentenceText.length > 30) sentenceText.take(30) + "..." else sentenceText
                repository.insertBookmark(
                    Bookmark(
                        bookId = book.id,
                        sentenceIndex = sentenceIdx,
                        note = "Bookmark: $shortNote"
                    )
                )
            }
        }
    }

    fun getBookmarksForActiveBook(): Flow<List<Bookmark>> {
        val book = _selectedBook.value ?: return flowOf(emptyList())
        return repository.getBookmarksForBook(book.id)
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmarkById(id)
        }
    }

    // Highlights
    fun addHighlight(colorHex: String = "#FFF59D", note: String = "") {
        val book = _selectedBook.value ?: return
        val sentenceIdx = currentSentenceIndex.value
        val sentences = ttsEngine.getSentences()
        if (sentenceIdx in sentences.indices) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertHighlight(
                    Highlight(
                        bookId = book.id,
                        sentenceIndex = sentenceIdx,
                        text = sentences[sentenceIdx],
                        note = note,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun getHighlightsForActiveBook(): Flow<List<Highlight>> {
        val book = _selectedBook.value ?: return flowOf(emptyList())
        return repository.getHighlightsForBook(book.id)
    }

    fun deleteHighlight(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHighlightById(id)
        }
    }

    // Setters
    fun setPalette(palette: ReaderThemePalette) {
        _currentPalette.value = palette
    }

    fun toggleDarkMode() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setReaderMode(mode: ReaderModeStyle) {
        _readerMode.value = mode
    }

    fun setFontSizeMultiplier(multiplier: Float) {
        _fontSizeMultiplier.value = multiplier.coerceIn(0.7f, 2.2f)
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    // Calculates current daily consecutive streaks
    private fun calculateStreak(stats: List<ListeningStat>): Int {
        if (stats.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsedDates = stats.mapNotNull {
            try {
                dateFormat.parse(it.dateString)
            } catch (e: Exception) {
                null
            }
        }.sortedDescending()

        if (parsedDates.isEmpty()) return 0

        var streak = 0
        val currentCheck = todayCalendar.time

        // Check if there's text read today or yesterday to start streak computation
        val firstDate = parsedDates.first()
        val diffInMs = currentCheck.time - firstDate.time
        val diffInDays = diffInMs / (1000 * 60 * 60 * 24)

        if (diffInDays > 1) {
            // Broken streak
            return 0
        }

        var expectedDateCalendar = Calendar.getInstance().apply {
            time = firstDate
        }

        for (date in parsedDates) {
            val diff = expectedDateCalendar.time.time - date.time
            val days = diff / (1000 * 60 * 60 * 24)
            if (days == 0L) {
                streak++
                expectedDateCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else if (days == 1L) {
                streak++
                expectedDateCalendar.time = date
                expectedDateCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    override fun onCleared() {
        ttsEngine.onDestroy()
        super.onCleared()
    }
}
