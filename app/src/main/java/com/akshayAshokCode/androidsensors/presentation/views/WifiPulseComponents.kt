package com.akshayAshokCode.androidsensors.presentation.views

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.network.InternetBlocker
import com.akshayAshokCode.androidsensors.network.InternetState
import com.akshayAshokCode.androidsensors.network.InternetStatus
import com.akshayAshokCode.androidsensors.network.InternetTarget
import com.akshayAshokCode.androidsensors.network.SignalLevel
import com.akshayAshokCode.androidsensors.network.SignalReading
import kotlinx.coroutines.delay
import java.util.Date

private val WPVoid    = DashVoid
private val WPSurface = DashSurface
private val WPCyan    = DashCyan
private val WPGrid    = DashGrid
private val WPGreen   = DashGreen
private val WPAmber   = DashAmber
private val WPRed     = DashRed

/** The phone stopped a waiting alert in the background; found when the user opens the screen. */
enum class StoppedBySystem {
    RESUMED, // the internet is still down, so the wait was turned back on
    MISSED   // the internet came back while the alert was stopped
}

private fun statusColor(state: InternetState): Color = when {
    state.blocker != null                      -> WPRed
    state.status == InternetStatus.ONLINE      -> WPGreen
    state.status == InternetStatus.NO_INTERNET -> WPAmber
    else                                       -> WPRed
}

