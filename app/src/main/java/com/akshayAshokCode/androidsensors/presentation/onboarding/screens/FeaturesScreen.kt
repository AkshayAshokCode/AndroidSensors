package com.akshayAshokCode.androidsensors.presentation.onboarding.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val FCyan    = Color(0xFF00D4FF)
private val FGrid    = Color(0xFF1A1A3E)
private val FSurface = Color(0xFF0D0D2B)

private data class FeatureData(
    val icon    : ImageVector,
    val name    : String,
    val useCase : String,
    val tag     : String
)

private val features = listOf(
    FeatureData(
        icon    = Icons.Filled.Search,
        name    = "METAL DETECTOR",
        useCase = "Find studs, pipes & hidden metal behind walls",
        tag     = "MAGNETOMETER"
    ),
    FeatureData(
        icon    = Icons.Filled.Public,
        name    = "GRAVITY METER",
        useCase = "Measure 3-axis g-force & device orientation",
        tag     = "GRAVITY SENSOR"
    ),
    FeatureData(
        icon    = Icons.Filled.Architecture,
        name    = "BUBBLE LEVEL",
        useCase = "Level shelves, frames & surfaces precisely",
        tag     = "ACCELEROMETER"
    ),
    FeatureData(
        icon    = Icons.Filled.SportsBaseball,
        name    = "SPACE BALL",
        useCase = "Physics simulation — tilt to roll, drag to throw",
        tag     = "GRAVITY SENSOR"
    )
)

@Composable
fun FeaturesScreen() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text          = "WHAT'S INSIDE",
            color         = Color.White,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 3.sp,
            textAlign     = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text          = "4 TOOLS · 1 DEVICE",
            color         = FCyan,
            fontSize      = 10.sp,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 2.sp,
            textAlign     = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        features.forEachIndexed { index, feature ->
            FeatureRow(feature = feature, delayMs = index * 120L)
            if (index < features.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun FeatureRow(feature: FeatureData, delayMs: Long) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label         = "row_alpha"
    )

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .background(FSurface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .border(0.5.dp, FGrid, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .background(FCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .border(0.5.dp, FCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = feature.icon,
                contentDescription = null,
                tint               = FCyan,
                modifier           = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text          = feature.name,
                color         = Color.White,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = feature.useCase,
                color      = Color.White.copy(alpha = 0.55f),
                fontSize   = 11.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .background(FGrid, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp)
        ) {
            Text(
                text          = feature.tag,
                color         = FCyan.copy(alpha = 0.6f),
                fontSize      = 7.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
        }
    }
}
