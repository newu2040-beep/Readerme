package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WavyPlaybackIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_eq")
    
    // Smooth scrolling phase for the sine wave
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Breathing anim for wave height
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "height"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val maxAmplitude = height * 0.4f * (if (isPlaying) heightScale else 0.15f)

        val pathEffect = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw multiple beautiful layered wavy lines
        val points = 80
        val step = width / points

        // Dynamic gradient
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                waveColor.copy(alpha = 0.3f),
                waveColor,
                waveColor.copy(alpha = 0.5f)
            )
        )

        // Wave 1
        val path1 = androidx.compose.ui.graphics.Path()
        for (i in 0..points) {
            val x = i * step
            // Calculate a beautiful sine wave with frequency and wave phase
            val angle = (i.toFloat() / points) * 3f * Math.PI.toFloat() + (if (isPlaying) wavePhase else 0f)
            val y = midY + sin(angle) * maxAmplitude
            if (i == 0) {
                path1.moveTo(x, y)
            } else {
                path1.lineTo(x, y)
            }
        }
        drawPath(path = path1, brush = gradient, style = pathEffect)

        // Wave 2 (Dampened, offset phase for rich layered depth)
        val path2 = androidx.compose.ui.graphics.Path()
        for (i in 0..points) {
            val x = i * step
            val angle = (i.toFloat() / points) * 4f * Math.PI.toFloat() - (if (isPlaying) wavePhase * 1.3f else 1.0f)
            val y = midY + sin(angle) * (maxAmplitude * 0.6f)
            if (i == 0) {
                path2.moveTo(x, y)
            } else {
                path2.lineTo(x, y)
            }
        }
        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(
                listOf(waveColor.copy(alpha = 0.6f), waveColor.copy(alpha = 0.2f))
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