// ─────────────────────────────────────────────────────────────────────────────
// WifiPulseScreen — public entry point
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WifiPulseScreen(
    state                : InternetState,   // watched connection (Wi-Fi or mobile data) and its status
    signal               : SignalReading?,  // null when not connected
    armedSince           : Long?,           // null when the alert is not armed
    notificationsBlocked : Boolean,
    isAvailable          : Boolean,
    ringMode             : Boolean,   // alert style: true = ring, false = notification
    onRingModeChange     : (Boolean) -> Unit,
    stoppedBySystem      : StoppedBySystem?,
    onOpenAppSettings    : () -> Unit,
    onArm                : () -> Unit,
    onCancel             : () -> Unit,
    onFixBlocker         : (InternetBlocker) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBottomSheetToggleClick  : () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PulseBackground(modifier = Modifier.fillMaxSize())

        if (!isAvailable) {
            Text(
                text      = stringResource(R.string.wifi_pulse_not_available),
                color     = WPCyan.copy(alpha = 0.7f),
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(32.dp)
            )
            return@Box
        }

        val isArmed  = armedSince != null
        val blocker  = state.blocker
        val isOnline = blocker == null && state.status == InternetStatus.ONLINE
        val color    = statusColor(state)

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PulseStatusLabel(state = state, signal = signal, color = color)

            PulseOrb(
                color    = color,
                target   = state.target,
                signal   = signal,
                isArmed  = isArmed,
                // Shrinks on short screens so the button and style toggle always fit
                modifier = Modifier
                    .weight(1f, fill = false)
                    .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )

            PulseMessage(
                armedSince           = armedSince,
                ringMode             = ringMode,
                stoppedBySystem      = stoppedBySystem,
                onOpenAppSettings    = onOpenAppSettings,
                isOnline             = isOnline,
                blocker              = blocker,
                notificationsBlocked = notificationsBlocked
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    isArmed -> PulseButton(
                        text    = stringResource(R.string.wifi_pulse_button_cancel),
                        color   = WPRed,
                        onClick = onCancel
                    )
                    blocker != null -> PulseButton(
                        text    = stringResource(
                            if (blocker == InternetBlocker.WIFI_OFF) R.string.wifi_pulse_button_wifi_settings
                            else R.string.wifi_pulse_button_airplane_settings
                        ),
                        color   = WPCyan,
                        onClick = { onFixBlocker(blocker) }
                    )
                    isOnline -> PulseButton(
                        text    = "✓  " + stringResource(R.string.wifi_pulse_button_online),
                        color   = WPGreen,
                        enabled = false,
                        onClick = {}
                    )
                    notificationsBlocked -> PulseButton(
                        text    = stringResource(R.string.wifi_pulse_button_notification_settings),
                        color   = WPAmber,
                        onClick = onOpenNotificationSettings
                    )
                    else -> {
                        PulseButton(
                            text     = "◉  " + stringResource(R.string.wifi_pulse_button_arm),
                            color    = WPCyan,
                            emphasis = true,
                            onClick  = onArm
                        )
                        AlertStyleToggle(ringMode = ringMode, onRingModeChange = onRingModeChange)
                    }
                }

                Text(
                    text          = "↑  " + stringResource(R.string.tap_for_sensor_details).uppercase(),
                    color         = WPCyan.copy(alpha = 0.35f),
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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PulseBackground — same dot grid as the other tools
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PulseBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(WPVoid)) {
        val dotSpacing = 28.dp.toPx()
        val dotRadius  = 1.1.dp.toPx()
        val cols = (size.width  / dotSpacing).toInt() + 2
        val rows = (size.height / dotSpacing).toInt() + 2
        repeat(rows) { r -> repeat(cols) { c ->
            drawCircle(WPGrid, dotRadius, Offset(c * dotSpacing, r * dotSpacing))
        }}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PulseStatusLabel — status of the watched connection (Wi-Fi or mobile data) + signal line
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PulseStatusLabel(
    state  : InternetState,
    signal : SignalReading?,
    color  : Color
) {
    val wifi = state.target == InternetTarget.WIFI
    val (symbol, textRes) = when {
        state.blocker == InternetBlocker.WIFI_OFF      -> "○" to R.string.wifi_pulse_status_wifi_off
        state.blocker == InternetBlocker.AIRPLANE_MODE -> "○" to R.string.wifi_pulse_status_airplane
        state.status == InternetStatus.ONLINE ->
            "●" to if (wifi) R.string.wifi_pulse_status_online else R.string.wifi_pulse_status_online_mobile
        state.status == InternetStatus.NO_INTERNET ->
            "◈" to if (wifi) R.string.wifi_pulse_status_no_internet else R.string.wifi_pulse_status_no_internet_mobile
        else ->
            "●" to if (wifi) R.string.wifi_pulse_status_disconnected else R.string.wifi_pulse_status_disconnected_mobile
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text          = "$symbol  " + stringResource(textRes).uppercase(),
            color         = color,
            fontSize      = 12.sp,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            textAlign     = TextAlign.Center
        )
        if (signal != null && state.blocker == null) {
            // e.g. "-54 dBm · EXCELLENT · 5 GHz" (Wi-Fi) or "-95 dBm · GOOD · Carrier" (mobile data)
            val parts = listOfNotNull(
                signal.dbm?.let { "$it dBm" },
                stringResource(signalLabelRes(signal.level)).uppercase(),
                signal.detail
            )
            Text(
                text          = parts.joinToString("  ·  "),
                color         = WPCyan.copy(alpha = 0.6f),
                fontSize      = 10.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                textAlign     = TextAlign.Center
            )
        }
    }
}

private fun signalLabelRes(level: SignalLevel): Int = when (level) {
    SignalLevel.EXCELLENT -> R.string.wifi_pulse_signal_excellent
    SignalLevel.GOOD      -> R.string.wifi_pulse_signal_good
    SignalLevel.FAIR      -> R.string.wifi_pulse_signal_fair
    SignalLevel.WEAK      -> R.string.wifi_pulse_signal_weak
}

// ─────────────────────────────────────────────────────────────────────────────
// PulseOrb — connection glyph (Wi-Fi arcs or mobile bars) with heartbeat rings; fast triple ping while armed
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PulseOrb(
    color    : Color,
    target   : InternetTarget,
    signal   : SignalReading?, // null: all bars lit (no reading); otherwise lit bars = strength
    isArmed  : Boolean,
    modifier : Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val ping by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(if (isArmed) 1800 else 3600, easing = LinearEasing)),
        label         = "ping"
    )
    val breathe by transition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label         = "breathe"
    )
    val ringCount = if (isArmed) 3 else 1

    Canvas(modifier = modifier) {
        val c    = center
        val maxR = size.minDimension / 2f

        // Static guide rings
        for (i in 1..4) {
            drawCircle(WPGrid, maxR * i / 4f, c, style = Stroke(1.dp.toPx()))
        }

        // Expanding heartbeat rings
        repeat(ringCount) { k ->
            val p = (ping + k / ringCount.toFloat()) % 1f
            drawCircle(
                color  = color.copy(alpha = (1f - p) * 0.55f),
                radius = maxR * (0.3f + 0.7f * p),
                center = c,
                style  = Stroke(2.dp.toPx())
            )
        }

        // Core glow + disc
        val coreR = maxR * 0.3f
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f * breathe), Color.Transparent),
                center = c,
                radius = maxR * 0.5f
            ),
            radius = maxR * 0.5f,
            center = c
        )
        drawCircle(WPSurface, coreR, c)
        drawCircle(color.copy(alpha = 0.8f), coreR, c, style = Stroke(1.5.dp.toPx()))

        // With a signal reading, the glyph doubles as a meter: lit bars = strength,
        // unlit bars faint; a weak signal lights one bar, dimmed.
        val bars     = signal?.level?.bars ?: 3
        val litAlpha = if (signal?.level == SignalLevel.WEAK) 0.5f else 1f
        fun barColor(i: Int) = if (i < bars) color.copy(alpha = litAlpha) else color.copy(alpha = 0.15f)

        if (target == InternetTarget.MOBILE) {
            // Mobile data glyph: three rising bars
            val barWidth = coreR * 0.18f
            val gap      = coreR * 0.12f
            val baseY    = c.y + coreR * 0.4f
            val left     = c.x - (barWidth * 3 + gap * 2) / 2f
            listOf(0.3f, 0.55f, 0.8f).forEachIndexed { i, h ->
                val height = coreR * h
                drawRoundRect(
                    color        = barColor(i),
                    topLeft      = Offset(left + i * (barWidth + gap), baseY - height),
                    size         = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 3f)
                )
            }
            return@Canvas
        }

        // Wi-Fi glyph: three arcs + dot, pointing up
        val glyph  = Offset(c.x, c.y + coreR * 0.42f)
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        listOf(0.3f, 0.55f, 0.8f).forEachIndexed { i, f ->
            val r = coreR * f
            drawArc(
                color      = barColor(i),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter  = false,
                topLeft    = Offset(glyph.x - r, glyph.y - r),
                size       = Size(r * 2, r * 2),
                style      = stroke
            )
        }
        drawCircle(color, 3.dp.toPx(), glyph)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PulseMessage — explains the current state in plain words
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PulseMessage(
    armedSince           : Long?,
    ringMode             : Boolean,
    stoppedBySystem      : StoppedBySystem?,
    onOpenAppSettings    : () -> Unit,
    isOnline             : Boolean,
    blocker              : InternetBlocker?,
    notificationsBlocked : Boolean
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            stoppedBySystem != null -> {
                MessageBody(
                    stringResource(
                        if (stoppedBySystem == StoppedBySystem.RESUMED) R.string.wifi_pulse_stopped_resumed
                        else R.string.wifi_pulse_stopped_missed
                    ),
                    WPAmber
                )
                MessageBody(stringResource(R.string.wifi_pulse_battery_hint))
                Text(
                    text          = stringResource(R.string.wifi_pulse_button_app_settings).uppercase() + "  ›",
                    color         = WPCyan,
                    fontSize      = 10.sp,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier      = Modifier
                        .clickable(onClick = onOpenAppSettings)
                        .padding(8.dp)
                )
            }
            armedSince != null -> {
                MessageTitle(stringResource(R.string.wifi_pulse_armed_title), WPGreen)
                RotatingLine()
                MessageCaption(
                    stringResource(
                        R.string.wifi_pulse_armed_since,
                        DateFormat.getTimeFormat(LocalContext.current).format(Date(armedSince))
                    )
                )
                MessageCaption(
                    stringResource(
                        if (ringMode) R.string.wifi_pulse_armed_style_ring
                        else R.string.wifi_pulse_armed_style_notification
                    )
                )
            }
            blocker != null -> MessageBody(
                stringResource(
                    if (blocker == InternetBlocker.WIFI_OFF) R.string.wifi_pulse_wifi_off_hint
                    else R.string.wifi_pulse_airplane_hint
                ),
                WPAmber
            )
            isOnline -> MessageBody(stringResource(R.string.wifi_pulse_online_hint))
            notificationsBlocked ->
                MessageBody(stringResource(R.string.wifi_pulse_notifications_off_hint), WPAmber)
            else -> {
                MessageTitle(stringResource(R.string.wifi_pulse_headline), Color.White)
                MessageBody(stringResource(R.string.wifi_pulse_trust_1))
                MessageBody(stringResource(R.string.wifi_pulse_trust_4))
            }
        }
    }
}

