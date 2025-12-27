package com.akshayAshokCode.androidsensors.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages app preferences including first launch detection
 */
object PreferencesManager {
    private const val PREFS_NAME = "android_sensors_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

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
}