package com.akshayAshokCode.androidsensors.data

data class SensorModel(
    val id: Int,
    val name: String,
    val icon: Int,
    val sensorType: Int,
    val isAvailable: Boolean = true
)
