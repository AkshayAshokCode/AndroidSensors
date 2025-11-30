package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.GravityAxisCard
import com.akshayAshokCode.androidsensors.presentation.views.GravityGraph
import com.akshayAshokCode.androidsensors.presentation.views.GravityVectorCard
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.SensorUtils

class GravityMeter : Fragment(), SensorEventListener {
    private val TAG = "GravityMeter"
    private lateinit var sensorManager: SensorManager

    private var xValue by mutableStateOf("")
    private var yValue by mutableStateOf("")
    private var zValue by mutableStateOf("")
    private var phoneStatus by mutableStateOf("")
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)

    private val gravityHistory = mutableStateListOf<Triple<Float, Float, Float>>()
    private val maxHistorySize = 50

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        // initialize string resources
        xValue = getString(R.string.intial_x_value)
        yValue = getString(R.string.intial_y_value)
        zValue = getString(R.string.intial_z_value)
        phoneStatus = getString(R.string.initial_phone_status)

        return ComposeView(requireContext()).apply {
            setContent {
                GravityMeterScreen(
                    xValue = xValue,
                    yValue = yValue,
                    zValue = zValue,
                    phoneStatus = phoneStatus,
                    isAvailable = isAvailable,
                    gravityHistory = gravityHistory.toList(),
                    onBottomSheetToggleClick = { showBottomSheet = true }
                )

                SensorDetailsBottomSheet(
                    isVisible = showBottomSheet,
                    onDismiss = { showBottomSheet = false },
                    title = stringResource(R.string.gravity_meter_details_title),
                    content = stringResource(R.string.gravity_meter_details)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) == null) {
            isAvailable = false
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_GRAVITY) {

            val xAxis = getString(R.string.x_axis)
            val yAxis = getString(R.string.y_axis)
            val zAxis = getString(R.string.z_axis)
            val gravityUnit = getString(R.string.ms)


            xValue = "$xAxis ${String.format("%.2f", event.values[0])} $gravityUnit"
            yValue = "$yAxis ${String.format("%.2f", event.values[1])} $gravityUnit"
            zValue = "$zAxis ${String.format("%.2f", event.values[2])} $gravityUnit"

            gravityHistory.add(Triple(event.values[0], event.values[1], event.values[2]))
            if (gravityHistory.size > maxHistorySize) gravityHistory.removeAt(0)

            phoneStatus = when {
                event.values[0] > 7 -> getString(R.string.positive_x_axis)
                event.values[0] < -7 -> getString(R.string.negative_x_axis)
                event.values[1] > 7 -> getString(R.string.positive_y_axis)
                event.values[1] < -7 -> getString(R.string.negative_y_axis)
                event.values[2] > 7 -> getString(R.string.positive_z_axis)
                event.values[2] < -7 -> getString(R.string.negative_z_axis)
                else -> phoneStatus
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}

@Composable
fun GravityMeterScreen(
    xValue: String,
    yValue: String,
    zValue: String,
    phoneStatus: String,
    isAvailable: Boolean,
    gravityHistory: List<Triple<Float, Float, Float>>,
    onBottomSheetToggleClick: () -> Unit
) {
    val xFloat = SensorUtils.extractNumericValue(xValue)
    val yFloat = SensorUtils.extractNumericValue(yValue)
    val zFloat = SensorUtils.extractNumericValue(zValue)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.gravity_background_start),
                        colorResource(R.color.gravity_background_middle),
                        colorResource(R.color.gravity_background_end)
                    )
                )
            )
    ) {
        if (!isAvailable) {
            Text(
                text = stringResource(R.string.gravity_meter_not_available),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                GravityVectorCard(
                    xValue = xFloat,
                    yValue = yFloat,
                    zValue = zFloat,
                    phoneStatus = phoneStatus,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GravityAxisCard(
                        stringResource(R.string.x_axis),
                        xValue,
                        colorResource(R.color.axis_x_color),
                        Modifier.weight(1f)
                    )
                    GravityAxisCard(
                        stringResource(R.string.y_axis),
                        yValue,
                        colorResource(R.color.axis_y_color),
                        Modifier.weight(1f)
                    )
                    GravityAxisCard(
                        stringResource(R.string.z_axis),
                        zValue,
                        colorResource(R.color.axis_z_color),
                        Modifier.weight(1f)
                    )
                }

                if (gravityHistory.isNotEmpty()) {
                    GravityGraph(
                        history = gravityHistory,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { onBottomSheetToggleClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = stringResource(R.string.tap_for_sensor_details),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                }
            }
        }
    }
}


