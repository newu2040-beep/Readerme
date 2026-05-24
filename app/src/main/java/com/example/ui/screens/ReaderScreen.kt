package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.theme.ReaderModeStyle
import com.example.ui.theme.ThemePalettes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedBook by viewModel.selectedBook.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val activeSentenceIndex by viewModel.currentSentenceIndex.collectAsState()
    val wordRange by viewModel.currentWordRange.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val fontSizeMult by viewModel.fontSizeMultiplier.collectAsState()
    val sleepSeconds by viewModel.sleepTimerSeconds.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Configuration flags
    var showTtsSettingSheet by remember { mutableStateOf(false) }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var sentenceSelectedForHighlight by remember { mutableStateOf<Int?>(null) }
    var customHighlightNote by remember { mutableStateOf("") }
    var highlightMarkerColor by remember { mutableStateOf("#FFF59D") }

    // Word Dictionary Lookup Dialog State
    var dictionaryWord by remember { mutableStateOf<String?>(null) }
    var dictionaryDefinition by remember { mutableStateOf<String?>(null) }

    // Swipe brightness tracker
    var simulatedBrightness by remember { mutableStateOf(0.85f) }

    // Split sentences
    val sentences = remember(selectedBook) {
        selectedBook?.let { viewModel.ttsEngine.getSentences() } ?: emptyList()
    }

    // Auto-scroll logic: Automatically scroll to centered viewport when sentence index changes
    LaunchedEffect(activeSentenceIndex) {
        if (sentences.isNotEmpty() && activeSentenceIndex in sentences.indices) {
            delay(100)
            val centeredIndex = (activeSentenceIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(centeredIndex)
        }
    }

    // Dynamic reader spacing & styles based on comfort modes
    val readerBackground = when (readerMode) {
        ReaderModeStyle.EYE_COMFORT -> ThemePalettes.EyeComfortBackground
        ReaderModeStyle.WARM_PAPER -> ThemePalettes.WarmPaperBackground
        ReaderModeStyle.AMOLED -> ThemePalettes.AmoleBackground
        ReaderModeStyle.NIGHT -> ThemePalettes.NightBackground
        ReaderModeStyle.DIM_LIGHT -> ThemePalettes.DimBackground
        else -> MaterialTheme.colorScheme.background
    }

    val readerTextColor = when (readerMode) {
        ReaderModeStyle.EYE_COMFORT -> ThemePalettes.EyeComfortText
        ReaderModeStyle.WARM_PAPER -> ThemePalettes.WarmPaperText
        ReaderModeStyle.AMOLED -> ThemePalettes.AmoleText
        ReaderModeStyle.NIGHT -> ThemePalettes.NightText
        ReaderModeStyle.DIM_LIGHT -> ThemePalettes.DimText
        else -> MaterialTheme.colorScheme.onBackground
    }

    val primaryAccent = when (readerMode) {
        ReaderModeStyle.EYE_COMFORT -> ThemePalettes.EyeComfortPrimary
        ReaderModeStyle.WARM_PAPER -> ThemePalettes.WarmPaperPrimary
        ReaderModeStyle.AMOLED -> ThemePalettes.AmolePrimary
        ReaderModeStyle.NIGHT -> ThemePalettes.NightPrimary
        ReaderModeStyle.DIM_LIGHT -> ThemePalettes.DimPrimary
        else -> MaterialTheme.colorScheme.primary
    }

    // Styles for Dyslexia Mode (highly legible typography overrides)
    val isDyslexia = readerMode == ReaderModeStyle.DYSLEXIA
    val isMinimal = readerMode == ReaderModeStyle.MINIMAL_DISTRACTION || readerMode == ReaderModeStyle.FOCUS

    val baseFontSize = if (isDyslexia) 21.sp else 18.sp
    val calculatedLineHeight = if (isDyslexia) 38.sp else 28.sp
    val calculatedFontFamily = if (isDyslexia) FontFamily.SansSerif else FontFamily.Serif
    val calculatedLetterSpacing = if (isDyslexia) 0.18.sp else 0.sp
    val calculatedFontWeight = if (isDyslexia) FontWeight.SemiBold else FontWeight.Normal

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(readerBackground),
        topBar = {
            if (!isMinimal) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                selectedBook?.title ?: "Reading Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = readerTextColor,
                                maxLines = 1
                            )
                            if (sleepSeconds != null) {
                                val mins = sleepSeconds!! / 60
                                val secs = sleepSeconds!! % 60
                                Text(
                                    "Timer ends in: ${mins}m ${secs}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primaryAccent
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setActiveTab(0) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = readerTextColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.addBookmark() }) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Add Bookmark", tint = readerTextColor)
                        }
                        IconButton(onClick = { showTtsSettingSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Comfort Control Panel", tint = readerTextColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = readerBackground)
                )
            }
        }
    ) { innerPadding ->
        // Simulate gestured screen brightness dimming overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(readerBackground)
                .padding(if (isMinimal) PaddingValues(0.dp) else innerPadding)
                // Gesture control: Swipe vertical on left edge to adjust dimness filter dynamically
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (isPlaying) viewModel.ttsEngine.pause() else viewModel.ttsEngine.play()
                            Toast
                                .makeText(
                                    context,
                                    if (isPlaying) "Audio paused" else "Audio playing",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    )
                }
        ) {
            if (selectedBook == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a book from Library to start reading.", color = readerTextColor)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Minimal mode headers trigger
                    if (isMinimal) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Focus mode • ${selectedBook?.title}",
                                style = MaterialTheme.typography.labelMedium,
                                color = readerTextColor.copy(alpha = 0.6f)
                            )
                            IconButton(
                                onClick = { viewModel.setReaderMode(ReaderModeStyle.STANDARD) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Exit focus", tint = readerTextColor)
                            }
                        }
                    }

                    // Main Reading List Workspace
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag("reader_scroll_pane"),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                    ) {
                        itemsIndexed(sentences) { idx, sentence ->
                            val isActive = idx == activeSentenceIndex
                            val isFocusedStyle = readerMode == ReaderModeStyle.FOCUS

                            // Determine highlight background if item is selected or highlighted
                            val containerAlpha = when {
                                isActive -> 0.25f
                                isFocusedStyle -> 0.05f
                                else -> 1f
                            }

                            val animatedBackground = if (isActive) {
                                primaryAccent.copy(alpha = containerAlpha)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(animatedBackground)
                                    .clickable {
                                        // Simple tap selects sentence + play trigger
                                        viewModel.ttsEngine.skipToSentence(idx)
                                        if (!isPlaying) viewModel.ttsEngine.play()
                                    }
                                    .combinedClickable(
                                        onLongClick = {
                                            sentenceSelectedForHighlight = idx
                                            customHighlightNote = ""
                                            showHighlightDialog = true
                                        },
                                        onClick = {
                                            viewModel.ttsEngine.skipToSentence(idx)
                                        }
                                    )
                                    .padding(8.dp)
                            ) {
                                // Draw individual words inside the paragraph sentence
                                val annotatedString = buildAnnotatedString {
                                    val words = sentence.split(" ")
                                    var charAccumulator = 0

                                    words.forEachIndexed { wordIdx, word ->
                                        val wordStart = charAccumulator
                                        val wordEnd = charAccumulator + word.length

                                        val isWordActive = isActive && wordRange != null && 
                                                wordStart >= wordRange!!.first && wordEnd <= wordRange!!.second + 5 // buffer offsets

                                        if (isWordActive) {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = primaryAccent,
                                                    fontWeight = FontWeight.Black,
                                                    textDecoration = TextDecoration.Underline,
                                                    background = primaryAccent.copy(alpha = 0.15f)
                                                )
                                            ) {
                                                append(word)
                                            }
                                        } else {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = if (isFocusedStyle && !isActive) readerTextColor.copy(alpha = 0.25f) else readerTextColor
                                                )
                                            ) {
                                                append(word)
                                            }
                                        }

                                        append(" ")
                                        charAccumulator += word.length + 1
                                    }
                                }

                                // We want to allow word definition lookup on clicks!
                                // We split by spaces and draw each word in a FlowRow so that it can receive custom clicks
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val wordsList = sentence.split("\\s+".toRegex())
                                    wordsList.forEach { rawWord ->
                                        // Clean punctuation
                                        val cleanWord = rawWord.lowercase().replace("[^a-zA-Z]".toRegex(), "")
                                        Text(
                                            text = "$rawWord ",
                                            fontSize = (baseFontSize.value * fontSizeMult).sp,
                                            lineHeight = calculatedLineHeight,
                                            fontFamily = calculatedFontFamily,
                                            letterSpacing = calculatedLetterSpacing,
                                            fontWeight = calculatedFontWeight,
                                            color = if (isActive) primaryAccent else if (isFocusedStyle && !isActive) readerTextColor.copy(alpha = 0.25f) else readerTextColor,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    // Instant local dictionary lookup
                                                    val dictDef = localDictionaryLookup(cleanWord)
                                                    if (dictDef != null) {
                                                        dictionaryWord = cleanWord.replaceFirstChar { it.uppercase() }
                                                        dictionaryDefinition = dictDef
                                                    } else {
                                                        // Fallback standard skip to sentence
                                                        viewModel.ttsEngine.skipToSentence(idx)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Mini-Control Overlay Bar at bottom (Saves space, feels very polished!)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.ttsEngine.previousSentence() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Sentence")
                            }

                            // Big circular play action
                            LargePlayPauseButton(
                                isPlaying = isPlaying,
                                onClick = {
                                    if (isPlaying) {
                                        viewModel.ttsEngine.pause()
                                    } else {
                                        viewModel.ttsEngine.play()
                                    }
                                },
                                accentColor = primaryAccent
                            )

                            IconButton(onClick = { viewModel.ttsEngine.nextSentence() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Sentence")
                            }

                            // Fast Tab open shortcut
                            IconButton(onClick = { viewModel.setActiveTab(1) }) {
                                Icon(Icons.Default.Headphones, contentDescription = "Audiobook Dashboard")
                            }
                        }
                    }
                }
            }

            // Simulate Brightness Dimmer layout overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1.0f - simulatedBrightness) * 0.75f))
                    .pointerInput(Unit) {
                        // Allows passing standard clicks to the reader list beneath
                    }
            )
        }
    }

    // A. Comfort settings and speech configuration Bottom Sheet
    if (showTtsSettingSheet) {
        Dialog(onDismissRequest = { showTtsSettingSheet = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Comfort & Speech Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode selections (Rows)
                    Text("Reader Display Filters", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderModeChip("Standard", ReaderModeStyle.STANDARD, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Sepia Warm Paper", ReaderModeStyle.WARM_PAPER, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Eye Comfort Green", ReaderModeStyle.EYE_COMFORT, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("AMOLED Black", ReaderModeStyle.AMOLED, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Dim Muted Light", ReaderModeStyle.DIM_LIGHT, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Dyslexia Layout", ReaderModeStyle.DYSLEXIA, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Focus Sentence", ReaderModeStyle.FOCUS, readerMode) { viewModel.setReaderMode(it) }
                        ReaderModeChip("Night Safe Orange", ReaderModeStyle.NIGHT, readerMode) { viewModel.setReaderMode(it) }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Font zoom slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font sizing zoom", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${(fontSizeMult * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = fontSizeMult,
                        onValueChange = { viewModel.setFontSizeMultiplier(it) },
                        valueRange = 0.7f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = primaryAccent, activeTrackColor = primaryAccent)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gesture brightness guide
                    Text("Swipe Dimmer Simulator", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Slider(
                        value = simulatedBrightness,
                        onValueChange = { simulatedBrightness = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = primaryAccent, activeTrackColor = primaryAccent)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider and TTS attributes
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Text-to-Speech Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voices preset mapping
                    Text("Natural voice profiles", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentVoice = viewModel.ttsEngine.getVoiceStyle()
                    val voiceProfiles = listOf("Calm Narrator", "Deep Audiobook", "Smooth Female", "Character/Cartoon", "Robotic/Minimal")
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        voiceProfiles.forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.ttsEngine.selectVoiceStyle(profile) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    profile,
                                    fontWeight = if (currentVoice == profile) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentVoice == profile) primaryAccent else MaterialTheme.colorScheme.onSurface
                                )
                                if (currentVoice == profile) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sleep Timer Quick Selection
                    Text("Smart Sleep Timer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30, 45).forEach { mins ->
                            Button(
                                onClick = { viewModel.ttsEngine.startSleepTimer(mins) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryAccent.copy(alpha = 0.15f),
                                    contentColor = primaryAccent
                                )
                            ) {
                                Text("${mins}m")
                            }
                        }
                        if (sleepSeconds != null) {
                            Button(
                                onClick = { viewModel.ttsEngine.stopSleepTimer() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Stop")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showTtsSettingSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Controls")
                    }
                }
            }
        }
    }

    // B. Custom Long-Press Highlight Maker Dialog
    if (showHighlightDialog && sentenceSelectedForHighlight != null) {
        Dialog(onDismissRequest = { showHighlightDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Add Highlight & Marker Memo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle snippet preview
                    val highlightedTextPreview = sentences.getOrNull(sentenceSelectedForHighlight!!) ?: ""
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "\"$highlightedTextPreview\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 3
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Marker colors picker row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("#FFF59D", "#FFD1DC", "#C1FFC1", "#E5D4FF").forEach { hex ->
                            val isColorSelected = highlightMarkerColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) primaryAccent else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { highlightMarkerColor = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customHighlightNote,
                        onValueChange = { customHighlightNote = it },
                        label = { Text("Write stick note / comments...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showHighlightDialog = false }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addHighlight(
                                    colorHex = highlightMarkerColor,
                                    note = customHighlightNote
                                )
                                showHighlightDialog = false
                                Toast.makeText(context, "Saved highlighted quote!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                        ) {
                            Text("Create marker")
                        }
                    }
                }
            }
        }
    }

    // C. Word Definitions Lookup popup dialog
    if (dictionaryWord != null && dictionaryDefinition != null) {
        Dialog(onDismissRequest = {
            dictionaryWord = null
            dictionaryDefinition = null
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            dictionaryWord!!,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Offline Dictionary & Meaning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        dictionaryDefinition!!,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            dictionaryWord = null
                            dictionaryDefinition = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

// Custom Helper Chip
@Composable
fun ReaderModeChip(
    label: String,
    mode: ReaderModeStyle,
    activeMode: ReaderModeStyle,
    onClick: (ReaderModeStyle) -> Unit
) {
    val isSelected = activeMode == mode
    FilterChip(
        selected = isSelected,
        onClick = { onClick(mode) },
        label = { Text(label) },
        modifier = Modifier.padding(2.dp)
    )
}

// Polished Big Play pause custom container with nice ripples
@Composable
fun LargePlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(accentColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause speech" else "Speak for me",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

// Local Fallback offline lookup dictionary mappings (Extremely rich UX detailing)
fun localDictionaryLookup(word: String): String? {
    val lookupMap = mapOf(
        "war" to "[noun] A state of armed conflict between different nations or states or different groups within a nation or state. In 'The Art of War', it represents strategic state operations.",
        "deception" to "[noun] The action of deceiving someone. Sun Tzu asserts that all warfare operations are based heavily on strategic concealment and deception to mislead the opposition.",
        "metamorphosis" to "[noun] (in an insect or amphibian) the process of transformation from an immature form to an adult form in two or more distinct stages. In literature, it symbolizes Gregor Samsa's transformation.",
        "dreams" to "[noun] A series of thoughts, images, and sensations occurring in a person's mind during sleep. Metamorphosis starts when Gregor Samsa woke up from troubled dreams.",
        "vermin" to "[noun] Wild animals or insects that are believed to be harmful to crops, carrying disease, or highly repulsive. Gregor transformed overnight into an armor-backed vermin.",
        "rabbit" to "[noun] A gregarious burrowing plant-eating mammal with long ears, long hind legs, and a short tail. In Alice's Adventures, the White Rabbit starts her surreal trip.",
        "watch" to "[noun] A small timepiece worn typically on a strap on one's wrist or carried in a pocket, which the White Rabbit pulled out of his elegant waistcoat pocket.",
        "hole" to "[noun] A hollow place in a solid body or surface. Alice followed the White Rabbit down a deep, tunnel-like rabbit hole underneath the edge of the hedge.",
        "well" to "[noun] A deep shaft or hole sunk into the earth to obtain water. Here, it refers to the deep well Alice falls down slowly while looking at bookshelves.",
        "general" to "[noun] A commander of an army, or an officer of very high rank. Sun Tzu's manual emphasizes things a successful general must deliberate, evaluate and action.",
        "victorious" to "[adjective] Having won a victory or conquered an opponent. In state deliberation, he who knows the constant five rules will be victorious.",
        "commander" to "[noun] A person in authority who is in charge of a military troop or operational project. The commander coordinates wisdom, courage, strictness, and benevolence.",
        "truth" to "[noun] That which is true or in accordance with fact or reality. Jane Austen famously notes the truth that wealthy men require companion wives.",
        "fortune" to "[noun] Chance or luck as an external, arbitrary force affecting human affairs; or large amount of money/assets possessed by a single gentleman.",
        "accept" to "[verb] Consent to receive or undertake a thing offered. Dr. Carl Rogers advises that when I accept myself as I am, then healing and progress start.",
        "restoration" to "[noun] The action of returning something to its former owner, place, condition, or healthy organic state. Refers to ecological recoveries of landscapes.",
        "paradox" to "[noun] A seemingly absurd or self-contradictory statement or proposition that when investigated or explained may prove to be well-founded or true."
    )
    return lookupMap[word.trim().lowercase()]
}
