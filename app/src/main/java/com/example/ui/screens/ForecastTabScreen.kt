package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SavedCityEntity
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.DailyForecastItem
import com.example.model.HourlyChartMode
import com.example.model.HourlyForecastItem
import com.example.model.SkyAtmospherePreset
import com.example.model.SkyParticleEffect
import com.example.model.WeatherProvider
import com.example.model.WeatherUnits
import com.example.model.WmoWeatherMapper
import com.example.ui.components.AirQualityArcCanvas
import com.example.ui.components.AnimatedWeatherStatusIcon
import com.example.ui.components.EnsembleConfidenceAndSaharaDustCard
import com.example.ui.components.HeroWeatherSceneOverlay
import com.example.ui.components.HomeCitySearchAndQuickSwitchCard
import com.example.ui.components.HyperLocalRainAndCommuteCard
import com.example.ui.components.InteractiveHourlyCurveChart
import com.example.ui.components.LottieStyleWeatherHeroBadge
import com.example.ui.components.SevereWeatherAlertsDashboardSection
import com.example.ui.components.SolarHorizonCanvas
import com.example.ui.components.TenDayRangeBar
import com.example.ui.components.WeekendOutlookAndQuickShareCard
import com.example.ui.components.WindCompassCanvas
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons
import com.example.util.SevereWeatherAlertEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun rememberLiveGreeceClock(timezoneId: String?): String {
    val effectiveZoneId = remember(timezoneId) {
        val trimmed = timezoneId?.trim().orEmpty()
        if (trimmed.isBlank() ||
            trimmed.equals("GMT", ignoreCase = true) ||
            trimmed.equals("UTC", ignoreCase = true) ||
            trimmed.equals("auto", ignoreCase = true)
        ) {
            "Europe/Athens"
        } else {
            trimmed
        }
    }
    var currentClock by remember(effectiveZoneId) {
        mutableStateOf(formatClockInZone(effectiveZoneId))
    }
    val isResumed = com.example.ui.components.rememberIsAppResumed()
    LaunchedEffect(effectiveZoneId, isResumed) {
        if (!isResumed) return@LaunchedEffect
        while (true) {
            currentClock = formatClockInZone(effectiveZoneId)
            val millisUntilNextMinute = (60_000L - (System.currentTimeMillis() % 60_000L))
                .coerceIn(5_000L, 60_000L)
            delay(millisUntilNextMinute)
        }
    }
    return currentClock
}

