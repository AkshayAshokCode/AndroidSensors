package com.akshayAshokCode.metaldetector

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.akshayAshokCode.metaldetector.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

    // Add Gravity meter
    // Add Heart rate meter
    // Add pressure meter
    // Add Relative Humidity
class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private val TAG="MainActivity"
    private lateinit var sensorManager: SensorManager
    private lateinit var DECIMAL_FORMATTER: DecimalFormat
    private var magneticValue="0"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // define decimal formatter
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.decimalSeparator = '.'
        DECIMAL_FORMATTER = DecimalFormat("#.000", symbols)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // get values for each axes X,Y,Z
            val magX = p0.values[0];
            val magY = p0.values[1];
            val magZ = p0.values[2];
            val magnitude=Math.sqrt((magX.times(magX.toDouble())).plus(magY.times(magY.toDouble())).plus(magZ.times(magZ.toDouble())))
             magneticValue=DECIMAL_FORMATTER.format(magnitude);
            // set value on the screen
            binding.value.text = magneticValue + " µTesla";

            val progressValue=magneticValue.toDouble().toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.progressBar.setProgress(progressValue,true)
                when {
                    progressValue<100 -> {
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(this,R.color.light_red))
                    }
                    progressValue<150 -> {
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(this,R.color.red))
                    }
                    else -> {
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(this,R.color.dark_red))
                    }
                }
            }
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}