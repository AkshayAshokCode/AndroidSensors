package com.akshayAshokCode.androidsensors.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.graphics.toColorInt
import com.akshayAshokCode.androidsensors.presentation.MainActivity
import com.akshayAshokCode.androidsensors.presentation.onboarding.screens.OnboardingScreen
import com.akshayAshokCode.androidsensors.presentation.onboarding.theme.OnboardingTheme
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Analytics
        AnalyticsManager.initialize(Firebase.analytics)

        // Enable edge-to-edge display with custom system bar styling
        val statusBarColor = "#1E1E32".toColorInt()  // toolbar_background
        val navigationBarColor = "#2A2A3E".toColorInt()  // gravity_card_outer

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(statusBarColor),
            navigationBarStyle = SystemBarStyle.dark(navigationBarColor)
        )

        // Check if this is the first launch
        if (!PreferencesManager.isFirstLaunch(this)) {
            // Not first launch - go directly to MainActivity
            launchMainActivity()
            return
        }

        // First launch - show onboarding screens
        setContent {
            OnboardingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        onFinish = {
                            // Mark first launch as complete
                            PreferencesManager.setFirstLaunchComplete(this)
                            // Launch MainActivity
                            launchMainActivity()
                        }
                    )
                }
            }
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}