private fun formatClockInZone(zoneId: String): String {
    return runCatching {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone(zoneId)
        sdf.format(Date())
    }.getOrDefault("12:00")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastTabScreen(
    bundle: CityWeatherBundle?,
    savedCities: List<SavedCityEntity>,
    suggestedCities: List<SavedCityEntity> = emptyList(),
    selectedCityId: Long,
    selectedProvider: WeatherProvider = bundle?.provider ?: WeatherProvider.BEST_MATCH,
    chartMode: HourlyChartMode,
    selectedHourIndex: Int,
    expandedDayIndex: Int?,
    units: WeatherUnits,
    language: AppLanguage = AppLanguage.ENGLISH,
    skyAtmospherePreset: SkyAtmospherePreset = SkyAtmospherePreset.LIVE_GPS,
    isRefreshing: Boolean = false,
    isLocatingGps: Boolean = false,
    searchQuery: String = "",
    searchResults: List<SavedCityEntity> = emptyList(),
    isSearching: Boolean = false,
    animationTriggerKey: Int = 1,
    activeSevereAlert: SevereWeatherAlertEvent? = null,
    activeSevereAlerts: List<SevereWeatherAlertEvent> = emptyList(),
    dismissedAlertIds: Set<String> = emptySet(),
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onAddAndSelectCity: (SavedCityEntity) -> Unit = {},
    onRequestGpsLocation: () -> Unit = {},
    onTriggerTestAlert: () -> Unit = {},
    onDismissAlert: (String) -> Unit = {},
    onRestoreDismissedAlerts: () -> Unit = {},
    onSelectSkyPreset: (SkyAtmospherePreset) -> Unit = {},
    onSelectCity: (SavedCityEntity) -> Unit,
    onSelectChartMode: (HourlyChartMode) -> Unit,
    onSelectHourIndex: (Int) -> Unit,
    onToggleExpandDay: (Int) -> Unit,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedAlerts = remember(activeSevereAlerts, activeSevereAlert) {
        val raw = if (activeSevereAlerts.isNotEmpty()) {
            activeSevereAlerts
        } else {
            listOfNotNull(activeSevereAlert)
        }
        raw.filter { it.isSevereOrRainActive }
    }

    val pullToRefreshState = rememberPullToRefreshState()

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
                    .testTag("forecast_pull_to_refresh_indicator")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("forecast_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("forecast_tab_list"),
            contentPadding = PaddingValues(top = 6.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Home Screen City Search Bar & Quick City Chips
            item(key = "home_search_and_chips", contentType = "search_bar") {
                HomeCitySearchAndQuickSwitchCard(
                    savedCities = savedCities,
                    selectedCityId = selectedCityId,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    isLocatingGps = isLocatingGps,
                    units = units,
                    language = language,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectCity = onSelectCity,
                    onAddAndSelectCity = onAddAndSelectCity,
                    onRequestGpsLocation = onRequestGpsLocation
                )
            }

            if (bundle == null) {
                item(key = "weather_loading_skeleton", contentType = "loading_skeleton") {
                    WeatherLoadingSkeletonCard(
                        selectedProvider = selectedProvider,
                        language = language,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
                return@LazyColumn
            }

            // 1. Swipeable Hero Weather Pager (Automatic Live Environment Sky, without manual sky selector)
            item(key = "hero_weather_pager_section", contentType = "hero_pager") {
                SwipeableCityHeroPager(
                    bundle = bundle,
                    savedCities = savedCities,
                    selectedCityId = selectedCityId,
                    selectedProvider = selectedProvider,
                    units = units,
                    language = language,
                    skyAtmospherePreset = SkyAtmospherePreset.LIVE_GPS,
                    animationTriggerKey = animationTriggerKey,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onSelectCity = onSelectCity,
                    onNavigateToRadar = onNavigateToRadar,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1b. One-by-one Horizontal Swipeable Severe Weather Alert Cards on Dashboard
            if (resolvedAlerts.isNotEmpty()) {
                item(key = "severe_weather_alerts_section", contentType = "severe_alerts") {
                    SevereWeatherAlertsDashboardSection(
                        alerts = resolvedAlerts,
                        dismissedAlertIds = dismissedAlertIds,
                        language = language,
                        units = units,
                        onDismissAlert = onDismissAlert,
                        onRestoreDismissedAlerts = onRestoreDismissedAlerts,
                        onTriggerTestAlert = onTriggerTestAlert,
                        onNavigateToRadar = onNavigateToRadar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // 2. 10-Day Temperature Range Bar Forecast Card
            item(key = "ten_day_forecast_section", contentType = "ten_day_forecast") {
                val overallMin = remember(bundle.daily) { bundle.daily.minOfOrNull { it.minTempC } ?: 5.0 }
                val overallMax = remember(bundle.daily) { bundle.daily.maxOfOrNull { it.maxTempC } ?: 28.0 }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("ten_day_forecast_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.48f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.GREEK) "Πρόγνωση 10 Ημερών" else "10-Day Forecast",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (language == AppLanguage.GREEK) "Πατήστε για λεπτομέρειες" else "Tap day for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        bundle.daily.forEachIndexed { idx, day ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            DailyRangeRow(
                                day = day,
                                isToday = idx == 0,
                                currentTempC = if (idx == 0) bundle.current.tempC else null,
                                overallMinC = overallMin,
                                overallMaxC = overallMax,
                                isExpanded = expandedDayIndex == idx,
                                units = units,
                                language = language,
                                onToggleExpand = { onToggleExpandDay(idx) }
                            )
                        }
                    }
                }
            }

            // 3. Interactive 24-Hour Curve & Hourly Scrubber Card
            item(key = "hourly_curve_section") {
                val safeHourIdx = selectedHourIndex.coerceIn(0, (bundle.hourly.size - 1).coerceAtLeast(0))
                val inspectedHour = bundle.hourly.getOrNull(safeHourIdx)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("hourly_curve_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.48f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (language == AppLanguage.GREEK) "Ωριαία Πρόγνωση 24 Ωρών" else "24-Hour Horizon Curve",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (inspectedHour != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            val feelsWord = if (language == AppLanguage.GREEK) "Αίσθηση" else "Feels"
                            val rainWord = if (language == AppLanguage.GREEK) "βροχή" else "rain"
                            Text(
                                text = "${inspectedHour.displayHour}: ${units.formatTemp(inspectedHour.tempC)} ($feelsWord ${units.formatTemp(inspectedHour.feelsLikeC)}) • ${inspectedHour.precipProbability}% $rainWord • ${units.formatWind(inspectedHour.windSpeedKmh)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanGlow,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HourlyChartMode.entries.forEach { mode ->
                                val selected = mode == chartMode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectChartMode(mode) },
                                    shape = RoundedCornerShape(50),
                                    label = {
                                        Text(
                                            text = mode.localizedTitle(language),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanZephyr.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.06f),
                                        labelColor = Color.White.copy(alpha = 0.72f)
                                    ),
                                    modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        InteractiveHourlyCurveChart(
                            hourly = bundle.hourly,
                            mode = chartMode,
                            selectedIndex = safeHourIdx,
                            onSelectIndex = onSelectHourIndex
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("hourly_items_row")
                        ) {
                            itemsIndexed(
                                items = bundle.hourly.take(24),
                                key = { idx, hourItem -> "${idx}_${hourItem.displayHour}" }
                            ) { idx, hourItem ->
                                HourlyPillItem(
                                    item = hourItem,
                                    isSelected = idx == safeHourIdx,
                                    units = units,
                                    language = language,
                                    onClick = { onSelectHourIndex(idx) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Quick Environmental Bento Grid
            item(key = "environmental_bento_grid", contentType = "environmental_bento") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickMetricBentoCard(
                            title = if (language == AppLanguage.GREEK) "ΑΝΕΜΟΣ & ΡΙΠΕΣ" else "WIND & GUSTS",
                            primaryValue = units.formatWind(bundle.current.windSpeedKmh),
                            subtitle = "${bundle.current.windCardinal} (${bundle.current.windDirectionDeg}°) • ${units.formatWind(bundle.current.windGustsKmh)}",
                            icon = WeatherIcons.Air,
                            accent = CyanZephyr,
                            onClick = onNavigateToRadar,
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricBentoCard(
                            title = if (language == AppLanguage.GREEK) "ΥΓΡΑΣΙΑ & ΔΡΟΣΟΣ" else "HUMIDITY & DEW",
                            primaryValue = "${bundle.current.humidity}%",
                            subtitle = if (language == AppLanguage.GREEK) {
                                "Σημείο δρόσου ${units.formatTemp(bundle.current.dewPointC)}"
                            } else {
                                "Dew point ${units.formatTemp(bundle.current.dewPointC)}"
                            },
                            icon = WeatherIcons.WaterDrop,
                            accent = EmeraldBreeze,
                            onClick = onNavigateToRadar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickMetricBentoCard(
                            title = if (language == AppLanguage.GREEK) "ΑΚΤΙΝΟΒΟΛΙΑ UV" else "UV RADIATION",
                            primaryValue = String.format(Locale.US, "%.1f", bundle.current.uvIndex),
                            subtitle = uvCategoryDescription(bundle.current.uvIndex, language),
                            icon = WeatherIcons.WbSunny,
                            accent = AmberSol,
                            onClick = onNavigateToRadar,
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricBentoCard(
                            title = if (language == AppLanguage.GREEK) "ΠΙΕΣΗ & ΟΡΑΤΟΤΗΤΑ" else "PRESSURE & VISIBILITY",
                            primaryValue = units.formatPressure(bundle.current.pressureHpa),
                            subtitle = if (language == AppLanguage.GREEK) {
                                "Ορατότητα ${bundle.current.visibilityKm.roundToInt()} km • Νέφη ${bundle.current.cloudCoverPercent}%"
                            } else {
                                "Visibility ${bundle.current.visibilityKm.roundToInt()} km • Cloud ${bundle.current.cloudCoverPercent}%"
                            },
                            icon = WeatherIcons.Speed,
                            accent = CyanGlow,
                            onClick = onNavigateToRadar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Circular Solar Horizon, Wind Compass & Air Quality Observatory Card
            item(key = "circular_observatory_section") {
                CircularObservatoryDashboardCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    onNavigateToRadar = onNavigateToRadar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 6b. Live Night Sky Visible Star Count & Stargazing Telemetry Card
            item(key = "night_sky_star_count_section") {
                NightSkyStarCountCard(
                    bundle = bundle,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 6c. Live Lightning Strike Distance & Storm ETA Radar Card
            item(key = "live_lightning_storm_distance_section") {
                LiveLightningStormDistanceCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    onNavigateToRadar = onNavigateToRadar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 6d. v1.3 Update: 60-Min Hyper-Local Rain Nowcast Bar Chart & Daily Commute Windows
            item(key = "hyper_local_rain_commute_section") {
                HyperLocalRainAndCommuteCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    onNavigateToRadar = onNavigateToRadar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 6e. v1.3 Update: 4-Model Ensemble Confidence, Sahara Dust (Αφρικανική Σκόνη) & Bio-Weather Pressure
            item(key = "ensemble_confidence_sahara_dust_section") {
                EnsembleConfidenceAndSaharaDustCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 6f. v1.3 Update: Weekend Weather Planner (Sat/Sun) & One-Tap Share Forecast Snapshot
            item(key = "weekend_outlook_share_section") {
                WeekendOutlookAndQuickShareCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 7. City / Village Elevation (Υψόμετρο) Card
            item(key = "city_elevation_bottom_section") {
                CityElevationBottomFooterCard(
                    bundle = bundle,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 8. Bottom Clothing & Outdoor Activity Advisor Card
            item(key = "clothing_outdoor_advisor_bottom_section") {
                ClothingOutdoorActivityAdvisorCard(
                    bundle = bundle,
                    units = units,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLocationPickerCard(
    suggestedCities: List<SavedCityEntity>,
    units: WeatherUnits,
    language: AppLanguage,
    onAddAndSelectCity: (SavedCityEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    Card(
        modifier = modifier.testTag("empty_location_picker_card"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E2138).copy(alpha = 0.50f)
        ),
        border = BorderStroke(1.2.dp, CyanZephyr.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyanZephyr.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.50f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = WeatherIcons.PartlySunny,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGreek) "Επιλέξτε την Τοποθεσία σας" else "Choose Your Location",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isGreek) {
                            "Αναζητήστε οποιαδήποτε πόλη ή χωριό παραπάνω ή επιλέξτε μία πόλη από τη λίστα."
                        } else {
                            "Search for any city or village above, or select a location below to view live weather."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f)
                    )
                }
            }

            if (suggestedCities.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                Text(
                    text = if (isGreek) "ΔΗΜΟΦΙΛΕΙΣ ΠΟΛΕΙΣ" else "POPULAR LOCATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanGlow
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestedCities.forEach { city ->
                        val spec = WmoWeatherMapper.resolve(city.cachedWeatherCode, city.cachedIsDay)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onAddAndSelectCity(city) }
                                .testTag("quick_pick_city_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AnimatedWeatherStatusIcon(
                                        visualSpec = spec,
                                        iconSize = 24.dp,
                                        contentDescription = spec.localizedLabel(language)
                                    )
                                    Column {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${city.regionName}, ${city.countryName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.68f)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = units.formatTemp(city.cachedTempC),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanGlow
                                    )
                                    Icon(
                                        imageVector = WeatherIcons.AddCircle,
                                        contentDescription = if (isGreek) "Προσθήκη" else "Add",
                                        tint = CyanZephyr,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal Pager wrapping the Hero Weather Card so the user can swipe left or right
 * to smoothly switch between their saved cities (`savedCities`), without pagination dots.
 */
@Composable
fun SwipeableCityHeroPager(
    bundle: CityWeatherBundle,
    savedCities: List<SavedCityEntity>,
    selectedCityId: Long,
    selectedProvider: WeatherProvider = bundle.provider,
    units: WeatherUnits,
    language: AppLanguage,
    skyAtmospherePreset: SkyAtmospherePreset,
    animationTriggerKey: Int,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectCity: (SavedCityEntity) -> Unit,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (savedCities.size <= 1) {
        CurrentWeatherHeroSection(
            bundle = bundle,
            selectedProvider = selectedProvider,
            units = units,
            language = language,
            skyAtmospherePreset = skyAtmospherePreset,
            animationTriggerKey = animationTriggerKey,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onNavigateToRadar = onNavigateToRadar,
            modifier = modifier.padding(horizontal = 16.dp)
        )
        return
    }

    val selectedIndex = remember(savedCities, selectedCityId) {
        savedCities.indexOfFirst { it.id == selectedCityId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { savedCities.size }
    )

    // Sync external selectedCityId changes (e.g. clicking a chip) -> Pager page
    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    // Sync user swipe on HorizontalPager -> selectedCityId in ViewModel
    LaunchedEffect(pagerState.settledPage) {
        val targetCity = savedCities.getOrNull(pagerState.settledPage)
        if (targetCity != null && targetCity.id != selectedCityId) {
            onSelectCity(targetCity)
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_city_pager")
    ) { page ->
        val pageCity = savedCities.getOrNull(page)
        val isCurrentSelected = pageCity == null || pageCity.id == bundle.cityId || page == selectedIndex
        val pageBundle = if (isCurrentSelected) {
            bundle
        } else {
            bundle.copy(
                cityId = pageCity!!.id,
                cityName = pageCity.name,
                regionName = pageCity.regionName,
                countryName = pageCity.countryName,
                latitude = pageCity.latitude,
                longitude = pageCity.longitude,
                elevationMeters = pageCity.elevationMeters,
                provider = selectedProvider,
                current = bundle.current.copy(
                    tempC = pageCity.cachedTempC,
                    highC = pageCity.cachedHighC,
                    lowC = pageCity.cachedLowC,
                    weatherCode = pageCity.cachedWeatherCode,
                    isDay = pageCity.cachedIsDay
                )
            )
        }

        CurrentWeatherHeroSection(
            bundle = pageBundle,
            selectedProvider = selectedProvider,
            units = units,
            language = language,
            skyAtmospherePreset = skyAtmospherePreset,
            animationTriggerKey = animationTriggerKey,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onNavigateToRadar = onNavigateToRadar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CircularObservatoryDashboardCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val today = bundle.daily.firstOrNull()
    val sunrise = today?.sunriseTime ?: "06:42"
    val sunset = today?.sunsetTime ?: "18:05"
    val daylightHrs = today?.daylightHours ?: 11.4
    val sunPct = (bundle.sunProgressFraction * 100f).roundToInt().coerceIn(0, 100)

    Card(
        modifier = modifier.testTag("circular_observatory_card"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF102238).copy(alpha = 0.48f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AmberSol.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.28f),
                    CyanZephyr.copy(alpha = 0.36f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A1836).copy(alpha = 0.28f),
                            Color(0xFF102238).copy(alpha = 0.46f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row: Sunset & Solar Horizon Title + Daylight Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AmberSol.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.45f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WeatherIcons.WbTwilight,
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isGreek) "Ηλιοβασίλεμα & Ηλιακή Τροχιά" else "Sunset & Solar Horizon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isGreek) {
                                    "Διάρκεια ημέρας ${String.format(Locale.US, "%.1f", daylightHrs)} ώρες • $sunPct% τροχιάς"
                                } else {
                                    "Daylight ${String.format(Locale.US, "%.1f", daylightHrs)} hrs • $sunPct% horizon progress"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDBA74),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmberSol.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (isGreek) "Δύση $sunset" else "Sunset $sunset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberSol,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Solar Horizon Trajectory Arc Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0A1628).copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        SolarHorizonCanvas(
                            sunProgress = bundle.sunProgressFraction,
                            isDay = bundle.current.isDay
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3-Column Sunrise / Golden Dusk / Sunset Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.22f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isGreek) "ΑΝΑΤΟΛΗ" else "SUNRISE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sunrise,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberSol,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color(0xFFFB923C).copy(alpha = 0.28f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isGreek) "ΛΥΚΟΦΩΣ" else "GOLDEN HOUR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f h", daylightHrs),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFDBA74),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isGreek) "ΔΥΣΗ ΗΛΙΟΥ" else "SUNSET",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sunset,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanGlow,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentWeatherHeroSection(
    bundle: CityWeatherBundle?,
    selectedProvider: WeatherProvider = bundle?.provider ?: WeatherProvider.BEST_MATCH,
    units: WeatherUnits = WeatherUnits(),
    language: AppLanguage = AppLanguage.ENGLISH,
    skyAtmospherePreset: SkyAtmospherePreset = SkyAtmospherePreset.LIVE_GPS,
    animationTriggerKey: Int = 1,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateToRadar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val liveClockLabel = rememberLiveGreeceClock(bundle?.timezoneId)
    val sunsetTime = bundle?.daily?.firstOrNull()?.sunsetTime ?: "18:05"
    val isLiveSunset = remember(liveClockLabel, sunsetTime) {
        WmoWeatherMapper.isSunsetClockTime(liveClockLabel, sunsetTime)
    }
    val isLiveNight = remember(bundle?.current?.isDay, liveClockLabel) {
        if (bundle != null) {
            !bundle.current.isDay
        } else {
            WmoWeatherMapper.isNightHourLabel(liveClockLabel)
        }
    }

    val visualSpec = remember(bundle?.current?.weatherCode, isLiveNight, isLiveSunset) {
        WmoWeatherMapper.resolve(
            code = bundle?.current?.weatherCode ?: 2,
            isDay = !isLiveNight,
            isSunset = isLiveSunset
        )
    }

    val heroTextureRes = when (visualSpec.particleEffect) {
        SkyParticleEffect.SUNSET_DUSK -> R.drawable.img_sunset_dusk
        SkyParticleEffect.STARFIELD -> R.drawable.img_night_moon
        else -> R.drawable.img_atmosphere_hero
    }
    val heroTextureAlpha = when (visualSpec.particleEffect) {
        SkyParticleEffect.SUNSET_DUSK -> 0.50f
        SkyParticleEffect.STARFIELD -> 0.52f
        else -> 0.24f
    }

    val cityTitle = bundle?.cityName?.takeIf { it.isNotBlank() }
        ?: "Zephyr Weather"
    val locationSubtitle = if (bundle != null) {
        val isAthensZone = bundle.timezoneId.equals("Europe/Athens", ignoreCase = true)
        val localLabel = if (isAthensZone) {
            if (language == AppLanguage.GREEK) "Ώρα Ελλάδας" else "Greece Time"
        } else {
            if (language == AppLanguage.GREEK) "Τοπική Ώρα" else "Local Time"
        }
        val elevText = if (bundle.elevationMeters > 0.0) {
            if (language == AppLanguage.GREEK) {
                " • Υψόμετρο ${bundle.elevationMeters.roundToInt()} m"
            } else {
                " • Elev ${bundle.elevationMeters.roundToInt()} m"
            }
        } else {
            ""
        }
        "${bundle.regionName}, ${bundle.countryName}$elevText • $localLabel $liveClockLabel"
    } else {
        if (language == AppLanguage.GREEK) "Zephyr Weather • Τοπική Ώρα $liveClockLabel" else "Zephyr Weather • Local Time $liveClockLabel"
    }

    val temperatureDisplay = remember(bundle?.current?.tempC, units.useFahrenheit) {
        if (bundle != null) {
            units.formatTemp(bundle.current.tempC)
        } else {
            "--°C"
        }
    }

    val feelsLikeDisplay = remember(bundle?.current, units, language) {
        if (bundle != null) {
            if (language == AppLanguage.GREEK) {
                "Αίσθηση ${units.formatTemp(bundle.current.feelsLikeC)}  •  Μέγ: ${units.formatTemp(bundle.current.highC, false)}  Ελάχ: ${units.formatTemp(bundle.current.lowC, false)}"
            } else {
                "Feels like ${units.formatTemp(bundle.current.feelsLikeC)}  •  H: ${units.formatTemp(bundle.current.highC, false)}  L: ${units.formatTemp(bundle.current.lowC, false)}"
            }
        } else {
            "Feels like --°  •  H: --°  L: --°"
        }
    }
    val conditionLabel = if (bundle != null) {
        visualSpec.localizedLabel(language)
    } else {
        "Zephyr Weather"
    }
    val conditionDescription = if (bundle != null) {
        visualSpec.localizedSummary(language)
    } else {
        if (language == AppLanguage.GREEK) {
            "Πρόγνωση καιρού υψηλής ακρίβειας για την πόλη σας."
        } else {
            "High-precision weather forecast for your city."
        }
    }

    Card(
        modifier = modifier.testTag("hero_weather_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F223A).copy(alpha = 0.48f)),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.26f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = heroTextureRes),
                contentDescription = "Atmospheric sky texture background",
                contentScale = ContentScale.Crop,
                alpha = heroTextureAlpha,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                visualSpec.gradientTop.copy(alpha = 0.34f),
                                Color(0xFF091526).copy(alpha = 0.54f)
                            )
                        )
                    )
            )
            // Live weather condition overlay inside the Hero Card
            HeroWeatherSceneOverlay(
                visualSpec = visualSpec,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Full-width City Header Row + Live Greece Clock Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanZephyr.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.LocationOn,
                                contentDescription = "Active city",
                                tint = CyanZephyr,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cityTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_city_name_text")
                        )
                        Text(
                            text = locationSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_location_subtitle_text")
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0A172A).copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("hero_live_clock_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiveNight) WeatherIcons.NightsStay else WeatherIcons.WbSunny,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = liveClockLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (bundle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = bundle.airQuality.badgeColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, bundle.airQuality.badgeColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .clickable { onNavigateToRadar() }
                                .testTag("hero_aqi_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bundle.airQuality.badgeColor)
                                )
                                Text(
                                    text = "AQI ${bundle.airQuality.usAqi} • ${bundle.airQuality.localizedCategory(language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = temperatureDisplay,
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("hero_temperature_text")
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        val heroRainProb = bundle?.let {
                            val currentHourProb = it.hourly.firstOrNull()?.precipProbability ?: 0
                            if (it.current.weatherCode >= 51) {
                                maxOf(currentHourProb, 60)
                            } else if (currentHourProb >= 30) {
                                currentHourProb
                            } else {
                                0
                            }
                        } ?: 0

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (heroRainProb > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CyanZephyr.copy(alpha = 0.20f),
                                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.50f)),
                                    modifier = Modifier.testTag("hero_rain_percent_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = WeatherIcons.WaterDrop,
                                            contentDescription = "Rain probability",
                                            tint = CyanZephyr,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$heroRainProb%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanGlow,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.testTag("hero_condition_icon")) {
                                LottieStyleWeatherHeroBadge(
                                    visualSpec = visualSpec,
                                    triggerKey = animationTriggerKey
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = conditionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("hero_condition_label")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = feelsLikeDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanGlow,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_feels_like_text")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = conditionDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.testTag("hero_condition_description")
                )

                // Clean Hero Weather summary without embedded Live Location Weather sub-box

                if (bundle != null && bundle.rainOutlook.hasRainInNext48h) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.WaterDrop,
                            contentDescription = null,
                            tint = CyanZephyr,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = bundle.rainOutlook.nextRainOnsetLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanGlow,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays live temperature, weather conditions, and humidity based on the user's current location coordinates.
 */
@Composable
fun CurrentWeather(
    temperatureText: String,
    conditionText: String,
    humidityPercent: Int,
    latitude: Double,
    longitude: Double,
    elevationMeters: Double = 0.0,
    language: AppLanguage = AppLanguage.ENGLISH,
    visualSpec: com.example.model.WeatherVisualSpec = WmoWeatherMapper.resolve(2, isDay = true),
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val elevationBadgeLabel = remember(elevationMeters, isGreek) {
        if (elevationMeters > 0.0) {
            val elevWord = if (isGreek) "Υψόμετρο" else "Elevation"
            "$elevWord ${elevationMeters.roundToInt()} m"
        } else {
            if (isGreek) "Ενεργή Τηλεμετρία" else "Active Telemetry"
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0A1628).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_weather_composable")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live location telemetry header row (no raw GPS coordinates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = WeatherIcons.MyLocation,
                        contentDescription = null,
                        tint = EmeraldBreeze,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isGreek) "Ζωντανός Καιρός Τοποθεσίας" else "Live Location Weather",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldBreeze
                    )
                }
                Text(
                    text = elevationBadgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("current_weather_coordinates_text")
                )
            }

            // Dynamic Weather Condition Animation Banner (Sun Rays, Falling Rain, Snow, Clouds, Thunder, Stars)
            com.example.ui.components.WeatherConditionAnimation(
                visualSpec = visualSpec,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            // 3-Column Telemetry Strip: Temperature, Conditions, Humidity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Temperature
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if (isGreek) "ΘΕΡΜΟΚΡΑΣΙΑ" else "TEMPERATURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.64f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = temperatureText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberSol,
                        modifier = Modifier.testTag("current_weather_temperature_value")
                    )
                }

                // 2. Conditions
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isGreek) "ΣΥΝΘΗΚΕΣ" else "CONDITIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.64f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        AnimatedWeatherStatusIcon(
                            visualSpec = visualSpec,
                            iconSize = 18.dp,
                            contentDescription = conditionText
                        )
                        Text(
                            text = conditionText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("current_weather_condition_value")
                        )
                    }
                }

                // 3. Humidity
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (isGreek) "ΥΓΡΑΣΙΑ" else "HUMIDITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.64f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.WaterDrop,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$humidityPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanGlow,
                            modifier = Modifier.testTag("current_weather_humidity_value")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyPillItem(
    item: HourlyForecastItem,
    isSelected: Boolean,
    units: WeatherUnits,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val isNightHour = !item.isDay || (item.displayHour != "Now" && WmoWeatherMapper.isNightHourLabel(item.displayHour))
    val spec = WmoWeatherMapper.resolve(item.weatherCode, isDay = !isNightHour)
    val rainPercent = if (item.weatherCode >= 51 && item.precipProbability == 0) 60 else item.precipProbability
    val showRainPercent = rainPercent > 0 || item.weatherCode >= 51
    val hourText = if (item.displayHour == "Now" && language == AppLanguage.GREEK) "Τώρα" else item.displayHour

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) CyanZephyr.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = hourText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                softWrap = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                AnimatedWeatherStatusIcon(
                    visualSpec = spec,
                    iconSize = 22.dp,
                    contentDescription = spec.localizedLabel(language)
                )
                if (showRainPercent) {
                    Text(
                        text = "$rainPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanZephyr,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Text(
                text = units.formatTemp(item.tempC, includeUnit = false),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DailyRangeRow(
    day: DailyForecastItem,
    isToday: Boolean,
    currentTempC: Double?,
    overallMinC: Double,
    overallMaxC: Double,
    isExpanded: Boolean,
    units: WeatherUnits,
    language: AppLanguage,
    onToggleExpand: () -> Unit
) {
    val spec = WmoWeatherMapper.resolve(day.weatherCode, isDay = true)
    val rainPercent = if (day.maxPrecipProb > 0) {
        day.maxPrecipProb
    } else if (day.precipSumMm >= 0.3) {
        (day.precipSumMm * 18.0).roundToInt().coerceIn(20, 85)
    } else {
        0
    }
    val showRainPercent = rainPercent >= 10
    val localizedDay = localizeDayLabel(day.dayLabel, language)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(68.dp)) {
                Text(
                    text = localizedDay,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isToday) CyanGlow else Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = day.fullDateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier.width(74.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedWeatherStatusIcon(
                    visualSpec = spec,
                    iconSize = 22.dp,
                    contentDescription = spec.localizedLabel(language)
                )
                if (showRainPercent) {
                    Text(
                        text = "$rainPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanZephyr,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = units.formatTemp(day.minTempC, includeUnit = false),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )

            TenDayRangeBar(
                dayMinC = day.minTempC,
                dayMaxC = day.maxTempC,
                overallMinC = overallMinC,
                overallMaxC = overallMaxC,
                currentTempC = currentTempC,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = units.formatTemp(day.maxTempC, includeUnit = false),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(36.dp)
            )

            Icon(
                imageVector = if (isExpanded) WeatherIcons.ExpandLess else WeatherIcons.ExpandMore,
                contentDescription = if (isExpanded) "Collapse day details" else "Expand day details",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, spec.iconTint.copy(alpha = 0.34f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    spec.gradientTop.copy(alpha = 0.82f),
                                    spec.gradientMid.copy(alpha = 0.68f),
                                    Color(0xFF081426).copy(alpha = 0.92f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Compact inner window for the selected day's hourly forecast
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF061020).copy(alpha = 0.62f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_expanded_hourly_window")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
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
                                        AnimatedWeatherStatusIcon(
                                            visualSpec = spec,
                                            iconSize = 16.dp,
                                            contentDescription = spec.localizedLabel(language)
                                        )
                                        Text(
                                            text = if (language == AppLanguage.GREEK) {
                                                "Ωριαία Πρόγνωση • $localizedDay"
                                            } else {
                                                "Daily Hourly • $localizedDay"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = spec.iconTint.copy(alpha = 0.18f),
                                        border = BorderStroke(0.8.dp, spec.iconTint.copy(alpha = 0.40f))
                                    ) {
                                        Text(
                                            text = spec.localizedLabel(language),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.92f),
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (day.hourly.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        itemsIndexed(
                                            items = day.hourly,
                                            key = { idx, hItem -> "${hItem.isoTime}_$idx" }
                                        ) { _, hItem ->
                                            DailyCompactHourChip(
                                                item = hItem,
                                                sunsetTime = day.sunsetTime,
                                                units = units,
                                                language = language
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayDetailMiniStat(
                                label = if (language == AppLanguage.GREEK) "Αίσθηση" else "Feels H/L",
                                value = "${units.formatTemp(day.apparentMaxC, false)} / ${units.formatTemp(day.apparentMinC, false)}",
                                modifier = Modifier.weight(1f)
                            )
                            DayDetailMiniStat(
                                label = if (language == AppLanguage.GREEK) "Υετός" else "Precip Sum",
                                value = String.format(Locale.US, "%.1f mm", day.precipSumMm),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayDetailMiniStat(
                                label = if (language == AppLanguage.GREEK) "Ανατολή/Δύση" else "Sunrise / Set",
                                value = "${day.sunriseTime} • ${day.sunsetTime}",
                                modifier = Modifier.weight(1f)
                            )
                            DayDetailMiniStat(
                                label = if (language == AppLanguage.GREEK) "UV / Άνεμος" else "Peak UV / Wind",
                                value = "UV ${day.uvIndexMax.roundToInt()} • ${units.formatWind(day.windMaxKmh)}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCompactHourChip(
    item: HourlyForecastItem,
    sunsetTime: String,
    units: WeatherUnits,
    language: AppLanguage
) {
    val isNightHour = !item.isDay || WmoWeatherMapper.isNightHourLabel(item.displayHour)
    val isSunsetHour = WmoWeatherMapper.isSunsetClockTime(item.displayHour, sunsetTime)
    val spec = WmoWeatherMapper.resolve(item.weatherCode, !isNightHour, isSunsetHour)
    val precipPct = if (item.weatherCode >= 51 && item.precipProbability == 0) 65 else item.precipProbability
    val showPrecip = precipPct > 0 || item.weatherCode >= 51
    val isSnowCode = item.weatherCode in 71..77 || item.weatherCode in 85..86 || item.weatherCode in 56..57 || item.weatherCode in 66..67

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isSnowCode) Color(0xFFBAE6FD).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier.width(56.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (item.displayHour == "Now" && language == AppLanguage.GREEK) "Τώρα" else item.displayHour,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                softWrap = false
            )
            AnimatedWeatherStatusIcon(
                visualSpec = spec,
                iconSize = 18.dp,
                contentDescription = spec.localizedLabel(language)
            )
            if (showPrecip) {
                Text(
                    text = "$precipPct%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSnowCode) Color(0xFFBAE6FD) else CyanZephyr,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
            Text(
                text = units.formatTemp(item.tempC, includeUnit = false),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun localizeDayLabel(raw: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return raw
    return when (raw) {
        "Today" -> "Σήμερα"
        "Mon" -> "Δευ"
        "Tue" -> "Τρί"
        "Wed" -> "Τετ"
        "Thu" -> "Πέμ"
        "Fri" -> "Παρ"
        "Sat" -> "Σάβ"
        "Sun" -> "Κυρ"
        else -> raw
    }
}

@Composable
private fun DayDetailMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeatherLoadingSkeletonCard(
    selectedProvider: WeatherProvider,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    Card(
        modifier = modifier.testTag("weather_loading_skeleton_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F223A).copy(alpha = 0.48f)),
        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isGreek) "Συγχρονισμός ζωντανών δεδομένων καιρού..." else "Synchronizing live weather telemetry...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }
    }
}

@Composable
private fun QuickMetricBentoCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF102238).copy(alpha = 0.46f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun uvCategoryDescription(uv: Double, language: AppLanguage): String =
    if (language == AppLanguage.GREEK) {
        when {
            uv < 3.0 -> "Χαμηλό UV • Ελάχιστη προστασία"
            uv < 6.0 -> "Μέτριο UV • Γυαλιά ηλίου & SPF 30+"
            uv < 8.0 -> "Υψηλό UV • Αναζητήστε σκιά"
            else -> "Πολύ Υψηλό UV • Αποφύγετε τον ήλιο"
        }
    } else {
        when {
            uv < 3.0 -> "Low UV • Minimal protection"
            uv < 6.0 -> "Moderate UV • Wear SPF 30+"
            uv < 8.0 -> "High UV • Seek midday shade"
            else -> "Very High UV • Avoid direct sun"
        }
    }

@Composable
private fun CityElevationBottomFooterCard(
    bundle: CityWeatherBundle,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val elevMeters = bundle.elevationMeters.coerceAtLeast(1.0).roundToInt()
    val terrainZoneLabel = when {
        elevMeters >= 1100 -> if (isGreek) "Αλπική / Υψηλή Ορεινή Ζώνη" else "Alpine / High Mountain Zone"
        elevMeters >= 600 -> if (isGreek) "Ορεινή Ζώνη" else "Mountainous Zone"
        elevMeters >= 250 -> if (isGreek) "Ημιορεινή Ζώνη" else "Semi-Mountainous Zone"
        else -> if (isGreek) "Πεδινή / Παραθαλάσσια Ζώνη" else "Lowland / Coastal Zone"
    }
    val regionLabel = listOf(bundle.regionName, bundle.countryName)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")

    Card(
        modifier = modifier.testTag("city_elevation_bottom_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E2036).copy(alpha = 0.48f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyanZephyr.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = WeatherIcons.LocationOn,
                            contentDescription = if (isGreek) "Υψόμετρο" else "Elevation",
                            tint = CyanGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGreek) {
                            "ΥΨΟΜΕΤΡΟ ΤΟΠΟΘΕΣΙΑΣ • ${bundle.cityName}"
                        } else {
                            "LOCATION ELEVATION • ${bundle.cityName}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (regionLabel.isNotBlank()) "$terrainZoneLabel • $regionLabel" else terrainZoneLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldBreeze.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$elevMeters m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (isGreek) "Υψόμετρο" else "Altitude",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBreeze,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun NightSkyStarCountCard(
    bundle: CityWeatherBundle,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    // Select current night hour if currently night, or best upcoming night hour tonight if daytime
    val nightHours = remember(bundle.hourly) {
        bundle.hourly.filter { !it.isDay }
    }
    val evaluatedHour = remember(bundle.current, nightHours) {
        if (!bundle.current.isDay) {
            bundle.hourly.firstOrNull()
        } else {
            nightHours.minByOrNull { it.cloudCoverPercent + it.precipProbability } ?: bundle.hourly.firstOrNull()
        }
    }

    val cloudCoverPct = (evaluatedHour?.cloudCoverPercent ?: bundle.current.cloudCoverPercent).coerceIn(0, 100)
    val humidityPct = (evaluatedHour?.humidity ?: bundle.current.humidity).coerceIn(5, 100)
    val precipProb = (evaluatedHour?.precipProbability ?: 0).coerceIn(0, 100)
    val visibilityKm = bundle.current.visibilityKm.coerceIn(0.5, 45.0)
    val pm25 = bundle.airQuality.pm25.coerceIn(1.0, 150.0)
    val elevationM = bundle.elevationMeters.coerceAtLeast(1.0)

    // Astronomical Synodic Lunar Cycle (29.530588853 days from J2000 New Moon epoch 947182440000L)
    val moonIlluminationPct = remember(bundle.fetchedAtEpochMs) {
        val synodicMs = 29.530588853 * 86_400_000.0
        val ageRatio = (((System.currentTimeMillis() - 947182440000L) % synodicMs) + synodicMs) % synodicMs / synodicMs
        val illum = (1.0 - kotlin.math.cos(2.0 * Math.PI * ageRatio)) / 2.0
        (illum * 100.0).roundToInt().coerceIn(0, 100)
    }

    // Physical visible naked-eye star count model (hemisphere max ~3,500 stars at Bortle 1-2 alpine site)
    val starMetrics = remember(cloudCoverPct, humidityPct, precipProb, visibilityKm, pm25, elevationM, moonIlluminationPct) {
        val cloudTransmission = ((100.0 - cloudCoverPct) / 100.0).let { it * it }
        val aerosolFactor = (1.0 - (pm25 / 135.0)).coerceIn(0.38, 1.0)
        val humidityFactor = (1.0 - ((humidityPct - 40).coerceAtLeast(0) / 155.0)).coerceIn(0.52, 1.0)
        val visibilityFactor = (visibilityKm / 20.0).coerceIn(0.30, 1.08)
        val altitudeBonus = (1.0 + (elevationM / 3200.0).coerceAtMost(0.24))
        val moonlightDampening = (1.0 - (moonIlluminationPct / 100.0) * 0.34)
        val rainSuppression = if (precipProb >= 65) 0.25 else if (precipProb >= 35) 0.65 else 1.0

        val compositeClarity = (cloudTransmission * aerosolFactor * humidityFactor * visibilityFactor * moonlightDampening * rainSuppression)
            .coerceIn(0.0, 1.0)
        val visibleStars = (2950.0 * compositeClarity * altitudeBonus).roundToInt().coerceIn(0, 3500)
        val clarityScorePct = (compositeClarity * 100.0).roundToInt().coerceIn(2, 100)
        val limitingMag = (3.2 + compositeClarity * 3.1 + (elevationM / 2500.0).coerceAtMost(0.35))
            .coerceIn(2.5, 6.6)
        Triple(visibleStars, clarityScorePct, limitingMag)
    }

    val visibleStarCount = starMetrics.first
    val skyClarityPct = starMetrics.second
    val limitingMagnitude = starMetrics.third

    val qualityBadgeText = when {
        visibleStarCount >= 2100 -> if (isGreek) "Εξαιρετικός Έναστρος Ουρανός" else "Crystal Stargazing Sky"
        visibleStarCount >= 1200 -> if (isGreek) "Καλή Ορατότητα Άστρων" else "Good Star Visibility"
        visibleStarCount >= 450 -> if (isGreek) "Μερική Νέφωση / Φεγγάρι" else "Partial Clouds / Moonlight"
        else -> if (isGreek) "Χαμηλή Ορατότητα Άστρων" else "Low Star Visibility"
    }

    val bestNightWindowLabel = remember(nightHours, bundle.daily) {
        val sunset = bundle.daily.firstOrNull()?.sunsetTime ?: "20:00"
        val bestH = nightHours.minByOrNull { it.cloudCoverPercent }?.displayHour
        if (!bestH.isNullOrBlank() && bestH != "Now") "$sunset – $bestH" else "21:30 – 03:30"
    }

    Card(
        modifier = modifier.testTag("night_sky_star_count_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0C1D33).copy(alpha = 0.50f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGreek) {
                            "ΜΕΤΡΗΤΗΣ ΑΣΤΡΩΝ & ΝΥΧΤΕΡΙΝΟΣ ΟΥΡΑΝΟΣ"
                        } else {
                            "VISIBLE STAR COUNT & NIGHT SKY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = qualityBadgeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyanZephyr.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (!bundle.current.isDay) {
                            if (isGreek) "ΖΩΝΤΑΝΑ ΤΩΡΑ" else "LIVE NOW"
                        } else {
                            if (isGreek) "ΑΠΟΨΕ" else "TONIGHT"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Starfield Preview Banner + Big Visible Star Counter
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF071324),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(94.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val starSeeds = listOf(
                            Triple(0.08f, 0.24f, 2.3f),
                            Triple(0.15f, 0.68f, 1.7f),
                            Triple(0.22f, 0.35f, 2.8f),
                            Triple(0.29f, 0.18f, 1.5f),
                            Triple(0.36f, 0.52f, 2.5f),
                            Triple(0.44f, 0.28f, 2.1f),
                            Triple(0.51f, 0.74f, 1.8f),
                            Triple(0.58f, 0.22f, 3.0f),
                            Triple(0.64f, 0.58f, 2.2f),
                            Triple(0.71f, 0.32f, 2.6f),
                            Triple(0.78f, 0.72f, 1.6f),
                            Triple(0.84f, 0.26f, 2.9f),
                            Triple(0.91f, 0.54f, 2.0f),
                            Triple(0.95f, 0.19f, 1.7f)
                        )
                        val activeRatio = (visibleStarCount / 2800f).coerceIn(0.15f, 1f)
                        val drawCount = (starSeeds.size * activeRatio).roundToInt().coerceAtLeast(3)

                        // Subtle constellation lines
                        for (idx in 0 until (drawCount - 1).coerceAtMost(6)) {
                            val s1 = starSeeds[idx]
                            val s2 = starSeeds[idx + 1]
                            drawLine(
                                color = CyanGlow.copy(alpha = 0.18f * activeRatio),
                                start = Offset(s1.first * w, s1.second * h),
                                end = Offset(s2.first * w, s2.second * h),
                                strokeWidth = 1.2f
                            )
                        }

                        for (idx in 0 until drawCount) {
                            val (sx, sy, radiusPx) = starSeeds[idx]
                            val center = Offset(sx * w, sy * h)
                            drawCircle(
                                color = CyanGlow.copy(alpha = 0.24f * activeRatio),
                                radius = radiusPx * 2.8f,
                                center = center
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = (0.55f + 0.45f * activeRatio).coerceIn(0.3f, 1f)),
                                radius = radiusPx,
                                center = center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%,d", visibleStarCount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("visible_star_count_value")
                            )
                            Text(
                                text = if (isGreek) {
                                    "Ορατά άστρα με γυμνό μάτι"
                                } else {
                                    "Naked-eye visible stars"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanGlow
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$skyClarityPct%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldBreeze
                            )
                            Text(
                                text = if (isGreek) "Διαύγεια Ουρανού" else "Sky Clarity",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "Mag %.1f", limitingMagnitude),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberSol
                            )
                        }
                    }
                }
            }

            // 3 Mini Astronomy Pill Metrics: Cloud Cover, Moon Illumination, Best Viewing Window
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΝΕΦΩΣΗ" else "CLOUDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Text(
                            text = "$cloudCoverPct%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΣΕΛΗΝΗ" else "MOONLIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Text(
                            text = "$moonIlluminationPct%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberSol,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΙΔΑΝΙΚΗ ΩΡΑ" else "BEST WINDOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Text(
                            text = bestNightWindowLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLightningStormDistanceCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    // Scan the next 12 hours for thunderstorm (95,96,99) or convective rain cell
    val upcomingHours = remember(bundle.hourly) { bundle.hourly.take(12) }
    val firstThunderIdx = remember(upcomingHours) {
        upcomingHours.indexOfFirst { it.weatherCode in setOf(95, 96, 99) }
    }
    val firstConvectiveRainIdx = remember(upcomingHours) {
        upcomingHours.indexOfFirst {
            it.weatherCode in setOf(63, 65, 67, 80, 81, 82, 85, 86) ||
                it.precipProbability >= 60 ||
                it.precipMm >= 1.5
        }
    }

    val stormMetrics = remember(bundle.current, upcomingHours, firstThunderIdx, firstConvectiveRainIdx) {
        val currentCode = bundle.current.weatherCode
        val propagationSpeedKmh = (bundle.current.windSpeedKmh * 0.78).coerceIn(14.0, 58.0)
        val capeFactor = ((bundle.current.humidity / 100.0) * (bundle.current.cloudCoverPercent / 100.0)).coerceIn(0.05, 1.0)

        when {
            currentCode in setOf(95, 96, 99) || firstThunderIdx == 0 -> {
                val distKm = (2.4 + (1.0 - capeFactor) * 5.2).coerceIn(1.8, 8.5)
                val etaMin = ((distKm / propagationSpeedKmh) * 60.0).roundToInt().coerceIn(4, 18)
                val riskPct = (84 + (capeFactor * 15).roundToInt()).coerceIn(82, 99)
                Triple(distKm, etaMin, riskPct)
            }
            firstThunderIdx > 0 -> {
                val distKm = (firstThunderIdx * propagationSpeedKmh * 0.65).coerceIn(9.0, 95.0)
                val etaMin = (firstThunderIdx * 48).coerceIn(25, 480)
                val riskPct = (62 + (upcomingHours[firstThunderIdx].precipProbability / 4)).coerceIn(60, 92)
                Triple(distKm, etaMin, riskPct)
            }
            firstConvectiveRainIdx >= 0 -> {
                val hIdx = firstConvectiveRainIdx.coerceAtLeast(1)
                val distKm = (hIdx * propagationSpeedKmh * 0.72 + 11.0).coerceIn(12.0, 120.0)
                val etaMin = (hIdx * 50).coerceIn(30, 540)
                val riskPct = (28 + (upcomingHours[firstConvectiveRainIdx].precipProbability / 3)).coerceIn(24, 58)
                Triple(distKm, etaMin, riskPct)
            }
            else -> {
                // Clear / no storm cell in 120 km radius
                val clearRadiusKm = 120.0
                val riskPct = (4 + (capeFactor * 14).roundToInt()).coerceIn(3, 18)
                Triple(clearRadiusKm, -1, riskPct)
            }
        }
    }

    val strikeDistanceKm = stormMetrics.first
    val stormEtaMinutes = stormMetrics.second
    val lightningRiskPct = stormMetrics.third
    val isStormDetected = stormEtaMinutes > 0
    val isActiveThunder = bundle.current.weatherCode in setOf(95, 96, 99) || firstThunderIdx == 0

    val statusBadgeText = when {
        isActiveThunder -> if (isGreek) "ΕΝΕΡΓΟΙ ΚΕΡΑΥΝΟΙ" else "ACTIVE LIGHTNING"
        firstThunderIdx > 0 -> if (isGreek) "Πλησιάζει Καταιγίδα" else "Storm Approaching"
        isStormDetected -> if (isGreek) "Πυρήνας Βροχής στο Ραντάρ" else "Rain Cell on Radar"
        else -> if (isGreek) "Καθαρή Ακτίνα 120 km" else "120 km Radius Clear"
    }

    val statusAccent = when {
        isActiveThunder -> Color(0xFFFF6B6B)
        firstThunderIdx > 0 -> AmberSol
        isStormDetected -> CyanZephyr
        else -> EmeraldBreeze
    }

    val etaCountdownText = remember(stormEtaMinutes, isGreek) {
        when {
            stormEtaMinutes <= 0 -> if (isGreek) "Κανένα μέτωπο" else "No active front"
            stormEtaMinutes < 60 -> if (isGreek) "Σε $stormEtaMinutes λεπτά" else "In $stormEtaMinutes min"
            else -> {
                val hrs = stormEtaMinutes / 60
                val mins = stormEtaMinutes % 60
                if (isGreek) "Σε ${hrs}ώ ${mins}λ" else "In ${hrs}h ${mins}m"
            }
        }
    }

    // Approach direction from wind vector (storms travel along the wind vector from the upwind quadrant)
    val approachVectorLabel = remember(bundle.current.windCardinal, bundle.current.windSpeedKmh, units) {
        "${bundle.current.windCardinal} • ${units.formatWind((bundle.current.windSpeedKmh * 0.78).coerceAtLeast(12.0))}"
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onNavigateToRadar)
            .testTag("live_lightning_storm_distance_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E2138).copy(alpha = 0.50f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    statusAccent.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.24f),
                    CyanZephyr.copy(alpha = 0.36f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.48f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.Thunderstorm,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) {
                                "ΑΠΟΣΤΑΣΗ ΚΕΡΑΥΝΩΝ & ΑΦΙΞΗ ΚΑΤΑΙΓΙΔΑΣ"
                            } else {
                                "LIGHTNING DISTANCE & STORM ETA"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isStormDetected) {
                                if (isGreek) {
                                    "Πλησιέστερος πυρήνας στα ${String.format(Locale.US, "%.1f", strikeDistanceKm)} km"
                                } else {
                                    "Nearest cell at ${String.format(Locale.US, "%.1f", strikeDistanceKm)} km"
                                }
                            } else {
                                if (isGreek) {
                                    "Καμία ηλεκτρική δραστηριότητα κοντά σας"
                                } else {
                                    "No lightning strikes detected nearby"
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusAccent.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.50f))
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 3 Glass Telemetry Cells: Strike Distance, Storm ETA Countdown, & Lightning Risk / Drift Vector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΑΠΟΣΤΑΣΗ" else "DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isStormDetected) {
                                String.format(Locale.US, "%.1f km", strikeDistanceKm)
                            } else {
                                "> 120 km"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.testTag("lightning_distance_value")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΕΚΤΙΜΩΜΕΝΗ ΑΦΙΞΗ" else "STORM ETA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = etaCountdownText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusAccent,
                            maxLines = 1,
                            modifier = Modifier.testTag("storm_eta_value")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΚΙΝΔΥΝΟΣ / ΤΡΟΧΙΑ" else "RISK & DRIFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$lightningRiskPct% • ${bundle.current.windCardinal}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGreek) {
                        "Ταχύτητα μετώπου: $approachVectorLabel"
                    } else {
                        "Cell drift vector: $approachVectorLabel"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = if (isGreek) "Άνοιγμα Ραντάρ →" else "Open Live Radar →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanGlow
                )
            }
        }
    }
}

@Composable
private fun ClothingOutdoorActivityAdvisorCard(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val feelsC = bundle.current.feelsLikeC
    val windKmh = bundle.current.windSpeedKmh
    val uv = bundle.current.uvIndex
    val aqi = bundle.airQuality.europeanAqi

    // Evaluate rain probability over the next 6 hours
    val next6hPeakRainProb = remember(bundle.hourly) {
        bundle.hourly.take(6).maxOfOrNull { it.precipProbability } ?: 0
    }
    val isCurrentlyRaining = bundle.current.weatherCode in setOf(51, 53, 55, 61, 63, 65, 67, 80, 81, 82, 95, 96, 99) ||
        bundle.current.precipitationMm >= 0.4

    // 1. Outfit Recommendation
    val outfitTitle = when {
        feelsC <= 3.0 -> if (isGreek) "Βαρύ Χειμερινό Μπουφάν & Γάντια" else "Heavy Winter Coat & Thermal Layers"
        feelsC <= 11.0 -> if (isGreek) "Ζεστό Μπουφάν ή Παλτό" else "Warm Jacket or Insulated Coat"
        feelsC <= 17.5 -> if (isGreek) "Ελαφρύ Μπουφάν ή Φούτερ" else "Light Jacket or Hoodie"
        feelsC <= 24.5 -> if (isGreek) "Άνετο T-Shirt / Ελαφρύ Πουκάμισο" else "Breathable T-Shirt & Light Layers"
        else -> if (isGreek) "Καλοκαιρινά Δροσερά Ρούχα" else "Light Summer Breathable Clothing"
    }

    // 2. Umbrella / Rain Gear Badge
    val umbrellaAdvice = when {
        isCurrentlyRaining || next6hPeakRainProb >= 65 -> if (isGreek) "Απαραίτητη Ομπρέλα ($next6hPeakRainProb%)" else "Bring Umbrella ($next6hPeakRainProb%)"
        next6hPeakRainProb >= 35 -> if (isGreek) "Πιθανή Ψιχάλα • Αδιάβροχο" else "Light Rain Chance • Pack Shell"
        else -> if (isGreek) "Δεν Χρειάζεται Ομπρέλα" else "No Umbrella Needed"
    }

    // 3. UV & Wind Protection Badge
    val uvProtectionAdvice = when {
        uv >= 6.0 -> if (isGreek) "Γυαλιά Ηλίου & Αντηλιακό SPF 50" else "Sunglasses & SPF 50 Sunscreen"
        uv >= 3.0 -> if (isGreek) "Γυαλιά Ηλίου & SPF 30" else "UV Sunglasses & SPF 30"
        windKmh >= 32.0 -> if (isGreek) "Αντιανεμικό (${units.formatWind(windKmh)})" else "Windbreaker (${units.formatWind(windKmh)})"
        else -> if (isGreek) "Χαμηλό UV • Άνετες Συνθήκες" else "Low UV • Comfortable Outdoors"
    }

    // 4. Find Best Outdoor Running / Walking Hour in the next 12 hours
    val bestOutdoorHour = remember(bundle.hourly) {
        bundle.hourly.take(12).minByOrNull { h ->
            val tempPenalty = kotlin.math.abs(h.feelsLikeC - 18.5) * 2.2
            val rainPenalty = h.precipProbability * 1.4
            val windPenalty = (h.windSpeedKmh - 18.0).coerceAtLeast(0.0) * 1.1
            tempPenalty + rainPenalty + windPenalty
        }
    }

    val bestActivityWindowText = remember(bestOutdoorHour, units, isGreek) {
        if (bestOutdoorHour != null) {
            val hLabel = if (bestOutdoorHour.displayHour == "Now" && isGreek) "Τώρα" else bestOutdoorHour.displayHour
            "$hLabel (${units.formatTemp(bestOutdoorHour.tempC)})"
        } else {
            if (isGreek) "Τώρα" else "Now"
        }
    }

    val outdoorScorePct = remember(feelsC, next6hPeakRainProb, windKmh, aqi) {
        val tempScore = (100.0 - kotlin.math.abs(feelsC - 19.0) * 2.8).coerceIn(25.0, 100.0)
        val rainFactor = (1.0 - (next6hPeakRainProb / 145.0)).coerceIn(0.25, 1.0)
        val windFactor = (1.0 - ((windKmh - 22.0).coerceAtLeast(0.0) / 85.0)).coerceIn(0.45, 1.0)
        val aqiFactor = (1.0 - ((aqi - 40).coerceAtLeast(0) / 220.0)).coerceIn(0.50, 1.0)
        (tempScore * rainFactor * windFactor * aqiFactor).roundToInt().coerceIn(18, 99)
    }

    Card(
        modifier = modifier.testTag("clothing_outdoor_advisor_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E2138).copy(alpha = 0.50f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    EmeraldBreeze.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.25f),
                    AmberSol.copy(alpha = 0.40f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldBreeze.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.48f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.WbSunny,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) {
                                "ΣΥΜΒΟΥΛΟΣ ΕΝΔΥΣΗΣ & ΕΞΩΤΕΡΙΚΩΝ ΔΡΑΣΤΗΡΙΟΤΗΤΩΝ"
                            } else {
                                "CLOTHING & OUTDOOR ACTIVITY ADVISOR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldBreeze,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = outfitTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldBreeze.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (isGreek) "Άνεση $outdoorScorePct%" else "Comfort $outdoorScorePct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 3 Quick Glass Advisor Badges: Umbrella, UV/Wind Gear, Best Walk/Run Hour
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΟΜΠΡΕΛΑ" else "UMBRELLA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = umbrellaAdvice,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentlyRaining || next6hPeakRainProb >= 55) CyanGlow else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΠΡΟΣΤΑΣΙΑ" else "UV & WIND",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uvProtectionAdvice,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberSol,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGreek) "ΙΔΑΝΙΚΗ ΩΡΑ ΕΞΩ" else "BEST OUTDOOR HR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bestActivityWindowText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldBreeze,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("best_outdoor_hour_value")
                        )
                    }
                }
            }
        }
    }
}
