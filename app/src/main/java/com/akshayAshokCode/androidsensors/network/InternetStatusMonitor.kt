package com.akshayAshokCode.androidsensors.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

enum class InternetStatus {
    ONLINE,        // connected and Android has confirmed the internet works
    NO_INTERNET,   // connected but the internet does not work (router up, provider down)
    DISCONNECTED   // not connected
}

/**
 * Which connection Wi-Fi Pulse watches — chosen automatically, never asked:
 * Wi-Fi switched on means the user expects Wi-Fi (even while it is not connected, e.g. the router
 * has no power); Wi-Fi switched off means the user is on mobile data.
 */
enum class InternetTarget { WIFI, MOBILE }

/** Something the user must change before the watched connection can ever come back. */
enum class InternetBlocker {
    WIFI_OFF,      // Wi-Fi off on a device without mobile data
    AIRPLANE_MODE  // watching mobile data, but airplane mode is on
}

data class InternetState(
    val target  : InternetTarget,
    val status  : InternetStatus,
    val blocker : InternetBlocker? = null
)

/**
 * Watches Wi-Fi and mobile networks and reports the state of the watched connection.
 *
 * "Working" means Android has validated the network (NET_CAPABILITY_VALIDATED), so a router
 * that comes back before the internet provider does reports NO_INTERNET, not ONLINE.
 * [onChange] is always called on the main thread, and only when the state changes.
 */
class InternetStatusMonitor(
    context: Context,
    private val onChange: (InternetState) -> Unit
) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Network → (connection type, validated). Written on the connectivity thread, read on main.
    private val networks = ConcurrentHashMap<Network, TrackedNetwork>()

    private var lastState: InternetState? = null
    private var isRunning = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // API 24–25 does not always follow up with onCapabilitiesChanged, so read them here
            track(network, connectivityManager.getNetworkCapabilities(network))
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            track(network, capabilities)
        }

        override fun onLost(network: Network) {
            networks.remove(network)
            publish()
        }
    }

    // Wi-Fi or airplane mode switched — changes which connection is watched
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish()
    }

    /**
     * While connected without internet, Android re-checks the internet with growing gaps
     * (up to several minutes), so the alert could be minutes late after the provider comes back.
     * Every 30 s we ask Android to re-check now.
     */
    private val recheck = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val target = targetOf(appContext)
            networks.filterValues { it.target == target && !it.validated }.keys.forEach { network ->
                try {
                    // "true" on an unvalidated network is what triggers a re-check: Android only
                    // re-evaluates when an app's report disagrees with its current result.
                    connectivityManager.reportNetworkConnectivity(network, true)
                } catch (_: SecurityException) {
                    // Some OEM builds restrict this — the system's own re-checks still apply
                }
            }
            mainHandler.postDelayed(this, RECHECK_INTERVAL_MS)
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        lastState = null
        networks.clear()
        networks.putAll(currentNetworks(connectivityManager))
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        ContextCompat.registerReceiver(
            appContext,
            settingsReceiver,
            IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        publish()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        mainHandler.removeCallbacks(recheck)
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        try {
            appContext.unregisterReceiver(settingsReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        networks.clear()
    }

    private fun track(network: Network, capabilities: NetworkCapabilities?) {
        val tracked = capabilities?.let { trackedNetworkOf(it) }
        if (tracked == null) networks.remove(network) else networks[network] = tracked
        publish()
    }

    private fun publish() {
        val state = stateOf(appContext, networks.values)
        mainHandler.post {
            if (!isRunning || state == lastState) return@post
            lastState = state
            mainHandler.removeCallbacks(recheck)
            if (state.status == InternetStatus.NO_INTERNET) {
                mainHandler.postDelayed(recheck, RECHECK_INTERVAL_MS)
            }
            onChange(state)
        }
    }

    private data class TrackedNetwork(val target: InternetTarget, val validated: Boolean)

    companion object {
        private const val RECHECK_INTERVAL_MS = 30_000L

        /** One-off synchronous check, for callers that cannot wait for a callback (e.g. the tile). */
        fun snapshot(context: Context): InternetState {
            val app = context.applicationContext
            val cm = app.getSystemService(ConnectivityManager::class.java)
            return stateOf(app, currentNetworks(cm).values)
        }

        fun targetOf(context: Context): InternetTarget =
            if (isWifiEnabled(context) || !hasMobileData(context)) InternetTarget.WIFI
            else InternetTarget.MOBILE

        fun isWifiEnabled(context: Context): Boolean =
            context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled == true

        /** True when the device can use Wi-Fi or mobile data at all. */
        fun isSupported(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI) || hasMobileData(context)

        private fun hasMobileData(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        private fun isAirplaneModeOn(context: Context): Boolean =
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

        private fun stateOf(context: Context, tracked: Collection<TrackedNetwork>): InternetState {
            val target = targetOf(context)
            val validatedFlags = tracked.filter { it.target == target }.map { it.validated }
            val status = when {
                validatedFlags.any { it } -> InternetStatus.ONLINE
                validatedFlags.isNotEmpty() -> InternetStatus.NO_INTERNET
                else -> InternetStatus.DISCONNECTED
            }
            val blocker = when {
                target == InternetTarget.WIFI && !isWifiEnabled(context) -> InternetBlocker.WIFI_OFF
                target == InternetTarget.MOBILE && isAirplaneModeOn(context) -> InternetBlocker.AIRPLANE_MODE
                else -> null
            }
            return InternetState(target, status, blocker)
        }

        private fun trackedNetworkOf(caps: NetworkCapabilities): TrackedNetwork? {
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null
            val target = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> InternetTarget.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> InternetTarget.MOBILE
                else -> return null // Ethernet, Bluetooth tethering, VPN-only: not watched
            }
            return TrackedNetwork(target, caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }

        @Suppress("DEPRECATION") // allNetworks is still the only synchronous way to list networks
        private fun currentNetworks(cm: ConnectivityManager): Map<Network, TrackedNetwork> =
            cm.allNetworks.mapNotNull { network ->
                val tracked = cm.getNetworkCapabilities(network)?.let { trackedNetworkOf(it) }
                tracked?.let { network to it }
            }.toMap()
    }
}
