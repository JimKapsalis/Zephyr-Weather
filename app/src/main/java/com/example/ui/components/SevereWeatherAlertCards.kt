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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = WeatherIcons.NotificationsActive,
                    contentDescription = null,
                    tint = AmberSol,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isGreek) "Ζωντανές Ειδοποιήσεις Καιρού" else "Live Weather Alerts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            if (dismissedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onRestoreDismissedAlerts() }
                        .testTag("restore_dismissed_alerts_button")
                ) {
                    Text(
                        text = if (isGreek) "Επαναφορά ($dismissedCount)" else "Restore ($dismissedCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
 * inside the horizontal alert pager.
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
            containerColor = Color(0xFF102238).copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.3.dp, accentColor.copy(alpha = 0.60f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = severityBadgeLabel,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = accentColor.copy(alpha = 0.24f)
                                ) {
                                    Text(
                                        text = severityBadgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                if (alert.onsetHourLabel != "--") {
                                    Text(
                                        text = "• ${alert.onsetHourLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
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
                            color = Color.White.copy(alpha = 0.10f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WeatherIcons.Close,
                                    contentDescription = if (isGreek) "Κλείσιμο" else "Dismiss",
                                    tint = Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("severe_alert_message_${alert.id}")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Telemetry chips row (Rain %, Precip mm, Wind Gusts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlertTelemetryPill(
                        label = if (isGreek) "Βροχή ${alert.precipProbability}%" else "Rain ${alert.precipProbability}%",
                        color = CyanZephyr
                    )
                    if (alert.precipMm > 0.0) {
                        AlertTelemetryPill(
                            label = String.format(Locale.US, "%.1f mm/h", alert.precipMm),
                            color = CyanGlow
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
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}
