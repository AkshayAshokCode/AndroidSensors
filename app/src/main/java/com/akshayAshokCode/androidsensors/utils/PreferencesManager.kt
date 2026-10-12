package com.akshayAshokCode.androidsensors.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages app preferences including first launch detection
 */
object PreferencesManager {
    private const val PREFS_NAME               = "android_sensors_prefs"
    private const val KEY_FIRST_LAUNCH         = "is_first_launch"
    private const val KEY_FIRST_FEATURE_OPENED = "first_feature_opened"
    private const val KEY_PENDING_REVIEW       = "pending_review"
    private const val KEY_WIN_COUNT            = "win_count"
    private const val KEY_SESSION_COUNT        = "session_count"
    private const val KEY_ALERT_ARMED_AT       = "internet_alert_armed_at"
    private const val KEY_ALERT_RING           = "internet_alert_ring"

    private const val WINS_REQUIRED = 4

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if this is the first time the app is launched
     */
    fun isFirstLaunch(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark the first launch as complete
     */
    fun setFirstLaunchComplete(context: Context) {
        getPreferences(context).edit { putBoolean(KEY_FIRST_LAUNCH, false) }
    }

    /**
     * Check if user has opened their first feature
     */
    fun hasOpenedFirstFeature(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_FIRST_FEATURE_OPENED, false)
    }

    /**
     * Mark that user has opened their first feature
     */
    fun setFirstFeatureOpened(context: Context) {
        getPreferences(context).edit { putBoolean(KEY_FIRST_FEATURE_OPENED, true) }
    }

    /** Call once per feature session win. Triggers the review request after WINS_REQUIRED distinct wins. */
    fun recordWin(context: Context) {
        val prefs    = getPreferences(context)
        val newCount = prefs.getInt(KEY_WIN_COUNT, 0) + 1
        if (newCount >= WINS_REQUIRED) {
            prefs.edit {
                putBoolean(KEY_PENDING_REVIEW, true)
                putInt(KEY_WIN_COUNT, 0)
            }
        } else {
            prefs.edit { putInt(KEY_WIN_COUNT, newCount) }
        }
    }

    /** Returns true and clears the flag atomically; false if no review was pending. */
    fun consumePendingReview(context: Context): Boolean {
        val prefs = getPreferences(context)
        val pending = prefs.getBoolean(KEY_PENDING_REVIEW, false)
        if (pending) prefs.edit { putBoolean(KEY_PENDING_REVIEW, false) }
        return pending
    }

    /** When the internet alert was armed, so a restarted service can resume the same wait. 0 = not armed. */
    fun getAlertArmedAt(context: Context): Long =
        getPreferences(context).getLong(KEY_ALERT_ARMED_AT, 0L)

    fun setAlertArmedAt(context: Context, armedAt: Long) {
        getPreferences(context).edit { putLong(KEY_ALERT_ARMED_AT, armedAt) }
    }

    fun clearAlertArmedAt(context: Context) {
        getPreferences(context).edit { remove(KEY_ALERT_ARMED_AT) }
    }

    /** Wi-Fi Pulse alert style: true = ring (default), false = notification. */
    fun isAlertRing(context: Context): Boolean =
        getPreferences(context).getBoolean(KEY_ALERT_RING, true)

    fun setAlertRing(context: Context, ring: Boolean) {
        getPreferences(context).edit { putBoolean(KEY_ALERT_RING, ring) }
    }
}
