package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.data.SensorModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — single source of truth for the futuristic palette
// ─────────────────────────────────────────────────────────────────────────────
internal val DashVoid    = Color(0xFF050510)   // deepest background
internal val DashSurface = Color(0xFF0D0D2B)   // card / surface layer
internal val DashCyan    = Color(0xFF00D4FF)   // primary accent — data, borders, badges
internal val DashPurple  = Color(0xFF7B2FFF)   // secondary accent — gradient highlight
internal val DashGreen   = Color(0xFF00FF88)   // success / READY state
internal val DashRed     = Color(0xFFFF3355)   // danger / OFFLINE state
internal val DashAmber   = Color(0xFFFFB800)   // warning / calibrating state
internal val DashOrange  = Color(0xFFFF6B00)   // strong signal / high energy state
internal val DashGrid    = Color(0xFF1A1A3E)   // subtle dot-grid and dividers

// ─────────────────────────────────────────────────────────────────────────────
// DashboardScreen — public entry point; assembles all layers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    sensors            : List<SensorModel>,
    availability       : Map<Int, Boolean>,
    onSensorClick      : (SensorModel) -> Unit,
    onMenuClick        : () -> Unit = {},
    playEntryAnimation : Boolean = true
) {
    // Single shared transition for all card border pulses — one animation clock instead of four.
    val sharedPulse = rememberInfiniteTransition(label = "cardPulse")
    val ba0 by sharedPulse.animateFloat(initialValue = 0.22f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(   0)), label = "ba0")
    val ba1 by sharedPulse.animateFloat(initialValue = 0.22f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(1760, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset( 420)), label = "ba1")
    val ba2 by sharedPulse.animateFloat(initialValue = 0.22f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(1920, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset( 840)), label = "ba2")
    val ba3 by sharedPulse.animateFloat(initialValue = 0.22f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(2080, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(1260)), label = "ba3")

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 0 — deep-space background
        DashboardBackground(modifier = Modifier.fillMaxSize())

        // Layer 1 — header + sensor grid, pushed below the status bar
        Column(modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            DashboardHeader(onMenuClick = onMenuClick)
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                modifier              = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding        = PaddingValues(top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = sensors,
                    key   = { _, sensor -> sensor.id }
                ) { index, sensor ->
                    SensorCard(
                        sensor             = sensor,
                        isAvailable        = availability[sensor.id] ?: true,
                        index              = index,
                        borderAlpha        = when (index) { 0 -> ba0; 1 -> ba1; 2 -> ba2; else -> ba3 },
                        playEntryAnimation = playEntryAnimation,
                        onClick            = { onSensorClick(sensor) }
                    )
                }
            }
        }

        // Layer 2 — boot scanline (drawn last so it's on top)
        BootScanline(modifier = Modifier.fillMaxSize())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardBackground — static deep-space dot grid on near-black fill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun DashboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(DashVoid)) {
        val spacing = 28.dp.toPx()
        val radius  = 1.2.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2
        repeat(rows) { r ->
            repeat(cols) { c ->
                drawCircle(
                    color  = DashGrid,
                    radius = radius,
                    center = Offset(c * spacing, r * spacing)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardHeader — app name left, live HH:mm:ss clock right
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun DashboardHeader(onMenuClick: () -> Unit = {}) {
    var timeText by remember { mutableStateOf(currentTimeString()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(1_000)
                timeText = currentTimeString()
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 20.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically
        ) {
            // Hamburger — opens the nav drawer (Write Review, Feedback)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector        = Icons.Default.Menu,
                    contentDescription = "Open menu",
                    tint               = DashCyan.copy(alpha = 0.85f)
                )
            }

            Text(
                text          = "SENSOR SUITE",
                color         = Color.White,
                fontSize      = 15.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.4.sp
            )

            Text(
                text       = timeText,
                color      = DashCyan,
                fontFamily = FontFamily.Monospace,
                fontSize   = 13.sp
            )
        }
        HorizontalDivider(color = DashGrid, thickness = 0.5.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BootScanline — one-shot cyan line that sweeps top→bottom on screen entry
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun BootScanline(modifier: Modifier = Modifier) {
    val progress  = remember { Animatable(0f) }
    val lineAlpha = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        delay(80)
        progress.animateTo(1f, tween(240, easing = LinearEasing))
        lineAlpha.animateTo(0f, tween(180, easing = FastOutLinearInEasing))
    }

    Canvas(modifier = modifier) {
        val alpha = lineAlpha.value
        if (alpha <= 0f) return@Canvas
        val y = size.height * progress.value

        // soft glow above the line
        drawRect(
            brush = Brush.verticalGradient(
                colors  = listOf(
                    Color.Transparent,
                    DashCyan.copy(alpha = alpha * 0.14f),
                    DashCyan.copy(alpha = alpha * 0.06f)
                ),
                startY = maxOf(0f, y - 24.dp.toPx()),
                endY   = y
            )
        )
        // sharp scan line
        drawLine(
            color       = DashCyan.copy(alpha = alpha),
            start       = Offset(0f, y),
            end         = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SensorCard — the premium card with 3-D tilt, holographic shimmer,
//              pulsing border, staggered entry, and READY / OFFLINE badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun SensorCard(
    sensor             : SensorModel,
    isAvailable        : Boolean,
    index              : Int,
    borderAlpha        : Float,
    playEntryAnimation : Boolean = true,
    onClick            : () -> Unit
) {
    // Shimmer colors — cached once to avoid re-allocating the List on each recomposition
    val shimmerColors = remember { listOf(Color(0x1400D4FF), Color.Transparent) }

    // ── 1. Staggered entry — graphicsLayer-only so no clipping during spring overshoot
    val slideY     = remember { Animatable(if (playEntryAnimation) -80f else 0f) }
    val entryAlpha = remember { Animatable(if (playEntryAnimation) 0f   else 1f) }
    LaunchedEffect(Unit) {
        if (playEntryAnimation) {
            delay(350L + index * 100L)
            launch { slideY.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 270f)) }
            entryAlpha.animateTo(1f, tween(200))
        }
    }

    val accentColor  = if (isAvailable) DashCyan else DashRed
    val availAlpha   = if (isAvailable) 1f       else 0.52f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .graphicsLayer {
                translationY = slideY.value
                alpha        = entryAlpha.value * availAlpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
    ) {
            // Glass panel + glowing border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = DashSurface.copy(alpha = 0.93f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(0.5.dp, accentColor.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                // Static glass shimmer — centred radial highlight
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = shimmerColors,
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.minDimension * 0.95f
                        )
                    )
                }

                // Content — icon + name + tagline clustered at top, badge pinned to bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Icon at top-left
                    Icon(
                        painter            = painterResource(sensor.icon),
                        contentDescription = null,
                        tint               = accentColor.copy(alpha = 0.88f),
                        modifier           = Modifier.size(30.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name + tagline immediately below icon
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text          = stringResource(sensor.nameResId).uppercase(),
                            color         = Color.White,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            maxLines      = 2,
                            overflow      = TextOverflow.Ellipsis
                        )
                        Text(
                            text          = sensorTagline(sensor.id),
                            color         = Color.White.copy(alpha = 0.48f),
                            fontSize      = 9.sp,
                            letterSpacing = 0.1.sp,
                            maxLines      = 2,
                            overflow      = TextOverflow.Ellipsis
                        )
                    }

                    // Pushes badge to the bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Divider + status row pinned at bottom
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(color = DashGrid, thickness = 0.5.dp)
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Pulsing status dot
                            Canvas(modifier = Modifier.size(5.dp)) {
                                drawCircle(accentColor.copy(alpha = (borderAlpha + 0.18f).coerceAtMost(1f)))
                            }
                            Text(
                                text          = if (isAvailable) "READY" else "OFFLINE",
                                color         = accentColor,
                                fontSize      = 9.sp,
                                fontFamily    = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Short tagline shown on each sensor card. */
internal fun sensorTagline(sensorId: Int): String = when (sensorId) {
    Constants.METAL_DETECTOR    -> "Detect magnetic fields"
    Constants.GRAVITY_METER     -> "3-axis gravity analysis"
    Constants.BUBBLE_LEVEL_TOOL -> "Precision leveling tool"
    Constants.SPACE_BALL        -> "Gravity physics sim"
    else                        -> ""
}

internal fun currentTimeString(): String {
    val c = Calendar.getInstance()
    return "%02d:%02d:%02d".format(
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND)
    )
}
