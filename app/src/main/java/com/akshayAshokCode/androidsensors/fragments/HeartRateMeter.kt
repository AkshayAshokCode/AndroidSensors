package com.akshayAshokCode.androidsensors.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.databinding.HeartRateMeterBinding

class HeartRateMeter: Fragment(), SensorEventListener {
    private val TAG="HeartRateMeter"
    private lateinit var binding: HeartRateMeterBinding
    private lateinit var sensorManager: SensorManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= DataBindingUtil.inflate(inflater, R.layout.heart_rate_meter,container,false)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        var sensors= listOf<Sensor>()
        sensors=sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in sensors){

        }
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)==null){
            binding.notAvailable.visibility=View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor?.type == Sensor.TYPE_HEART_RATE) {
          //  Log.d(TAG,"TYPE_HEART_RATE")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}
