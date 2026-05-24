package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Book
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    var searchQueries by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Dialog flags
    var showImportOptionDialog by remember { mutableStateOf(false) }
    var showCustomNoteDialog by remember { mutableStateOf(false) }
    var showOfflineFileSelector by remember { mutableStateOf(false) }

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

    val categories = listOf("All", "Philosophy", "Fiction", "Fantasy", "Philosophy", "Imports")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "ReaderMe",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge
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
                            contentDescription = "Import content",
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
            // Search field
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                label = { Text("Search inside books, notes, offline files...") },
                placeholder = { Text("Enter a word or author...") },
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Category list (Chips)
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {},
                indicator = {}
            ) {
                categories.distinct().forEach { category ->
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

            Spacer(modifier = Modifier.height(12.dp))

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
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Your Library is waiting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No books match current search filters. You can import new files, notes, or copied clipboard content using the (+) button.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showImportOptionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Now")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
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
                        "Import Content Safely",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ReaderMe parses documents offline on your device, ensuring total privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Option 1: Paste Clipboard
                    ListOptionItem(
                        icon = Icons.Default.Assignment,
                        title = "Read from Clipboard",
                        description = "Instantly load what you copied last",
                        onClick = {
                            showImportOptionDialog = false
                            val textValue = clipboardManager.getText()?.text ?: ""
                            if (textValue.isNotBlank()) {
                                viewModel.importBook(
                                    title = "Clipboard Doc (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())})",
                                    author = "Clipboard",
                                    content = textValue,
                                    category = "Imports",
                                    format = "CLIP"
                                )
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Option 2: Custom Note
                    ListOptionItem(
                        icon = Icons.Default.EditNote,
                        title = "Write a Note / Pasted text",
                        description = "Type or paste custom articles and books",
                        onClick = {
                            showImportOptionDialog = false
                            showCustomNoteDialog = true
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Option 3: Local storage file (Simulated for SD card / Local scan)
                    ListOptionItem(
                        icon = Icons.Default.FolderOpen,
                        title = "Import Files (PDF, EPUB, TXT)",
                        description = "Load ebooks and PDFs from storage",
                        onClick = {
                            showImportOptionDialog = false
                            showOfflineFileSelector = true
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showImportOptionDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // 2. Custom text Note writer dialog
    if (showCustomNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteAuthor by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

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
                        "Add custom Note / Article",
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
                        label = { Text("Full text content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showCustomNoteDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (noteContent.isNotBlank()) {
                                    viewModel.importBook(
                                        title = noteTitle,
                                        author = noteAuthor,
                                        content = noteContent,
                                        category = "Imports",
                                        format = "Note"
                                    )
                                    showCustomNoteDialog = false
                                }
                            },
                            enabled = noteContent.isNotBlank()
                        ) {
                            Text("Save & Read")
                        }
                    }
                }
            }
        }
    }

    // 3. Simulated storage File Importer (provides beautiful realistic sample files)
    if (showOfflineFileSelector) {
        val simulatedLocalFiles = listOf(
            SimulatedFile(
                "Pride_And_Prejudice.epub",
                "Jane Austen",
                "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife. However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.",
                "Ebook",
                "EPUB"
            ),
            SimulatedFile(
                "Modern_Psychology_Guide.pdf",
                "Dr. Carl Rogers",
                "The curious paradox is that when I accept myself just as I am, then I can change. I believe that an individual has within himself or herself vast resources for self-understanding, for altering his or her self-concept, and for self-directed behavior. These resources can be tapped if only we can provide a sufficiently defined climate of warm, supportive, and empathetic acceptance.",
                "Philosophy",
                "PDF"
            ),
            SimulatedFile(
                "Offline_Article_Eco_Restoration.html",
                "NatGeo Journal",
                "Eco-restoration is the intentional practice of assisting the recovery of ecosystems that have been degraded, damaged, or completely destroyed. Active reforestation, riparian planting buffers, and marsh restoration play critical roles in sequestering carbon and re-establishing migratory pathways for local wildlife.",
                "Articles",
                "HTML"
            ),
            SimulatedFile(
                "My_Creative_Story.txt",
                "Myself",
                "The ship set sail under an amber twilight skies. The waves hummed a gentle acoustic melody against the wooden planks. Marcus stood on the forecastle, holding the dusty vintage compass. The compass wasn't pointing north. Instead, it was spinning erratically, reflecting the strange stardust forming directly overhead.",
                "Imports",
                "TXT"
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
                            "Device Storage & SD Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Select any of these scanned files to trigger smart OCR and text-to-speech loading instantly:",
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
                                        format = file.format
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
                            Text("Close")
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
            // Elegant gradient dynamic book top cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                currentHexColor,
                                currentHexColor.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                // Book format badge (e.g. PDF, EPUB)
                Box(
                    modifier = Modifier
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
                    modifier = Modifier.align(Alignment.BottomStart)
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
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.outlineVariantColor())
        }
    }
}

// Extension to fetch proper helper colors easily
@Composable
fun MaterialTheme.outlineVariantColor(): Color {
    return this.colorScheme.onSurface.copy(alpha = 0.6f)
}

data class SimulatedFile(
    val title: String,
    val author: String,
    val content: String,
    val category: String,
    val format: String
)
