package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.presentation.fragments.BubbleLevelTool
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val LVoid    = DashVoid
private val LCyan    = DashCyan
private val LGrid    = DashGrid
private val LSurface = DashSurface
private val LGreen   = DashGreen
private val LRed     = DashRed
private val LAmber   = DashAmber

// ─────────────────────────────────────────────────────────────────────────────
// LevelBackground — same dot-grid void system
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LevelBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(LVoid)) {
        val sp = 28.dp.toPx(); val r = 1.1.dp.toPx()
        val cols = (size.width / sp).toInt() + 2
        val rows = (size.height / sp).toInt() + 2
        repeat(rows) { row -> repeat(cols) { col ->
            drawCircle(LGrid, r, Offset(col * sp, row * sp))
        }}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LevelStatusLabel — "LEVEL LOCKED ✓" or "ADJUSTING..."
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LevelStatusLabel(isLevel: Boolean, tolerance: Float) {
    val pulse = rememberInfiniteTransition(label = "lvl")
    val alpha by pulse.animateFloat(
        initialValue  = if (isLevel) 0.6f else 1f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "la"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text          = if (isLevel) "✓  LEVEL LOCKED" else "◈  ADJUSTING...",
            color         = (if (isLevel) LGreen else LAmber).copy(alpha = if (isLevel) alpha else 1f),
            fontSize      = 13.sp,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text          = "TOLERANCE  ±${"%.1f".format(tolerance)}°",
            color         = LCyan.copy(alpha = 0.45f),
            fontSize      = 9.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SensitivityToggle — three-way button strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SensitivityToggle(
    currentMode : BubbleLevelTool.SensitivityMode,
    onModeChange: (BubbleLevelTool.SensitivityMode) -> Unit
) {
    val modes = BubbleLevelTool.SensitivityMode.entries
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(LSurface.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .border(0.5.dp, LGrid, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        modes.forEach { mode ->
            val active = mode == currentMode
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(
                        if (active) LCyan.copy(alpha = 0.18f) else Color.Transparent,
                        RoundedCornerShape(7.dp)
                    )
                    .border(
                        0.5.dp,
                        if (active) LCyan.copy(alpha = 0.7f) else Color.Transparent,
                        RoundedCornerShape(7.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onModeChange(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = mode.name,
                    color      = if (active) LCyan else LCyan.copy(alpha = 0.35f),
                    fontSize   = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TargetReticle — crosshair + diamond indicator (replaces old bubble circle)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TargetReticle(
    pitch    : Float,
    roll     : Float,
    isLevel  : Boolean,
    tolerance: Float,
    modifier : Modifier = Modifier
) {
    val diamondX by animateFloatAsState(
        (roll  / 45f).coerceIn(-1f, 1f),
        spring(dampingRatio = 0.55f, stiffness = 100f), label = "dx"
    )
    val diamondY by animateFloatAsState(
        (pitch / 45f).coerceIn(-1f, 1f),
        spring(dampingRatio = 0.55f, stiffness = 100f), label = "dy"
    )

    val pulse = rememberInfiniteTransition(label = "rp")
    val glowAlpha by pulse.animateFloat(
        0.2f, if (isLevel) 0.7f else 0.35f,
        infiniteRepeatable(tween(if (isLevel) 500 else 1200), RepeatMode.Reverse),
        label = "glow"
    )

    val accentColor = if (isLevel) LGreen else LCyan
    val diamondPath = remember { Path() }

    Canvas(modifier = modifier) {
        val cx   = size.width  / 2f
        val cy   = size.height / 2f
        val maxR = size.minDimension / 2f * 0.88f

        // Outer ring
        drawCircle(accentColor.copy(alpha = 0.6f), maxR, Offset(cx, cy),
            style = Stroke(1.dp.toPx()))

        // Tolerance ring — smaller inner circle showing the "level zone"
        val tolFrac = (tolerance / 45f).coerceIn(0.05f, 0.5f)
        drawCircle(accentColor.copy(alpha = 0.25f), maxR * tolFrac, Offset(cx, cy),
            style = Stroke(1.dp.toPx()))
        // Filled tolerance zone (very faint)
        drawCircle(accentColor.copy(alpha = if (isLevel) 0.08f else 0.04f),
            maxR * tolFrac, Offset(cx, cy))

        // Full-length crosshair lines
        drawLine(LGrid.copy(alpha = 0.6f), Offset(0f, cy), Offset(size.width, cy), 0.5.dp.toPx())
        drawLine(LGrid.copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, size.height), 0.5.dp.toPx())

        // Tick marks around outer ring
        repeat(36) { i ->
            val angle  = i * 10.0
            val isMain = i % 9 == 0
            val r1     = maxR * (if (isMain) 0.88f else 0.92f)
            val r2     = maxR
            val rad    = Math.toRadians(angle)
            drawLine(
                LCyan.copy(alpha = if (isMain) 0.5f else 0.2f),
                Offset(cx + (r1 * cos(rad)).toFloat(), cy + (r1 * sin(rad)).toFloat()),
                Offset(cx + (r2 * cos(rad)).toFloat(), cy + (r2 * sin(rad)).toFloat()),
                if (isMain) 1.dp.toPx() else 0.5.dp.toPx()
            )
        }

        // Diamond indicator position
        val px = cx + diamondX * maxR * 0.85f
        val py = cy + diamondY * maxR * 0.85f
        val ds = 10.dp.toPx()  // diamond half-size

        // Glow behind diamond
        drawCircle(accentColor.copy(alpha = glowAlpha * 0.3f), ds * 2.5f, Offset(px, py))

        // Diamond shape (4 points) — reuse the remembered Path, resetting each frame
        diamondPath.apply {
            reset()
            moveTo(px,       py - ds)   // top
            lineTo(px + ds,  py)        // right
            lineTo(px,       py + ds)   // bottom
            lineTo(px - ds,  py)        // left
            close()
        }
        drawPath(diamondPath, accentColor.copy(alpha = 0.25f))
        drawPath(diamondPath, accentColor,
            style = Stroke(width = 1.5.dp.toPx()))

        // Centre dot
        drawCircle(accentColor, 3.dp.toPx(), Offset(cx, cy))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUDAngleCard — compact HUD panel showing one angle value
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HUDAngleCard(label: String, value: Float, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .background(LSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(0.5.dp, LGrid, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val color = when {
            abs(value) < 1f  -> LGreen
            abs(value) < 10f -> LCyan
            else             -> LAmber
        }
        Text(
            text          = "${"%.1f".format(value)}°",
            color         = color,
            fontSize      = 22.sp,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text          = label,
            color         = LCyan.copy(alpha = 0.45f),
            fontSize      = 8.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            textAlign     = TextAlign.Center
        )
    }
}
