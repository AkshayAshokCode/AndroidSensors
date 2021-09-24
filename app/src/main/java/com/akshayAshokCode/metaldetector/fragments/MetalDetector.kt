package com.akshayAshokCode.metaldetector.fragments

import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.akshayAshokCode.metaldetector.R
import com.akshayAshokCode.metaldetector.databinding.MetalDetectorBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class MetalDetector : Fragment(), SensorEventListener {
    private lateinit var binding: MetalDetectorBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var DECIMAL_FORMATTER: DecimalFormat
    private var magneticValue="0"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.metal_detector, container,false)
        // define decimal formatter
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.decimalSeparator = '.'
        DECIMAL_FORMATTER = DecimalFormat("#.000", symbols)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)==null){
            binding.notAvailable.visibility=View.VISIBLE
        }
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
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.light_red))
                    }
                    progressValue<150 -> {
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.red))
                    }
                    else -> {
                        binding.progressBar.progressTintList= ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.dark_red))
                    }
                }
            }
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}