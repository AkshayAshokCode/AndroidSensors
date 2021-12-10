package com.akshayAshokCode.androidsensors.data

import android.hardware.Sensor
import com.akshayAshokCode.androidsensors.R

class SensorRepository {
    fun getSensors(): List<SensorModel> {
        return listOf(
            SensorModel(
                "Metal detector",
                R.drawable.metal_icon,
                Sensor.TYPE_MAGNETIC_FIELD
            ),
            SensorModel(
                "Gravity meter",
                R.drawable.gravity_icon,
                Sensor.TYPE_GRAVITY
            ),
            SensorModel(
                "Heart rate meter",
                R.drawable.heart_icon,
                Sensor.TYPE_HEART_RATE
            ),
            SensorModel(
                "Pressure meter",
                R.drawable.pressure_icon,
                Sensor.TYPE_PRESSURE
            ),
            SensorModel(
                "Relative Humidity",
                R.drawable.humidity_icon,
                Sensor.TYPE_RELATIVE_HUMIDITY
            )
        )
    }
}