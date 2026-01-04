package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.MetalDetectorScreen
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class MetalDetector : Fragment(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var DECIMAL_FORMATTER: DecimalFormat

    private var magneticValue by mutableStateOf("0.000")
    private var rawMagneticValue by mutableStateOf("0.000")
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)
    private var showRawValues by mutableStateOf(false)

    // Calibration variables
    private var baselineMagnitude = 0.0
    private var isCalibrated = false
    private val calibrationSamples = mutableListOf<Double>()
    private val maxCalibrationSamples = 20

    // Add recalibration state for visual feedback
    private var isRecalibrating by mutableStateOf(false)

    private fun recalibrate() {
        isCalibrated = false
        calibrationSamples.clear()
        baselineMagnitude = 0.0
        isRecalibrating = true

        // Restart sensor with consistent delay for both modes
        sensorManager.unregisterListener(this)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isRecalibrating = false
        }, 1000)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize formatter
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.decimalSeparator = '.'
        DECIMAL_FORMATTER = DecimalFormat("#.000", symbols)

        // Initialize sensor manager
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        return ComposeView(requireContext()).apply {
            setContent {
                val displayValue = when {
                    isRecalibrating -> stringResource(R.string.recalibrating)
                    showRawValues -> rawMagneticValue
                    else -> magneticValue
                }

                MetalDetectorScreen(
                    magneticValue = displayValue,
                    isAvailable = isAvailable,
                    showRawValues = showRawValues,
                    onToggleMode = { showRawValues = !showRawValues },
                    onRecalibrate = { recalibrate() },
                    onBottomSheetToggleClick = {
                        showBottomSheet = true
                        AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.METAL_DETECTOR)
                    }
                )

                SensorDetailsBottomSheet(
                    isVisible = showBottomSheet,
                    onDismiss = { showBottomSheet = false },
                    title = stringResource(R.string.metal_detector_details_title),
                    instruction = stringResource(R.string.metal_detector_instruction),
                    content = stringResource(R.string.metal_detector_details)
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add menu to toolbar using MenuProvider
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.sensor_info_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_info -> {
                        showBottomSheet = true
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()

        // Check sensor availability
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            isAvailable = false
        } else {
            recalibrate()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magnitude = sqrt(
                (event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]).toDouble()
            )

            // Update raw value
            rawMagneticValue = DECIMAL_FORMATTER.format(magnitude)

            // Handle calibration for metal detection mode
            if (!isCalibrated) {
                calibrationSamples.add(magnitude)
                if (calibrationSamples.size >= maxCalibrationSamples) {
                    baselineMagnitude = calibrationSamples.average()
                    isCalibrated = true
                    calibrationSamples.clear()
                }
                magneticValue = context?.getString(R.string.calibrating) ?: "Calibrating..."
            } else {
                val deviation = abs(magnitude - baselineMagnitude)
                magneticValue = DECIMAL_FORMATTER.format(deviation)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
