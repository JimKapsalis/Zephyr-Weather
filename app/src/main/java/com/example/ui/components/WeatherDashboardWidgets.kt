package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SavedCityEntity
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.WeatherUnits
import com.example.model.WmoWeatherMapper
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Home screen city search bar and quick city switcher placed right at the top of the
 * Forecast Dashboard so users can search for their city (e.g. Λάρισα, Αθήνα, Θεσσαλονίκη)
 * or trigger GPS directly from the home screen.
 */
@Composable
fun HomeCitySearchAndQuickSwitchCard(
    savedCities: List<SavedCityEntity>,
    selectedCityId: Long,
    searchQuery: String,
    searchResults: List<SavedCityEntity>,
    isSearching: Boolean,
    isLocatingGps: Boolean,
    units: WeatherUnits,
    language: AppLanguage,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (SavedCityEntity) -> Unit,
    onAddAndSelectCity: (SavedCityEntity) -> Unit,
    onRequestGpsLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_city_search_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input Row on the Home Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = {
                    Text(
                        text = if (isGreek) "Αναζήτηση πόλης" else "Search city",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Surface(
                        shape = CircleShape,
                        color = CyanZephyr.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WeatherIcons.Search,
                                contentDescription = if (isGreek) "Αναζήτηση πόλης" else "Search city",
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = CyanZephyr,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = WeatherIcons.Close,
                                contentDescription = if (isGreek) "Καθαρισμός" else "Clear",
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isLocatingGps) {
                        CircularProgressIndicator(
                            color = EmeraldBreeze,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        IconButton(onClick = onRequestGpsLocation) {
                            Icon(
                                imageVector = WeatherIcons.MyLocation,
                                contentDescription = if (isGreek) "Τοποθεσία GPS" else "GPS Location",
                                tint = CyanZephyr.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.94f),
                    unfocusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.86f),
                    focusedBorderColor = CyanZephyr,
                    unfocusedBorderColor = CyanZephyr.copy(alpha = 0.32f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanZephyr
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_city_search_input")
            )
        }

        // Autocomplete Dropdown Results when searching on the Home Screen
        AnimatedVisibility(
            visible = searchResults.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_search_results_dropdown"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0E2138).copy(alpha = 0.96f)
                ),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    searchResults.take(6).forEachIndexed { idx, resultCity ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddAndSelectCity(resultCity) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("home_search_result_${resultCity.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIcons.LocationOn,
                                    contentDescription = null,
                                    tint = CyanZephyr,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resultCity.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val elevLabel = if (resultCity.elevationMeters > 0.0) {
                                        if (isGreek) " • Υψόμετρο ${resultCity.elevationMeters.roundToInt()} m" else " • Elev ${resultCity.elevationMeters.roundToInt()} m"
                                    } else ""
                                    Text(
                                        text = "${resultCity.regionName}, ${resultCity.countryName}$elevLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CyanZephyr.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (isGreek) "Επιλογή" else "Select",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick City Chips Row
        if (savedCities.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("home_saved_cities_chips_row")
            ) {
                items(savedCities, key = { it.id }) { city ->
                    val isSelected = city.id == selectedCityId
                    val spec = WmoWeatherMapper.resolve(city.cachedWeatherCode, city.cachedIsDay)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            CyanZephyr.copy(alpha = 0.26f)
                        } else {
                            Color(0xFF102238).copy(alpha = 0.80f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.4.dp else 1.dp,
                            color = if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectCity(city) }
                            .testTag("home_city_chip_${city.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (city.isCurrentGps) {
                                Icon(
                                    imageVector = WeatherIcons.MyLocation,
                                    contentDescription = if (isGreek) "Τρέχουσα τοποθεσία" else "Current location",
                                    tint = EmeraldBreeze,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                tint = spec.resolvedIconTint(),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = units.formatTemp(city.cachedTempC, includeUnit = false),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Showcase section containing 3 distinct, custom glassmorphic Weather Widgets
 * on the main Forecast Dashboard:
 * 1. Live Clock, Hourly & Daily Weather Widget (Ώρα + Καιρός ανά Ώρα + Καιρός ανά Ημέρα)
 * 2. 24h Helios Chrono-Dial & Comfort Advisor Widget (Ωρολόγιο Ηλίου & Άνεση Ημέρας)
 * 3. Analog Barometer & 6h Rain/Temp Timeline Widget (Βαρόμετρο & Ωριαία Βροχή)
 */
@Composable
fun WeatherWidgetsShowcaseSection(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_widgets_showcase_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    imageVector = WeatherIcons.AutoAwesome,
                    contentDescription = null,
                    tint = AmberSol,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = if (isGreek) {
                        "Widgets Ώρας & Καιρού (${bundle.cityName})"
                    } else {
                        "Clock, Hourly & Daily Widgets (${bundle.cityName})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = CyanZephyr.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("widgets_background_sync_badge")
            ) {
                Text(
                    text = if (isGreek) "ΩΡΑ • ΑΝΑ ΩΡΑ • ΑΝΑ ΜΕΡΑ" else "TIME • HOURLY • DAILY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanGlow,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        // WIDGET 1: Master Clock + Weather by Hour + Weather by Day Widget
        Helios24hChronoDialWidget(
            bundle = bundle,
            units = units,
            language = language,
            onClick = onNavigateToRadar
        )

        // WIDGET 2: Smart Daily Comfort, 24h Dial & Multi-Day Outlook Widget
        SmartComfortAndOutfitWidget(
            bundle = bundle,
            units = units,
            language = language
        )

        // WIDGET 3: Analog Barometric Pressure & 6-Hour Rain/Temp Pulse Widget
        AnalogBarometerAndRainPulseWidget(
            bundle = bundle,
            units = units,
            language = language,
            onClick = onNavigateToRadar
        )
    }
}

/**
 * WIDGET 1: Signature Clock + Weather by Hour (ανά ώρα) + Weather by Day (ανά μέρα) Widget.
 */
@Composable
fun Helios24hChronoDialWidget(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val spec = WmoWeatherMapper.resolve(bundle.current.weatherCode, bundle.current.isDay)
    val today = bundle.daily.firstOrNull()
    val dateLabel = today?.fullDateLabel ?: ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .testTag("widget_helios_chrono_dial"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A192E).copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    CyanZephyr.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.24f),
                    AmberSol.copy(alpha = 0.50f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyanZephyr.copy(alpha = 0.18f),
                            Color(0xFF0E233F).copy(alpha = 0.65f),
                            AmberSol.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Live Digital Clock & Date on Left, Current City & Temp + Realistic Multi-Tone Weather Icon on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CyanZephyr.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (isGreek) "WIDGET ΩΡΑΣ • ΑΝΑ ΩΡΑ & ΗΜΕΡΑ" else "CLOCK • HOURLY & DAILY WIDGET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = bundle.currentLocalTimeLabel,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${bundle.cityName} • $dateLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanGlow,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }

                // Current Condition & Temp Glass Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                spec.iconTint.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = spec.localizedLabel(language),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = units.formatTemp(bundle.current.tempC),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = spec.localizedLabel(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Section 1: Weather by Hour (Καιρός ανά Ώρα - Next 6 Hours)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGreek) "ΚΑΙΡΟΣ ΑΝΑ ΩΡΑ" else "WEATHER BY HOUR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberSol
                    )
                    Text(
                        text = if (isGreek) "Επόμενες 6 ώρες" else "Next 6 hours",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bundle.hourly.take(6).forEachIndexed { idx, hItem ->
                        val hSpec = WmoWeatherMapper.resolve(hItem.weatherCode, hItem.isDay)
                        val hourTitle = if (idx == 0) {
                            if (isGreek) "Τώρα" else "Now"
                        } else {
                            hItem.displayHour
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (idx == 0) CyanZephyr.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                width = if (idx == 0) 1.3.dp else 1.dp,
                                color = if (idx == 0) CyanGlow.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = hourTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (idx == 0) CyanGlow else Color.White.copy(alpha = 0.82f),
                                    fontWeight = if (idx == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = hSpec.icon,
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(21.dp)
                                )
                                Text(
                                    text = units.formatTemp(hItem.tempC, includeUnit = false),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${hItem.precipProbability}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hItem.precipProbability >= 35) CyanGlow else Color.White.copy(alpha = 0.56f),
                                    fontWeight = if (hItem.precipProbability >= 35) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Section 2: Weather by Day (Καιρός ανά Ημέρα - 5 Days)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGreek) "ΚΑΙΡΟΣ ΑΝΑ ΗΜΕΡΑ" else "WEATHER BY DAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                    Text(
                        text = if (isGreek) "Πρόγνωση 5 Ημερών" else "5-Day Outlook",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bundle.daily.take(5).forEachIndexed { idx, dItem ->
                        val dSpec = WmoWeatherMapper.resolve(dItem.weatherCode, isDay = true)
                        val dayTitle = localizeWidgetDay(dItem.dayLabel, isGreek, idx == 0)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (idx == 0) AmberSol.copy(alpha = 0.19f) else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                width = if (idx == 0) 1.3.dp else 1.dp,
                                color = if (idx == 0) AmberSol.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dayTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (idx == 0) AmberSol else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = dSpec.icon,
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(21.dp)
                                )
                                Text(
                                    text = "${units.formatTemp(dItem.maxTempC, false)}/${units.formatTemp(dItem.minTempC, false)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun localizeWidgetDay(raw: String, isGreek: Boolean, isToday: Boolean): String {
    if (!isGreek) return if (isToday) "Today" else raw
    if (isToday) return "Σήμερα"
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
fun SmartComfortAndOutfitWidget(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val tempC = bundle.current.tempC
    val rainProb = bundle.rainOutlook.peakPrecipProbability
    val windKmh = bundle.current.windSpeedKmh

    // Compute live comfort score (0..100)
    val tempPenalty = kotlin.math.abs(tempC - 21.0) * 2.2
    val rainPenalty = rainProb * 0.25
    val windPenalty = (windKmh - 12.0).coerceAtLeast(0.0) * 0.6
    val comfortScore = (100.0 - tempPenalty - rainPenalty - windPenalty).roundToInt().coerceIn(38, 99)

    val outfitSuggestion = when {
        rainProb >= 50 -> if (isGreek) "Ομπρέλα & Αδιάβροχο" else "Umbrella & Waterproof"
        tempC < 12.0 -> if (isGreek) "Ζεστό Παλτό / Μπουφάν" else "Warm Coat & Layers"
        tempC < 19.0 -> if (isGreek) "Ελαφρύ Μπουφάν / Ζακέτα" else "Light Jacket / Hoodie"
        else -> if (isGreek) "Άνετα Ρούχα & Γυαλιά Ηλίου" else "Light Tee & Sunglasses"
    }

    val outdoorStatus = when {
        rainProb >= 60 -> if (isGreek) "Προτιμήστε εσωτερικούς χώρους" else "Prefer indoor venues"
        comfortScore >= 80 -> if (isGreek) "Ιδανικός καιρός για βόλτα & καφέ" else "Ideal for outdoor walk & coffee"
        else -> if (isGreek) "Καλές συνθήκες για μετακίνηση" else "Good conditions for commuting"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("widget_smart_comfort_outfit"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A192E).copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    EmeraldBreeze.copy(alpha = 0.65f),
                    CyanZephyr.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldBreeze.copy(alpha = 0.18f),
                            CyanZephyr.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldBreeze.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.48f))
                    ) {
                        Text(
                            text = if (isGreek) "WIDGET 2 • ΑΝΕΣΗ & ΣΥΜΒΟΥΛΟΣ ΗΜΕΡΑΣ" else "WIDGET 2 • COMFORT & LIFESTYLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldBreeze,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = outfitSuggestion,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = outdoorStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Circular Comfort Ring Gauge
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.5.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(EmeraldBreeze, CyanZephyr, AmberSol, EmeraldBreeze)
                            ),
                            startAngle = -90f,
                            sweepAngle = (comfortScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$comfortScore%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (isGreek) "Άνεση" else "Comfort",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldBreeze,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 3 Lifestyle Capsules Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LifestyleMiniPill(
                    title = if (isGreek) "Έξοδος" else "Outdoor",
                    value = bundle.bestOutdoorWindow,
                    accent = CyanGlow,
                    modifier = Modifier.weight(1f)
                )
                LifestyleMiniPill(
                    title = if (isGreek) "Υγρασία" else "Humidity",
                    value = "${bundle.current.humidity}% • ${units.formatTemp(bundle.current.dewPointC)}",
                    accent = EmeraldBreeze,
                    modifier = Modifier.weight(1f)
                )
                LifestyleMiniPill(
                    title = if (isGreek) "Αέρας AQI" else "Clean Air",
                    value = "${bundle.airQuality.usAqi} (${bundle.airQuality.localizedCategory(language)})",
                    accent = bundle.airQuality.badgeColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AnalogBarometerAndRainPulseWidget(
    bundle: CityWeatherBundle,
    units: WeatherUnits,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = language == AppLanguage.GREEK
    val pressureHpa = bundle.current.pressureHpa
    val tendencyText = when {
        pressureHpa >= 1018.0 -> if (isGreek) "Υψηλή • Σταθερός Καιρός" else "High • Fair Weather"
        pressureHpa >= 1010.0 -> if (isGreek) "Κανονική • Ήπιες Συνθήκες" else "Normal • Steady Sky"
        else -> if (isGreek) "Χαμηλή • Πιθανή Βροχή" else "Low • Rain System"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .testTag("widget_barometer_rain_pulse"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A192E).copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    CyanZephyr.copy(alpha = 0.62f),
                    AmberSol.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.16f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CyanZephyr.copy(alpha = 0.18f),
                            AmberSol.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Analog Barometer Dial
            Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 6.dp.toPx()

                    drawCircle(
                        color = Color(0xFF061325).copy(alpha = 0.65f),
                        radius = radius * 0.92f,
                        center = center
                    )

                    drawArc(
                        color = Color.White.copy(alpha = 0.14f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val normalizedP = ((pressureHpa - 985.0) / 55.0).toFloat().coerceIn(0.05f, 0.98f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(CyanZephyr, EmeraldBreeze, AmberSol, CyanZephyr),
                            center = center
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * normalizedP,
                        useCenter = false,
                        style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Barometer Needle
                    val needleAngle = 135f + 270f * normalizedP
                    rotate(degrees = needleAngle, pivot = center) {
                        drawLine(
                            color = AmberSol,
                            start = center,
                            end = Offset(center.x + radius * 0.72f, center.y),
                            strokeWidth = 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = 4.2.dp.toPx(),
                        center = center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 18.dp)
                ) {
                    Text(
                        text = "${pressureHpa.roundToInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "hPa",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Right Side: Barometer Info + 6-Hour Rain Pulse Bars
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyanZephyr.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (isGreek) "WIDGET 3 • ΒΑΡΟΜΕΤΡΟ & ΠΑΛΜΟΣ ΒΡΟΧΗΣ" else "WIDGET 3 • BAROMETER & RAIN PULSE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = tendencyText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                // 6-Hour Rain Probability Equalizer Bars
                val next6 = bundle.hourly.take(6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    next6.forEachIndexed { idx, hItem ->
                        val prob = hItem.precipProbability.coerceIn(10, 100)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            ) {
                                val barH = (size.height * (prob / 100f)).coerceAtLeast(4.5.dp.toPx())
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.12f),
                                    size = size,
                                    cornerRadius = CornerRadius(7f, 7f)
                                )
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = if (prob >= 45) {
                                            listOf(CyanGlow, CyanZephyr)
                                        } else {
                                            listOf(EmeraldBreeze, CyanZephyr.copy(alpha = 0.75f))
                                        }
                                    ),
                                    topLeft = Offset(0f, size.height - barH),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(7f, 7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (idx == 0) {
                                    if (isGreek) "Τώρα" else "Now"
                                } else {
                                    hItem.displayHour.take(2)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifestyleMiniPill(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.1.dp, accent.copy(alpha = 0.38f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
