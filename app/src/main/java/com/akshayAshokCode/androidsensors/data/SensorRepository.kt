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
                Constants.HEART_RATE_METER,
                "Heart rate meter",
                R.drawable.heart_icon,
                Sensor.TYPE_HEART_RATE
            ),
            SensorModel(
                Constants.PRESSURE_METER,
                "Pressure meter",
                R.drawable.pressure_icon,
                Sensor.TYPE_PRESSURE
            ),
            SensorModel(
                Constants.RELATIVE_HUMIDITY,
                "Relative Humidity",
                R.drawable.humidity_icon,
                Sensor.TYPE_RELATIVE_HUMIDITY
            ),
           /* SensorModel(
                Constants.ORIENTATION_SENSOR,
                "Orientation Sensor",
                R.drawable.humidity_icon,
                Sensor.TYPE_ROTATION_VECTOR
            )*/
        )
    }
}