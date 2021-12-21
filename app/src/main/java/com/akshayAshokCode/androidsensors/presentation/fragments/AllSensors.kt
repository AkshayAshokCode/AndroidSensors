package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.adapter.SensorAdapter
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.databinding.AllSensorsBinding
import com.akshayAshokCode.androidsensors.presentation.AllSensorsViewModel

class AllSensors : Fragment() {
    private val TAG = "AllSensors"
    private lateinit var binding: AllSensorsBinding
    private lateinit var sensorAdapter: SensorAdapter
    private lateinit var allSensorsViewModel: AllSensorsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.all_sensors, container, false)
        binding.recyclerview.layoutManager = GridLayoutManager(context, 2)
        allSensorsViewModel = ViewModelProvider(this).get(AllSensorsViewModel::class.java)
        allSensorsViewModel.sensors.observe(viewLifecycleOwner, { sensor ->
            sensorAdapter = SensorAdapter(context, sensor) { selectedItem: SensorModel ->
                clickedSensor(selectedItem)
            }
            binding.recyclerview.adapter = sensorAdapter
        })
        return binding.root
    }

    private fun clickedSensor(sensor: SensorModel) {
        when(sensor.id){
            Constants.METAL_DETECTOR-> findNavController().navigate(R.id.action_allSensors_to_metalDetector)
            Constants.GRAVITY_METER-> findNavController().navigate(R.id.action_allSensors_to_gravityMeter)
            Constants.HEART_RATE_METER-> findNavController().navigate(R.id.action_allSensors_to_heartRateMeter)
            Constants.PRESSURE_METER-> findNavController().navigate(R.id.action_allSensors_to_pressureMeter)
            Constants.RELATIVE_HUMIDITY-> findNavController().navigate(R.id.action_allSensors_to_relativeHumidityMeter2)
        }
        //To handle update on clicks
    }
}