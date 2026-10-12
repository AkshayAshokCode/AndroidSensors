package com.akshayAshokCode.androidsensors.network

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.MainActivity
import java.util.Date

/** Channels and notifications for the Wi-Fi Pulse "notify me when it is back" alert. */
internal object InternetAlertNotifications {

    const val ID_WAITING = 4101
    const val ID_RINGING = 4103
    private const val ID_RESULT = 4102

    private const val CHANNEL_WAITING = "wifi_pulse_waiting"
    private const val CHANNEL_ALERT   = "wifi_pulse_alert"
    private const val CHANNEL_RINGING = "wifi_pulse_ringing" // silent: the service plays the alarm sound

    private const val ACCENT = 0xFF00D4FF.toInt() // DashCyan

    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_WAITING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.wifi_pulse_channel_waiting))
                .setDescription(context.getString(R.string.wifi_pulse_channel_waiting_desc))
                .setShowBadge(false)
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ALERT, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.wifi_pulse_channel_alert))
                .setDescription(context.getString(R.string.wifi_pulse_channel_alert_desc))
                .setVibrationEnabled(true)
                .setVibrationPattern(longArrayOf(0, 400, 200, 400))
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_RINGING, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.wifi_pulse_channel_ringing))
                .setDescription(context.getString(R.string.wifi_pulse_channel_ringing_desc))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build()
        )
    }

    /** Ongoing notification shown while waiting — visible on the lock screen so a glance is enough. */
    fun waiting(
        context      : Context,
        armedAt      : Long,
        state        : InternetState,
        lineIndex    : Int,
        isLongOutage : Boolean
    ): Notification {
        val lines = context.resources.getStringArray(
            if (isLongOutage) R.array.wifi_pulse_long_outage_lines else R.array.wifi_pulse_waiting_lines
        )
        val line = lines[lineIndex % lines.size]
        val isWifi = state.target == InternetTarget.WIFI
        val statusText = context.getString(
            when {
                state.status == InternetStatus.NO_INTERNET && isWifi -> R.string.wifi_pulse_notif_status_no_internet
                state.status == InternetStatus.NO_INTERNET -> R.string.wifi_pulse_notif_status_no_internet_mobile
                isWifi -> R.string.wifi_pulse_notif_status_disconnected
                else -> R.string.wifi_pulse_notif_status_disconnected_mobile
            }
        )
        return NotificationCompat.Builder(context, CHANNEL_WAITING)
            .setSmallIcon(R.drawable.ic_wifi_pulse)
            .setColor(ACCENT)
            .setContentTitle(context.getString(R.string.wifi_pulse_notif_waiting_title, formatTime(context, armedAt)))
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n\n$line"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openWifiPulse(context))
            .addAction(0, context.getString(R.string.wifi_pulse_notif_cancel),
                InternetAlertService.cancelPendingIntent(context))
            .build()
    }

    /**
     * "Back after 42 minutes" — counted from when the user armed the alert, which is the wait
     * they experienced (the outage itself may have started earlier).
     */
    fun backAfter(context: Context, waitedMs: Long): String {
        val res = context.resources
        val totalMinutes = (waitedMs / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val duration = when {
            totalMinutes < 1 -> res.getString(R.string.wifi_pulse_less_than_a_minute)
            hours == 0 -> res.getQuantityString(R.plurals.wifi_pulse_minutes, minutes, minutes)
            minutes == 0 -> res.getQuantityString(R.plurals.wifi_pulse_hours, hours, hours)
            else -> res.getString(
                R.string.wifi_pulse_hours_and_minutes,
                res.getQuantityString(R.plurals.wifi_pulse_hours, hours, hours),
                res.getQuantityString(R.plurals.wifi_pulse_minutes, minutes, minutes)
            )
        }
        return res.getString(R.string.wifi_pulse_back_after, duration)
    }

    fun randomBackLine(context: Context): String =
        context.resources.getStringArray(R.array.wifi_pulse_back_lines).random()

    /** Foreground notification while ringing: heads-up, with a Stop button. Swiping it away also stops the ring. */
    fun ringing(context: Context, waited: String, line: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_wifi_pulse)
            .setColor(ACCENT)
            .setContentTitle(context.getString(R.string.wifi_pulse_notif_back_title))
            .setContentText(waited)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$waited\n\n$line"))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS) // not CATEGORY_ALARM: that would bypass Do Not Disturb
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openWifiPulse(context))
            .setDeleteIntent(InternetAlertService.stopRingPendingIntent(context))
            .addAction(0, context.getString(R.string.wifi_pulse_notif_stop),
                InternetAlertService.stopRingPendingIntent(context))
            .build()

    /** The "internet is back" notification. [silent] is used after a ring, which already made the noise. */
    fun showBack(context: Context, waited: String, line: String, silent: Boolean) {
        notify(
            context,
            NotificationCompat.Builder(context, if (silent) CHANNEL_WAITING else CHANNEL_ALERT)
                .setSmallIcon(R.drawable.ic_wifi_pulse)
                .setColor(ACCENT)
                .setContentTitle(context.getString(R.string.wifi_pulse_notif_back_title))
                .setContentText(waited)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$waited\n\n$line"))
                .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
                .setDefaults(if (silent) 0 else NotificationCompat.DEFAULT_ALL) // sound + vibration below API 26
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openWifiPulse(context))
                .build()
        )
    }

    fun showTimedOut(context: Context, hours: Int) {
        val duration = context.resources.getQuantityString(R.plurals.wifi_pulse_hours, hours, hours)
        notify(
            context,
            NotificationCompat.Builder(context, CHANNEL_WAITING)
                .setSmallIcon(R.drawable.ic_wifi_pulse)
                .setColor(ACCENT)
                .setContentTitle(context.getString(R.string.wifi_pulse_notif_timeout_title))
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.wifi_pulse_notif_timeout_text, duration)))
                .setContentText(context.getString(R.string.wifi_pulse_notif_timeout_text, duration))
                .setAutoCancel(true)
                .setContentIntent(openWifiPulse(context))
                .build()
        )
    }

    fun showStoppedBySystem(context: Context) {
        notify(
            context,
            NotificationCompat.Builder(context, CHANNEL_ALERT)
                .setSmallIcon(R.drawable.ic_wifi_pulse)
                .setColor(ACCENT)
                .setContentTitle(context.getString(R.string.wifi_pulse_notif_timeout_title))
                .setContentText(context.getString(R.string.wifi_pulse_notif_stopped_text))
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.wifi_pulse_notif_stopped_text)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openWifiPulse(context))
                .build()
        )
    }

    fun update(context: Context, notification: Notification) {
        notify(context, notification, ID_WAITING)
    }

    /** Opens the app on the Wi-Fi Pulse screen, with the dashboard behind it. */
    fun openWifiPulse(context: Context): PendingIntent = deepLink(context).createPendingIntent()

    /** Same destination as [openWifiPulse], as a plain Intent for APIs that need one. */
    fun openWifiPulseIntent(context: Context): Intent =
        deepLink(context).createTaskStackBuilder().intents.first()

    fun formatTime(context: Context, millis: Long): String =
        DateFormat.getTimeFormat(context).format(Date(millis))

    private fun deepLink(context: Context) = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.wifiPulse)
        .setComponentName(MainActivity::class.java)

    @SuppressLint("MissingPermission") // checked by areNotificationsEnabled()
    private fun notify(context: Context, notification: Notification, id: Int = ID_RESULT) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }
}
