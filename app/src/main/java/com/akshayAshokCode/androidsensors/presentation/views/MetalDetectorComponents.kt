package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MetalDetectorScreen(
    magneticValue: String,
    isAvailable: Boolean,
    showRawValues: Boolean,
    onToggleMode: () -> Unit,
    onRecalibrate: () -> Unit,
    onBottomSheetToggleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.metal_detector_background),
                        colorResource(R.color.gravity_background_start),
                        colorResource(R.color.gravity_background_middle)
                    )
                )
            )
    ) {
        if (!isAvailable) {
            Text(
                text = stringResource(R.string.metal_detector_not_available),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Mode toggle
                ModeToggleCard(showRawValues, onToggleMode, onRecalibrate)

                Spacer(modifier = Modifier.height(24.dp))

                // Main detector display
                MetalDetectorRadar(magneticValue, showRawValues)

                Spacer(modifier = Modifier.height(32.dp))

                // Value and status cards
                MetalDetectorStatusCards(magneticValue, showRawValues)

                Spacer(modifier = Modifier.height(24.dp))

                // Signal strength indicator
                SignalStrengthIndicator(magneticValue, showRawValues)

                Spacer(modifier = Modifier.weight(1f))

                // Bottom info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBottomSheetToggleClick() }
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.tap_for_sensor_details),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetalDetectorRadar(magneticValue: String, showRawValues: Boolean) {
    // Handle recalibration state for both modes
    if (magneticValue == "Recalibrating...") {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = colorResource(R.color.metal_detector_green),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recalibrating...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Please wait",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    val value = magneticValue.toDoubleOrNull() ?: 0.0
    val normalizedValue = if (showRawValues) {
        ((value - 25.0) / 1000.0).coerceIn(0.0, 1.0) // Raw: 25-1025 µT range
    } else {
        (value / 1000.0).coerceIn(0.0, 1.0) // Deviation: 0-1000 µT range
    }

    // Get colors outside Canvas block
    val greenColor = colorResource(R.color.metal_detector_green)
    val orangeColor = colorResource(R.color.orange)
    val redColor = colorResource(R.color.dark_red)

    // Pulsing animation based on magnetic field strength
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 - (normalizedValue * 800)).toInt().coerceAtLeast(200),
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radar background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (normalizedValue > 0.7) pulseScale else 1f)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.9f

            // Draw concentric circles
            val circleColors = listOf(
                greenColor.copy(alpha = 0.1f),
                greenColor.copy(alpha = 0.2f),
                greenColor.copy(alpha = 0.3f)
            )

            circleColors.forEachIndexed { index, color ->
                drawCircle(
                    color = color,
                    radius = radius * (0.3f + index * 0.35f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw crosshairs
            drawLine(
                color = greenColor.copy(alpha = 0.5f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = greenColor.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // Draw sweep line
            val sweepX = center.x + cos(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius
            val sweepY = center.y + sin(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius

            drawLine(
                color = greenColor.copy(alpha = 0.8f),
                start = center,
                end = Offset(sweepX, sweepY),
                strokeWidth = 3.dp.toPx()
            )

            // Draw detection blips using pre-fetched colors
            if (normalizedValue > 0.3) {
                val blipRadius = (normalizedValue * 20).toFloat()
                val blipColor = when {
                    normalizedValue > 0.8 -> redColor
                    normalizedValue > 0.5 -> orangeColor
                    else -> greenColor
                }

                drawCircle(
                    color = blipColor.copy(alpha = 0.8f),
                    radius = blipRadius,
                    center = center
                )
                drawCircle(
                    color = blipColor,
                    radius = blipRadius * 0.5f,
                    center = center
                )
            }
        }

        // Center value display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${String.format("%.1f", value)}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "µTesla",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ModeToggleCard(
    showRawValues: Boolean,
    onToggleMode: () -> Unit,
    onRecalibrate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.gravity_card_outer)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (showRawValues) "ADVANCED MODE" else "SIMPLE MODE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (showRawValues) "Raw magnetic field readings" else "Metal detection with alerts",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = showRawValues,
                    onCheckedChange = { onToggleMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorResource(R.color.metal_detector_green),
                        checkedTrackColor = colorResource(R.color.metal_detector_green).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }

            // Recalibration button (show for both modes)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRecalibrate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.metal_detector_green).copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "RECALIBRATE",
                    color = colorResource(R.color.metal_detector_green),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MetalDetectorStatusCards(magneticValue: String, showRawValues: Boolean) {
    if (magneticValue == "Recalibrating...") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.gravity_card_outer)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "---",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        return
    }

    val value = magneticValue.toDoubleOrNull() ?: 0.0

    val (detectionLevel, statusColor, rangeText) = if (showRawValues) {
        // Raw magnetic field thresholds
        val level = when {
            value < 30 -> "NORMAL"
            value < 100 -> "SLIGHT ANOMALY"
            value < 300 -> "MODERATE"
            value < 600 -> "STRONG"
            else -> "VERY STRONG"
        }
        val color = when {
            value < 30 -> colorResource(R.color.gravity_low)
            value < 100 -> colorResource(R.color.gravity_low)
            value < 300 -> colorResource(R.color.gravity_normal)
            value < 600 -> colorResource(R.color.orange)
            else -> colorResource(R.color.dark_red)
        }
        Triple(level, color, "25-1000+ µT")
    } else {
        // Metal detection thresholds (deviation from baseline)
        val level = when {
            value < 10 -> "CLEAR"
            value < 50 -> "WEAK"
            value < 200 -> "MODERATE"
            value < 500 -> "STRONG"
            else -> "VERY STRONG"
        }
        val color = when {
            value < 10 -> colorResource(R.color.gravity_low)
            value < 50 -> colorResource(R.color.gravity_low)
            value < 200 -> colorResource(R.color.gravity_normal)
            value < 500 -> colorResource(R.color.orange)
            else -> colorResource(R.color.dark_red)
        }
        Triple(level, color, "0-1000+ µT")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.gravity_card_outer)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STATUS",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detectionLevel,
                    color = statusColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.gravity_card_outer)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RANGE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rangeText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SignalStrengthIndicator(magneticValue: String, showRawValues: Boolean) {
    if (magneticValue == "Recalibrating...") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.gravity_card_outer)
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showRawValues) "Recalibrating sensor..." else "Recalibrating baseline...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val value = magneticValue.toDoubleOrNull() ?: 0.0
    val normalizedValue = if (showRawValues) {
        ((value - 25.0) / 1000.0).coerceIn(0.0, 1.0) // Raw: 25-1025 µT range
    } else {
        (value / 1000.0).coerceIn(0.0, 1.0) // Deviation: 0-1000 µT range
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.gravity_card_outer)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "SIGNAL STRENGTH",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Signal bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(20) { index ->
                    val barHeight = 8.dp + (index * 2).dp
                    val isActive = normalizedValue >= (index / 20.0)

                    val barColor = when {
                        index < 7 -> colorResource(R.color.gravity_low) // Green
                        index < 14 -> colorResource(R.color.gravity_normal) // Yellow
                        else -> colorResource(R.color.dark_red) // Red
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) barColor else colorResource(R.color.white).copy(alpha = 0.1f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage display
            Text(
                text = "${(normalizedValue * 100).toInt()}%",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
