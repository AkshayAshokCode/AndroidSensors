package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.fragments.BubbleLevelTool
import com.akshayAshokCode.androidsensors.utils.SensorUtils

@Composable
fun SensitivityModeSelector(
    currentMode: BubbleLevelTool.SensitivityMode,
    onModeChange: (BubbleLevelTool.SensitivityMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BubbleLevelTool.SensitivityMode.entries.forEach { mode ->
            FilterChip(
                onClick = { onModeChange(mode) },
                label = {
                    Text(
                        text = stringResource(mode.displayNameResId),
                        fontSize = 12.sp
                    )
                },
                selected = currentMode == mode,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorResource(R.color.gravity_low),
                    selectedLabelColor = Color.White,
                    containerColor = colorResource(R.color.gravity_card_outer),
                    labelColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun LevelBubbleView(
    roll: Float,
    pitch: Float,
    isLevel: Boolean,
    tolerance: Float,
    modifier: Modifier = Modifier
) {

    val tiltMagnitude = SensorUtils.getTiltMagnitude(pitch, roll)
    val bubbleColorRes = SensorUtils.getBubbleColorForTilt(isLevel, tiltMagnitude, tolerance)
    val bubbleColor = colorResource(bubbleColorRes)
    val greenColor = colorResource(R.color.gravity_low)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 40.dp.toPx()

        // Draw grid lines for better reference
        drawGripLines(center, radius)

        // Draw tolerance circle (inner circle)
        val toleranceRadius = radius * (tolerance / 45f) * 0.8f
        drawCircle(
            color = greenColor.copy(alpha = 0.2f),
            radius = toleranceRadius,
            center = center
        )

        // Draw outer circle
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw crossHairs
        drawLine(
            color = Color.White,
            start = Offset(center.x - 30.dp.toPx(), center.y),
            end = Offset(center.x + 30.dp.toPx(), center.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - 30.dp.toPx()),
            end = Offset(center.x, center.y + 30.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // Calculate bubble position
        val maxOffset = radius * 0.8f
        val bubblePosition = SensorUtils.calculateBubblePosition(
            center.x, center.y, roll, pitch, maxOffset
        )

        val finalBubblePos = Offset(
            bubblePosition.first,
            bubblePosition.second
        )

        val baseBubbleSize = 20.dp.toPx()
        val bubbleSize = SensorUtils.getBubbleSizeForPrecision(tolerance, baseBubbleSize)

        // Draw bubble shadow for depth
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = bubbleSize,
            center = Offset(finalBubblePos.x + 2.dp.toPx(), finalBubblePos.y + 2.dp.toPx())
        )

        // Draw main bubble
        drawCircle(
            color = bubbleColor,
            radius = bubbleSize,
            center = finalBubblePos
        )

        // Draw bubble highlight for 3D effect
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = bubbleSize * 0.4f,
            center = Offset(
                finalBubblePos.x - bubbleSize * 0.3f,
                finalBubblePos.y - bubbleSize * 0.3f
            )
        )

        // Draw inner glow when level
        if (isLevel) {
            drawCircle(
                color = greenColor.copy(alpha = 0.3f),
                radius = bubbleSize * 1.5f,
                center = finalBubblePos
            )
        }
    }
}

@Composable
fun AngleCard(label: String, angle: Float) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.gravity_card_outer)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "${String.format("%.1f", angle)}°",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Extension function to draw grid lines
private fun DrawScope.drawGripLines(center: Offset, radius: Float) {
    val gridColor = Color.White.copy(alpha = 0.2f)
    val strokeWidth = 1.dp.toPx()

    // Draw concentric circles
    val circles = SensorUtils.calculateGridCircles(
        center.x,
        center.y,
        radius
    )

    circles.forEach { circle ->
        drawCircle(
            color = gridColor,
            radius = circle.radius,
            center = Offset(circle.centerX, circle.centerY),
            style = Stroke(width = strokeWidth)
        )
    }

    // Draw radial lines (8 directions)
    val lines = SensorUtils.calculateGridLines(center.x, center.y, radius)
    lines.forEach { line ->
        drawLine(
            color = gridColor,
            start = Offset(line.startX, line.startY),
            end = Offset(line.endX, line.endY),
            strokeWidth = strokeWidth
        )
    }
}