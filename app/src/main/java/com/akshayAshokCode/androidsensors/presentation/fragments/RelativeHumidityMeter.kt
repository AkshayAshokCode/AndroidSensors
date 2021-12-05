package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.databinding.RelativeHumidityMeterBinding

class RelativeHumidityMeter: Fragment(), SensorEventListener {
    private val TAG="RelativeHumidityMeter"
    private lateinit var binding: RelativeHumidityMeterBinding
    private lateinit var sensorManager: SensorManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding=DataBindingUtil.inflate(inflater, R.layout.relative_humidity_meter,container,false)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)==null){
            binding.notAvailable.visibility=View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor?.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
            Log.d(TAG,"TYPE_RELATIVE_HUMIDITY")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}