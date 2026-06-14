package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Data classes — kept unchanged so SpaceBall.kt physics layer is unaffected
// ─────────────────────────────────────────────────────────────────────────────
data class Particle(
    var x          : Float,
    var y          : Float,
    var velocityX  : Float,
    var velocityY  : Float,
    var alpha      : Float = 1f,
    var lifetime   : Float = 0f,
    var maxLifetime: Float = 0.5f,
    var color      : Color = DashCyan
)

data class TrailPoint(
    val x    : Float,
    val y    : Float,
    var alpha: Float = 1f
)

data class BackgroundParticle(
    var x        : Float,
    var y        : Float,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    val size     : Float = 2f,
    val alpha    : Float = 0.1f
)

// ─────────────────────────────────────────────────────────────────────────────
// GravityBallScreen — main screen composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GravityBallScreen(
    ballX               : Float,
    ballY               : Float,
    particles           : SnapshotStateList<Particle>,
    trailPoints         : SnapshotStateList<TrailPoint>,
    backgroundParticles : SnapshotStateList<BackgroundParticle>,
    isDragging          : Boolean,
    isAvailable         : Boolean,
    onDragStart         : (Float, Float, Float, Float) -> Unit,
    onDrag              : (Float, Float) -> Unit,
    onDragEnd           : () -> Unit,
    onBottomSheetToggle : () -> Unit
) {
    // Pulse animation for the energy orb glow
    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "pulse"
    )

    // Hint shown once — disappears on first drag
    var hintVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DashVoid)
    ) {
        if (!isAvailable) {
            Text(
                text      = "Gravity sensor not available\non this device",
                color     = DashCyan.copy(alpha = 0.7f),
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                modifier  = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(32.dp)
            )
            return@Box
        }

        // Main physics canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hintVisible = false
                            val nx = offset.x / size.width
                            val ny = offset.y / size.height
                            onDragStart(nx, ny, size.width.toFloat(), size.height.toFloat())
                        },
                        onDrag = { change, _ ->
                            val nx = change.position.x / size.width
                            val ny = change.position.y / size.height
                            onDrag(nx, ny)
                            change.consume()
                        },
                        onDragEnd = { onDragEnd() }
                    )
                }
        ) {
            drawStarfield(backgroundParticles)
            drawCometTrail(trailPoints)
            drawEnergySparks(particles)
            drawEnergyOrb(ballX, ballY, isDragging, pulse)
        }

        // First-time hint overlay
        if (hintVisible) {
            HintOverlay(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HintOverlay — "DRAG TO LAUNCH · TILT TO ROLL" shown on first open
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HintOverlay(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "hint")
    val alpha by pulse.animateFloat(
        0.4f, 0.9f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "ha"
    )
    Text(
        text          = "DRAG  TO  LAUNCH\nTILT  TO  ROLL",
        color         = DashCyan.copy(alpha = alpha),
        fontSize      = 11.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 1.5.sp,
        textAlign     = TextAlign.Center,
        modifier      = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Draw functions
// ─────────────────────────────────────────────────────────────────────────────

private val OrbCyan   = DashCyan
private val OrbPurple = DashPurple

private fun DrawScope.drawEnergyOrb(
    ballX    : Float,
    ballY    : Float,
    isDragging: Boolean,
    pulse    : Float
) {
    val r  = 38.dp.toPx()
    val ax = ballX * (size.width  - r * 2) + r
    val ay = ballY * (size.height - r * 2) + r
    val c  = Offset(ax, ay)

    // Outer ambient aura
    drawCircle(OrbCyan.copy(alpha = pulse * 0.06f), r * 2.8f, c)
    drawCircle(OrbCyan.copy(alpha = pulse * 0.10f), r * 2.0f, c)

    // Glowing border ring
    drawCircle(OrbCyan.copy(alpha = 0.6f + pulse * 0.3f), r, c,
        style = Stroke(width = 2.dp.toPx()))

    // Ball body — radial gradient from white-cyan center to deep purple edge
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                OrbCyan.copy(alpha     = 0.85f),
                OrbPurple.copy(alpha   = 0.7f)
            ),
            center = Offset(ax - r * 0.2f, ay - r * 0.2f),
            radius = r * 1.1f
        ),
        radius = r,
        center = c
    )

    // Specular highlight (top-left bright spot)
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.28f,
        Offset(ax - r * 0.38f, ay - r * 0.38f))

    // Drag halo
    if (isDragging) {
        drawCircle(OrbCyan.copy(alpha = 0.18f), r * 1.8f, c)
        drawCircle(OrbCyan.copy(alpha = 0.4f),  r * 1.1f, c,
            style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.drawCometTrail(trail: SnapshotStateList<TrailPoint>) {
    if (trail.size < 2) return
    val r = 38.dp.toPx()

    // Draw trail as a tapering path
    for (i in 1 until trail.size) {
        val prev = trail[i - 1]
        val curr = trail[i]
        val fraction = i.toFloat() / trail.size
        val segAlpha = fraction * curr.alpha * 0.7f

        val px = prev.x * (size.width  - r * 2) + r
        val py = prev.y * (size.height - r * 2) + r
        val cx = curr.x * (size.width  - r * 2) + r
        val cy = curr.y * (size.height - r * 2) + r

        drawLine(
            color       = OrbCyan.copy(alpha = segAlpha),
            start       = Offset(px, py),
            end         = Offset(cx, cy),
            strokeWidth = (fraction * 12.dp.toPx()).coerceAtLeast(1.dp.toPx()),
            cap         = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawEnergySparks(particles: SnapshotStateList<Particle>) {
    particles.forEach { p ->
        val ax    = p.x * size.width
        val ay    = p.y * size.height
        val speed = hypot(p.velocityX, p.velocityY)
        val len   = (speed * 40f).coerceIn(4f, 18f)
        val angle = atan2(p.velocityY, p.velocityX)

        // Streak in movement direction
        val tailX = ax - cos(angle) * len
        val tailY = ay - sin(angle) * len

        drawLine(
            color       = p.color.copy(alpha = p.alpha * 0.9f),
            start       = Offset(tailX, tailY),
            end         = Offset(ax, ay),
            strokeWidth = 2.dp.toPx(),
            cap         = StrokeCap.Round
        )

        // Bright tip
        drawCircle(Color.White.copy(alpha = p.alpha * 0.8f), 2.dp.toPx(), Offset(ax, ay))
    }
}

private fun DrawScope.drawStarfield(stars: SnapshotStateList<BackgroundParticle>) {
    stars.forEach { s ->
        drawCircle(
            color  = Color.White.copy(alpha = s.alpha * 0.6f),
            radius = (s.size * 0.5f).dp.toPx(),
            center = Offset(s.x, s.y)
        )
    }
}
