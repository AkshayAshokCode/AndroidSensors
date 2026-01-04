package com.akshayAshokCode.androidsensors.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized manager for Firebase Analytics events
 * Tracks user interactions and feature usage across the app
 */
object AnalyticsManager {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * Initialize Firebase Analytics
     * Call this in Application class or MainActivity onCreate
     */
    fun initialize(analytics: FirebaseAnalytics) {
        firebaseAnalytics = analytics
    }

    // Event Names
    object Events {
        const val FEATURE_OPENED = "feature_opened"
        const val FIRST_FEATURE_OPENED = "first_feature_opened"
        const val BOTTOM_SHEET_OPENED = "bottom_sheet_opened"
        const val ONBOARDING_SKIPPED = "onboarding_skipped"
        const val ONBOARDING_GET_STARTED = "onboarding_get_started"
        const val FEEDBACK_SENT = "feedback_sent"
        const val REVIEW_REQUESTED = "review_requested"
    }

    // Parameter Names
    object Params {
        const val FEATURE_NAME = "feature_name"
    }

    // Feature Names
    object Features {
        const val METAL_DETECTOR = "metal_detector"
        const val GRAVITY_METER = "gravity_meter"
        const val BUBBLE_LEVEL = "bubble_level"
    }

    /**
     * Log when a feature/screen is opened
     */
    fun logFeatureOpened(featureName: String) {
        val bundle = Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        }
        firebaseAnalytics.logEvent(Events.FEATURE_OPENED, bundle)
    }

    /**
     * Log the first feature opened by a new user
     */
    fun logFirstFeatureOpened(featureName: String) {
        val bundle = Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        }
        firebaseAnalytics.logEvent(Events.FIRST_FEATURE_OPENED, bundle)
    }

    /**
     * Log when bottom sheet is opened
     */
    fun logBottomSheetOpened(featureName: String) {
        val bundle = Bundle().apply {
            putString(Params.FEATURE_NAME, featureName)
        }
        firebaseAnalytics.logEvent(Events.BOTTOM_SHEET_OPENED, bundle)
    }

    /**
     * Log when user skips onboarding
     */
    fun logOnboardingSkipped() {
        firebaseAnalytics.logEvent(Events.ONBOARDING_SKIPPED, null)
    }

    /**
     * Log when user clicks Get Started on onboarding
     */
    fun logOnboardingGetStarted() {
        firebaseAnalytics.logEvent(Events.ONBOARDING_GET_STARTED, null)
    }

    /**
     * Log feedback sent
     */
    fun logFeedbackSent() {
        firebaseAnalytics.logEvent(Events.FEEDBACK_SENT, null)
    }

    /**
     * Log review request
     */
    fun logReviewRequested() {
        firebaseAnalytics.logEvent(Events.REVIEW_REQUESTED, null)
    }

    /**
     * Set user property for analytics segmentation
     */
    private fun setUserProperty(propertyName: String, propertyValue: String) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue)
    }
}