package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.MainViewModel
import com.example.ui.LanguageManager
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
    val activeUiOption by viewModel.playerUiOption.collectAsState()
    val rawLangState by viewModel.currentLanguage.collectAsState()

    val sentences = remember(selectedBook) {
        selectedBook?.let { viewModel.ttsEngine.getSentences() } ?: emptyList()
    }

    val currentSpeed = viewModel.ttsEngine.getSpeed()
    val currentVoice = viewModel.ttsEngine.getVoiceStyle()

    // 1. Spinnings & Rotatable animations (Universal parameters)
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
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
            .padding(16.dp)
            .testTag("audiobook_dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Style Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                LanguageManager.getString("audiobook", rawLangState),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Preset Mode ${activeUiOption + 1}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Beautiful on-the-fly horizontal tab style toggle switcher! Fully customizable
        ScrollableTabRow(
            selectedTabIndex = activeUiOption,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            val uiLabels = listOf(
                "Classic 💿" to 0,
                "Neon Synth ⚡" to 1,
                "Cover Art 🖼️" to 2,
                "Serene Beige ☕" to 3,
                "OLED Cosmos 🌌" to 4
            )
            uiLabels.forEach { (label, index) ->
                val isSelected = activeUiOption == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .clickable { viewModel.setPlayerUiOption(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedBook == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select a document from your safe library to start playing speech.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // RENDER 1 of the 5 PRESET OPTIONS DYNAMICALLY
            when (activeUiOption) {
                1 -> {
                    // PRESET 1: NEON DIGITAL SYNTHWAVE PULSE (⚡)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF120024)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007F))))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "DIGITAL PULSE WAVE",
                                color = Color(0xFF00F0FF),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )

                            // Waveform dynamic rods mapping
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val waveSpeeds = listOf(300, 450, 250, 600, 350, 500, 400, 550, 300)
                                waveSpeeds.forEach { duration ->
                                    val infiniteStep = rememberInfiniteTransition(label = "pulse_rods")
                                    val sizeFactor by infiniteStep.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(duration, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "scale"
                                    )
                                    val scaleAmount = if (isPlaying) sizeFactor else 0.15f
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight(scaleAmount)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF00F0FF), Color(0xFFFF007F))
                                                )
                                            )
                                    )
                                }
                            }

                            Text(
                                selectedBook!!.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Synth Engine Narration Active",
                                color = Color(0xFFFF007F),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                2 -> {
                    // PRESET 2: CLASSIC PREMIUM LARGE FULL-BLEED ARTBOOK (🖼️)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp, 240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bookCoverColor)
                            ) {
                                val hasUrl = selectedBook!!.coverColorHex.startsWith("http")
                                if (hasUrl) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedBook!!.coverColorHex)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Solid beautiful placeholder details
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                selectedBook!!.title.take(3).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                selectedBook!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Narration by $currentVoice",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                3 -> {
                    // PRESET 3: SERENE VINTAGE COFFEEHOUSE / BEIGE NEWSPAPER (☕)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4ECD8)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "THE NEW YORK RECIPIENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A3E26),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            HorizontalDivider(color = Color(0xFF4A3E26).copy(alpha = 0.3f))

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                selectedBook!!.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2211),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "A spoken translation essay chronicle from ${selectedBook!!.author}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Serif,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFF4A3E26),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF4A3E26).copy(alpha = 0.3f))
                        }
                    }
                }

                4 -> {
                    // PRESET 4: DEEP COSMIC OLED NEBULA VOYAGER (🌌)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Double futuristic tracking circle
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .rotate(discRotateAngle)
                                        .border(2.dp, Brush.sweepGradient(listOf(Color.Transparent, Color(0xFFBB86FC), Color.Transparent, Color(0xFF03DAC6))), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .rotate(-discRotateAngle * 1.5f)
                                        .border(1.dp, Brush.sweepGradient(listOf(Color(0xFF03DAC6), Color.Transparent, Color(0xFFBB86FC))), CircleShape)
                                )

                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    selectedBook!!.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFBB86FC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Cosmic Wave telemetry reader model active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                else -> {
                    // PRESET 0: RETRO CLASSIC VINYL DISC SPINNER (💿)
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

                    Spacer(modifier = Modifier.height(4.dp))

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
                }
            }

            // Real-time flowing voice wavy equalizer
            WavyPlaybackIndicator(
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                waveColor = if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF4A3E26) else bookCoverColor
            )

            // Sentence progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${LanguageManager.getString("sentence", rawLangState)} ${activeSentenceIndex + 1} of ${sentences.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (activeUiOption == 1) Color.LightGray else MaterialTheme.colorScheme.outline
                )
                
                // Sleep Timer state indicator
                if (sleepSeconds != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = if (activeUiOption == 1) Color(0xFFFF007F) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${sleepSeconds!! / 60}m Left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (activeUiOption == 1) Color(0xFFFF007F) else MaterialTheme.colorScheme.primary
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
                    thumbColor = if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF4A3E26) else bookCoverColor,
                    activeTrackColor = if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF4A3E26) else bookCoverColor
                )
            )

            // Playback Actions control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Return 1 sentence
                IconButton(onClick = { viewModel.ttsEngine.previousSentence() }) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back sentence",
                        tint = if (activeUiOption == 1) Color.White else if (activeUiOption == 3) Color(0xFF2B2211) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play Button Card Wrapper
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (activeUiOption == 1) Color(0xFFFF007F) else if (activeUiOption == 3) Color(0xFF2B2211) else bookCoverColor)
                        .clickable {
                            if (isPlaying) viewModel.ttsEngine.pause() else viewModel.ttsEngine.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play speech",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Advance 1 sentence
                IconButton(onClick = { viewModel.ttsEngine.nextSentence() }) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward sentence",
                        tint = if (activeUiOption == 1) Color.White else if (activeUiOption == 3) Color(0xFF2B2211) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Speed voice Accent panel controller
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeUiOption == 1) Color(0xFF1B0B30) else if (activeUiOption == 3) Color(0xFFEFE6CE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        LanguageManager.getString("voice_style", rawLangState) + " Panel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF2B2211) else MaterialTheme.colorScheme.primary
                    )

                    // Displays raw voice accent and toggle matching
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Voice: $currentVoice",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (activeUiOption == 1) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Choices Grid representing natural sounding offline voices
                    val voiceChoices = listOf("Calm Narrator", "Deep Audiobook", "Smooth Female", "Character/Cartoon", "Robotic/Minimal", "Classic")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        voiceChoices.forEach { styleKey ->
                            val isSelected = currentVoice == styleKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            if (activeUiOption == 1) Color(0xFFFF007F) else if (activeUiOption == 3) Color(0xFF2B2211) else bookCoverColor
                                        } else {
                                            if (activeUiOption == 1) Color(0xFF2B1B44) else if (activeUiOption == 3) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.background
                                        }
                                    )
                                    .clickable { viewModel.ttsEngine.selectVoiceStyle(styleKey) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    styleKey,
                                    color = if (isSelected) Color.White else if (activeUiOption == 1) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (activeUiOption == 3) Color(0xFF2B2211).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Playback speed configurations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LanguageManager.getString("speed", rawLangState),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (activeUiOption == 1) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${currentSpeed}x",
                            fontWeight = FontWeight.Bold,
                            color = if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF2B2211) else MaterialTheme.colorScheme.primary
                        )
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
                                        if (isSelected) {
                                            if (activeUiOption == 1) Color(0xFF00F0FF) else if (activeUiOption == 3) Color(0xFF2B2211) else bookCoverColor
                                        } else {
                                            if (activeUiOption == 1) Color(0xFF2B1B44) else if (activeUiOption == 3) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.background
                                        }
                                    )
                                    .clickable { viewModel.ttsEngine.setSpeed(spd) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${spd}x",
                                    color = if (isSelected) {
                                        if (activeUiOption == 3) Color.White else Color.Black
                                    } else {
                                        if (activeUiOption == 1) Color.LightGray else MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (activeUiOption == 3) Color(0xFF2B2211).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Sleep count selector
                    Text(
                        LanguageManager.getString("timer", rawLangState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (activeUiOption == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30, 60).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (activeUiOption == 1) Color(0xFF2B1B44) else if (activeUiOption == 3) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.background
                                    )
                                    .clickable { viewModel.ttsEngine.startSleepTimer(mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${mins}m",
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeUiOption == 1) Color.LightGray else MaterialTheme.colorScheme.onSurface,
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
