package com.akshayAshokCode.androidsensors.data

import androidx.annotation.StringRes

data class SensorModel(
    val id: Int,
    @StringRes val nameResId: Int,
    val icon: Int,
    val sensorType: Int,
    val isAvailable: Boolean = true
)
