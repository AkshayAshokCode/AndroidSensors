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
import com.akshayAshokCode.androidsensors.databinding.GravityMeterBinding

class GravityMeter : Fragment(), SensorEventListener {
    private val TAG = "GravityMeter"
    private lateinit var binding: GravityMeterBinding
    private lateinit var sensorManager: SensorManager
    private var gravityUnit = ""
    private var xAxis = ""
    private var yAxis = ""
    private var zAxis = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.gravity_meter, container, false)
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        gravityUnit = getString(R.string.ms)
        xAxis = getString(R.string.x_axis)
        yAxis = getString(R.string.y_axis)
        zAxis = getString(R.string.z_axis)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) == null) {
            binding.notAvailable.visibility = View.VISIBLE
            binding.cardView.visibility=View.GONE
            binding.cardView1.visibility=View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor?.type == Sensor.TYPE_GRAVITY) {
            val x = "$xAxis ${String.format("%.2f",p0.values[0])} $gravityUnit"
            val y = "$yAxis ${String.format("%.2f",p0.values[1])} $gravityUnit"
            val z = "$zAxis ${String.format("%.2f",p0.values[2])} $gravityUnit"
            binding.gravityX.text = x
            binding.gravityY.text = y
            binding.gravityZ.text = z

            when{
                p0.values[0]>7 ->binding.phoneStatus.text=getString(R.string.positive_x_axis)
                p0.values[0]< -7 ->binding.phoneStatus.text=getString(R.string.negative_x_axis)
                p0.values[1]>7 ->binding.phoneStatus.text=getString(R.string.positive_y_axis)
                p0.values[1]< -7 ->binding.phoneStatus.text=getString(R.string.negative_y_axis)
                p0.values[2]>7 ->binding.phoneStatus.text=getString(R.string.positive_z_axis)
                p0.values[2]< -7 ->binding.phoneStatus.text=getString(R.string.negative_z_axis)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}