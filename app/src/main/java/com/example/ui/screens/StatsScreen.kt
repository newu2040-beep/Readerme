package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val listeningStats by viewModel.listeningStats.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    val todayWords by viewModel.todayWords.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()

    // Calculate sum statistics
    val totalSeconds = listeningStats.sumOf { it.secondsListened }
    val totalMinutes = totalSeconds / 60
    val totalWords = listeningStats.sumOf { it.wordsRead }

    // Map last 7 days of statistics for drawing inside custom Canvas graph
    val last7DaysStats = remember(listeningStats) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelFormat = SimpleDateFormat("E", Locale.getDefault())

        List(7) { idx ->
            val tempCal = calendar.clone() as Calendar
            tempCal.add(Calendar.DAY_OF_YEAR, -idx)
            val dayStr = dateFormat.format(tempCal.time)
            val dayLabel = dayLabelFormat.format(tempCal.time)

            // fetch database values
            val matchingStat = listeningStats.firstOrNull { it.dateString == dayStr }
            val mins = (matchingStat?.secondsListened ?: 0) / 60f
            val words = matchingStat?.wordsRead ?: 0

            BarData(label = dayLabel, minutes = mins, words = words)
        }.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("stats_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Daily Streak Banner (Extreme motivation 🔥)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "STREAK MODE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Active Streak Flame",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (streakCount > 0) "$streakCount Days Streak!" else "Start daily reading!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (streakCount > 0) "Keep listening daily to level up your brain voice profiles." 
                        else "Speak or read at least 1 minute today to trigger streak flame!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔥",
                        fontSize = 32.sp
                    )
                }
            }
        }

        // Stats Quick Cards Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatSummaryCard(
                icon = Icons.Default.Timer,
                title = "Today's Speech",
                value = "$todayMinutes mins",
                subtitle = "Goal: 30 mins",
                modifier = Modifier.weight(1f)
            )

            StatSummaryCard(
                icon = Icons.Default.Spellcheck,
                title = "Words Read",
                value = todayWords.toString(),
                subtitle = "Today total",
                modifier = Modifier.weight(1f)
            )
        }

        // Canvas drawn custom elegant Bar Chart
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Weekly Activity (Listening Mins)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Bar Drawing
                val barColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outlineVariant
                val textCol = MaterialTheme.colorScheme.onSurface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomY = height - 30f
                    val topY = 20f
                    val chartHeight = bottomY - topY

                    // Find max value in last 7 days to set relative scale
                    val maxMins = last7DaysStats.maxOfOrNull { it.minutes }?.coerceAtLeast(10f) ?: 10f

                    // Draw Horizontal Gridlines (Mins markers)
                    val gridLinesCount = 4
                    for (i in 0..gridLinesCount) {
                        val fraction = i.toFloat() / gridLinesCount
                        val y = bottomY - (fraction * chartHeight)
                        val valMarker = (fraction * maxMins).toInt()

                        drawLine(
                            color = gridColor,
                            start = Offset(40f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )

                        // Draw tiny values labels
                        drawContext.canvas.nativeCanvas.drawText(
                            "${valMarker}m",
                            0f,
                            y + 10f,
                            android.graphics.Paint().apply {
                                color = textCol.value.toInt()
                                textSize = 24f
                                isFakeBoldText = true
                            }
                        )
                    }

                    // Draw Vertical Columns/Bars
                    val colCount = last7DaysStats.size
                    val colWidth = (width - 60f) / colCount
                    val barWidthFraction = colWidth * 0.5f

                    last7DaysStats.forEachIndexed { idx, barData ->
                        val centerX = 60f + (idx * colWidth) + (colWidth / 2f)
                        val relativeHeight = (barData.minutes / maxMins) * chartHeight
                        val barTopY = bottomY - relativeHeight

                        // draw bar column with beautiful rounded corners
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(barColor, barColor.copy(alpha = 0.5f))
                            ),
                            topLeft = Offset(centerX - (barWidthFraction / 2f), barTopY),
                            size = Size(barWidthFraction, relativeHeight.coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Draw Days labels under bottom line
                        drawContext.canvas.nativeCanvas.drawText(
                            barData.label,
                            centerX - 15f,
                            height - 5f,
                            android.graphics.Paint().apply {
                                color = textCol.value.toInt()
                                textSize = 24f
                            }
                        )
                    }
                }
            }
        }

        // Lifetime Statistics History Box
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "ReaderMe Milestones achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                MilestoneRow(
                    metricName = "Total Listening Time",
                    metricValue = "$totalMinutes mins",
                    description = "Aggregate duration saved offline"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                MilestoneRow(
                    metricName = "Accumulated Words",
                    metricValue = totalWords.toString(),
                    description = "Calculated words parsed and spoken"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                MilestoneRow(
                    metricName = "Imported Articles",
                    metricValue = "${listeningStats.size} logs",
                    description = "Consistently read documents"
                )
            }
        }
    }
}

@Composable
fun StatSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun MilestoneRow(
    metricName: String,
    metricValue: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(metricName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            metricValue,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}

data class BarData(
    val label: String,
    val minutes: Float,
    val words: Int
)
