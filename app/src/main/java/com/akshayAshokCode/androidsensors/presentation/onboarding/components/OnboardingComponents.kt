package com.akshayAshokCode.androidsensors.presentation.onboarding.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R

private val OCyan    = Color(0xFF00D4FF)
private val OGrid    = Color(0xFF1A1A3E)
private val OSurface = Color(0xFF0D0D2B)
private val OVoid    = Color(0xFF050510)

// ─────────────────────────────────────────────────────────────────────────────
// PageIndicator — cyan line segments (long = active, short = inactive)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PageIndicator(
    pageCount  : Int,
    currentPage: Int,
    modifier   : Modifier = Modifier
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width by animateDpAsState(
                targetValue   = if (isActive) 28.dp else 8.dp,
                animationSpec = tween(250),
                label         = "pw$index"
            )
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(width)
                    .background(
                        if (isActive) OCyan else OGrid,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SkipButton — intentionally subtle so it doesn't compete with GET STARTED
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SkipButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text          = stringResource(R.string.onboarding_skip).uppercase(),
        color         = Color.White.copy(alpha = 0.28f),
        fontSize      = 10.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier      = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication        = null,
            onClick           = onClick
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingBottomBar — page dots + back (subtle) + primary CTA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    pageCount  : Int,
    onBack     : (() -> Unit)?,
    onNext     : (() -> Unit)?,
    modifier   : Modifier = Modifier
) {
    val isLastPage = currentPage == pageCount - 1

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageIndicator(pageCount = pageCount, currentPage = currentPage)

        if (isLastPage) {
            // GET STARTED — full-width primary action
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(OCyan, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onNext ?: {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = stringResource(R.string.onboarding_get_started_button).uppercase(),
                    color         = OVoid,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
        } else {
            // Next page — outlined secondary button
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Box(
                        modifier         = Modifier
                            .border(0.5.dp, OGrid, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = onBack
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.onboarding_back).uppercase(),
                            color         = Color.White.copy(alpha = 0.45f),
                            fontSize      = 10.sp,
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(0.dp))
                }

                Box(
                    modifier         = Modifier
                        .background(OSurface, RoundedCornerShape(10.dp))
                        .border(0.5.dp, OCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onNext ?: {}
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.onboarding_next).uppercase() + " →",
                        color         = OCyan,
                        fontSize      = 10.sp,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnimatedIcon — kept for compatibility, not used in new screens
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedIcon(
    icon    : @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) { icon() }
}
