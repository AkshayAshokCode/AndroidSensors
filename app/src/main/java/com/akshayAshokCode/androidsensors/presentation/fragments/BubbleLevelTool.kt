package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.HUDAngleCard
import com.akshayAshokCode.androidsensors.presentation.views.LevelBackground
import com.akshayAshokCode.androidsensors.presentation.views.SensitivityToggle
import com.akshayAshokCode.androidsensors.presentation.views.LevelStatusLabel
import com.akshayAshokCode.androidsensors.presentation.views.TargetReticle
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import com.akshayAshokCode.androidsensors.utils.SensorUtils

class BubbleLevelTool : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator

    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Smoothing variables
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothingFactor = 0.1f

    private val mainHandler = Handler(Looper.getMainLooper())

    // Haptic feedback tracking
    private var wasLevel = false
    private var reviewFlaggedThisVisit = false

    // Sensitivity modes
    enum class SensitivityMode(val tolerance: Float, val displayNameResId: Int) {
        PRECISION(0.5f, R.string.sensitivity_precision),
        STANDARD(2.0f, R.string.sensitivity_standard),
        ROUGH(5.0f, R.string.sensitivity_rough)
    }

    private var currentMode by mutableStateOf(SensitivityMode.STANDARD)
    private var pitch by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)
    private var isLevel by mutableStateOf(false)
    private var isAvailable by mutableStateOf(true)

    // Add calibration offset
    private var calibrationOffsetPitch = 0f
    private var calibrationOffsetRoll = 0f
    private var showBottomSheet by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        vibrator = requireContext().getSystemService(Vibrator::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                BubbleLevelToolScreen(
                    pitch = pitch,
                    roll = roll,
                    isLevel = isLevel,
                    currentMode = currentMode,
                    isAvailable = isAvailable,
                    onModeChange = {
                        currentMode = it
                        AnalyticsManager.logSensitivityChanged(it.name)
                    },
                    showBottomSheet = showBottomSheet,
                    onBottomSheetChange = { showBottomSheet = it }
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

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            isAvailable = false
            AnalyticsManager.logSensorUnavailable(AnalyticsManager.Features.BUBBLE_LEVEL)
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
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, accelerometerData, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, magnetometerData, 0, 3)
            else -> return
        }
        // Compute orientation on sensor thread — avoids allocating two snapshot FloatArrays per event
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) return
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rawRoll  = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        mainHandler.post {
            if (!isAdded) return@post
            updateLevelTool(rawPitch, rawRoll)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun updateLevelTool(rawPitch: Float, rawRoll: Float) {
        smoothedPitch = SensorUtils.applySmoothingFilter(smoothedPitch, rawPitch, smoothingFactor)
        smoothedRoll  = SensorUtils.applySmoothingFilter(smoothedRoll, rawRoll, smoothingFactor)

        val calibratedAngles = SensorUtils.calculateCalibratedAngles(
            smoothedPitch, smoothedRoll, calibrationOffsetPitch, calibrationOffsetRoll
        )
        pitch = calibratedAngles.first
        roll  = calibratedAngles.second

        val newIsLevel = SensorUtils.isDeviceLevel(pitch, roll, currentMode.tolerance)
        if (newIsLevel && !wasLevel) {
            triggerHapticFeedback()
            if (!reviewFlaggedThisVisit) {
                reviewFlaggedThisVisit = true
                AnalyticsManager.logWinAchieved(AnalyticsManager.Features.BUBBLE_LEVEL)
                PreferencesManager.recordWin(requireContext())
            }
        }
        isLevel  = newIsLevel
        wasLevel = newIsLevel
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
    onModeChange: (BubbleLevelTool.SensitivityMode) -> Unit,
    showBottomSheet: Boolean,
    onBottomSheetChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LevelBackground(modifier = Modifier.fillMaxSize())

        if (!isAvailable) {
            Text(
                text = stringResource(R.string.sensor_not_available),
                fontSize = 16.sp,
                color = Color(0xFF00D4FF).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(32.dp)
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SensitivityToggle(currentMode = currentMode, onModeChange = onModeChange)

            LevelStatusLabel(isLevel = isLevel, tolerance = currentMode.tolerance)

            TargetReticle(
                pitch     = pitch,
                roll      = roll,
                isLevel   = isLevel,
                tolerance = currentMode.tolerance,
                modifier  = Modifier.size(300.dp)
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HUDAngleCard(label = "PITCH\nFWD ↔ BACK",  value = pitch, modifier = Modifier.weight(1f))
                HUDAngleCard(label = "ROLL\nLEFT ↔ RIGHT", value = roll,  modifier = Modifier.weight(1f))
            }

            Text(
                text          = "TAP  ↑  FOR SENSOR DETAILS",
                color         = Color(0xFF00D4FF).copy(alpha = 0.35f),
                fontSize      = 9.sp,
                fontFamily    = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier      = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) {
                    AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.BUBBLE_LEVEL)
                    onBottomSheetChange(true)
                }
            )
        }
    }

    // Reusable Bottom Sheet - IMPROVE STATE HANDLING
    if (showBottomSheet) {
        SensorDetailsBottomSheet(
            isVisible = showBottomSheet,
            onDismiss = { onBottomSheetChange(false) },
            title = stringResource(R.string.bubble_level_details_title),
            instruction = stringResource(R.string.bubble_level_instruction),
            content = stringResource(R.string.bubble_level_details)
        )
    }
}
