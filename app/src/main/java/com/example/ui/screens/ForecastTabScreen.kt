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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.M3BodyText
import com.example.ui.theme.M3CardSurface
import com.example.ui.theme.M3CardSurfaceElevated
import com.example.ui.theme.M3IconTint
import com.example.ui.theme.M3PeriwinkleAction
import com.example.ui.theme.M3PillSelectedContainer
import com.example.ui.theme.M3PillUnselectedContainer
import com.example.ui.theme.M3SubtleBorder
import com.example.ui.theme.M3TitleText
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
    var selectedForecastPillTab by remember { mutableStateOf(0) } // 0 = Recommended, 1 = All weather

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = M3CardSurface,
                color = M3PeriwinkleAction,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            // 1. Swipeable Hero Weather Pager (Rich v1.3.1 Glassmorphic Hero with Sky Scene & Animated Weather Icon)
            item(key = "hero_weather_pager_section", contentType = "hero_pager") {
                SwipeableCityHeroPager(
                    bundle = bundle,
                    savedCities = savedCities,
                    selectedCityId = selectedCityId,
                    selectedProvider = selectedProvider,
                    units = units,
                    language = language,
                    skyAtmospherePreset = skyAtmospherePreset,
                    animationTriggerKey = animationTriggerKey,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onSelectCity = onSelectCity,
                    onNavigateToRadar = onNavigateToRadar,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1b. Severe Weather Alert Cards (only when a real severe alert is active)
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

            // 2. 10-Day Temperature Range Bar Forecast Card (First)
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
                        containerColor = Color(0xFF0E223A).copy(alpha = 0.56f)
                    ),
                    border = BorderStroke(
                        width = 1.1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AmberSol.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.24f),
                                CyanZephyr.copy(alpha = 0.45f)
                            )
                        )
                    )
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
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (language == AppLanguage.GREEK) "Πατήστε για λεπτομέρειες" else "Tap day for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow,
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

            // 3. Interactive 24-Hour Curve & Hourly Scrubber Card (Second)
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
                        containerColor = Color(0xFF0E223A).copy(alpha = 0.56f)
                    ),
                    border = BorderStroke(
                        width = 1.1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CyanZephyr.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.24f),
                                AmberSol.copy(alpha = 0.40f)
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (language == AppLanguage.GREEK) "Ωριαία Πρόγνωση 24 Ωρών" else "24-Hour Horizon Curve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (inspectedHour != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            val feelsWord = if (language == AppLanguage.GREEK) "Αίσθηση" else "Feels"
                            Text(
                                text = "${inspectedHour.displayHour}: ${units.formatTemp(inspectedHour.tempC)} ($feelsWord ${units.formatTemp(inspectedHour.feelsLikeC)}) • ${units.formatWind(inspectedHour.windSpeedKmh)}",
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
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanZephyr.copy(alpha = 0.28f),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        labelColor = Color.White.copy(alpha = 0.78f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color.White.copy(alpha = 0.16f),
                                        selectedBorderColor = CyanGlow
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

            // 4. 2-Column Material You Environmental Bento Cards (UV Index, Wind & Gusts, Humidity & Dew, Pressure & Visibility)
            item(key = "environmental_bento_grid", contentType = "environmental_bento") {
                var selectedMetricTab by remember { mutableIntStateOf(0) }
                val isGreek = language == AppLanguage.GREEK
                val peakUv = bundle.daily.firstOrNull()?.uvIndexMax ?: bundle.current.uvIndex

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("forecast_environmental_bento_section"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialYouSegmentedPillBar(
                        leftLabel = if (isGreek) "Προτεινόμενα" else "Recommended",
                        rightLabel = if (isGreek) "Όλες οι μετρήσεις" else "All metrics",
                        selectedIndex = selectedMetricTab,
                        onSelectIndex = { selectedMetricTab = it },
                        leftTag = "forecast_pill_recommended",
                        rightTag = "forecast_pill_all_metrics"
                    )

                    // Row 1: UV Index (Amber Gold) & Wind & Gusts (Sky Cyan)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MaterialYouBentoServiceCard(
                            icon = WeatherIcons.WbSunny,
                            accent = AmberSol,
                            title = if (isGreek) "Δείκτης\nUV" else "UV\nIndex",
                            description = if (isGreek) {
                                "Δείκτης UV ${String.format(Locale.US, "%.1f", bundle.current.uvIndex)} (Μέγιστο ${String.format(Locale.US, "%.1f", peakUv)}). ${uvCategoryDescription(bundle.current.uvIndex, language)}."
                            } else {
                                "UV ${String.format(Locale.US, "%.1f", bundle.current.uvIndex)} right now (Peak ${String.format(Locale.US, "%.1f", peakUv)} today). ${uvCategoryDescription(bundle.current.uvIndex, language)}."
                            },
                            actionLabel = if (isGreek) "Έλεγχος UV" else "Check UV",
                            onCardClick = onNavigateToRadar,
                            onActionClick = onNavigateToRadar,
                            testTag = "bento_metric_uv_index",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        MaterialYouBentoServiceCard(
                            icon = WeatherIcons.Air,
                            accent = CyanZephyr,
                            title = if (isGreek) "Άνεμος &\nΡιπές" else "Wind &\nGusts",
                            description = if (isGreek) {
                                "Άνεμος ${units.formatWind(bundle.current.windSpeedKmh)} από ${bundle.current.windCardinal} (${bundle.current.windDirectionDeg}°) με ριπές έως ${units.formatWind(bundle.current.windGustsKmh)}."
                            } else {
                                "${units.formatWind(bundle.current.windSpeedKmh)} sustained wind from ${bundle.current.windCardinal} (${bundle.current.windDirectionDeg}°) with gusts up to ${units.formatWind(bundle.current.windGustsKmh)}."
                            },
                            actionLabel = if (isGreek) "Ραντάρ" else "Open radar",
                            onCardClick = onNavigateToRadar,
                            onActionClick = onNavigateToRadar,
                            testTag = "bento_metric_wind_gusts",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Row 2: Humidity & Dew Point (Emerald Green) & Pressure & Visibility (Cyan Glow)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MaterialYouBentoServiceCard(
                            icon = WeatherIcons.WaterDrop,
                            accent = EmeraldBreeze,
                            title = if (isGreek) "Υγρασία &\nΔρόσος" else "Humidity &\nDew Point",
                            description = if (isGreek) {
                                "Σχετική υγρασία ${bundle.current.humidity}% με σημείο δρόσου ${units.formatTemp(bundle.current.dewPointC)} και ισορροπημένη ατμόσφαιρα."
                            } else {
                                "${bundle.current.humidity}% relative air humidity with a ${units.formatTemp(bundle.current.dewPointC)} dew point and balanced atmospheric moisture."
                            },
                            actionLabel = if (isGreek) "Χάρτης" else "View map",
                            onCardClick = onNavigateToRadar,
                            onActionClick = onNavigateToRadar,
                            testTag = "bento_metric_humidity_dew",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        MaterialYouBentoServiceCard(
                            icon = WeatherIcons.Speed,
                            accent = CyanGlow,
                            title = if (isGreek) "Πίεση &\nΟρατότητα" else "Pressure &\nVisibility",
                            description = if (isGreek) {
                                "Βαρομετρική πίεση ${units.formatPressure(bundle.current.pressureHpa)} με ορατότητα ${bundle.current.visibilityKm.roundToInt()} km και νέφωση ${bundle.current.cloudCoverPercent}%."
                            } else {
                                "${units.formatPressure(bundle.current.pressureHpa)} barometric pressure with ${bundle.current.visibilityKm.roundToInt()} km visibility and ${bundle.current.cloudCoverPercent}% cloud cover."
                            },
                            actionLabel = if (isGreek) "Λεπτομέρειες" else "Details",
                            onCardClick = onNavigateToRadar,
                            onActionClick = onNavigateToRadar,
                            testTag = "bento_metric_pressure_visibility",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Optional Row 3 when "All metrics" pill is selected: Air Quality (AQI) & Feels Like / Cloud Cover
                    if (selectedMetricTab == 1) {
                        val todayPrecipProb = bundle.rainOutlook.peakPrecipProbability
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MaterialYouBentoServiceCard(
                                icon = WeatherIcons.CompassCalibration,
                                accent = Color(0xFFA5B4FC),
                                title = if (isGreek) "Ποιότητα\nΑέρα AQI" else "Air Quality\n& PM2.5",
                                description = if (isGreek) {
                                    "Δείκτης AQI ${bundle.airQuality.europeanAqi} (${bundle.airQuality.localizedCategory(language)}) • PM2.5 ${String.format(Locale.US, "%.1f", bundle.airQuality.pm25)} μg/m³ & PM10 ${String.format(Locale.US, "%.1f", bundle.airQuality.pm10)} μg/m³."
                                } else {
                                    "European AQI ${bundle.airQuality.europeanAqi} (${bundle.airQuality.localizedCategory(language)}) • PM2.5 ${String.format(Locale.US, "%.1f", bundle.airQuality.pm25)} μg/m³ & PM10 ${String.format(Locale.US, "%.1f", bundle.airQuality.pm10)} μg/m³."
                                },
                                actionLabel = if (isGreek) "Ανάλυση" else "Continue",
                                onCardClick = onNavigateToRadar,
                                onActionClick = onNavigateToRadar,
                                testTag = "bento_metric_air_quality",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            MaterialYouBentoServiceCard(
                                icon = WeatherIcons.Cloud,
                                accent = CoralWarmth,
                                title = if (isGreek) "Αίσθηση &\nΝεφώσεις" else "Feels Like\n& Clouds",
                                description = if (isGreek) {
                                    "Πραγματική αίσθηση ${units.formatTemp(bundle.current.feelsLikeC)} με κάλυψη νεφών ${bundle.current.cloudCoverPercent}% και πιθανότητα υετού $todayPrecipProb%."
                                } else {
                                    "Apparent temperature feels like ${units.formatTemp(bundle.current.feelsLikeC)} with ${bundle.current.cloudCoverPercent}% cloud cover and $todayPrecipProb% rain chance."
                                },
                                actionLabel = if (isGreek) "Πρόγνωση" else "Set up",
                                onCardClick = onNavigateToRadar,
                                onActionClick = onNavigateToRadar,
                                testTag = "bento_metric_feels_like",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // 5. Sunset & Solar Horizon Trajectory Arc Card (kept in its dedicated arc layout)
            item(key = "circular_observatory_section", contentType = "solar_horizon") {
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
            !bundle.current.isDay || WmoWeatherMapper.isNightHourLabel(liveClockLabel)
        } else {
            WmoWeatherMapper.isNightHourLabel(liveClockLabel)
        }
    }

    val visualSpec = remember(bundle?.current?.weatherCode, isLiveNight, isLiveSunset, skyAtmospherePreset) {
        skyAtmospherePreset.resolveVisualSpec(
            liveCode = bundle?.current?.weatherCode ?: 2,
            liveIsDay = !isLiveNight,
            liveIsSunset = isLiveSunset
        )
    }

    val isNightHeroScene = visualSpec.isNight || visualSpec.particleEffect == SkyParticleEffect.STARFIELD
    val heroTextureRes = when {
        visualSpec.particleEffect == SkyParticleEffect.SUNSET_DUSK -> R.drawable.img_sunset_dusk
        isNightHeroScene -> R.drawable.img_night_moon
        else -> R.drawable.img_atmosphere_hero
    }
    val heroTextureAlpha = when {
        visualSpec.particleEffect == SkyParticleEffect.SUNSET_DUSK -> 0.50f
        isNightHeroScene -> 0.56f
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.56f)),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    CyanZephyr.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.28f),
                    AmberSol.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = heroTextureRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = heroTextureAlpha,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
            )
            HeroWeatherSceneOverlay(
                visualSpec = visualSpec,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A192F).copy(alpha = 0.35f),
                                Color(0xFF0A192F).copy(alpha = 0.62f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Full-width City Header Row + Live Greece Clock Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanZephyr.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.55f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.LocationOn,
                                contentDescription = "Active city",
                                tint = CyanGlow,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cityTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
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
                        color = Color(0xFF0F2847).copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("hero_live_clock_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                fontWeight = FontWeight.Bold,
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
                            color = Color(0xFF0F2847).copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, bundle.airQuality.badgeColor.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clickable { onNavigateToRadar() }
                                .testTag("hero_aqi_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    fontWeight = FontWeight.SemiBold,
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
                            fontWeight = FontWeight.ExtraBold,
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
                            if (it.current.weatherCode >= 51 && it.current.precipitationMm >= 0.20) {
                                maxOf(currentHourProb, 60)
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
                                    color = CyanZephyr.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.55f)),
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
                                            tint = CyanGlow,
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
                            fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.SemiBold,
                    color = CyanGlow,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_feels_like_text")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = conditionDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("hero_condition_description")
                )
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
        color = Color(0xFF0E223A).copy(alpha = 0.56f),
        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.40f)),
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
                        tint = CyanGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isGreek) "Ζωντανός Καιρός Τοποθεσίας" else "Live Location Weather",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }
                Text(
                    text = elevationBadgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
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

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

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
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = temperatureText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
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
                        color = Color.White.copy(alpha = 0.72f)
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
                        color = Color.White.copy(alpha = 0.72f)
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
    val rainPercent = if (item.weatherCode >= 51 && item.precipMm >= 0.20) {
        if (item.precipProbability == 0) 60 else item.precipProbability
    } else {
        0
    }
    val showRainPercent = rainPercent >= 50
    val hourText = if (item.displayHour == "Now" && language == AppLanguage.GREEK) "Τώρα" else item.displayHour

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) CyanZephyr.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(22.dp))
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
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
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Text(
                text = units.formatTemp(item.tempC, includeUnit = false),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
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
    val rainPercent = if (day.weatherCode >= 51 && day.precipSumMm >= 0.50) {
        if (day.maxPrecipProb > 0) day.maxPrecipProb else 60
    } else {
        0
    }
    val showRainPercent = rainPercent >= 50
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
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isToday) AmberSol else Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = day.fullDateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.68f),
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
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = units.formatTemp(day.minTempC, includeUnit = false),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
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
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(36.dp)
            )

            Icon(
                imageVector = if (isExpanded) WeatherIcons.ExpandLess else WeatherIcons.ExpandMore,
                contentDescription = if (isExpanded) "Collapse day details" else "Expand day details",
                tint = CyanGlow,
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
                color = Color(0xFF0A192F).copy(alpha = 0.65f),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
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
                        color = Color(0xFF0F223A).copy(alpha = 0.72f),
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
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CyanGlow.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.50f))
                                ) {
                                    Text(
                                        text = spec.localizedLabel(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanGlow,
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
    val precipPct = if (item.weatherCode >= 51 && item.precipMm >= 0.20) {
        if (item.precipProbability == 0) 65 else item.precipProbability
    } else {
        0
    }
    val showPrecip = precipPct >= 50

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A192F).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
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
                    color = CyanGlow,
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
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E223A).copy(alpha = 0.54f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 2,
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

