package com.akshayAshokCode.androidsensors.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Quality label and lit bars. Derived from the phone's own signal level (the one behind the
 * status-bar icon), so Wi-Fi Pulse always agrees with the status bar on every brand.
 */
enum class SignalLevel(val bars: Int) {
    EXCELLENT(3), // top status-bar level
    GOOD(2),
    FAIR(1),
    WEAK(1)       // drawn dimmed
}

/**
 * Signal of the watched connection.
 * [dbm] can be null for mobile data on older phones; [detail] is the Wi-Fi band ("5 GHz")
 * or the mobile carrier name.
 */
data class SignalReading(val dbm: Int?, val level: SignalLevel, val detail: String?) {

    companion object {
        /** Null when there is nothing to show (not connected, or not reported by the phone). */
        fun read(context: Context, target: InternetTarget): SignalReading? = when (target) {
            InternetTarget.WIFI   -> readWifi(context.applicationContext)
            InternetTarget.MOBILE -> readMobile(context.applicationContext)
        }

        // ── Wi-Fi ─────────────────────────────────────────────────────────────

        /**
         * Needs only ACCESS_WIFI_STATE: Android protects the network name (SSID/BSSID) with
         * location permission, but not the signal strength or frequency.
         */
        private fun readWifi(context: Context): SignalReading? {
            val info = wifiInfo(context) ?: return null
            val rssi = info.rssi
            if (rssi <= -100 || rssi >= 0) return null // INVALID_RSSI (-127) and junk values
            val (level, max) = wifiSystemLevel(context, rssi)
            return SignalReading(rssi, levelOf(level, max), bandOf(info.frequency)?.let { "$it GHz" })
        }

        private fun wifiInfo(context: Context): WifiInfo? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cm = context.getSystemService(ConnectivityManager::class.java)
                @Suppress("DEPRECATION") // allNetworks: still the only synchronous way to list networks
                for (network in cm.allNetworks) {
                    val info = cm.getNetworkCapabilities(network)?.transportInfo as? WifiInfo
                    if (info != null) return info
                }
                // Not found there (some OEM builds) — fall back to the older API below
            }
            @Suppress("DEPRECATION") // deprecated in API 31 but still works, and is the only option below it
            return context.getSystemService(WifiManager::class.java)?.connectionInfo
        }

        /** The status-bar level for [rssi] and the highest possible level. */
        private fun wifiSystemLevel(context: Context, rssi: Int): Pair<Int, Int> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Uses the phone maker's own thresholds
                val wm = context.getSystemService(WifiManager::class.java)
                if (wm != null) return wm.calculateSignalLevel(rssi) to wm.maxSignalLevel
            }
            @Suppress("DEPRECATION") // the only option below API 30: Android's standard 5-level scale
            return WifiManager.calculateSignalLevel(rssi, 5) to 4
        }

        private fun bandOf(frequencyMhz: Int): String? = when (frequencyMhz) {
            in 2400..2500 -> "2.4"
            in 4900..5899 -> "5"
            in 5925..7125 -> "6"
            else          -> null
        }

        // ── Mobile data ───────────────────────────────────────────────────────

        /**
         * Signal strength and carrier name need no runtime permission. Network type (4G/5G) and
         * cell details are deliberately not read: they need the Phone or location permission.
         */
        private fun readMobile(context: Context): SignalReading? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null // no synchronous API below 28
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
            val strength = try {
                tm.signalStrength
            } catch (_: SecurityException) {
                null
            } ?: return null
            val level = strength.level // 0 (none) .. 4, same as the status bar
            val dbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                strength.cellSignalStrengths.firstOrNull()?.dbm?.takeIf { it in -140..-20 }
            } else {
                null
            }
            if (level == 0 && dbm == null) return null // no service
            return SignalReading(dbm, levelOf(level, 4), tm.networkOperatorName?.takeIf { it.isNotBlank() })
        }

        // ── Shared ────────────────────────────────────────────────────────────

        private fun levelOf(level: Int, max: Int): SignalLevel {
            if (max <= 0) return SignalLevel.WEAK
            val fraction = level.toFloat() / max
            return when {
                level >= max     -> SignalLevel.EXCELLENT // full bars in the status bar
                fraction >= 0.6f -> SignalLevel.GOOD      // e.g. 3 of 4 bars
                fraction >= 0.3f -> SignalLevel.FAIR      // e.g. 2 of 4 bars
                else             -> SignalLevel.WEAK
            }
        }
    }
}
