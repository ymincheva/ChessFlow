package com.chessflow.jni.componets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.chessflow.jni.R

// ✅ Helper composables
@Composable
fun PuzzleNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    if (outlined) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, colorResource(id = R.color.moss_dark)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled, onClick = onClick)
            ) {
                Icon(icon, contentDescription = null, tint = colorResource(id = R.color.moss_dark))
            }
        }
    } else {
        Card(
            modifier = Modifier
                .size(48.dp)
                .clickable(enabled = enabled, onClick = onClick),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled)
                    colorResource(R.color.moss_dark)
                else Color.LightGray,
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}