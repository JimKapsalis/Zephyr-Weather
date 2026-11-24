package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons

/**
 * Lightweight, zero-WebView native Compose sponsored banner bar anchored above the bottom
 * navigation bar. Renders instantaneously without blocking the UI thread or requiring
 * Google Play Services Ads WebView processes on Android 16-17 emulators and devices.
 */
@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    var campaignIndex by remember { mutableIntStateOf(0) }
    val campaigns = remember {
        listOf(
             Triple(
                "ECMWF IFS 0.25° • Live Doppler Radar",
                "High-resolution European satellite & rain nowcast active",
                CyanZephyr
            ),
            Triple(
                "EMY Severe Weather Guard • Greece",
                "Real-time storm, thunderstorm & rainfall alerts enabled",
                AmberSol
            ),
            Triple(
                "Zephyr Pro • Multi-Model Telemetry",
                "Instant 15-min precipitation & wind gust tracking",
                EmeraldBreeze
            )
        )
    }
    val active = campaigns[campaignIndex % campaigns.size]

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A1628).copy(alpha = 0.90f),
        border = BorderStroke(0.8.dp, active.third.copy(alpha = 0.32f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { campaignIndex = (campaignIndex + 1) % campaigns.size }
            .testTag("admob_banner_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            active.third.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberSol.copy(alpha = 0.22f),
                    border = BorderStroke(0.8.dp, AmberSol.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "AD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberSol,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = active.first,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = active.second,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(active.third.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WeatherIcons.CompassCalibration,
                    contentDescription = "Sponsored Weather Feed",
                    tint = active.third,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
