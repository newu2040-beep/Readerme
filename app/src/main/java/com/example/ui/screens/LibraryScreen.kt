package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Book
import com.example.ui.LanguageManager
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenReader: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    val rawLangState by viewModel.currentLanguage.collectAsState()
    
    var searchQueries by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Dialog state flags
    var showImportOptionDialog by remember { mutableStateOf(false) }
    var showCustomNoteDialog by remember { mutableStateOf(false) }
    var showOfflineFileSelector by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current

    // Categorized and searched filters
    val filteredBooks = remember(books, searchQueries, selectedCategory) {
        books.filter { book ->
            val matchCategory = selectedCategory == "All" || book.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQueries.isBlank() || 
                    book.title.contains(searchQueries, ignoreCase = true) ||
                    book.author.contains(searchQueries, ignoreCase = true) ||
                    book.content.contains(searchQueries, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val categories = listOf("All", "Philosophy", "Fiction", "Fantasy", "Imports")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            LanguageManager.getString("app_title", rawLangState),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_hamburger_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = LanguageManager.getString("hamburger_menu", rawLangState)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showImportOptionDialog = true },
                        modifier = Modifier
                            .testTag("import_fab")
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = LanguageManager.getString("import_now", rawLangState),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field with dynamic localized label
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                label = { Text(LanguageManager.getString("search_desc", rawLangState)) },
                placeholder = { Text("Enter a keyword...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQueries.isNotEmpty()) {
                        IconButton(onClick = { searchQueries = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("library_search_input"),
                shape = RoundedCornerShape(16.dp)
            )

            // Category list (Chips)
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {},
                indicator = {}
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid list of books
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOff,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            LanguageManager.getString("no_books", rawLangState),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showImportOptionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.getString("import_now", rawLangState))
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onSelect = {
                                viewModel.selectBook(book)
                                onOpenReader()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(book) },
                            onDelete = { viewModel.deleteBook(book.id) }
                        )
                    }
                }
            }
        }
    }

    // 1. Selector sheet for Import source
    if (showImportOptionDialog) {
        Dialog(onDismissRequest = { showImportOptionDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        LanguageManager.getString("import_safe", rawLangState),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "ReaderMe parses text secure & offline on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Option 1: Paste Clipboard
                    ListOptionItem(
                        icon = Icons.Default.Assignment,
                        title = LanguageManager.getString("read_clip", rawLangState),
                        description = "Instantly load what you copied last",
                        onClick = {
                            showImportOptionDialog = false
                            val textValue = clipboardManager.getText()?.text ?: ""
                            if (textValue.isNotBlank() && textValue.length >= 15) {
                                viewModel.importBook(
                                    title = "Clipboard ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                                    author = "Clipboard",
                                    content = textValue,
                                    category = "Imports",
                                    format = "CLIP",
                                    customCover = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400"
                                )
                            } else {
                                validationErrorMessage = LanguageManager.getString("validation_too_short", rawLangState)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Option 2: Custom Note
                    ListOptionItem(
                        icon = Icons.Default.EditNote,
                        title = LanguageManager.getString("write_note", rawLangState),
                        description = "Type note manually with beautiful cover art",
                        onClick = {
                            showImportOptionDialog = false
                            showCustomNoteDialog = true
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Option 3: Local storage scan
                    ListOptionItem(
                        icon = Icons.Default.FolderOpen,
                        title = LanguageManager.getString("import_files", rawLangState),
                        description = "Load ebooks and PDFs securely",
                        onClick = {
                            showImportOptionDialog = false
                            showOfflineFileSelector = true
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showImportOptionDialog = false }) {
                        Text(LanguageManager.getString("cancel", rawLangState))
                    }
                }
            }
        }
    }

    // Validation alert if clip content failed
    if (validationErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMessage = null },
            title = { Text("Validation Alert") },
            text = { Text(validationErrorMessage!!) },
            confirmButton = {
                Button(onClick = { validationErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // 2. Custom text Note writer dialog with Cover Picker presets
    if (showCustomNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteAuthor by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }
        var noteCoverUrl by remember { mutableStateOf("") }

        val imageCoverPresets = listOf(
            Pair("Vintage", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400"),
            Pair("Nature", "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=400"),
            Pair("Space", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400"),
            Pair("Retro", "https://images.unsplash.com/photo-1534067783941-51c9c23eccfd?w=400"),
            Pair("Tech", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400")
        )

        val colorCoverPresets = listOf("#FF6B6B", "#4ECDC4", "#FFE66D", "#95A5A6", "#9B59B6")

        Dialog(onDismissRequest = { showCustomNoteDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        LanguageManager.getString("write_note", rawLangState),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteAuthor,
                        onValueChange = { noteAuthor = it },
                        label = { Text("Author (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Full text content (Min 15 chars)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Thumbnail / cover photo picker
                    Text(
                        LanguageManager.getString("choose_preset_cover", rawLangState),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Cover preview thumbnails row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        imageCoverPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { noteCoverUrl = preset.second }
                                    .background(Color.Gray)
                            ) {
                                AsyncImage(
                                    model = preset.second,
                                    contentDescription = preset.first,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (noteCoverUrl == preset.second) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showCustomNoteDialog = false }) {
                            Text(LanguageManager.getString("cancel", rawLangState))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (noteContent.length < 15) {
                                    validationErrorMessage = LanguageManager.getString("validation_too_short", rawLangState)
                                    return@Button
                                }
                                if (noteTitle.isBlank()) {
                                    validationErrorMessage = LanguageManager.getString("validation_title_empty", rawLangState)
                                    return@Button
                                }
                                viewModel.importBook(
                                    title = noteTitle,
                                    author = noteAuthor.ifBlank { "Myself" },
                                    content = noteContent,
                                    category = "Imports",
                                    format = "Note",
                                    customCover = noteCoverUrl.ifBlank { colorCoverPresets.random() }
                                )
                                showCustomNoteDialog = false
                            },
                            enabled = noteTitle.isNotBlank() && noteContent.isNotBlank()
                        ) {
                            Text(LanguageManager.getString("save_read", rawLangState))
                        }
                    }
                }
            }
        }
    }

    // 3. Simulated storage File Importer (provides beautiful validated sample files)
    if (showOfflineFileSelector) {
        val simulatedLocalFiles = listOf(
            SimulatedFile(
                "Pride_And_Prejudice.epub",
                "Jane Austen",
                "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife. However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.",
                "Fiction",
                "EPUB",
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400"
            ),
            SimulatedFile(
                "Modern_Psychology_Guide.pdf",
                "Dr. Carl Rogers",
                "The curious paradox is that when I accept myself just as I am, then I can change. I believe that an individual has within himself or herself vast resources for self-understanding, for altering his or her self-concept, and for self-directed behavior. These resources can be tapped if only we can provide a sufficiently defined climate of warm, supportive, and empathetic acceptance.",
                "Philosophy",
                "PDF",
                "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=400"
            ),
            SimulatedFile(
                "Offline_Article_Eco_Restoration.html",
                "NatGeo Journal",
                "Eco-restoration is the intentional practice of assisting the recovery of ecosystems that have been degraded, damaged, or completely destroyed. Active reforestation, riparian planting buffers, and marsh restoration play critical roles in sequestering carbon and re-establishing migratory pathways for local wildlife.",
                "Imports",
                "HTML",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400"
            ),
            SimulatedFile(
                "My_Creative_Story.txt",
                "Writer",
                "The ship set sail under an amber twilight skies. The waves hummed a gentle acoustic melody against the wooden planks. Marcus stood on the forecastle, holding the dusty vintage compass. The compass wasn't pointing north. Instead, it was spinning erratically, reflecting the strange stardust forming directly overhead.",
                "Fantasy",
                "TXT",
                "https://images.unsplash.com/photo-1534067783941-51c9c23eccfd?w=400"
            )
        )

        Dialog(onDismissRequest = { showOfflineFileSelector = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            LanguageManager.getString("scanned_title", rawLangState),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        LanguageManager.getString("sim_file_alert", rawLangState),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    simulatedLocalFiles.forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.importBook(
                                        title = file.title,
                                        author = file.author,
                                        content = file.content,
                                        category = file.category,
                                        format = file.format,
                                        customCover = file.customCover
                                    )
                                    showOfflineFileSelector = false
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = when (file.format) {
                                    "PDF" -> Icons.Default.PictureAsPdf
                                    "EPUB" -> Icons.Default.Book
                                    "HTML" -> Icons.Default.Html
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${file.format} • ${file.author}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showOfflineFileSelector = false }) {
                            Text(LanguageManager.getString("close", rawLangState))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHexColor = remember(book.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(book.coverColorHex))
        } catch (e: Exception) {
            Color(0xFF6E44FF) // Fallback lavender
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("book_card_${book.id}")
    ) {
        Column {
            // Elegant gradient or Unsplash image dynamic book top cover
            val isUrl = book.coverColorHex.startsWith("http")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                if (isUrl) {
                    AsyncImage(
                        model = book.coverColorHex,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        currentHexColor,
                                        currentHexColor.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }

                // Book format badge (e.g. PDF, EPUB, CLIP)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        book.format,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Title overlay
                Text(
                    book.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            // Information details
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        book.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color.Red else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Option item in lists
@Composable
fun ListOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

data class SimulatedFile(
    val title: String,
    val author: String,
    val content: String,
    val category: String,
    val format: String,
    val customCover: String
)
