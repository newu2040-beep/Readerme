package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.WavyPlaybackIndicator
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedBook by viewModel.selectedBook.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val activeSentenceIndex by viewModel.currentSentenceIndex.collectAsState()
    val sleepSeconds by viewModel.sleepTimerSeconds.collectAsState()
    
    val sentences = remember(selectedBook) {
        selectedBook?.let { viewModel.ttsEngine.getSentences() } ?: emptyList()
    }

    val currentSpeed = viewModel.ttsEngine.getSpeed()
    val currentVoice = viewModel.ttsEngine.getVoiceStyle()

    // Rotatable Vinyl Disc Animation State
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val discRotateAngle = if (isPlaying) rotationAngle else 0f

    val bookCoverColor = remember(selectedBook?.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedBook?.coverColorHex ?: "#6E44FF"))
        } catch (e: Exception) {
            Color(0xFF6E44FF)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("audiobook_dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Details
        Text(
            "Audiobook Theater",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        if (selectedBook == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Please select a book to listen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // Elegant Rotating Vinyl Disc (Brutalist artistic audiobook design)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        shadowElevation = 16f
                        shape = CircleShape
                        clip = true
                    }
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                // outer vinyl record boundaries
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .rotate(discRotateAngle)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color(0xFF232329),
                                    Color.Black,
                                    Color(0xFF1F1F24),
                                    Color.Black
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // record vinyl ridges rings
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.Transparent, shape = CircleShape)
                            .graphicsLayer {
                                renderEffect = null
                            }
                    )

                    // Inner Custom Album Cover Badge representing book
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(bookCoverColor, bookCoverColor.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                selectedBook!!.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                // Small vinyl record pin hole center
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.background, shape = CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Card for active stream
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    selectedBook!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "by ${selectedBook!!.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }

            // Real-time flowing voice wavy equalizer
            WavyPlaybackIndicator(
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                waveColor = bookCoverColor
            )

            // Sentence progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sentence ${activeSentenceIndex + 1} of ${sentences.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Sleep Timer state indicator
                if (sleepSeconds != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${sleepSeconds!! / 60}m Left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sentence-level slider tracking
            val progressFactor = if (sentences.isNotEmpty()) activeSentenceIndex.toFloat() / sentences.size else 0f
            Slider(
                value = progressFactor,
                onValueChange = { factor ->
                    val sentenceTarget = (factor * sentences.size).toInt().coerceIn(sentences.indices)
                    viewModel.ttsEngine.skipToSentence(sentenceTarget)
                },
                colors = SliderDefaults.colors(
                    thumbColor = bookCoverColor,
                    activeTrackColor = bookCoverColor
                )
            )

            // Audio physical controllers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back 15s represented by 1 sentence back
                IconButton(onClick = { viewModel.ttsEngine.previousSentence() }) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back sentence",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Speak Play pause container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(bookCoverColor)
                        .clickable {
                            if (isPlaying) viewModel.ttsEngine.pause() else viewModel.ttsEngine.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play speech",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Forward 10 represented by 1 sentence forward
                IconButton(onClick = { viewModel.ttsEngine.nextSentence() }) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward sentence",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed and voice sliders quick choices card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Voice Accent & Speed Panel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick speeds choices row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Playback Speed", style = MaterialTheme.typography.bodyMedium)
                        Text("${currentSpeed}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val isSelected = currentSpeed == spd
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) bookCoverColor else MaterialTheme.colorScheme.background
                                    )
                                    .clickable { viewModel.ttsEngine.setSpeed(spd) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${spd}x",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Quick sleep countdown selector
                    Text("Auto Sleep timer countdown", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30, 60).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable { viewModel.ttsEngine.startSleepTimer(mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${mins}m",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (sleepSeconds != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable { viewModel.ttsEngine.stopSleepTimer() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Stop",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
