package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// ─────────────────────────────────────────────────────────────────────────────
// Local colour aliases (shared with DashboardComponents tokens)
// ─────────────────────────────────────────────────────────────────────────────
private val MDVoid   = DashVoid
private val MDCyan   = DashCyan
private val MDGrid   = DashGrid
private val MDAmber  = DashAmber
private val MDOrange = DashOrange
private val MDRed    = DashRed
private val MDGreen  = DashGreen

// Maps 0-1 signal strength to a colour (cyan → amber → orange → red)
private fun signalColor(strength: Float): Color = when {
    strength < 0.25f -> MDCyan
    strength < 0.55f -> MDAmber
    strength < 0.80f -> MDOrange
    else             -> MDRed
}

// ─────────────────────────────────────────────────────────────────────────────
// MetalDetectorScreen — public entry point
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MetalDetectorScreen(
    magneticValue       : String,
    signalStrength      : Float,   // 0-1 normalised deviation
    calibrationProgress : Int,     // 0-20 samples collected
    isCalibrating       : Boolean,
    isAvailable         : Boolean,
    showRawValues       : Boolean,
    onToggleMode        : () -> Unit,
    onRecalibrate       : () -> Unit,
    onBottomSheetToggleClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 0 — dot grid background
        ReactiveBackground(modifier = Modifier.fillMaxSize())

        if (!isAvailable) {
            Text(
                text      = "Magnetometer not available\non this device",
                color     = MDCyan.copy(alpha = 0.7f),
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(32.dp)
            )
            return@Box
        }

        // Layer 1 — main content
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status label
            SignalStatusLabel(
                signalStrength = signalStrength,
                isCalibrating  = isCalibrating
            )

            // Sonar radar
            SonarRadar(
                magneticValue  = magneticValue,
                signalStrength = signalStrength,
                isCalibrating  = isCalibrating,
                showRawValues  = showRawValues,
                modifier       = Modifier.size(300.dp)
            )

            // Progress / signal bar
            if (isCalibrating) {
                CalibrationBar(progress = calibrationProgress)
            } else {
                SignalStrengthBar(signalStrength = signalStrength)
            }

            // Controls
            ControlsRow(
                showRawValues        = showRawValues,
                onToggleMode         = onToggleMode,
                onRecalibrate        = onRecalibrate,
                onBottomSheetToggleClick = onBottomSheetToggleClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReactiveBackground — concentric pulse rings that scale with signal strength
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReactiveBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(MDVoid)) {
        val dotSpacing = 28.dp.toPx()
        val dotRadius  = 1.1.dp.toPx()
        val cols = (size.width  / dotSpacing).toInt() + 2
        val rows = (size.height / dotSpacing).toInt() + 2
        repeat(rows) { r -> repeat(cols) { c ->
            drawCircle(MDGrid, dotRadius, Offset(c * dotSpacing, r * dotSpacing))
        }}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SignalStatusLabel — single animated line replacing the old 4 status cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignalStatusLabel(signalStrength: Float, isCalibrating: Boolean) {
    val (label, color) = when {
        isCalibrating        -> "● CALIBRATING" to MDCyan
        signalStrength < 0.05f -> "● SCANNING..." to MDCyan.copy(alpha = 0.6f)
        signalStrength < 0.25f -> "◈ WEAK SIGNAL" to MDAmber
        signalStrength < 0.55f -> "▲ ANOMALY DETECTED" to MDOrange
        else                 -> "⚠ STRONG SIGNATURE" to MDRed
    }

    // Flicker the label at high signal
    val flickerTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by flickerTransition.animateFloat(
        initialValue = 1f, targetValue = if (signalStrength > 0.55f) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            tween(if (signalStrength > 0.55f) 180 else 1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "fa"
    )

    Text(
        text          = label,
        color         = color.copy(alpha = flickerAlpha),
        fontSize      = 11.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 1.2.sp,
        fontWeight    = FontWeight.Bold
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SonarRadar — 5 rings + comet-tail sweep + blips + centre readout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SonarRadar(
    magneticValue : String,
    signalStrength: Float,
    isCalibrating : Boolean,
    showRawValues : Boolean,
    modifier      : Modifier = Modifier
) {
    val color = signalColor(signalStrength)

    // Pulse rings — emanate from radar centre
    val pulseDuration = ((2500 - signalStrength * 2000).toInt()).coerceIn(500, 2500)
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val p0 by pulseTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(pulseDuration, easing = LinearEasing), RepeatMode.Restart),
        label = "p0"
    )
    val p1 by pulseTransition.animateFloat(
        0.333f, 1.333f,
        infiniteRepeatable(tween(pulseDuration, easing = LinearEasing), RepeatMode.Restart),
        label = "p1"
    )
    val p2 by pulseTransition.animateFloat(
        0.666f, 1.666f,
        infiniteRepeatable(tween(pulseDuration, easing = LinearEasing), RepeatMode.Restart),
        label = "p2"
    )

    // Sweep rotation — faster when signal is higher
    val sweepDuration = ((3000 - signalStrength * 1800).toInt()).coerceIn(1200, 3000)
    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by sweepTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(sweepDuration, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "sa"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Radar canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx     = size.width  / 2f
            val cy     = size.height / 2f
            val center = Offset(cx, cy)
            val maxR   = size.minDimension / 2f * 0.88f

            // ── Pulse rings from radar centre ───────────────────────────
            if (!isCalibrating && signalStrength > 0.04f) {
                val maxPulseR = size.minDimension * 3f
                listOf(p0, p1 % 1f, p2 % 1f).forEach { phase ->
                    val r     = phase * maxPulseR
                    val alpha = signalStrength * (1f - phase).coerceIn(0f, 1f) * 0.35f
                    if (alpha > 0.005f) {
                        drawCircle(
                            color  = color.copy(alpha = alpha),
                            radius = r,
                            center = center,
                            style  = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // ── 5 concentric rings ──────────────────────────────────────
            val ringAlphas = listOf(0.55f, 0.40f, 0.28f, 0.18f, 0.10f)
            ringAlphas.forEachIndexed { i, alpha ->
                val r = maxR * (i + 1) / 5f
                drawCircle(
                    color  = color.copy(alpha = alpha * 0.6f),
                    radius = r,
                    center = center,
                    style  = Stroke(width = if (i == 0) 1.dp.toPx() else 0.5.dp.toPx())
                )
            }

            // ── 8 radial grid lines ─────────────────────────────────────
            repeat(8) { i ->
                val angle  = i * 45.0
                val angleR = Math.toRadians(angle)
                drawLine(
                    color       = MDGrid.copy(alpha = 0.5f),
                    start       = center,
                    end         = Offset(
                        cx + maxR * cos(angleR).toFloat(),
                        cy + maxR * sin(angleR).toFloat()
                    ),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // ── Comet-tail sweep (30 lines fanning 120°) ────────────────
            val trailSteps = 36
            val trailSpan  = 120f
            repeat(trailSteps) { i ->
                val fraction = i.toFloat() / trailSteps
                val angle    = Math.toRadians(
                    (sweepAngle - trailSpan + fraction * trailSpan).toDouble()
                )
                val alpha    = fraction * 0.55f
                drawLine(
                    color       = color.copy(alpha = alpha),
                    start       = center,
                    end         = Offset(
                        cx + maxR * cos(angle).toFloat(),
                        cy + maxR * sin(angle).toFloat()
                    ),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            // Bright leading edge
            val leadRad = Math.toRadians(sweepAngle.toDouble())
            drawLine(
                color       = color,
                start       = center,
                end         = Offset(
                    cx + maxR * cos(leadRad).toFloat(),
                    cy + maxR * sin(leadRad).toFloat()
                ),
                strokeWidth = 2.dp.toPx(),
                cap         = StrokeCap.Round
            )

            // ── Outer ring clip border ──────────────────────────────────
            drawCircle(
                color  = color.copy(alpha = 0.8f),
                radius = maxR,
                center = center,
                style  = Stroke(width = 1.dp.toPx())
            )
        }

        // Centre readout — monospace value over the radar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text          = if (isCalibrating) "---" else magneticValue,
                color         = Color.White,
                fontSize      = 28.sp,
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text          = if (showRawValues) "µT  RAW" else "µT  DEV",
                color         = signalColor(signalStrength).copy(alpha = 0.75f),
                fontSize      = 9.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CalibrationBar — 20 segments filling one-by-one during baseline collection
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalibrationBar(progress: Int) {
    val total = 20
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text          = if (progress == 0) "INITIALIZING..." else "CALIBRATING BASELINE",
            color         = MDCyan.copy(alpha = 0.8f),
            fontSize      = 10.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(total) { i ->
                val filled = i < progress
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            color = if (filled) MDCyan else MDGrid,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text(
            text          = "${progress}/${total}",
            color         = MDCyan.copy(alpha = 0.5f),
            fontSize      = 9.sp,
            fontFamily    = FontFamily.Monospace
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SignalStrengthBar — 20 segments cyan → amber → red
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignalStrengthBar(signalStrength: Float) {
    val total   = 20
    val filled  by animateIntAsState(
        targetValue   = (signalStrength * total).toInt().coerceIn(0, total),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "bar"
    )
    val label = when {
        signalStrength < 0.05f -> "NO SIGNAL"
        signalStrength < 0.25f -> "WEAK"
        signalStrength < 0.55f -> "MODERATE"
        signalStrength < 0.80f -> "STRONG"
        else                   -> "VERY STRONG"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(total) { i ->
                val segStrength = (i + 1).toFloat() / total
                val segColor    = signalColor(segStrength)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            color = if (i < filled) segColor else MDGrid,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text(
            text          = label,
            color         = signalColor(signalStrength).copy(alpha = 0.8f),
            fontSize      = 9.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ControlsRow — recalibrate + mode toggle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ControlsRow(
    showRawValues        : Boolean,
    onToggleMode         : () -> Unit,
    onRecalibrate        : () -> Unit,
    onBottomSheetToggleClick: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Recalibrate button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(MDGrid, RoundedCornerShape(10.dp))
                .border(0.5.dp, MDCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onRecalibrate
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = "◉  RECALIBRATE",
                color         = MDCyan,
                fontSize      = 10.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }

        // Mode toggle button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(MDGrid, RoundedCornerShape(10.dp))
                .border(0.5.dp, MDCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onToggleMode
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = if (showRawValues) "⇄  DEVIATION" else "⇄  RAW",
                color         = MDCyan,
                fontSize      = 10.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }

    // Info tap
    Text(
        text          = "TAP  ↑  FOR SENSOR DETAILS",
        color         = MDCyan.copy(alpha = 0.35f),
        fontSize      = 9.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier      = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication        = null,
            onClick           = onBottomSheetToggleClick
        )
    )
}
