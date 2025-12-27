package com.akshayAshokCode.androidsensors.presentation.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.akshayAshokCode.androidsensors.presentation.MainActivity
import com.akshayAshokCode.androidsensors.presentation.onboarding.screens.OnboardingScreen
import com.akshayAshokCode.androidsensors.presentation.onboarding.theme.OnboardingTheme
import com.akshayAshokCode.androidsensors.utils.PreferencesManager

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set status bar and navigation bar colors to match app theme
        window.statusBarColor = Color.parseColor("#1E1E32")  // toolbar_background
        window.navigationBarColor = Color.parseColor("#2A2A3E")  // gravity_card_outer

        // Set status bar icons to light (white) for dark background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false  // false = light icons (white)
            isAppearanceLightNavigationBars = false  // false = light icons (white)
        }

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