package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.WeatherUnits
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons
import com.example.util.SevereAlertSeverity
import com.example.util.SevereAlertType
import com.example.util.SevereWeatherAlertEvent
import java.util.Locale

/**
 * Dashboard section that displays active weather cards in a clean horizontal pager
 * without "Ειδοποιήσεις", "Σύρετε δίπλα", or pagination dots.
 */
@Composable
fun SevereWeatherAlertsDashboardSection(
    alerts: List<SevereWeatherAlertEvent>,
    dismissedAlertIds: Set<String>,
    units: WeatherUnits,
    language: AppLanguage,
    onDismissAlert: (String) -> Unit,
    onRestoreDismissedAlerts: () -> Unit,
    onTriggerTestAlert: () -> Unit,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val visibleAlerts = alerts.filter { it.id !in dismissedAlertIds }
    val dismissedCount = alerts.count { it.id in dismissedAlertIds }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { visibleAlerts.size.coerceAtLeast(1) }
    )

    LaunchedEffect(visibleAlerts.size) {
        if (visibleAlerts.isNotEmpty() && pagerState.currentPage >= visibleAlerts.size) {
            pagerState.animateScrollToPage((visibleAlerts.size - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("severe_alerts_dashboard_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = WeatherIcons.NotificationsActive,
                    contentDescription = null,
                    tint = AmberSol,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isGreek) "Ζωντανές Ειδοποιήσεις Καιρού" else "Live Weather Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dismissedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onRestoreDismissedAlerts() }
                            .testTag("restore_dismissed_alerts_button")
                    ) {
                        Text(
                            text = if (isGreek) "Επαναφορά ($dismissedCount)" else "Restore ($dismissedCount)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyanZephyr.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onNavigateToRadar() }
                        .testTag("alerts_open_radar_view_button")
                ) {
                    Text(
                        text = if (isGreek) "Προβολή Ραντάρ →" else "Radar View →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visibleAlerts.isNotEmpty(),
            enter = expandVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("severe_alerts_horizontal_pager")
            ) { pageIndex ->
                val alert = visibleAlerts.getOrNull(pageIndex)
                if (alert != null) {
                    DismissibleSevereWeatherAlertCard(
                        alert = alert,
                        units = units,
                        language = language,
                        currentCardIndex = pageIndex + 1,
                        totalCardsCount = visibleAlerts.size,
                        onDismiss = { onDismissAlert(alert.id) },
                        onClickCard = onNavigateToRadar
                    )
                }
            }
        }
    }
}

/**
 * Distinct, dismissible Material 3 card representing a single [SevereWeatherAlertEvent]
 * inside the horizontal alert pager, designed with large high-contrast typography
 * so it remains effortlessly readable even at thumbnail / social feed scale.
 */
@Composable
fun DismissibleSevereWeatherAlertCard(
    alert: SevereWeatherAlertEvent,
    units: WeatherUnits,
    language: AppLanguage,
    currentCardIndex: Int = 1,
    totalCardsCount: Int = 1,
    onDismiss: () -> Unit,
    onClickCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val accentColor = when (alert.severity) {
        SevereAlertSeverity.CRITICAL -> CoralWarmth
        SevereAlertSeverity.WARNING -> AmberSol
        SevereAlertSeverity.ADVISORY -> if (alert.type == SevereAlertType.ALL_CLEAR) EmeraldBreeze else CyanZephyr
    }
    val severityBadgeLabel = when (alert.severity) {
        SevereAlertSeverity.CRITICAL -> if (isGreek) "ΚΡΙΣΙΜΟ" else "CRITICAL"
        SevereAlertSeverity.WARNING -> if (isGreek) "ΠΡΟΕΙΔΟΠΟΙΗΣΗ" else "WARNING"
        SevereAlertSeverity.ADVISORY -> if (alert.type == SevereAlertType.ALL_CLEAR) {
            if (isGreek) "ΕΝΕΡΓΟ" else "LIVE"
        } else {
            if (isGreek) "ΔΕΛΤΙΟ" else "ADVISORY"
        }
    }
    val iconVector = when (alert.type) {
        SevereAlertType.THUNDERSTORM -> WeatherIcons.Thunderstorm
        SevereAlertType.HEAVY_RAIN, SevereAlertType.RAIN_ONSET -> WeatherIcons.Grain
        SevereAlertType.GALE_WIND -> WeatherIcons.PartlySunny
        SevereAlertType.ALL_CLEAR -> WeatherIcons.WbSunny
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClickCard() }
            .testTag("dismissible_alert_card_${alert.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E2138).copy(alpha = 0.62f)
        ),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.72f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color(0xFF0B1A2E).copy(alpha = 0.35f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.25f),
                            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = severityBadgeLabel,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = accentColor.copy(alpha = 0.28f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.60f))
                                ) {
                                    Text(
                                        text = severityBadgeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }

                                if (alert.onsetHourLabel != "--") {
                                    Text(
                                        text = "• ${alert.onsetHourLabel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberSol,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("severe_alert_title_${alert.id}")
                            )
                        }
                    }

                    // Accessible 48dp dismiss button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dismiss_severe_alert_${alert.id}")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WeatherIcons.Close,
                                    contentDescription = if (isGreek) "Κλείσιμο" else "Dismiss",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.96f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("severe_alert_message_${alert.id}")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // High-contrast oversized telemetry chips row (Rain %, Precip mm, Wind Gusts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlertTelemetryPill(
                        label = if (isGreek) "Βροχή ${alert.precipProbability}%" else "Rain ${alert.precipProbability}%",
                        color = CyanGlow
                    )
                    if (alert.precipMm > 0.0) {
                        AlertTelemetryPill(
                            label = String.format(Locale.US, "%.1f mm/h", alert.precipMm),
                            color = AmberSol
                        )
                    }
                    AlertTelemetryPill(
                        label = if (isGreek) {
                            "Ριπές ${units.formatWind(alert.windGustsKmh)}"
                        } else {
                            "Gusts ${units.formatWind(alert.windGustsKmh)}"
                        },
                        color = EmeraldBreeze
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertTelemetryPill(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.20f),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
