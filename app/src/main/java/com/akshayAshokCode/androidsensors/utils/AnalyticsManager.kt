package com.akshayAshokCode.androidsensors.utils

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

object AnalyticsManager {

    // Self-initializing: FirebaseApp is ready at process start (FirebaseInitProvider),
    // so this is safe even when the process is restored directly into MainActivity.
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // ── Event names ───────────────────────────────────────────────────────────
    object Events {
        const val FEATURE_OPENED       = "feature_opened"
        const val FIRST_FEATURE_OPENED = "first_feature_opened"
        const val BOTTOM_SHEET_OPENED  = "bottom_sheet_opened"
        const val ONBOARDING_SKIPPED   = "onboarding_skipped"
        const val ONBOARDING_GET_STARTED = "onboarding_get_started"
        const val FEEDBACK_SENT        = "feedback_sent"
        const val REVIEW_REQUESTED     = "review_requested"
        const val WIN_ACHIEVED         = "win_achieved"
        const val SENSITIVITY_CHANGED  = "sensitivity_changed"
        const val RECALIBRATE_TAPPED   = "recalibrate_tapped"
        const val BALL_THROWN          = "ball_thrown"
        const val SENSOR_UNAVAILABLE   = "sensor_unavailable"
        const val INTERNET_ALERT_ARMED     = "internet_alert_armed"
        const val INTERNET_ALERT_FIRED     = "internet_alert_fired"
        const val INTERNET_ALERT_CANCELLED = "internet_alert_cancelled"
        const val INTERNET_ALERT_TIMED_OUT = "internet_alert_timed_out"
        const val INTERNET_ALERT_STOPPED_BY_SYSTEM = "internet_alert_stopped_by_system"
    }

    // ── Parameter names ───────────────────────────────────────────────────────
    object Params {
        const val FEATURE_NAME     = "feature_name"
        const val SENSITIVITY_MODE = "sensitivity_mode"
        const val SOURCE           = "source"
        const val WAIT_MINUTES     = "wait_minutes"
        const val ALERT_STYLE      = "alert_style"
        const val OUTCOME          = "outcome"
        const val TARGET           = "target"
    }

    // ── Feature name constants ────────────────────────────────────────────────
    object Features {
        const val METAL_DETECTOR = "metal_detector"
        const val GRAVITY_METER  = "gravity_meter"
        const val BUBBLE_LEVEL   = "bubble_level"
        const val SPACE_BALL     = "space_ball"
        const val WIFI_PULSE     = "wifi_pulse"
    }

    // ── Logging methods ───────────────────────────────────────────────────────

    fun logFeatureOpened(featureName: String) {
        firebaseAnalytics.logEvent(Events.FEATURE_OPENED, Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        })
    }

    fun logFirstFeatureOpened(featureName: String) {
        firebaseAnalytics.logEvent(Events.FIRST_FEATURE_OPENED, Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        })
    }

    fun logBottomSheetOpened(featureName: String) {
        firebaseAnalytics.logEvent(Events.BOTTOM_SHEET_OPENED, Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        })
    }

    fun logOnboardingSkipped() {
        firebaseAnalytics.logEvent(Events.ONBOARDING_SKIPPED, null)
    }

    fun logOnboardingGetStarted() {
        firebaseAnalytics.logEvent(Events.ONBOARDING_GET_STARTED, null)
    }

    fun logFeedbackSent() {
        firebaseAnalytics.logEvent(Events.FEEDBACK_SENT, null)
    }

    fun logReviewRequested() {
        firebaseAnalytics.logEvent(Events.REVIEW_REQUESTED, null)
    }

    /** Fired when a user achieves a win condition in a feature (max once per visit). */
    fun logWinAchieved(featureName: String) {
        firebaseAnalytics.logEvent(Events.WIN_ACHIEVED, Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        })
    }

    /** Fired when the user changes sensitivity mode in Bubble Level. */
    fun logSensitivityChanged(mode: String) {
        firebaseAnalytics.logEvent(Events.SENSITIVITY_CHANGED, Bundle().apply {
            putString(Params.SENSITIVITY_MODE, mode)
        })
    }

    /** Fired when the user manually taps Recalibrate in Metal Detector. */
    fun logRecalibrateTapped() {
        firebaseAnalytics.logEvent(Events.RECALIBRATE_TAPPED, null)
    }

    /** Fired when the user throws the ball in Space Ball (drag end with velocity). */
    fun logBallThrown() {
        firebaseAnalytics.logEvent(Events.BALL_THROWN, null)
    }

    /** Fired when a required sensor is unavailable on the device. */
    fun logSensorUnavailable(featureName: String) {
        firebaseAnalytics.logEvent(Events.SENSOR_UNAVAILABLE, Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        })
    }

    /** Fired when the user arms "notify me when it is back" — source "screen"/"tile", style "ring"/"notification", target "wifi"/"mobile". */
    fun logInternetAlertArmed(source: String, style: String, target: String) {
        firebaseAnalytics.logEvent(Events.INTERNET_ALERT_ARMED, Bundle().apply {
            putString(Params.SOURCE, source)
            putString(Params.ALERT_STYLE, style)
            putString(Params.TARGET, target)
        })
    }

    /** Fired when the internet came back and the alert was delivered — style is what actually played. */
    fun logInternetAlertFired(waitMinutes: Int, style: String, target: String) {
        firebaseAnalytics.logEvent(Events.INTERNET_ALERT_FIRED, Bundle().apply {
            putLong(Params.WAIT_MINUTES, waitMinutes.toLong())
            putString(Params.ALERT_STYLE, style)
            putString(Params.TARGET, target)
        })
    }

    fun logInternetAlertCancelled() {
        firebaseAnalytics.logEvent(Events.INTERNET_ALERT_CANCELLED, null)
    }

    fun logInternetAlertTimedOut() {
        firebaseAnalytics.logEvent(Events.INTERNET_ALERT_TIMED_OUT, null)
    }

    // Outcomes for logInternetAlertStoppedBySystem
    const val OUTCOME_RESTART_BLOCKED = "restart_blocked" // Android refused to restart the service
    const val OUTCOME_RESUMED         = "resumed"         // found killed on app open, turned back on
    const val OUTCOME_MISSED          = "missed"          // found killed on app open, internet already back

    /** Fired when the phone stopped a waiting alert in the background (battery savers, Android limits). */
    fun logInternetAlertStoppedBySystem(outcome: String) {
        firebaseAnalytics.logEvent(Events.INTERNET_ALERT_STOPPED_BY_SYSTEM, Bundle().apply {
            putString(Params.OUTCOME, outcome)
        })
    }
}
