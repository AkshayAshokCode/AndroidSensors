package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R

// Data class for sparkle particles
data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Float = 1f,
    var lifetime: Float = 0f,
    var maxLifetime: Float = 0.5f,
    var color: Color = Color(0xFF4FC3F7) // Default blue, but will be set to wall color
)

// Data class for ball trail effect
data class TrailPoint(
    val x: Float,
    val y: Float,
    var alpha: Float = 1f
)

// Data class for background particle field
data class BackgroundParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    val size: Float = 2f,
    val alpha: Float = 0.1f
)

@Composable
fun GravityBallScreen(
    ballX: Float,
    ballY: Float,
    particles: SnapshotStateList<Particle>,
    trailPoints: SnapshotStateList<TrailPoint>,
    backgroundParticles: SnapshotStateList<BackgroundParticle>,
    isDragging: Boolean,
    isAvailable: Boolean,
    onDragStart: (Float, Float, Float, Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onBottomSheetToggleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.gravity_background_start),
                        colorResource(R.color.gravity_background_middle),
                        colorResource(R.color.gravity_background_end)
                    )
                )
            )
    ) {
        if (!isAvailable) {
            Text(
                text = stringResource(R.string.gravity_ball_not_available),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            // Main canvas for ball and particles with drag gesture
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Convert pixel coordinates to normalized (0-1)
                                val normalizedX = offset.x / size.width
                                val normalizedY = offset.y / size.height
                                onDragStart(normalizedX, normalizedY, size.width.toFloat(), size.height.toFloat())
                            },
                            onDrag = { change, _ ->
                                // Convert pixel coordinates to normalized (0-1)
                                val normalizedX = change.position.x / size.width
                                val normalizedY = change.position.y / size.height
                                onDrag(normalizedX, normalizedY)
                                change.consume()
                            },
                            onDragEnd = {
                                onDragEnd()
                            }
                        )
                    }
            ) {
                drawBackgroundParticles(backgroundParticles)
                drawTrail(trailPoints)
                drawGravityBall(ballX, ballY, isDragging)
                drawParticles(particles)
            }
        }
    }
}

private fun DrawScope.drawGravityBall(ballX: Float, ballY: Float, isDragging: Boolean) {
    val ballRadius = 40.dp.toPx()
    val borderWidth = 6.dp.toPx()

    // Draw subtle border at the screen edges
    drawRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth)
    )

    // Calculate actual pixel position from normalized coordinates
    val actualX = ballX * (size.width - ballRadius * 2) + ballRadius
    val actualY = ballY * (size.height - ballRadius * 2) + ballRadius

    val ballCenter = Offset(actualX, actualY)

    // Draw shadow (offset slightly down and right)
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = ballRadius * 0.9f,
        center = Offset(actualX + 8f, actualY + 8f)
    )

    // Draw ball with gradient for 3D effect
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4FC3F7), // Light blue
                Color(0xFF0288D1), // Medium blue
                Color(0xFF01579B)  // Dark blue
            ),
            center = Offset(actualX - ballRadius * 0.3f, actualY - ballRadius * 0.3f),
            radius = ballRadius * 1.2f
        ),
        radius = ballRadius,
        center = ballCenter
    )

    // Draw highlight for shine effect
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = ballRadius * 0.3f,
        center = Offset(actualX - ballRadius * 0.4f, actualY - ballRadius * 0.4f)
    )

    // Draw outer rim for depth
    drawCircle(
        color = Color(0xFF01579B).copy(alpha = 0.5f),
        radius = ballRadius,
        center = ballCenter,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )

    // Draw subtle glow overlay when dragging
    if (isDragging) {
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = ballRadius * 1.3f,
            center = ballCenter
        )
        drawCircle(
            color = Color(0xFF4FC3F7).copy(alpha = 0.15f),
            radius = ballRadius * 1.15f,
            center = ballCenter
        )
    }
}

private fun DrawScope.drawParticles(particles: SnapshotStateList<Particle>) {
    particles.forEach { particle ->
        // Calculate actual pixel position from normalized coordinates
        val actualX = particle.x * size.width
        val actualY = particle.y * size.height

        // Draw particle with gradient for glow effect
        val particleRadius = 3.dp.toPx()

        // Outer glow - uses wall color
        drawCircle(
            color = particle.color.copy(alpha = particle.alpha * 0.3f),
            radius = particleRadius * 2f,
            center = Offset(actualX, actualY)
        )

        // Inner bright core - white for brightness
        drawCircle(
            color = Color.White.copy(alpha = particle.alpha),
            radius = particleRadius,
            center = Offset(actualX, actualY)
        )
    }
}

private fun DrawScope.drawTrail(trailPoints: SnapshotStateList<TrailPoint>) {
    val ballRadius = 40.dp.toPx()

    // Draw trail points from oldest to newest
    trailPoints.forEachIndexed { index, point ->
        // Calculate actual position (accounting for ball radius offset)
        val actualX = point.x * (size.width - ballRadius * 2) + ballRadius
        val actualY = point.y * (size.height - ballRadius * 2) + ballRadius

        // Trail circle size decreases for older points
        val trailRadius = (8.dp.toPx() * point.alpha).coerceAtLeast(2.dp.toPx())

        // Draw trail point with gradient
        drawCircle(
            color = Color(0xFF4FC3F7).copy(alpha = point.alpha * 0.4f),
            radius = trailRadius,
            center = Offset(actualX, actualY)
        )

        drawCircle(
            color = Color.White.copy(alpha = point.alpha * 0.6f),
            radius = trailRadius * 0.5f,
            center = Offset(actualX, actualY)
        )
    }
}

private fun DrawScope.drawBackgroundParticles(backgroundParticles: SnapshotStateList<BackgroundParticle>) {
    backgroundParticles.forEach { particle ->
        // Draw small subtle particles
        drawCircle(
            color = Color(0xFF4FC3F7).copy(alpha = particle.alpha),
            radius = particle.size.dp.toPx(),
            center = Offset(particle.x, particle.y)
        )
    }
}