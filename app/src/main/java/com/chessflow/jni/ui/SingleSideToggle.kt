package com.chessflow.jni.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.chessflow.jni.R


@Composable
fun SingleSideToggle(
    currentSide: String,
    onSideChange: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (currentSide == "w") Color.White else colorResource(R.color.graphite),
        animationSpec = tween(durationMillis = 300),
        label = "SideBackgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (currentSide == "w") Color.LightGray else colorResource(R.color.olive),
        animationSpec = tween(durationMillis = 300),
        label = "SideBorderColor"
    )

    Surface(
        modifier = Modifier
            .size(24.dp)
            .clickable { onSideChange() },
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (currentSide == "b") {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(size.width * 0.1f, size.height * 0.05f),
                        size = Size(size.width * 0.8f, size.height * 0.4f)
                    )
                }
            }
        }
    }
}