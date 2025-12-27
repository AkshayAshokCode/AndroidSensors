package com.akshayAshokCode.androidsensors.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val binding: SensorItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.sensor_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sensorsList[position], context, clickListener)

    }

    override fun getItemCount(): Int {
        return sensorsList.size
    }

    class ViewHolder(val binding: SensorItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(sensor: SensorModel, context: Context?, clickListener: (SensorModel) -> Unit) {
            binding.sensorName.text = context?.getString(sensor.nameResId)
            binding.sensorIcon.setImageResource(sensor.icon)

            if (!sensor.isAvailable){
                binding.root.alpha = 0.5f
                binding.comingSoonLabel.visibility = View.VISIBLE
                binding.root.setOnClickListener { /* Disabled */ }
            }else{
                binding.root.alpha = 1f
                binding.comingSoonLabel.visibility = View.GONE
                binding.root.setOnClickListener { clickListener(sensor) }
            }
        }

    }
}
