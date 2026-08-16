package com.chessflow.jni.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chessflow.jni.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Safely retrieve application version information
    val packageInfo = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 1L
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong() ?: 1L
    }

    val fullVersionText = stringResource(id = R.string.version_format, versionName, versionCode)
    val noBrowserErrorText = stringResource(id = R.string.error_no_browser)
    val versionCopiedText = stringResource(id = R.string.version_copied, fullVersionText)
    val appVersionLabel = stringResource(id = R.string.app_version_label)

    // Helper function to safely open URLs
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, noBrowserErrorText, Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to copy version info to system clipboard
    fun copyVersionToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(appVersionLabel, fullVersionText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, versionCopiedText, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Adaptive application launcher icon using AndroidView to prevent Compose XML rendering issues
        Surface(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            shadowElevation = 4.dp
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // App Name Header
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.brown)
            )
        )

        // Interactive Version Badge (Click to copy)
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { copyVersionToClipboard() }
        ) {
            Text(
                text = fullVersionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // About description text
        Text(
            text = stringResource(id = R.string.about_description),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        // Open Source / GPL Notice Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.champagne)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.open_source_notice_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colorResource(id = R.color.brown)
                )
                Text(
                    text = stringResource(id = R.string.stockfish_license_text),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { openUrl("https://www.gnu.org/licenses/gpl-3.0.html") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.read_gpl_license),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorResource(id = R.color.moss_dark),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // GitHub Repository Link Button
        Button(
            onClick = { openUrl("https://github.com/ymincheva/ChessFlow") },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.moss_dark),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(id = R.string.view_source_code),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Developer Credits Footer
        Text(
            text = stringResource(id = R.string.developer_credits),
            style = MaterialTheme.typography.bodySmall.copy(
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        )
    }
}