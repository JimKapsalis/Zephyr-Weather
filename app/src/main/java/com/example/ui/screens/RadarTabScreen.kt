package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.RadarLayerMode
import com.example.model.WeatherUnits
import com.example.ui.components.LiveWeatherRadarCanvas
import com.example.ui.components.WindCompassCanvas
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons
import com.example.util.SatelliteMapStyle
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarTabScreen(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    selectedLayer: RadarLayerMode,
    selectedTimelineIndex: Int,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectLayer: (RadarLayerMode) -> Unit,
    onSelectTimelineIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val safeTimelineIdx = selectedTimelineIndex.coerceIn(0, (bundle.hourly.size - 1).coerceAtLeast(0))
    val activeHour = bundle.hourly.getOrNull(safeTimelineIdx)
    val pullToRefreshState = rememberPullToRefreshState()

    var showSatellitePhoto by remember { mutableStateOf(true) }
    var satelliteOnlyMode by remember { mutableStateOf(false) }
    var satelliteZoom by remember { mutableIntStateOf(14) }
    var mapStyle by remember { mutableStateOf(SatelliteMapStyle.HYBRID_SATELLITE) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Marshall-Palmer physical radar reflectivity equation: Z = 200 * R^1.6 -> dBZ = 10 * log10(Z)
    val activePrecipRateMmH = (activeHour?.precipMm ?: bundle.current.precipitationMm).coerceAtLeast(0.0)
    val activeCloudPct = activeHour?.cloudCoverPercent ?: bundle.current.cloudCoverPercent
    val activeRainProb = activeHour?.precipProbability ?: 0
    val liveReflectivityDbz = remember(activePrecipRateMmH, activeCloudPct, activeRainProb) {
        when {
            activePrecipRateMmH >= 0.05 -> {
                val z = 200.0 * activePrecipRateMmH.pow(1.6)
                (10.0 * log10(z.coerceAtLeast(1.0))).coerceIn(8.0, 62.0).roundToInt()
            }
            activeRainProb >= 30 || activeCloudPct >= 65 -> {
                (6.0 + activeRainProb * 0.14 + activeCloudPct * 0.06).roundToInt().coerceIn(5, 22)
            }
            else -> (activeCloudPct * 0.08).roundToInt().coerceIn(0, 10)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = Color(0xFF0E223B),
                color = CyanZephyr,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("radar_pull_to_refresh_indicator")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("radar_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("radar_tab_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Circular Live Doppler Weather Radar Card (with Satellite Town Photo & Interactive Zoom)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_radar_card"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0E2138).copy(alpha = 0.92f)
                    ),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.28f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldBreeze)
                                    )
                                    Text(
                                        text = if (isGreek) "Ζωντανό Ραντάρ & Χάρτης Πόλης" else "Live Doppler & Town Map",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = listOf(bundle.cityName, bundle.regionName, bundle.countryName)
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                        .joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanGlow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CyanZephyr.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "${activeHour?.displayHour ?: "LIVE"} • ${String.format(Locale.US, "%.1fx", zoomScale)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Map Base Selector Pills: ONLY Radar + Satellite Map and Basic Radar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SatelliteModeChip(
                                label = if (isGreek) "Ραντάρ + Δορυφορικός Χάρτης" else "Radar + Satellite Map",
                                selected = showSatellitePhoto,
                                onClick = {
                                    showSatellitePhoto = true
                                    satelliteOnlyMode = false
                                    mapStyle = SatelliteMapStyle.HYBRID_SATELLITE
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("radar_mode_satellite_hybrid")
                            )
                            SatelliteModeChip(
                                label = if (isGreek) "Βασικό Ραντάρ" else "Basic Radar",
                                selected = !showSatellitePhoto,
                                onClick = {
                                    showSatellitePhoto = false
                                    satelliteOnlyMode = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("radar_mode_grid_only")
                            )
                        }

                        if (showSatellitePhoto) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    16 to (if (isGreek) "16x Οδοί" else "16x Streets"),
                                    14 to (if (isGreek) "14x Γειτονιά" else "14x Town"),
                                    12 to (if (isGreek) "12x Πόλη" else "12x City"),
                                    9 to (if (isGreek) "9x Περιοχή" else "9x Region")
                                ).forEach { (zoomVal, label) ->
                                    val isZoomSelected = satelliteZoom == zoomVal
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isZoomSelected) EmeraldBreeze.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.06f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isZoomSelected) EmeraldBreeze else Color.White.copy(alpha = 0.14f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                satelliteZoom = zoomVal
                                                zoomScale = 1.0f
                                                panOffset = Offset.Zero
                                            }
                                            .testTag("radar_satellite_zoom_$zoomVal")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isZoomSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isZoomSelected) Color.White else Color.White.copy(alpha = 0.76f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Unclipped 2x2 Equal-Width Layer Switcher Grid (Rain, Wind, Clouds, Thermal)
                        val layerModes = RadarLayerMode.entries
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            layerModes.chunked(2).forEach { rowModes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowModes.forEach { mode ->
                                        val selected = mode == selectedLayer
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onSelectLayer(mode) },
                                            shape = RoundedCornerShape(50),
                                            label = {
                                                Text(
                                                    text = mode.localizedTitle(language),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyanZephyr.copy(alpha = 0.28f),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color.White.copy(alpha = 0.06f),
                                                labelColor = Color.White.copy(alpha = 0.78f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("radar_layer_${mode.id}")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Circular Doppler Radar Viewport with Floating + / - / 1x Zoom Controls
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LiveWeatherRadarCanvas(
                                bundle = bundle,
                                layerMode = selectedLayer,
                                timelineIndex = safeTimelineIdx,
                                language = language,
                                showSatellitePhoto = showSatellitePhoto,
                                satelliteOnlyMode = satelliteOnlyMode,
                                satelliteZoom = satelliteZoom,
                                mapStyle = mapStyle,
                                zoomScale = zoomScale,
                                panOffset = panOffset,
                                onZoomPanChange = { newScale, newPan ->
                                    zoomScale = newScale.coerceIn(1.0f, 5.0f)
                                    panOffset = if (zoomScale <= 1.01f) Offset.Zero else newPan
                                },
                                modifier = Modifier
                                    .size(284.dp)
                                    .testTag("radar_sweep_canvas")
                            )

                            // Floating Glass + / 1x / - Zoom Buttons on the right edge
                            Column(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0A1B2E).copy(alpha = 0.88f),
                                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            zoomScale = (zoomScale + 0.6f).coerceIn(1.0f, 5.0f)
                                        }
                                        .testTag("radar_zoom_in_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (zoomScale > 1.05f) CyanZephyr.copy(alpha = 0.25f) else Color(0xFF0A1B2E).copy(alpha = 0.82f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (zoomScale > 1.05f) CyanGlow else Color.White.copy(alpha = 0.22f)
                                    ),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            zoomScale = 1.0f
                                            panOffset = Offset.Zero
                                        }
                                        .testTag("radar_zoom_reset_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "1×",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0A1B2E).copy(alpha = 0.88f),
                                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val next = (zoomScale - 0.6f).coerceIn(1.0f, 5.0f)
                                            zoomScale = next
                                            if (next <= 1.05f) panOffset = Offset.Zero
                                        }
                                        .testTag("radar_zoom_out_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "−",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Zoom Slider Bar + Gesture Hint
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isGreek) {
                                            "Ζουμ Ραντάρ (${String.format(Locale.US, "%.1fx", zoomScale)})"
                                        } else {
                                            "Radar Zoom (${String.format(Locale.US, "%.1fx", zoomScale)})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isGreek) {
                                            "Τσιμπήστε, διπλό πάτημα ή σύρετε"
                                        } else {
                                            "Pinch, double-tap, or drag to pan"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanGlow,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Slider(
                                    value = zoomScale,
                                    onValueChange = { newVal ->
                                        zoomScale = newVal.coerceIn(1.0f, 5.0f)
                                        if (zoomScale <= 1.03f) panOffset = Offset.Zero
                                    },
                                    valueRange = 1.0f..5.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanGlow,
                                        activeTrackColor = CyanZephyr,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .testTag("radar_zoom_slider")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // dBZ Radar Reflectivity Legend Bar with Live Marshall-Palmer dBZ Telemetry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isGreek) {
                                    "Ανακλαστικότητα: $liveReflectivityDbz dBZ"
                                } else {
                                    "Reflectivity: $liveReflectivityDbz dBZ"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    CyanZephyr,
                                                    EmeraldBreeze,
                                                    AmberSol,
                                                    CoralWarmth
                                                )
                                            )
                                        )
                                )
                                Text(
                                    text = if (isGreek) "5 → 60 dBZ" else "5 → 60 dBZ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanGlow,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Frame Readout Pill
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val rainProb = activeHour?.precipProbability ?: 0
                                val precipMm = activeHour?.precipMm ?: bundle.current.precipitationMm
                                val windSpd = activeHour?.windSpeedKmh ?: bundle.current.windSpeedKmh
                                val tempVal = activeHour?.tempC ?: bundle.current.tempC

                                RadarMiniStat(
                                    label = if (isGreek) "ΒΡΟΧΗ" else "RAIN %",
                                    value = "$rainProb% (${String.format(Locale.US, "%.1f", precipMm)} mm)",
                                    accent = CyanZephyr,
                                    modifier = Modifier.weight(1.2f)
                                )
                                RadarMiniStat(
                                    label = if (isGreek) "ΑΝΕΜΟΣ" else "WIND",
                                    value = "${units.formatWind(windSpd)} ${bundle.current.windCardinal}",
                                    accent = EmeraldBreeze,
                                    modifier = Modifier.weight(1.1f)
                                )
                                RadarMiniStat(
                                    label = if (isGreek) "ΘΕΡΜ." else "TEMP",
                                    value = units.formatTemp(tempVal),
                                    accent = AmberSol,
                                    modifier = Modifier.weight(0.75f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Radar Timeline Scrubber Row
                        Text(
                            text = if (isGreek) "Χρονοδιάγραμμα Ραντάρ (Επόμενες 12 Ώρες)" else "Radar Forecast Timeline (Next 12 Hours)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("radar_timeline_row")
                        ) {
                            itemsIndexed(bundle.hourly.take(12)) { idx, h ->
                                val isSelected = idx == safeTimelineIdx
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) CyanZephyr.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.10f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSelectTimelineIndex(idx) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = if (idx == 0 && isGreek) "Τώρα" else h.displayHour,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (h.precipProbability > 0) {
                                            Text(
                                                text = "${h.precipProbability}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanGlow,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Wind Compass & Vector Telemetry Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wind_compass_card"),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = WeatherIcons.Air,
                                contentDescription = null,
                                tint = CyanZephyr,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isGreek) "Πυξίδα Ανέμων & Ριπές" else "Wind Vector & Gust Radar",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                WindCompassCanvas(windDirectionDeg = bundle.current.windDirectionDeg)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = bundle.current.windCardinal,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${bundle.current.windDirectionDeg}°",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanGlow
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bundle.current.beaufortLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanGlow
                                )
                                RadarDetailRow(
                                    label = if (isGreek) "Ένταση Ανέμου" else "Sustained Wind",
                                    value = units.formatWind(bundle.current.windSpeedKmh)
                                )
                                RadarDetailRow(
                                    label = if (isGreek) "Μέγιστες Ριπές" else "Peak Gusts",
                                    value = units.formatWind(bundle.current.windGustsKmh)
                                )
                                RadarDetailRow(
                                    label = if (isGreek) "Νεφοκάλυψη" else "Cloud Cover",
                                    value = "${bundle.current.cloudCoverPercent}%"
                                )
                                RadarDetailRow(
                                    label = if (isGreek) "Πίεση Θαλάσσης" else "Barometric Pressure",
                                    value = units.formatPressure(bundle.current.pressureHpa)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Rain Radar Outlook Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radar_rain_summary_card"),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CyanZephyr.copy(alpha = 0.20f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WeatherIcons.WaterDrop,
                                        contentDescription = null,
                                        tint = CyanZephyr,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isGreek) "Ανάλυση Βροχόπτωσης Ραντάρ" else "Precipitation Radar Outlook",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = bundle.rainOutlook.nextRainOnsetLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanGlow
                                )
                            }
                        }

                        RadarDetailRow(
                            label = if (isGreek) "Μέγιστη Πιθανότητα Βροχής" else "Peak Rain Probability",
                            value = "${bundle.rainOutlook.peakPrecipProbability}%"
                        )
                        RadarDetailRow(
                            label = if (isGreek) "Συνολικός Υετός 7 Ημερών" else "7-Day Total Precipitation",
                            value = String.format(Locale.US, "%.1f mm", bundle.rainOutlook.totalRainNext7DaysMm)
                        )
                        RadarDetailRow(
                            label = if (isGreek) "Ορατότητα" else "Horizontal Visibility",
                            value = "${bundle.current.visibilityKm.roundToInt()} km"
                        )
                        RadarDetailRow(
                            label = if (isGreek) "Ποιότητα Αέρα (AQI)" else "Air Quality (US AQI)",
                            value = "${bundle.airQuality.usAqi} • ${bundle.airQuality.localizedCategory(language)}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SatelliteModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) CyanZephyr.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) CyanZephyr else Color.White.copy(alpha = 0.14f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RadarMiniStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.60f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RadarDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.70f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1
        )
    }
}
