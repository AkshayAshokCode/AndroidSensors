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
                R.string.tagline_metal_detector,
                R.drawable.metal_detector_icon,
                Sensor.TYPE_MAGNETIC_FIELD
            ),
            SensorModel(
                Constants.GRAVITY_METER,
                R.string.sensor_name_gravity_meter,
                R.string.tagline_gravity_meter,
                R.drawable.gravity_icon,
                Sensor.TYPE_GRAVITY
            ),
            SensorModel(
                Constants.BUBBLE_LEVEL_TOOL,
                R.string.sensor_name_bubble_level,
                R.string.tagline_bubble_level,
                R.drawable.bubble_level_icon,
                Sensor.TYPE_ACCELEROMETER
            ),
            SensorModel(
                Constants.SPACE_BALL,
                R.string.sensor_name_space_ball,
                R.string.tagline_space_ball,
                R.drawable.gravity_ball_icon,
                Sensor.TYPE_GRAVITY
            ),
            SensorModel(
                Constants.WIFI_PULSE,
                R.string.sensor_name_wifi_pulse,
                R.string.tagline_wifi_pulse,
                R.drawable.ic_wifi_pulse,
                Constants.NO_SENSOR_TYPE
            )
        )
    }
}