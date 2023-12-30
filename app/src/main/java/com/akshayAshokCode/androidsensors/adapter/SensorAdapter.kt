package com.akshayAshokCode.androidsensors.adapter

import android.content.Context
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.databinding.SensorItemBinding

class SensorAdapter(
    private val context: Context?,
    private val sensorsList: List<SensorModel>,
    private val clickListener: (SensorModel) -> Unit
) :
    RecyclerView.Adapter<SensorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val binding:SensorItemBinding=DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.sensor_item,parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sensorsList[position], context, clickListener)

    }

    override fun getItemCount(): Int {
        return sensorsList.size
    }

    class ViewHolder(val binding:SensorItemBinding) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(sensor: SensorModel, context: Context?, clickListener: (SensorModel) -> Unit){
            binding.sensorName.text=sensor.name
            binding.sensorIcon.setImageResource(sensor.icon)
            binding.root.setOnClickListener {
                clickListener(sensor)
            }
          val  sensorManager:SensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
//            if (sensorManager.getDefaultSensor(sensor.sensorType)==null){
//                  //binding.root.visibility= View.GONE
//            }
        }

    }
}
