package com.akshayAshokCode.androidsensors.fragments

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
import com.akshayAshokCode.androidsensors.databinding.GravityMeterBinding

class GravityMeter : Fragment(), SensorEventListener {
    private val TAG="GravityMeter"
    private lateinit var binding: GravityMeterBinding
    private lateinit var sensorManager: SensorManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.gravity_meter, container, false)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)==null){
            binding.notAvailable.visibility=View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor?.type == Sensor.TYPE_GRAVITY) {
            Log.d(TAG,"GRAVITY")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}