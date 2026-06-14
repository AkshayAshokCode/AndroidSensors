package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.utils.SensorUtils
import kotlin.math.abs
import kotlin.math.sqrt

private val GMVoid    = DashVoid
private val GMCyan    = DashCyan
private val GMGrid    = DashGrid
private val GMSurface = DashSurface
private val GMGreen   = DashGreen
private val GMRed     = Color(0xFFE91E63)   // X axis — intentionally distinct from DashRed
private val GMYellow  = Color(0xFFFFB800)   // Y axis
private val GMBlue    = Color(0xFF2196F3)   // Z axis

// ─────────────────────────────────────────────────────────────────────────────
// GravityBackground — same dot-grid void as the dashboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GravityBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(GMVoid)) {
        val sp = 28.dp.toPx(); val r = 1.1.dp.toPx()
        val cols = (size.width / sp).toInt() + 2
        val rows = (size.height / sp).toInt() + 2
        repeat(rows) { row -> repeat(cols) { col ->
            drawCircle(GMGrid, r, Offset(col * sp, row * sp))
        }}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OrientationBadge — tells user what position their phone is in
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OrientationBadge(orientation: SensorUtils.PhoneOrientation) {
    val (label, color) = when (orientation) {
        SensorUtils.PhoneOrientation.PORTRAIT    -> "↑  PORTRAIT"     to GMGreen
        SensorUtils.PhoneOrientation.UPSIDE_DOWN -> "↓  UPSIDE DOWN"  to GMYellow
        SensorUtils.PhoneOrientation.LANDSCAPE   -> "↔  LANDSCAPE"    to GMCyan
        SensorUtils.PhoneOrientation.FACE_UP     -> "⊙  FACE UP"      to GMCyan
        SensorUtils.PhoneOrientation.FACE_DOWN   -> "⊗  FACE DOWN"    to GMYellow
        else                                     -> "·  UNKNOWN"       to GMGrid
    }
    Box(
        modifier = Modifier
            .background(GMSurface.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text          = label,
            color         = color,
            fontSize      = 10.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GravitySphere — globe visualization where dot = direction of "down"
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GravitySphere(
    rawGX    : Float,
    rawGY    : Float,
    rawGZ    : Float,
    modifier : Modifier = Modifier
) {
    // Smooth dot position with spring animation
    val dotX by animateFloatAsState(
        (rawGX / 9.8f).coerceIn(-1f, 1f),
        spring(dampingRatio = 0.6f, stiffness = 120f), label = "dx"
    )
    val dotY by animateFloatAsState(
        (rawGY / 9.8f).coerceIn(-1f, 1f),
        spring(dampingRatio = 0.6f, stiffness = 120f), label = "dy"
    )
    val magnitude = sqrt(rawGX * rawGX + rawGY * rawGY + rawGZ * rawGZ)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            val r  = size.minDimension / 2f * 0.85f

            // Outer border
            drawCircle(GMCyan.copy(alpha = 0.7f), r, Offset(cx, cy), style = Stroke(1.dp.toPx()))

            // Latitude rings (33%, 66%)
            drawCircle(GMGrid.copy(alpha = 0.5f), r * 0.66f, Offset(cx, cy), style = Stroke(0.5.dp.toPx()))
            drawCircle(GMGrid.copy(alpha = 0.3f), r * 0.33f, Offset(cx, cy), style = Stroke(0.5.dp.toPx()))

            // Cross-hair grid lines
            drawLine(GMGrid.copy(alpha = 0.5f), Offset(cx - r, cy), Offset(cx + r, cy), 0.5.dp.toPx())
            drawLine(GMGrid.copy(alpha = 0.5f), Offset(cx, cy - r), Offset(cx, cy + r), 0.5.dp.toPx())

            // Gravity dot position (clamped to sphere radius)
            val px = cx + dotX * r * 0.9f
            val py = cy + dotY * r * 0.9f
            val dotR = (6f + (magnitude / 9.8f) * 4f).dp.toPx()

            // Glow
            drawCircle(GMCyan.copy(alpha = 0.15f), dotR * 2.8f, Offset(px, py))
            drawCircle(GMCyan.copy(alpha = 0.35f), dotR * 1.6f, Offset(px, py))
            // Solid dot
            drawCircle(GMCyan, dotR, Offset(px, py))
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AxisMeters — X / Y / Z horizontal fill bars with values
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AxisMeters(rawGX: Float, rawGY: Float, rawGZ: Float) {
    val axes = listOf(
        Triple("X", rawGX, GMRed),
        Triple("Y", rawGY, GMYellow),
        Triple("Z", rawGZ, GMBlue)
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.fillMaxWidth()
    ) {
        axes.forEach { (label, value, color) ->
            val fill by animateFloatAsState(
                (abs(value) / 9.8f).coerceIn(0f, 1f),
                spring(stiffness = 150f), label = "f$label"
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = label,
                    color      = color,
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.width(14.dp)
                )
                // Bar track
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(GMGrid, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(color.copy(alpha = 0.6f), color)
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
                Text(
                    text       = "${"%.2f".format(value)}",
                    color      = Color.White.copy(alpha = 0.7f),
                    fontSize   = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.width(48.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OscilloscopeGraph — 3-channel waveform (X=red, Y=yellow, Z=blue)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OscilloscopeGraph(
    history : List<Triple<Float, Float, Float>>,
    modifier: Modifier = Modifier
) {
    // Pre-allocate paths once and reset before each draw — avoids 3 allocations per frame
    val xPath = remember { Path() }
    val yPath = remember { Path() }
    val zPath = remember { Path() }

    Box(
        modifier = modifier
            .background(GMSurface.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .border(0.5.dp, GMGrid, RoundedCornerShape(12.dp))
    ) {
        if (history.size < 2) return@Box
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w       = size.width
            val h       = size.height
            val mid     = h / 2f
            val scl     = h / 2f / 11f   // ±11 m/s² range
            val n       = history.size
            val strokeW = 1.5.dp.toPx()

            // Grid centre line
            drawLine(GMGrid, Offset(0f, mid), Offset(w, mid), 0.5.dp.toPx())

            fun buildAndDraw(path: Path, extract: (Triple<Float,Float,Float>) -> Float, color: Color) {
                path.reset()
                history.forEachIndexed { i, pt ->
                    val x = w * i / (n - 1).toFloat()
                    val y = (mid - extract(pt) * scl).coerceIn(0f, h)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round))
            }

            buildAndDraw(xPath, { it.first  }, GMRed)
            buildAndDraw(yPath, { it.second }, GMYellow)
            buildAndDraw(zPath, { it.third  }, GMBlue)
        }

        // Channel labels — top right
        Row(
            modifier              = Modifier.align(Alignment.TopEnd).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("X" to GMRed, "Y" to GMYellow, "Z" to GMBlue).forEach { (l, c) ->
                Text(l, color = c, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
