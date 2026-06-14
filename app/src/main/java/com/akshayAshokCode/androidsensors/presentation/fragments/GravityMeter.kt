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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.AxisMeters
import com.akshayAshokCode.androidsensors.presentation.views.GravityBackground
import com.akshayAshokCode.androidsensors.presentation.views.GravitySphere
import com.akshayAshokCode.androidsensors.presentation.views.OrientationBadge
import com.akshayAshokCode.androidsensors.presentation.views.OscilloscopeGraph
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import com.akshayAshokCode.androidsensors.utils.SensorUtils
import kotlin.math.sqrt

class GravityMeter : Fragment(), SensorEventListener {

    private val TAG = "GravityMeter"
    private lateinit var sensorManager: SensorManager

    private var rawGX by mutableStateOf(0f)
    private var rawGY by mutableStateOf(0f)
    private var rawGZ by mutableStateOf(0f)
    private var phoneOrientation by mutableStateOf(SensorUtils.PhoneOrientation.UNKNOWN)
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)
    private var reviewFlaggedThisVisit = false

    private val gravityHistory = mutableStateListOf<Triple<Float, Float, Float>>()
    private val maxHistorySize = 50

    private val mainHandler = Handler(Looper.getMainLooper())

    // Accelerometer fallback when TYPE_GRAVITY is unavailable
    @Volatile private var usingAccelerometerFallback = false
    private val lowPassGravity = FloatArray(3)
    private val lowPassAlpha = 0.8f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        phoneOrientation = SensorUtils.PhoneOrientation.UNKNOWN

        return ComposeView(requireContext()).apply {
            setContent {
                GravityMeterScreen(
                    rawGX            = rawGX,
                    rawGY            = rawGY,
                    rawGZ            = rawGZ,
                    phoneOrientation = phoneOrientation,
                    isAvailable      = isAvailable,
                    gravityHistory   = gravityHistory,
                    onBottomSheetToggleClick = {
                        showBottomSheet = true
                        AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.GRAVITY_METER)
                    }
                )

                SensorDetailsBottomSheet(
                    isVisible = showBottomSheet,
                    onDismiss = { showBottomSheet = false },
                    title = stringResource(R.string.gravity_meter_details_title),
                    instruction = stringResource(R.string.gravity_meter_instruction),
                    content = stringResource(R.string.gravity_meter_details)
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
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (gravitySensor != null) {
            usingAccelerometerFallback = false
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                usingAccelerometerFallback = true
                lowPassGravity.fill(0f)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                isAvailable = false
                AnalyticsManager.logSensorUnavailable(AnalyticsManager.Features.GRAVITY_METER)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorType = event.sensor?.type
        val isGravityEvent = sensorType == Sensor.TYPE_GRAVITY
        val isAccelFallback = sensorType == Sensor.TYPE_ACCELEROMETER && usingAccelerometerFallback
        if (!isGravityEvent && !isAccelFallback) return

        // Apply low-pass filter on sensor thread to extract gravity from accelerometer
        val gx: Float
        val gy: Float
        val gz: Float
        if (isAccelFallback) {
            lowPassGravity[0] = lowPassAlpha * lowPassGravity[0] + (1 - lowPassAlpha) * event.values[0]
            lowPassGravity[1] = lowPassAlpha * lowPassGravity[1] + (1 - lowPassAlpha) * event.values[1]
            lowPassGravity[2] = lowPassAlpha * lowPassGravity[2] + (1 - lowPassAlpha) * event.values[2]
            gx = lowPassGravity[0]; gy = lowPassGravity[1]; gz = lowPassGravity[2]
        } else {
            gx = event.values[0]; gy = event.values[1]; gz = event.values[2]
        }

        // Determine orientation on sensor thread (no Compose state read/write)
        val orientationResult = when {
            gx > 7 -> SensorUtils.PhoneOrientation.LANDSCAPE
            gx < -7 -> SensorUtils.PhoneOrientation.LANDSCAPE
            gy > 7 -> SensorUtils.PhoneOrientation.PORTRAIT
            gy < -7 -> SensorUtils.PhoneOrientation.UPSIDE_DOWN
            gz > 7 -> SensorUtils.PhoneOrientation.FACE_UP
            gz < -7 -> SensorUtils.PhoneOrientation.FACE_DOWN
            else -> null // no change
        }

        // Post all Compose state updates to main thread
        mainHandler.post {
            if (!isAdded) return@post
            rawGX = gx; rawGY = gy; rawGZ = gz

            gravityHistory.add(Triple(gx, gy, gz))
            if (gravityHistory.size > maxHistorySize) gravityHistory.removeAt(0)

            if (orientationResult != null) {
                phoneOrientation = orientationResult
                if (!reviewFlaggedThisVisit) {
                    reviewFlaggedThisVisit = true
                    AnalyticsManager.logWinAchieved(AnalyticsManager.Features.GRAVITY_METER)
                    PreferencesManager.recordWin(requireContext())
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}

@Composable
fun GravityMeterScreen(
    rawGX            : Float,
    rawGY            : Float,
    rawGZ            : Float,
    phoneOrientation : SensorUtils.PhoneOrientation,
    isAvailable      : Boolean,
    gravityHistory   : List<Triple<Float, Float, Float>>,
    onBottomSheetToggleClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GravityBackground(modifier = Modifier.fillMaxSize())

        if (!isAvailable) {
            Text(
                text      = stringResource(R.string.gravity_meter_not_available),
                color     = Color(0xFF00D4FF).copy(alpha = 0.7f),
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(32.dp)
            )
            return@Box
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OrientationBadge(phoneOrientation)
            val magnitude = sqrt(rawGX * rawGX + rawGY * rawGY + rawGZ * rawGZ)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GravitySphere(rawGX = rawGX, rawGY = rawGY, rawGZ = rawGZ,
                    modifier = Modifier.size(260.dp))
                Text(
                    text          = "${"%.2f".format(magnitude)} m/s²",
                    color         = Color.White,
                    fontSize      = 13.sp,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text          = "TOTAL G-FORCE",
                    color         = Color(0xFF00D4FF).copy(alpha = 0.5f),
                    fontSize      = 8.sp,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            AxisMeters(rawGX = rawGX, rawGY = rawGY, rawGZ = rawGZ)
            if (gravityHistory.size >= 3) {
                OscilloscopeGraph(history = gravityHistory,
                    modifier = Modifier.fillMaxWidth().height(80.dp))
            }
            Text(
                text     = "TAP  ↑  FOR SENSOR DETAILS",
                color    = Color(0xFF00D4FF).copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null, onClick = onBottomSheetToggleClick
                )
            )
        }
    }
}