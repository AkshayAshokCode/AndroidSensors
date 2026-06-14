package com.akshayAshokCode.androidsensors.presentation.fragments

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akshayAshokCode.androidsensors.Constants
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.data.SensorModel
import com.akshayAshokCode.androidsensors.data.SensorRepository
import com.akshayAshokCode.androidsensors.presentation.MainActivity
import com.akshayAshokCode.androidsensors.presentation.views.DashboardScreen
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager

class Dashboard : Fragment() {

    private lateinit var sensorManager: SensorManager
    private var rootView: View? = null

    // Sensor availability checked once on create (no live polling on home screen)
    private val availability = mutableStateMapOf<Int, Boolean>()

    private val mainHandler    = Handler(Looper.getMainLooper())
    private val pendingRunnables = mutableListOf<Runnable>()
    private var isNavigating = false
    private var isFirstView = true
    private var playEntryAnim = true

    private val sensors = SensorRepository().getSensors()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater : LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sensorManager = context
            ?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        // Single availability check — respects fallback logic from Tier-1 fixes
        sensors.forEach { sensor ->
            availability[sensor.id] = when (sensor.id) {
                Constants.GRAVITY_METER, Constants.SPACE_BALL ->
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null ||
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                else ->
                    sensorManager.getDefaultSensor(sensor.sensorType) != null
            }
        }

        playEntryAnim = isFirstView
        isFirstView = false

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                DashboardScreen(
                    sensors            = sensors,
                    availability       = availability,
                    playEntryAnimation = playEntryAnim,
                    onSensorClick      = { clickedSensor(it) },
                    onMenuClick        = { openDrawer() }
                )
            }
        }
        rootView = composeView
        return composeView
    }

    private fun openDrawer() {
        requireActivity()
            .findViewById<DrawerLayout>(R.id.drawer_layout)
            ?.openDrawer(GravityCompat.START)
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false

        // Haptic boot sequence — one CLOCK_TICK per card as they stagger in; only on app launch
        val view = rootView ?: return
        pendingRunnables.clear()
        if (playEntryAnim) {
            repeat(sensors.size) { i ->
                val r = Runnable {
                    if (isAdded) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                pendingRunnables += r
                mainHandler.postDelayed(r, 380L + i * 100L)
            }
        }

        // If a feature set the review flag, wait for the dashboard to settle then launch review
        if (PreferencesManager.consumePendingReview(requireContext())) {
            val r = Runnable {
                if (isAdded) (activity as? MainActivity)?.triggerInAppReview()
            }
            pendingRunnables += r
            mainHandler.postDelayed(r, 1200L)
        }
    }

    override fun onPause() {
        super.onPause()
        pendingRunnables.forEach { mainHandler.removeCallbacks(it) }
        pendingRunnables.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun clickedSensor(sensor: SensorModel) {
        if (isNavigating) return
        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.dashboard) return
        isNavigating = true

        val (featureName, actionId) = when (sensor.id) {
            Constants.METAL_DETECTOR ->
                AnalyticsManager.Features.METAL_DETECTOR to
                R.id.action_dashboard_to_metalDetector
            Constants.GRAVITY_METER ->
                AnalyticsManager.Features.GRAVITY_METER to
                R.id.action_dashboard_to_gravityMeter
            Constants.BUBBLE_LEVEL_TOOL ->
                AnalyticsManager.Features.BUBBLE_LEVEL to
                R.id.action_dashboard_to_bubbleLevelTool
            Constants.SPACE_BALL ->
                AnalyticsManager.Features.SPACE_BALL to
                R.id.action_dashboard_to_spaceBall
            else -> return
        }

        AnalyticsManager.logFeatureOpened(featureName)
        if (!PreferencesManager.hasOpenedFirstFeature(requireContext())) {
            AnalyticsManager.logFirstFeatureOpened(featureName)
            PreferencesManager.setFirstFeatureOpened(requireContext())
        }

        nav.navigate(actionId)
    }
}
