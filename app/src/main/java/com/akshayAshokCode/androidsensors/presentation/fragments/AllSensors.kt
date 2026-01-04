package com.akshayAshokCode.androidsensors.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.adapter.SensorAdapter
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.databinding.AllSensorsBinding
import com.akshayAshokCode.androidsensors.presentation.AllSensorsViewModel
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager

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
        allSensorsViewModel.sensors.observe(viewLifecycleOwner) { sensor ->
            sensorAdapter = SensorAdapter(context, sensor) { selectedItem: SensorModel ->
                clickedSensor(selectedItem)
            }
            binding.recyclerview.adapter = sensorAdapter
        }
        return binding.root
    }

    private fun clickedSensor(sensor: SensorModel) {
        val navController = findNavController()
        if(navController.currentDestination?.id != R.id.allSensors) {
            return // Already navigated away
        }

        // Determine feature name and navigation action
        val (featureName, navigationAction) = when (sensor.id) {
            Constants.METAL_DETECTOR -> AnalyticsManager.Features.METAL_DETECTOR to R.id.action_allSensors_to_metalDetector
            Constants.GRAVITY_METER -> AnalyticsManager.Features.GRAVITY_METER to R.id.action_allSensors_to_gravityMeter
            Constants.BUBBLE_LEVEL_TOOL -> AnalyticsManager.Features.BUBBLE_LEVEL to R.id.action_allSensors_to_bubbleLevelTool
            else -> return
        }

        // Log analytics
        AnalyticsManager.logFeatureOpened(featureName)
        if (!PreferencesManager.hasOpenedFirstFeature(requireContext())) {
            AnalyticsManager.logFirstFeatureOpened(featureName)
            PreferencesManager.setFirstFeatureOpened(requireContext())
        }

        // Navigate
        findNavController().navigate(navigationAction)
    }
}