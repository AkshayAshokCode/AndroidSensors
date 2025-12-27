package com.akshayAshokCode.androidsensors.data

import android.hardware.Sensor
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.R

class SensorRepository {
    fun getSensors(): List<SensorModel> {
        return listOf(
            SensorModel(
                Constants.METAL_DETECTOR,
                R.string.sensor_name_metal_detector,
                R.drawable.metal_detector_icon,
                Sensor.TYPE_MAGNETIC_FIELD
            ),
            SensorModel(
                Constants.GRAVITY_METER,
                R.string.sensor_name_gravity_meter,
                R.drawable.gravity_icon,
                Sensor.TYPE_GRAVITY
            ),
            SensorModel(
                Constants.BUBBLE_LEVEL_TOOL,
                R.string.sensor_name_bubble_level,
                R.drawable.bubble_level_icon,
                Sensor.TYPE_ACCELEROMETER
            )
        )
    }
}