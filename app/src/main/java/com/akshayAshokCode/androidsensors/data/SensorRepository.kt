package com.akshayAshokCode.androidsensors.data

import android.hardware.Sensor
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.R

class SensorRepository {
    fun getSensors(): List<SensorModel> {
        return listOf(
            SensorModel(
                Constants.METAL_DETECTOR,
                "Metal detector",
                R.drawable.metal_icon,
                Sensor.TYPE_MAGNETIC_FIELD
            ),
            SensorModel(
                Constants.GRAVITY_METER,
                "Gravity meter",
                R.drawable.gravity_icon,
                Sensor.TYPE_GRAVITY
            ),
            SensorModel(
                Constants.BUBBLE_LEVEL_TOOL,
                name = "Bubble Level",
                R.drawable.gravity_icon,
                Sensor.TYPE_ACCELEROMETER
            )
        )
    }
}