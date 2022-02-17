package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.akshayAshokCode.androidsensors.databinding.OrientationSensorBinding

class OrientationSensor : Fragment(),SensorEventListener {
    private lateinit var binding: OrientationSensorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= OrientationSensorBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}