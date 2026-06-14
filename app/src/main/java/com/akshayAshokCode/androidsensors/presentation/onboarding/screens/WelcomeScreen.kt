package com.akshayAshokCode.androidsensors.presentation.onboarding.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val WCyan   = Color(0xFF00D4FF)
private val WGrid   = Color(0xFF1A1A3E)
private val WPurple = Color(0xFF7B2FFF)

@Composable
fun WelcomeScreen() {
    var showTitle       by remember { mutableStateOf(false) }
    var showSubtitle    by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400); showTitle       = true
        delay(300); showSubtitle    = true
        delay(250); showDescription = true
    }

    val titleAlpha       by animateFloatAsState(if (showTitle)       1f else 0f, tween(450), label = "ta")
    val subtitleAlpha    by animateFloatAsState(if (showSubtitle)    1f else 0f, tween(450), label = "sa")
    val descriptionAlpha by animateFloatAsState(if (showDescription) 1f else 0f, tween(450), label = "da")

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RadarAnimation(modifier = Modifier.size(200.dp))

        Spacer(Modifier.height(48.dp))

        Text(
            text          = "SENSOR SUITE",
            color         = Color.White,
            fontSize      = 28.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.4.sp,
            textAlign     = TextAlign.Center,
            modifier      = Modifier.graphicsLayer { alpha = titleAlpha }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text          = "YOUR PHONE. REDEFINED.",
            color         = WCyan,
            fontSize      = 12.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 2.sp,
            textAlign     = TextAlign.Center,
            modifier      = Modifier.graphicsLayer { alpha = subtitleAlpha }
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text       = "Professional sensor tools built into the device you already carry.",
            color      = Color.White.copy(alpha = 0.55f),
            fontSize   = 14.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp,
            modifier   = Modifier.graphicsLayer { alpha = descriptionAlpha }
        )
    }
}

@Composable
private fun RadarAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "sweep"
    )
    val ringAlpha by transition.animateFloat(
        initialValue  = 0.15f,
        targetValue   = 0.45f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label         = "ring"
    )

    Canvas(modifier = modifier) {
        val cx   = size.width  / 2f
        val cy   = size.height / 2f
        val c    = Offset(cx, cy)
        val maxR = size.minDimension / 2f

        listOf(1.0f, 0.72f, 0.46f).forEachIndexed { i, frac ->
            drawCircle(
                WCyan.copy(alpha = ringAlpha * (1f - i * 0.2f)),
                maxR * frac, c, style = Stroke(0.8.dp.toPx())
            )
        }

        drawLine(WGrid, Offset(0f, cy), Offset(size.width, cy), 0.5.dp.toPx())
        drawLine(WGrid, Offset(cx, 0f), Offset(cx, size.height), 0.5.dp.toPx())

        val trailSteps = 30
        val trailSpan  = 110f
        repeat(trailSteps) { i ->
            val frac  = i.toFloat() / trailSteps
            val angle = Math.toRadians((sweepAngle - trailSpan + frac * trailSpan).toDouble())
            drawLine(
                WCyan.copy(alpha = frac * 0.5f),
                c,
                Offset(cx + maxR * cos(angle).toFloat(), cy + maxR * sin(angle).toFloat()),
                1.5.dp.toPx()
            )
        }

        val lead = Math.toRadians(sweepAngle.toDouble())
        drawLine(
            WCyan, c,
            Offset(cx + maxR * cos(lead).toFloat(), cy + maxR * sin(lead).toFloat()),
            2.dp.toPx(), cap = StrokeCap.Round
        )

        drawCircle(WPurple.copy(alpha = 0.6f), 6.dp.toPx(), c)
        drawCircle(WCyan,                      3.dp.toPx(), c)
    }
}
