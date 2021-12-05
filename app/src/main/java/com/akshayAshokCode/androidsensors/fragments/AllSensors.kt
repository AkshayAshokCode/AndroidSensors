package com.akshayAshokCode.androidsensors.fragments

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.adapter.SensorAdapter
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.databinding.AllSensorsBinding

class AllSensors : Fragment() {
    private val TAG = "AllSensors"
    private lateinit var binding: AllSensorsBinding
    private lateinit var sensorAdapter: SensorAdapter
    private val sensorsList: MutableList<SensorModel> = mutableListOf()
    private lateinit var sensorManager: SensorManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.all_sensors, container, false)
        binding.recyclerview.layoutManager = GridLayoutManager(context, 2)
        if(sensorsList.isEmpty()) {
            sensorsList.add(0, SensorModel("Metal detector", R.drawable.metal_icon,Sensor.TYPE_MAGNETIC_FIELD))
            sensorsList.add(1, SensorModel("Gravity meter", R.drawable.gravity_icon,Sensor.TYPE_GRAVITY))
            sensorsList.add(2, SensorModel("Heart rate meter", R.drawable.heart_icon,Sensor.TYPE_HEART_RATE))
            sensorsList.add(3, SensorModel("Pressure meter", R.drawable.pressure_icon,Sensor.TYPE_PRESSURE))
            sensorsList.add(4, SensorModel("Relative Humidity", R.drawable.humidity_icon,Sensor.TYPE_RELATIVE_HUMIDITY))
        }

     /*   sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        var sensors: List<Sensor>
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in sensors) {
            Log.d(TAG, "All sensors: ${sensor.name}")
        }
        */
        sensorAdapter = SensorAdapter(context,sensorsList)
        binding.recyclerview.adapter = sensorAdapter
        return binding.root
    }
}