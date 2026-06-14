package com.akshayAshokCode.androidsensors.presentation.onboarding.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.akshayAshokCode.androidsensors.presentation.onboarding.components.OnboardingBottomBar
import com.akshayAshokCode.androidsensors.presentation.onboarding.components.SkipButton
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import kotlinx.coroutines.launch

/**
 * Main onboarding screen with HorizontalPager
 * Now with 2 screens: Welcome + Features (combined with tips)
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val voidColor = Color(0xFF050510)
    val gridColor = Color(0xFF1A1A3E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(voidColor)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Dot grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sp = 28.dp.toPx(); val r = 1.1.dp.toPx()
            val cols = (size.width  / sp).toInt() + 2
            val rows = (size.height / sp).toInt() + 2
            repeat(rows) { row -> repeat(cols) { col ->
                drawCircle(gridColor, r, Offset(col * sp, row * sp))
            }}
        }

        // Main content column
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomeScreen()
                    1 -> FeaturesScreen()
                }
            }

            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount   = 2,
                onBack      = if (pagerState.currentPage > 0) {
                    { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                } else null,
                onNext      = {
                    if (pagerState.currentPage < 1) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        AnalyticsManager.logOnboardingGetStarted()
                        onFinish()
                    }
                }
            )
        }

        // Skip — subtle top-right overlay
        SkipButton(
            onClick  = { AnalyticsManager.logOnboardingSkipped(); onFinish() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(10f)
                .padding(20.dp)
        )
    }
}