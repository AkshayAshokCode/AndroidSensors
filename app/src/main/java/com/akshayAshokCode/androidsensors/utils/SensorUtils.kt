package com.akshayAshokCode.androidsensors.utils

import com.akshayAshokCode.androidsensors.R

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
}