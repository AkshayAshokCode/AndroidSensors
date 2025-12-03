package com.akshayAshokCode.androidsensors.utils

import com.akshayAshokCode.androidsensors.R
import kotlin.math.*

object SensorUtils {

    fun extractNumericValue(valueString: String): Float {
        return try {
            val regex = "[-+]?\\d*\\.?\\d+".toRegex()
            regex.find(valueString)?.value?.toFloat() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun getOrientationEmoji(status: String): String {
        return when {
            status.contains("horizontally") -> " \uD83D\uDCF1 "
            status.contains("straight") -> "\uD83D\uDDFF "
            status.contains("upside down") -> " \uD83D\uDE43 "
            status.contains("ceiling") -> " ⬆\uFE0F "
            status.contains("floor") -> " ⬇\uFE0F "
            else -> "  "
        }
    }

    fun getSimpleOrientationText(status: String): String {
        return when {
            status.contains("horizontally") -> "Landscape Mode"
            status.contains("straight") -> "Portrait Mode"
            status.contains("upside down") -> "Upside Down"
            status.contains("ceiling") -> "Face Up"
            status.contains("floor") -> "Face Down"
            else -> "Unknown Position"
        }
    }

    fun getGravityMagnitudeColorRes(magnitude: Float): Int {
        return when {
            magnitude < 8f -> R.color.gravity_low
            magnitude < 12f -> R.color.gravity_normal
            else -> R.color.gravity_high

        }
    }

    // Bubble Level utility functions
    fun applySmoothingFilter(
        currentValue: Float,
        newValue: Float,
        smoothingFactor: Float
    ): Float {
        return currentValue + smoothingFactor * (newValue - currentValue)
    }

    fun calculateCalibrationOffset(
        currentPitch: Float,
        currentRoll: Float,
    ): Pair<Float, Float> {
        return Pair(-currentPitch, -currentRoll)
    }

    fun calculateCalibratedAngles(
        smoothedPitch: Float,
        smoothedRoll: Float,
        calibrationOffsetPitch: Float,
        calibrationOffsetRoll: Float
    ): Pair<Float, Float> {
        return Pair(
            smoothedPitch + calibrationOffsetPitch,
            smoothedRoll + calibrationOffsetRoll
        )
    }

    fun isDeviceLevel(pitch: Float, roll: Float, tolerance: Float): Boolean {
        return abs(pitch) < tolerance && abs(roll) < tolerance
    }

    fun calculateBubblePosition(
        centerX: Float,
        centerY: Float,
        roll: Float,
        pitch: Float,
        maxOffset: Float
    ): Pair<Float, Float> {
        val bubbleX = centerX + (roll / 45f) * maxOffset
        val bubbleY = centerY - (pitch / 45f) * maxOffset

        // Clamp to circle bounds
        val distance = sqrt((bubbleX - centerX).pow(2) + (bubbleY - centerY).pow(2))

        return if (distance > maxOffset) {
            val angle = atan2(bubbleY - centerY, bubbleX - centerX)
            Pair(
                centerX + cos(angle) * maxOffset,
                centerY + sin(angle) * maxOffset
            )
        } else {
            Pair(bubbleX, bubbleY)
        }
    }

    fun getTiltMagnitude(pitch: Float, roll: Float): Float {
        return sqrt(pitch * pitch + roll * roll)
    }

    fun getBubbleColorForTilt(
        isLevel: Boolean,
        tiltMagnitude: Float,
        tolerance: Float
    ): Int {
        return when {
            isLevel -> R.color.gravity_low // Green - Level
            tiltMagnitude < tolerance * 2 -> R.color.gravity_normal // Yellow - Close to level
            tiltMagnitude < tolerance * 4 -> R.color.orange // Orange - Moderate tilt
            else -> R.color.gravity_high // Red - High tilt
        }
    }

    fun getBubbleSizeForPrecision(tolerance: Float, baseSizeDp: Float): Float {
        return when {
            tolerance <= 1f -> baseSizeDp * 0.75f // Smaller for precision
            tolerance >= 5f -> baseSizeDp * 1.25f // Larger for rough
            else -> baseSizeDp // Standard
        }
    }

    // Grid calculation utilities
    data class GridLine(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float
    )

    data class GridCircle(
        val centerX: Float,
        val centerY: Float,
        val radius: Float
    )

    fun calculateGridCircles(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): List<GridCircle> {
        return (1..3).map { i ->
            GridCircle(
                centerX = centerX,
                centerY = centerY,
                radius = radius * (i / 4f)
            )
        }
    }

    fun calculateGridLines(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): List<GridLine> {
        return (0..7).map { i ->
            val angle = (i * 45f) * (PI / 180f).toFloat()
            val startRadius = radius * 0.2f
            val endRadius = radius * 0.9f

            GridLine(
                startX = centerX + cos(angle) * startRadius,
                startY = centerY + sin(angle) * startRadius,
                endX = centerX + cos(angle) * endRadius,
                endY = centerY + sin(angle) * endRadius
            )
        }
    }
}