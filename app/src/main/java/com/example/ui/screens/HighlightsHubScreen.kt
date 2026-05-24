package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Highlight
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onJumpToBook: () -> Unit
) {
    val highlights by viewModel.allHighlights.collectAsState()
    val books by viewModel.allBooks.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Memo Ledger & Quotes", fontWeight = FontWeight.Bold) },
                actions = {
                    if (highlights.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                // Bulked formatted TXT export
                                val formattedTxt = highlights.joinToString("\n\n") { highlight ->
                                    val matchBook = books.firstOrNull { it.id == highlight.bookId }
                                    "Book: ${matchBook?.title ?: "Saved clip"}\n" +
                                    "Quote: \"${highlight.text}\"\n" +
                                    "Memo Note: ${highlight.note.ifBlank { "None" }}"
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(formattedTxt))
                                Toast.makeText(context, "Exported all highlights to clipboard (TXT)!", Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export all to TXT")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (highlights.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your thoughts compiled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Long-press any sentence or block when reading to highlight, add custom annotations/comments, and export them here as TXT documents.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("highlights_ledger_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(highlights, key = { it.id }) { highlight ->
                    val matchingBook = books.firstOrNull { it.id == highlight.bookId }
                    val highlightBgColor = remember(highlight.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.5f)
                        } catch (e: Exception) {
                            Color(0xFFFFF59D).copy(alpha = 0.5f) // default warm yellow
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (matchingBook != null) {
                                    viewModel.selectBook(matchingBook)
                                    viewModel.ttsEngine.skipToSentence(highlight.sentenceIndex)
                                    onJumpToBook()
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Marker colored Quote Excerpt
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(highlightBgColor)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "\"${highlight.text}\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = FontFamily.Serif,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Attachment Note if added
                            if (highlight.note.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        highlight.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Book title stamp and actions row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    matchingBook?.title ?: "Clipboard Note",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Row {
                                    // Single copy button
                                    IconButton(
                                        onClick = {
                                            val singleFormatted = "\"${highlight.text}\"\nNote: ${highlight.note}"
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(singleFormatted))
                                            Toast.makeText(context, "Quote copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Quote",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Delete highlight
                                    IconButton(
                                        onClick = { viewModel.deleteHighlight(highlight.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete highlight",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
