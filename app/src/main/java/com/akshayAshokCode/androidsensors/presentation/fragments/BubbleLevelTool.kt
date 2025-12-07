package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.AngleCard
import com.akshayAshokCode.androidsensors.presentation.views.LevelBubbleView
import com.akshayAshokCode.androidsensors.presentation.views.SensitivityModeSelector
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.SensorUtils

class BubbleLevelTool : Fragment(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator

    private val accelrometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Smoothing variables
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothingFactor = 0.1f

    // Haptic feedback tracking
    private var wasLevel = false

    // Sensitivity modes
    enum class SensitivityMode(val tolerance: Float, val displayName: String) {
        PRECISION(0.5f, "Precision"),
        STANDARD(2.0f, "Standard"),
        ROUGH(5.0f, "Rough")
    }

    private var currentMode by mutableStateOf(SensitivityMode.STANDARD)
    private var pitch by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)
    private var isLevel by mutableStateOf(false)
    private var isAvailabe by mutableStateOf(true)

    // Add calibration offset
    private var calibrationOffsetPitch = 0f
    private var calibrationOffsetRoll = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        vibrator = context?.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator

        return ComposeView(requireContext()).apply {
            setContent {
                BubbleLevelToolScreen(
                    pitch = pitch,
                    roll = roll,
                    isLevel = isLevel,
                    currentMode = currentMode,
                    isAvailable = isAvailabe,
                    onModeChange = { currentMode = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null && magnetometer == null) {
            isAvailabe = false
            return
        }

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelrometerData, 0, event.values.size)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerData, 0, event.values.size)
            }
        }
        updateLevelTool()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun updateLevelTool() {
        if (SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelrometerData,
                magnetometerData
            )
        ) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            smoothedPitch =
                SensorUtils.applySmoothingFilter(smoothedPitch, rawPitch, smoothingFactor)
            smoothedRoll = SensorUtils.applySmoothingFilter(smoothedRoll, rawRoll, smoothingFactor)

            val calibratedAngles = SensorUtils.calculateCalibratedAngles(
                smoothedPitch,
                smoothedRoll,
                calibrationOffsetPitch,
                calibrationOffsetRoll
            )

            // Apply calibration offset
            pitch = calibratedAngles.first
            roll = calibratedAngles.second

            val newIsLevel = SensorUtils.isDeviceLevel(
                pitch,
                roll,
                currentMode.tolerance
            )

            if (newIsLevel && !wasLevel) {
                triggerHapticFeedback()
            }

            isLevel = newIsLevel
            wasLevel = newIsLevel
        }
    }

    private fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Ultra-light tick sensation
            vibrator.vibrate(VibrationEffect.createOneShot(25, 50)) // 25ms, very low amplitude
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20) // Barely noticeable
        }
    }
}

@Composable
fun BubbleLevelToolScreen(
    pitch: Float,
    roll: Float,
    isLevel: Boolean,
    currentMode: BubbleLevelTool.SensitivityMode,
    isAvailable: Boolean,
    onModeChange: (BubbleLevelTool.SensitivityMode) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    if (!isAvailable) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorResource(R.color.gravity_background_start),
                            colorResource(R.color.gravity_background_middle)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sensor not available",
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.gravity_background_start),
                        colorResource(R.color.gravity_background_middle)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        // Sensitivity Mode Selector
        SensitivityModeSelector(
            currentMode = currentMode,
            onModeChange = onModeChange
        )

        // Status Text with tolerance info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isLevel) "LEVEL" else "NOT LEVEL",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLevel) colorResource(R.color.gravity_low) else colorResource(R.color.gravity_high)
            )
            Text(
                text = "Tolerance: \u00B1${currentMode.tolerance}\u00B0",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Level Bubble View
        LevelBubbleView(
            roll = roll,
            pitch = pitch,
            isLevel = isLevel,
            tolerance = currentMode.tolerance,
            modifier = Modifier.size(300.dp)
        )

        // Angle Displays
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AngleCard("PITCH", pitch)
            AngleCard("ROLL", roll)
        }

        // Bottom Info Section with Up Arrow - FIX THE CLICK BEHAVIOR
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    indication = null, // Remove ripple effect
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showBottomSheet = true
                }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "View Details",
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Tap for sensor details",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }

    // Reusable Bottom Sheet - IMPROVE STATE HANDLING
    if (showBottomSheet) {
        SensorDetailsBottomSheet(
            isVisible = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            title = "Bubble Level Details",
            content = stringResource(R.string.bubble_level_details)
        )
    }
}