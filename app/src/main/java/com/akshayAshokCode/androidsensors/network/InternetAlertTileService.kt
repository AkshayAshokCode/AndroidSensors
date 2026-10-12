package com.akshayAshokCode.androidsensors.network

import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationManagerCompat
import com.akshayAshokCode.androidsensors.R

/**
 * Quick Settings tile: arm or cancel the internet alert without opening the app.
 * When arming is not possible right now (already online, Wi-Fi off, airplane mode, notifications blocked)
 * it opens the Wi-Fi Pulse screen, which explains what to do.
 */
class InternetAlertTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (InternetAlertService.armedSince.value != null) {
            InternetAlertService.cancel(this)
            return
        }

        val state = InternetStatusMonitor.snapshot(this)
        val canArm = state.blocker == null &&
            state.status != InternetStatus.ONLINE &&
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!canArm) {
            openWifiPulse()
            return
        }

        try {
            InternetAlertService.arm(this, InternetAlertService.SOURCE_TILE)
        } catch (_: IllegalStateException) {
            // Android 12+ may refuse to start a foreground service from here — fall back to the app
            openWifiPulse()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isArmed = InternetAlertService.armedSince.value != null
        tile.state = if (isArmed) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.wifi_pulse_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (isArmed) R.string.wifi_pulse_tile_waiting else R.string.wifi_pulse_tile_off
            )
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // the Intent overload is the only one below API 34
    private fun openWifiPulse() {
        val open = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(InternetAlertNotifications.openWifiPulse(this))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(InternetAlertNotifications.openWifiPulseIntent(this))
            }
        }
        if (isLocked) unlockAndRun(open) else open.run()
    }
}
