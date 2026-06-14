package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class MetalDetector : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var DECIMAL_FORMATTER: DecimalFormat

    private var magneticValue by mutableStateOf("0.0")
    private var rawMagneticValue by mutableStateOf("0.0")
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)
    private var showRawValues by mutableStateOf(false)

    // Calibration variables
    private var baselineMagnitude = 0.0
    private var isCalibrated = false
    private val calibrationSamples = mutableListOf<Double>()
    private val maxCalibrationSamples = 20

    private var isRecalibrating by mutableStateOf(false)
    private var reviewFlaggedThisVisit = false

    // New state for futuristic UI
    private var signalStrength     by mutableStateOf(0f)  // 0-1 normalised deviation
    private var calibrationProgress by mutableStateOf(0)  // 0-20 samples collected

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recalibrateRunnable: Runnable? = null

    private fun recalibrate() {
        isCalibrated = false
        calibrationSamples.clear()
        baselineMagnitude = 0.0
        isRecalibrating = true
        signalStrength = 0f
        calibrationProgress = 0

        sensorManager.unregisterListener(this)

        // Cancel any pending recalibration before posting a new one
        recalibrateRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable {
            if (isResumed) {
                sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            isRecalibrating = false
        }
        recalibrateRunnable = r
        mainHandler.postDelayed(r, 1000)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize formatter
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.decimalSeparator = '.'
        DECIMAL_FORMATTER = DecimalFormat("#.0", symbols)

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
                    magneticValue       = displayValue,
                    signalStrength      = signalStrength,
                    calibrationProgress = calibrationProgress,
                    isCalibrating       = isRecalibrating || !isCalibrated,
                    isAvailable         = isAvailable,
                    showRawValues       = showRawValues,
                    onToggleMode        = { showRawValues = !showRawValues },
                    onRecalibrate       = {
                        AnalyticsManager.logRecalibrateTapped()
                        recalibrate()
                    },
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
        reviewFlaggedThisVisit = false

        // Check sensor availability
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            isAvailable = false
            AnalyticsManager.logSensorUnavailable(AnalyticsManager.Features.METAL_DETECTOR)
        } else {
            recalibrate()
        }
    }

    override fun onPause() {
        super.onPause()
        recalibrateRunnable?.let { mainHandler.removeCallbacks(it) }
        sensorManager.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recalibrateRunnable?.let { mainHandler.removeCallbacks(it) }
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Compute on sensor thread using only local / non-Compose variables
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())

            // Advance calibration state on sensor thread (no Compose state touched here)
            if (!isCalibrated) {
                calibrationSamples.add(magnitude)
                if (calibrationSamples.size >= maxCalibrationSamples) {
                    baselineMagnitude = calibrationSamples.average()
                    isCalibrated = true
                    calibrationSamples.clear()
                }
            }
            val calibrated  = isCalibrated
            val sampleCount = calibrationSamples.size
            val deviation   = if (calibrated) abs(magnitude - baselineMagnitude) else 0.0
            val strength    = if (calibrated) (deviation / 300.0).coerceIn(0.0, 1.0).toFloat() else 0f

            // Push Compose state updates from the main thread.
            // DecimalFormat is not thread-safe, so formatting happens here on the main thread.
            mainHandler.post {
                if (!isAdded) return@post
                rawMagneticValue    = DECIMAL_FORMATTER.format(magnitude)
                signalStrength      = strength
                calibrationProgress = sampleCount
                magneticValue = if (calibrated) {
                    DECIMAL_FORMATTER.format(deviation)
                } else {
                    getString(R.string.calibrating)
                }
                if (calibrated && strength > 0.6f && !reviewFlaggedThisVisit) {
                    reviewFlaggedThisVisit = true
                    AnalyticsManager.logWinAchieved(AnalyticsManager.Features.METAL_DETECTOR)
                    PreferencesManager.recordWin(requireContext())
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
