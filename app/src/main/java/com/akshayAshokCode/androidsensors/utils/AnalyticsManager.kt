package com.akshayAshokCode.androidsensors.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsManager {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(analytics: FirebaseAnalytics) {
        firebaseAnalytics = analytics
    }

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
    }

    // ── Parameter names ───────────────────────────────────────────────────────
    object Params {
        const val FEATURE_NAME     = "feature_name"
        const val SENSITIVITY_MODE = "sensitivity_mode"
    }

    // ── Feature name constants ────────────────────────────────────────────────
    object Features {
        const val METAL_DETECTOR = "metal_detector"
        const val GRAVITY_METER  = "gravity_meter"
        const val BUBBLE_LEVEL   = "bubble_level"
        const val SPACE_BALL     = "space_ball"
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
}
