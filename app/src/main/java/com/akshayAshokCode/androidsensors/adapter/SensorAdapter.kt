package com.akshayAshokCode.androidsensors.adapter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.databinding.SensorItemBinding

class SensorAdapter(private val context: Context?, private val sensorsList: List<SensorModel>) :
    RecyclerView.Adapter<SensorAdapter.ViewHolder>() {
    private lateinit var binding: SensorItemBinding
    private lateinit var sensorManager: SensorManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding=DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.sensor_item,parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sensor=sensorsList[position]
        binding.sensorName.text=sensor.name
        binding.sensorIcon.setImageResource(sensor.icon)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(sensor.sensorType)==null){
          //  binding.root.visibility= View.GONE
        }
        binding.root.setOnClickListener {
            when(position){
                0-> it.findNavController().navigate(R.id.action_allSensors_to_metalDetector)
                1-> it.findNavController().navigate(R.id.action_allSensors_to_gravityMeter)
                2-> it.findNavController().navigate(R.id.action_allSensors_to_heartRateMeter)
                3-> it.findNavController().navigate(R.id.action_allSensors_to_pressureMeter)
                4-> it.findNavController().navigate(R.id.action_allSensors_to_relativeHumidityMeter2)
            }
        }
    }

    override fun getItemCount(): Int {
        return sensorsList.size
    }

    class ViewHolder(val binding:SensorItemBinding) : RecyclerView.ViewHolder(binding.root)
    {

    }
}
