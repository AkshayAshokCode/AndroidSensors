package com.akshayAshokCode.androidsensors.presentation.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.network.InternetAlertService
import com.akshayAshokCode.androidsensors.network.InternetBlocker
import com.akshayAshokCode.androidsensors.network.InternetState
import com.akshayAshokCode.androidsensors.network.InternetStatus
import com.akshayAshokCode.androidsensors.network.InternetTarget
import com.akshayAshokCode.androidsensors.network.InternetStatusMonitor
import com.akshayAshokCode.androidsensors.network.SignalReading
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.presentation.views.StoppedBySystem
import com.akshayAshokCode.androidsensors.presentation.views.WifiPulseScreen
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager

class WifiPulse : Fragment() {

    private lateinit var monitor: InternetStatusMonitor

    private var state by mutableStateOf(InternetState(InternetTarget.WIFI, InternetStatus.DISCONNECTED))
    private var notificationsBlocked by mutableStateOf(false)
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)
    private var ringMode by mutableStateOf(true)
    private var stoppedBySystem by mutableStateOf<StoppedBySystem?>(null)
    private var signal by mutableStateOf<SignalReading?>(null)

    // Signal strength is polled while the screen is visible only — no background cost
    private val signalHandler = Handler(Looper.getMainLooper())
    private val pollSignal = object : Runnable {
        override fun run() {
            readSignal(requireContext())
            signalHandler.postDelayed(this, SIGNAL_POLL_MS)
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) armAlert() else notificationsBlocked = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        monitor = InternetStatusMonitor(requireContext()) { newState ->
            state = newState
            readSignal(requireContext())
        }
        // Real values before the first frame — the monitor's first update arrives a frame later,
        // which would otherwise flash the default DISCONNECTED (red) screen
        readCurrentState(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                val armedSince by InternetAlertService.armedSince.collectAsState()

                WifiPulseScreen(
                    state                = state,
                    signal               = signal,
                    armedSince           = armedSince,
                    notificationsBlocked = notificationsBlocked,
                    isAvailable          = isAvailable,
                    ringMode             = ringMode,
                    onRingModeChange     = { ring ->
                        ringMode = ring
                        PreferencesManager.setAlertRing(requireContext(), ring)
                    },
                    stoppedBySystem      = stoppedBySystem,
                    onOpenAppSettings    = { openAppSettings() },
                    onArm                = { onArmClicked() },
                    onCancel             = { InternetAlertService.cancel(requireContext()) },
                    onFixBlocker         = { blocker -> openBlockerSettings(blocker) },
                    onOpenNotificationSettings = { openNotificationSettings() },
                    onBottomSheetToggleClick = {
                        showBottomSheet = true
                        AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.WIFI_PULSE)
                    }
                )

                SensorDetailsBottomSheet(
                    isVisible = showBottomSheet,
                    onDismiss = { showBottomSheet = false },
                    title = stringResource(R.string.wifi_pulse_details_title),
                    instruction = stringResource(R.string.wifi_pulse_instruction),
                    content = stringResource(R.string.wifi_pulse_details)
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
        val context = requireContext()

        if (!InternetStatusMonitor.isSupported(context)) {
            isAvailable = false
            AnalyticsManager.logSensorUnavailable(AnalyticsManager.Features.WIFI_PULSE)
            return
        }

        readCurrentState(context) // may have changed while the screen was in the background
        checkStoppedBySystem(context)
        // Returning from Settings after allowing notifications
        if (notificationsBlocked && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationsBlocked = false
        }

        monitor.start()
        signalHandler.postDelayed(pollSignal, SIGNAL_POLL_MS)
    }

    override fun onPause() {
        super.onPause()
        stoppedBySystem = null // explained once per visit
        signalHandler.removeCallbacks(pollSignal)
        if (!isAvailable) return
        monitor.stop()
    }

    private fun readCurrentState(context: Context) {
        state = InternetStatusMonitor.snapshot(context)
        ringMode = PreferencesManager.isAlertRing(context)
        readSignal(context)
    }

    private fun readSignal(context: Context) {
        signal = if (state.blocker == null && state.status != InternetStatus.DISCONNECTED) {
            SignalReading.read(context, state.target)
        } else {
            null
        }
    }

    /**
     * The alert is armed in preferences but its service is not running: the phone killed it in the
     * background (usually a phone maker's battery saver) and Android did not restart it.
     */
    private fun checkStoppedBySystem(context: Context) {
        if (PreferencesManager.getAlertArmedAt(context) == 0L ||
            InternetAlertService.armedSince.value != null ||
            InternetAlertService.isRinging.value
        ) return

        if (InternetStatusMonitor.snapshot(context).status == InternetStatus.ONLINE) {
            PreferencesManager.clearAlertArmedAt(context)
            stoppedBySystem = StoppedBySystem.MISSED
            AnalyticsManager.logInternetAlertStoppedBySystem(AnalyticsManager.OUTCOME_MISSED)
        } else {
            InternetAlertService.resume(context)
            stoppedBySystem = StoppedBySystem.RESUMED
            AnalyticsManager.logInternetAlertStoppedBySystem(AnalyticsManager.OUTCOME_RESUMED)
        }
    }

    private fun onArmClicked() {
        val context = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationsBlocked = true
            return
        }
        armAlert()
    }

    private fun armAlert() {
        notificationsBlocked = false
        InternetAlertService.arm(requireContext(), InternetAlertService.SOURCE_SCREEN)
    }

    private fun openBlockerSettings(blocker: InternetBlocker) {
        val intent = when (blocker) {
            InternetBlocker.WIFI_OFF ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
                else Intent(Settings.ACTION_WIFI_SETTINGS)
            InternetBlocker.AIRPLANE_MODE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
        startActivitySafely(intent)
    }

    private fun openNotificationSettings() {
        val packageName = requireContext().packageName
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
        }
        startActivitySafely(intent)
    }

    /** App info is where phone makers put per-app battery and background settings. */
    private fun openAppSettings() {
        startActivitySafely(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri())
        )
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds strip these settings screens — nothing better to offer
        }
    }

    private companion object {
        const val SIGNAL_POLL_MS = 1_000L
    }
}
