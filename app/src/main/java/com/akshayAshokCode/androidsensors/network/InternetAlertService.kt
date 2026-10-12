package com.akshayAshokCode.androidsensors.network

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.TileService
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that runs only while the user has armed "notify me when it is back".
 * It waits for a Wi-Fi network with validated internet, alerts once, and stops itself.
 * It also gives up after [TIMEOUT_HOURS].
 *
 * Alert styles (chosen on the Wi-Fi Pulse screen, stored in PreferencesManager):
 * - Ring: the service keeps running and plays the alarm sound, getting louder, until the user
 *   taps Stop, opens the app, or [RING_SECONDS] pass. Skipped under Do Not Disturb.
 * - Notification: one high-priority notification with the normal notification sound.
 */
class InternetAlertService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var monitor: InternetStatusMonitor

    private var armedAt   = 0L
    private var state     = InternetState(InternetTarget.WIFI, InternetStatus.DISCONNECTED)
    private var lineIndex = 0
    private var isWatching = false

    // Ringing state
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringLine = ""
    private var ringWaited = ""
    private var ringVolume = RING_START_VOLUME

    private val rotateLine = object : Runnable {
        override fun run() {
            lineIndex++
            InternetAlertNotifications.update(this@InternetAlertService, waitingNotification())
            handler.postDelayed(this, LINE_ROTATE_MS)
        }
    }

    private val timeout = Runnable {
        AnalyticsManager.logInternetAlertTimedOut()
        InternetAlertNotifications.showTimedOut(this, TIMEOUT_HOURS)
        disarm()
    }

    // Starts quiet and gets louder, like an alarm clock
    private val rampVolume = object : Runnable {
        override fun run() {
            ringVolume = (ringVolume + RING_VOLUME_STEP).coerceAtMost(1f)
            player?.setVolume(ringVolume, ringVolume)
            if (ringVolume < 1f) handler.postDelayed(this, 1000L)
        }
    }

    // Nobody stopped the ring: the user may have missed it, so leave a record behind
    private val ringTimeout = Runnable { finishRinging(userSawIt = false) }

    override fun onCreate() {
        super.onCreate()
        InternetAlertNotifications.createChannels(this)
        monitor = InternetStatusMonitor(this) { onStateChanged(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RING, ACTION_CANCEL -> {
                if (_isRinging.value) {
                    // Stop tapped, notification tapped or swiped, or app opened: the user has seen it
                    finishRinging(userSawIt = true)
                } else {
                    if (intent.action == ACTION_CANCEL && armedAt != 0L) {
                        AnalyticsManager.logInternetAlertCancelled()
                    }
                    disarm()
                }
                return START_NOT_STICKY
            }
            ACTION_ARM -> if (armedAt == 0L && !_isRinging.value) {
                armedAt = System.currentTimeMillis()
                PreferencesManager.setAlertArmedAt(this, armedAt)
                AnalyticsManager.logInternetAlertArmed(
                    source = intent.getStringExtra(EXTRA_SOURCE) ?: SOURCE_SCREEN,
                    style  = alertStyle(),
                    target = targetName(InternetStatusMonitor.targetOf(this))
                )
            }
            else -> {
                // ACTION_RESUME from the app, or a restart by the system (null intent)
                // after the process was killed — resume the same wait
                armedAt = PreferencesManager.getAlertArmedAt(this)
                if (armedAt == 0L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        if (_isRinging.value) {
            // Armed again mid-ring: keep ringing, but still honour startForegroundService's contract
            startInForeground(InternetAlertNotifications.ID_RINGING, InternetAlertNotifications.ringing(this, ringWaited, ringLine))
            return START_NOT_STICKY
        }
        if (!startInForeground(InternetAlertNotifications.ID_WAITING, waitingNotification())) {
            return giveUpStoppedBySystem()
        }
        _armedSince.value = armedAt
        requestTileUpdate()
        startWatching()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (isWatching) monitor.stop()
        isWatching = false
        releaseRinging()
        _armedSince.value = null
        requestTileUpdate()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Returns false when Android refuses to put the service in the foreground. This can happen
     * on Android 12+ when the system restarts the service in the background after killing the app.
     */
    private fun startInForeground(id: Int, notification: android.app.Notification): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
        true
    } catch (_: IllegalStateException) { // includes ForegroundServiceStartNotAllowedException
        false
    } catch (_: SecurityException) {
        false
    }

    /** The wait cannot continue: tell the user instead of failing silently. */
    private fun giveUpStoppedBySystem(): Int {
        AnalyticsManager.logInternetAlertStoppedBySystem(AnalyticsManager.OUTCOME_RESTART_BLOCKED)
        InternetAlertNotifications.showStoppedBySystem(this)
        handler.removeCallbacksAndMessages(null)
        if (isWatching) monitor.stop()
        isWatching = false
        armedAt = 0L
        PreferencesManager.clearAlertArmedAt(this)
        _armedSince.value = null
        requestTileUpdate()
        stopSelf()
        return START_NOT_STICKY
    }

    private fun startWatching() {
        if (isWatching) return
        isWatching = true
        monitor.start()
        handler.postDelayed(rotateLine, LINE_ROTATE_MS)
        handler.postDelayed(
            timeout,
            (armedAt + TIMEOUT_MS - System.currentTimeMillis()).coerceAtLeast(0L)
        )
    }

    /**
     * The watched connection follows the user's Wi-Fi switch even during a wait: switching Wi-Fi
     * off mid-wait means they now want mobile data, so mobile data coming back counts.
     */
    private fun onStateChanged(newState: InternetState) {
        state = newState
        if (armedAt == 0L) return
        if (newState.status != InternetStatus.ONLINE) {
            InternetAlertNotifications.update(this, waitingNotification())
            return
        }

        // Validation already means Android reached the internet, so alert right away.
        // A delay here would be stretched by Doze and make the alert late.
        val waitedMs = System.currentTimeMillis() - armedAt
        val waitedMinutes = (waitedMs / 60_000L).toInt()
        val waited = InternetAlertNotifications.backAfter(this, waitedMs)
        val line = InternetAlertNotifications.randomBackLine(this)
        val ring = PreferencesManager.isAlertRing(this) && canRing()
        AnalyticsManager.logInternetAlertFired(
            waitMinutes = waitedMinutes,
            style       = if (ring) STYLE_RING else STYLE_NOTIFICATION,
            target      = targetName(newState.target)
        )
        PreferencesManager.recordWin(this)

        if (ring && startRinging(waited, line)) return
        InternetAlertNotifications.showBack(this, waited, line, silent = false)
        disarm()
    }

    private fun waitingNotification() = InternetAlertNotifications.waiting(
        context      = this,
        armedAt      = armedAt,
        state        = state,
        lineIndex    = lineIndex,
        isLongOutage = System.currentTimeMillis() - armedAt >= LONG_OUTAGE_MS
    )

    private fun disarm() {
        handler.removeCallbacksAndMessages(null)
        if (isWatching) monitor.stop()
        isWatching = false
        armedAt = 0L
        PreferencesManager.clearAlertArmedAt(this)
        _armedSince.value = null
        requestTileUpdate()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // Can be left behind when a ring failed to start after switching notifications
        NotificationManagerCompat.from(this).cancel(InternetAlertNotifications.ID_WAITING)
        stopSelf()
    }

    // ── Ringing ───────────────────────────────────────────────────────────────

    /** Do Not Disturb is respected: when it is on, fall back to a normal notification. */
    private fun canRing(): Boolean =
        getSystemService(NotificationManager::class.java).currentInterruptionFilter ==
            NotificationManager.INTERRUPTION_FILTER_ALL

    /** Returns false when no alarm sound could be played, so the caller can fall back. */
    private fun startRinging(waited: String, line: String): Boolean {
        // Stop waiting; from here on this service only rings
        handler.removeCallbacksAndMessages(null)
        if (isWatching) monitor.stop()
        isWatching = false
        armedAt = 0L
        PreferencesManager.clearAlertArmedAt(this)

        // A new notification id so it pops up as heads-up instead of silently updating
        if (!startInForeground(InternetAlertNotifications.ID_RINGING, InternetAlertNotifications.ringing(this, waited, line))) {
            return false
        }
        if (!startSound()) return false

        ringLine = line
        ringWaited = waited
        _isRinging.value = true
        _armedSince.value = null
        requestTileUpdate()
        NotificationManagerCompat.from(this).cancel(InternetAlertNotifications.ID_WAITING)

        startVibration()
        handler.postDelayed(rampVolume, 1000L)
        handler.postDelayed(ringTimeout, RING_SECONDS * 1000L)
        return true
    }

    private fun startSound(): Boolean {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return false
        ringVolume = RING_START_VOLUME
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@InternetAlertService, uri)
                isLooping = true
                // Keeps the CPU awake so the volume ramp and the 1-minute stop run on time
                setWakeMode(this@InternetAlertService, PowerManager.PARTIAL_WAKE_LOCK)
                setVolume(ringVolume, ringVolume)
                prepare()
                start()
            }
        } catch (_: Exception) {
            null
        }
        return player != null
    }

    private fun startVibration() {
        val v = getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() } ?: return
        vibrator = v
        val pattern = longArrayOf(0, 600, 400, 600, 1200)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> v.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> @Suppress("DEPRECATION") v.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
            else -> @Suppress("DEPRECATION") v.vibrate(pattern, 0)
        }
    }

    private fun finishRinging(userSawIt: Boolean) {
        handler.removeCallbacksAndMessages(null)
        releaseRinging()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (!userSawIt) {
            // Leave a quiet record so the user still sees it if they missed the ring
            InternetAlertNotifications.showBack(this, ringWaited, ringLine, silent = true)
        }
        stopSelf()
    }

    private fun releaseRinging() {
        player?.run {
            try { stop() } catch (_: IllegalStateException) { }
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        _isRinging.value = false
    }

    private fun targetName(target: InternetTarget) =
        if (target == InternetTarget.WIFI) TARGET_WIFI else TARGET_MOBILE

    private fun alertStyle() =
        if (PreferencesManager.isAlertRing(this)) STYLE_RING else STYLE_NOTIFICATION

    private fun requestTileUpdate() {
        try {
            TileService.requestListeningState(
                this, ComponentName(this, InternetAlertTileService::class.java)
            )
        } catch (_: Exception) {
            // Tile not added or system UI unavailable — nothing to refresh
        }
    }

    companion object {
        const val SOURCE_SCREEN = "screen"
        const val SOURCE_TILE   = "tile"
        const val TIMEOUT_HOURS = 12
        const val RING_SECONDS  = 60

        private const val STYLE_RING         = "ring"
        private const val STYLE_NOTIFICATION = "notification"
        private const val TARGET_WIFI        = "wifi"
        private const val TARGET_MOBILE      = "mobile"

        private const val TIMEOUT_MS      = TIMEOUT_HOURS * 60 * 60 * 1000L
        private const val LONG_OUTAGE_MS  = 60 * 60 * 1000L
        private const val LINE_ROTATE_MS  = 5 * 60 * 1000L

        private const val RING_START_VOLUME = 0.2f
        private const val RING_VOLUME_STEP  = 0.08f // full volume after ~10 s

        private const val ACTION_ARM       = "com.akshayAshokCode.androidsensors.action.ARM_INTERNET_ALERT"
        private const val ACTION_CANCEL    = "com.akshayAshokCode.androidsensors.action.CANCEL_INTERNET_ALERT"
        private const val ACTION_STOP_RING = "com.akshayAshokCode.androidsensors.action.STOP_INTERNET_ALERT_RING"
        private const val ACTION_RESUME    = "com.akshayAshokCode.androidsensors.action.RESUME_INTERNET_ALERT"
        private const val EXTRA_SOURCE     = "source"

        private val _armedSince = MutableStateFlow<Long?>(null)
        private val _isRinging  = MutableStateFlow(false)

        /** When the alert was armed (wall-clock millis), or null when it is not armed. */
        val armedSince: StateFlow<Long?> = _armedSince.asStateFlow()

        /** True while the "internet is back" ring is playing. */
        val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

        fun arm(context: Context, source: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, InternetAlertService::class.java)
                    .setAction(ACTION_ARM)
                    .putExtra(EXTRA_SOURCE, source)
            )
        }

        /** Restarts a wait that the phone killed, keeping the original start time. */
        fun resume(context: Context) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_RESUME))
        }

        fun cancel(context: Context) {
            context.startService(intent(context, ACTION_CANCEL))
        }

        /** Called when the user opens the app — they are back, so stop the ring. */
        fun stopRinging(context: Context) {
            if (_isRinging.value) context.startService(intent(context, ACTION_STOP_RING))
        }

        internal fun cancelPendingIntent(context: Context): PendingIntent =
            pendingIntent(context, ACTION_CANCEL, requestCode = 0)

        internal fun stopRingPendingIntent(context: Context): PendingIntent =
            pendingIntent(context, ACTION_STOP_RING, requestCode = 1)

        private fun pendingIntent(context: Context, action: String, requestCode: Int) =
            PendingIntent.getService(
                context, requestCode, intent(context, action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun intent(context: Context, action: String) =
            Intent(context, InternetAlertService::class.java).setAction(action)
    }
}
