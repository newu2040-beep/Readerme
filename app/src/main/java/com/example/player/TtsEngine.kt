package com.example.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private val _currentWordRange = MutableStateFlow<Pair<Int, Int>?>(null) // (startChar, endChar) in current sentence
    val currentWordRange: StateFlow<Pair<Int, Int>?> = _currentWordRange.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    // Configuration states
    private var speed = 1.0f
    private var pitch = 1.0f
    private var currentVoiceStyle = "Calm Narrator" // Default style

    // Book variables
    private var sentences = listOf<String>()
    private var originalContent = ""
    private var activeBookId = -1
    private var activeBookTitle = "ReaderMe Book"
    private var activeBookAuthor = "Unknown Author"
    private var onProgressCallback: (suspend (sentenceIndex: Int, progressPercent: Float) -> Unit)? = null
    private var onReadingTrackerCallback: (suspend (seconds: Int, words: Int) -> Unit)? = null

    // Coroutines scope for background timers and repository updates
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var sessionTrackerJob: Job? = null
    private var playStartTime: Long = 0L
    private var accumulatedSeconds = 0

    init {
        tts = TextToSpeech(context.applicationContext, this)
        instance = this
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            setupProgressListener()
            applyVoiceConfig()
        } else {
            Log.e("TtsEngine", "Failed to initialize standard TextToSpeech engine")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                val index = utteranceId?.toIntOrNull() ?: 0
                _currentSentenceIndex.value = index
                _currentWordRange.value = null

                // Fire callback to save sentence index
                val progress = if (sentences.isNotEmpty()) index.toFloat() / sentences.size else 0f
                scope.launch {
                    onProgressCallback?.invoke(index, progress)
                }
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: 0
                if (index + 1 < sentences.size) {
                    _currentSentenceIndex.value = index + 1
                    speakSentence(index + 1)
                } else {
                    stop()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Real-time word-by-word tracking!
                _currentWordRange.value = Pair(start, end)
            }
        })
    }

    // Set active book content and parsing
    fun loadBook(bookId: Int, title: String, author: String, content: String, lastIndex: Int, progressListener: suspend (Int, Float) -> Unit, readingTracker: suspend (Int, Int) -> Unit) {
        activeBookId = bookId
        activeBookTitle = title
        activeBookAuthor = author
        originalContent = content
        sentences = splitTextIntoSentences(content)
        _currentSentenceIndex.value = if (lastIndex in sentences.indices) lastIndex else 0
        onProgressCallback = progressListener
        onReadingTrackerCallback = readingTracker
        stop()
    }

    fun play() {
        if (!isInitialized || sentences.isEmpty()) return
        _isPlaying.value = true
        playStartTime = System.currentTimeMillis()
        speakSentence(_currentSentenceIndex.value)
        startSessionTracker()
        updateServiceNotification(PlaybackService.ACTION_PLAY)
    }

    fun pause() {
        tts?.stop()
        _isPlaying.value = false
        stopSessionTracker()
        updateServiceNotification(PlaybackService.ACTION_PAUSE)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentWordRange.value = null
        stopSessionTracker()
        try {
            context.stopService(Intent(context, PlaybackService::class.java))
        } catch (e: Exception) {
            Log.w("TtsEngine", "Service stop failed: ${e.message}")
        }
    }

    private fun updateServiceNotification(action: String) {
        try {
            val intent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
                putExtra("EXTRA_BOOK_TITLE", activeBookTitle)
                putExtra("EXTRA_BOOK_AUTHOR", activeBookAuthor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("TtsEngine", "Failed to update notification service: ${e.message}")
        }
    }

    fun nextSentence() {
        if (_currentSentenceIndex.value + 1 < sentences.size) {
            _currentSentenceIndex.value += 1
            if (_isPlaying.value) {
                speakSentence(_currentSentenceIndex.value)
            }
        }
    }

    fun previousSentence() {
        if (_currentSentenceIndex.value - 1 >= 0) {
            _currentSentenceIndex.value -= 1
            if (_isPlaying.value) {
                speakSentence(_currentSentenceIndex.value)
            }
        }
    }

    fun skipToSentence(index: Int) {
        if (index in sentences.indices) {
            _currentSentenceIndex.value = index
            if (_isPlaying.value) {
                speakSentence(index)
            }
        }
    }

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed
        tts?.setSpeechRate(speed)
    }

    fun setPitch(newPitch: Float) {
        pitch = newPitch
        tts?.setPitch(pitch)
    }

    fun getSpeed() = speed
    fun getPitch() = pitch
    fun getVoiceStyle() = currentVoiceStyle
    fun getSentences() = sentences

    // Set preset voice types
    fun selectVoiceStyle(style: String) {
        currentVoiceStyle = style
        applyVoiceConfig()
    }

    private fun applyVoiceConfig() {
        if (!isInitialized || tts == null) return

        when (currentVoiceStyle) {
            "Calm Narrator" -> {
                pitch = 0.95f
                speed = 0.90f
            }
            "Deep Audiobook" -> {
                pitch = 0.85f
                speed = 0.95f
            }
            "Smooth Female" -> {
                pitch = 1.15f
                speed = 1.0f
            }
            "Character/Cartoon" -> {
                pitch = 1.45f
                speed = 1.10f
            }
            "Robotic/Minimal" -> {
                pitch = 1.0f
                speed = 1.25f
            }
            else -> { // Classic
                pitch = 1.0f
                speed = 1.00f
            }
        }

        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)

        // Try to match System Voice Engine details (Offline gender configurations)
        try {
            val systemVoices = tts?.voices
            if (!systemVoices.isNullOrEmpty()) {
                val locale = Locale.getDefault()
                val isFemaleMatched = currentVoiceStyle == "Smooth Female" || currentVoiceStyle == "Character/Cartoon"
                val filtered = systemVoices.filter {
                    it.locale.language == locale.language &&
                    (!it.isNetworkConnectionRequired) // Highly optimized offline voices
                }

                if (filtered.isNotEmpty()) {
                    val matchingVoice = if (isFemaleMatched) {
                        filtered.firstOrNull { it.name.contains("female", ignoreCase = true) || it.features.contains("female") }
                            ?: filtered.first()
                    } else {
                        filtered.firstOrNull { it.name.contains("male", ignoreCase = true) || it.features.contains("male") }
                            ?: filtered.first()
                    }
                    tts?.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            Log.w("TtsEngine", "Custom offline voice matching mapping skipped: ${e.message}")
        }
    }

    private fun speakSentence(index: Int) {
        if (!isInitialized || index !in sentences.indices) return
        val text = sentences[index]
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    // Sleep Timer Support
    fun startSleepTimer(minutes: Int) {
        timerJob?.cancel()
        _sleepTimerSeconds.value = minutes * 60
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _sleepTimerSeconds.value ?: 0
                if (current > 1) {
                    _sleepTimerSeconds.value = current - 1
                } else {
                    _sleepTimerSeconds.value = null
                    pause()
                    cancel()
                }
            }
        }
    }

    fun stopSleepTimer() {
        timerJob?.cancel()
        _sleepTimerSeconds.value = null
    }

    // Capture reading and listening stats (Words Spoken + Seconds Listened)
    private fun startSessionTracker() {
        sessionTrackerJob?.cancel()
        playStartTime = System.currentTimeMillis()
        sessionTrackerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000) // update every 5 seconds to throttle DB writes
                val elapsedSeconds = ((System.currentTimeMillis() - playStartTime) / 1000).toInt() + accumulatedSeconds
                if (elapsedSeconds > 0) {
                    // Calculate roughly how many words were read based on current reading position progress
                    val sentenceIndex = _currentSentenceIndex.value
                    var wordsCountEstimate = 0
                    for (i in 0..sentenceIndex) {
                        if (i in sentences.indices) {
                            wordsCountEstimate += sentences[i].split("\\s+".toRegex()).size
                        }
                    }

                    // Report reading progress event to repository safely
                    scope.launch {
                        onReadingTrackerCallback?.invoke(elapsedSeconds, wordsCountEstimate)
                    }
                }
            }
        }
    }

    private fun stopSessionTracker() {
        sessionTrackerJob?.cancel()
        val sessionDuration = if (playStartTime > 0) ((System.currentTimeMillis() - playStartTime) / 1000).toInt() else 0
        accumulatedSeconds += sessionDuration
        playStartTime = 0L
    }

    fun onDestroy() {
        timerJob?.cancel()
        sessionTrackerJob?.cancel()
        scope.cancel()
        tts?.shutdown()
        try {
            context.stopService(Intent(context, PlaybackService::class.java))
        } catch (e: Exception) {
            // ignore
        }
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: TtsEngine? = null

        fun splitTextIntoSentences(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            val sentences = mutableListOf<String>()
            val matcher = java.util.regex.Pattern.compile("[^.!?]+([.!?]+|$)").matcher(text)
            while (matcher.find()) {
                val sentence = matcher.group().trim()
                if (sentence.isNotEmpty()) {
                    sentences.add(sentence)
                }
            }
            return if (sentences.isEmpty()) listOf(text) else sentences
        }
    }
}
