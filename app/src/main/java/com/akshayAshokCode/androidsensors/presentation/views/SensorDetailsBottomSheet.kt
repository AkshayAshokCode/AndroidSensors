package com.akshayAshokCode.androidsensors.presentation.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshayAshokCode.androidsensors.R

private val BSVoid    = DashVoid
private val BSSurface = DashSurface
private val BSCyan    = DashCyan
private val BSGrid    = DashGrid
private val BSGreen   = DashGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailsBottomSheet(
    isVisible  : Boolean,
    onDismiss  : () -> Unit,
    title      : String,
    instruction: String,
    content    : String
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isVisible) {
        if (!isVisible) sheetState.hide()
    }

    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = BSSurface,
        dragHandle       = {
            // Futuristic cyan drag handle
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .background(BSCyan.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title row
            Text(
                text          = title.uppercase(),
                color         = Color.White,
                fontSize      = 14.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(4.dp))

            HorizontalDivider(color = BSCyan.copy(alpha = 0.3f), thickness = 0.5.dp)

            Spacer(Modifier.height(16.dp))

            // How to use — highlighted section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(BSCyan.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(BSGreen, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text          = stringResource(R.string.how_to_use_label)
                            .replace("💡", "").trim().uppercase(),
                        color         = BSGreen,
                        fontSize      = 9.sp,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = instruction,
                        color      = Color.White.copy(alpha = 0.85f),
                        fontSize   = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Detail content — parse bullet points for better rendering
            val lines = content.trim().split("\n").filter { it.isNotBlank() }
            lines.forEach { line ->
                val cleaned = line.trim().removePrefix("►").trim()
                if (cleaned.isBlank()) return@forEach

                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text       = "·",
                        color      = BSCyan.copy(alpha = 0.6f),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(end = 8.dp, top = 1.dp)
                    )
                    Text(
                        text       = cleaned,
                        color      = Color.White.copy(alpha = 0.7f),
                        fontSize   = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
