package com.akshayAshokCode.androidsensors.presentation.onboarding.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Spacer for Skip button area
                Spacer(modifier = Modifier.height(56.dp))

                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> WelcomeScreen()
                        1 -> FeaturesScreen()
                    }
                }

                // Bottom navigation bar
                OnboardingBottomBar(
                    currentPage = pagerState.currentPage,
                    pageCount = 2,
                    onBack = if (pagerState.currentPage > 0) {
                        {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    } else null,
                    onNext = {
                        if (pagerState.currentPage < 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            // User clicked "Get Started" on the last page
                            AnalyticsManager.logOnboardingGetStarted()
                            onFinish()
                        }
                    }
                )
            }
        }

        // Skip button overlaid on top with higher z-index
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(10f)
                .align(Alignment.TopEnd)
        ) {
            SkipButton(
                onClick = {
                    AnalyticsManager.logOnboardingSkipped()
                    onFinish()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}