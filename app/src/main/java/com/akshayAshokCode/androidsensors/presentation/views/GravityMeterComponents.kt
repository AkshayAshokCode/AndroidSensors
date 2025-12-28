package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.utils.SensorUtils
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GravityVectorVisualization(
    xValue: Float,
    yValue: Float,
    zValue: Float,
    modifier: Modifier = Modifier
) {

    val magnitude = sqrt(xValue * xValue + yValue * yValue + zValue * zValue)
    val vectorColor = colorResource(SensorUtils.getGravityMagnitudeColorRes(magnitude))

    Canvas(modifier = modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = size.minDimension / 20f

        // Draw phone outline
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(center.x - 40.dp.toPx(), center.y - 60.dp.toPx()),
            size = Size(80.dp.toPx(), 120.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw small to indicate "screen side"
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(center.x, center.y - 40.dp.toPx())
        )

        // Direct gravity direction: Arrow points where gravity is pulling.
        // When phone is tilt left, gravity pulls left (negative X), arrow points left.
        // When phone is tilt down, as gravity always pulls downward towards earth, arrow points right.
        val vectorEnd = Offset(
            x = center.x - (xValue * scale),
            y = center.y + (yValue * scale)
        )

        drawLine(
            color = vectorColor,
            start = center,
            end = vectorEnd,
            strokeWidth = 4.dp.toPx()
        )

        // Draw arrowhead pointing in gravity direction
        val arrowSize = 8.dp.toPx()
        val angle = atan2(
            vectorEnd.y - center.y,
            vectorEnd.x - center.x
        )

        drawLine(
            color = vectorColor,
            start = vectorEnd,
            end = Offset(
                x = vectorEnd.x - arrowSize * cos(angle - 0.5f),
                y = vectorEnd.y - arrowSize * sin(angle - 0.5f)
            ),
            strokeWidth = 3.dp.toPx()
        )

        drawLine(
            color = vectorColor,
            start = vectorEnd,
            end = Offset(
                x = vectorEnd.x - arrowSize * cos(angle + 0.5f),
                y = vectorEnd.y - arrowSize * sin(angle + 0.5f)
            ),
            strokeWidth = 3.dp.toPx()
        )

        // Draw magnitude circle
        drawCircle(
            color = vectorColor.copy(alpha = 0.2f),
            radius = magnitude * scale / 4f,
            center = center
        )
    }
}

@Composable
fun GravityAxisCard(
    axis: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.3f),
                            color.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.axis_label, axis),
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.2f", SensorUtils.extractNumericValue(value)),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.axis_unit),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun GravityVectorCard(
    xValue: Float,
    yValue: Float,
    zValue: Float,
    phoneOrientation: SensorUtils.PhoneOrientation,
    modifier: Modifier = Modifier
) {
    val magnitude = sqrt(xValue * xValue + yValue * yValue + zValue * zValue)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorResource(R.color.gravity_card_outer),
                            colorResource(R.color.gravity_card_inner)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val context = LocalContext.current
                GravityMagnitudeHeader(magnitude)
                Spacer(modifier = Modifier.height(16.dp))
                GravityVectorVisualization(xValue, yValue, zValue)
                Spacer(modifier = Modifier.height(12.dp))
                // Phone orientation
                Text(
                    text = "${SensorUtils.getOrientationEmoji(phoneOrientation)} ${
                        SensorUtils.getSimpleOrientationText(
                            phoneOrientation,
                            context
                        )
                    }",
                    color = colorResource(R.color.gravity_low),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GravityMagnitudeHeader(
    magnitude: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.total_magnitude, magnitude),
            color = colorResource(SensorUtils.getGravityMagnitudeColorRes(magnitude)),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GravityGraph(
    history: List<Triple<Float, Float, Float>>,
    modifier: Modifier = Modifier
) {
    // Get colors outside Canvas block
    val axisXColor = colorResource(R.color.axis_x_color)
    val axisYColor = colorResource(R.color.axis_y_color)
    val axisZColor = colorResource(R.color.axis_z_color)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorResource(R.color.gravity_card_outer),
                            colorResource(R.color.gravity_card_inner)
                        )
                    ),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.real_time_graph),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Canvas(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                ) {
                    if (history.isEmpty()) return@Canvas

                    val width = size.width
                    val height = size.height
                    val stepX = width / maxOf(1, history.size - 1)

                    val colors = listOf(axisXColor, axisYColor, axisZColor)

                    for (axis in 0..2) {
                        val path = Path()
                        history.forEachIndexed { index, (x, y, z) ->
                            val value = when (axis) {
                                0 -> x
                                1 -> y
                                else -> z
                            }
                            val normalizedValue = (value + 10f) / 20f
                            val yPos = height - (normalizedValue * height).coerceIn(0f, height)

                            if (index == 0) {
                                path.moveTo(0f, yPos)
                            } else {
                                path.lineTo(stepX * index, yPos)
                            }
                        }

                        drawPath(
                            path = path,
                            color = colors[axis],
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(stringResource(R.string.x_axis), colorResource(R.color.axis_x_color))
                    LegendItem(stringResource(R.string.y_axis), colorResource(R.color.axis_y_color))
                    LegendItem(stringResource(R.string.z_axis), colorResource(R.color.axis_z_color))
                }
            }

        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}