/** Cycles the friendly "take a break" lines while waiting. */
@Composable
private fun RotatingLine() {
    val lines = stringArrayResource(R.array.wifi_pulse_waiting_lines)
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(lines.size) {
        while (true) {
            delay(8_000)
            index = (index + 1) % lines.size
        }
    }
    Crossfade(targetState = lines[index % lines.size], label = "line") { line ->
        MessageBody(line)
    }
}

@Composable
private fun MessageTitle(text: String, color: Color) {
    Text(
        text       = text,
        color      = color,
        fontSize   = 15.sp,
        fontWeight = FontWeight.Bold,
        textAlign  = TextAlign.Center
    )
}

@Composable
private fun MessageBody(text: String, color: Color = Color.White.copy(alpha = 0.7f)) {
    Text(
        text       = text,
        color      = color,
        fontSize   = 13.sp,
        lineHeight = 19.sp,
        textAlign  = TextAlign.Center,
        modifier   = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MessageCaption(text: String) {
    Text(
        text          = text.uppercase(),
        color         = WPCyan.copy(alpha = 0.5f),
        fontSize      = 9.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 0.8.sp,
        textAlign     = TextAlign.Center
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// AlertStyleToggle — Ring / Notification, chosen at the moment of arming
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlertStyleToggle(ringMode: Boolean, onRingModeChange: (Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(WPSurface.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .border(0.5.dp, WPGrid, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                true  to "🔔  " + stringResource(R.string.wifi_pulse_style_ring),
                false to "🔕  " + stringResource(R.string.wifi_pulse_style_notification)
            ).forEach { (ring, label) ->
                val active = ring == ringMode
                Box(
                    modifier         = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .background(
                            if (active) WPCyan.copy(alpha = 0.18f) else Color.Transparent,
                            RoundedCornerShape(7.dp)
                        )
                        .border(
                            0.5.dp,
                            if (active) WPCyan.copy(alpha = 0.7f) else Color.Transparent,
                            RoundedCornerShape(7.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onRingModeChange(ring) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = label.uppercase(),
                        color         = if (active) WPCyan else WPCyan.copy(alpha = 0.4f),
                        fontSize      = 10.sp,
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = if (active) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Text(
            text      = stringResource(
                if (ringMode) R.string.wifi_pulse_style_ring_hint
                else R.string.wifi_pulse_style_notification_hint
            ),
            color     = Color.White.copy(alpha = 0.5f),
            fontSize  = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PulseButton — full-width HUD button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PulseButton(
    text     : String,
    color    : Color,
    onClick  : () -> Unit,
    enabled  : Boolean = true,
    emphasis : Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(color.copy(alpha = if (emphasis) 0.18f else 0.08f), RoundedCornerShape(12.dp))
            .border(
                if (emphasis) 1.dp else 0.5.dp,
                color.copy(alpha = if (enabled) 0.7f else 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled           = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = text.uppercase(),
            color         = color.copy(alpha = if (enabled) 1f else 0.6f),
            fontSize      = 11.sp,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center,
            modifier      = Modifier.padding(horizontal = 12.dp)
        )
    }
}
