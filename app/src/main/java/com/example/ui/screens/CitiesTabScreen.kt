package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SavedCityEntity
import com.example.model.AppLanguage
import com.example.model.WeatherUnits
import com.example.model.WmoWeatherMapper
import com.example.ui.components.AnimatedWeatherStatusIcon
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.M3BodyText
import com.example.ui.theme.M3CardSurface
import com.example.ui.theme.M3PeriwinkleAction
import com.example.ui.theme.M3PillSelectedContainer
import com.example.ui.theme.M3SubtleBorder
import com.example.ui.theme.M3TitleText
import com.example.ui.theme.WeatherIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesTabScreen(
    savedCities: List<SavedCityEntity>,
    suggestedCities: List<SavedCityEntity> = emptyList(),
    selectedCityId: Long,
    searchQuery: String,
    searchResults: List<SavedCityEntity>,
    isSearching: Boolean,
    isRefreshing: Boolean = false,
    units: WeatherUnits,
    language: AppLanguage = AppLanguage.ENGLISH,
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onAddCity: (SavedCityEntity) -> Unit,
    onSelectCity: (SavedCityEntity) -> Unit,
    onTogglePinCity: (SavedCityEntity) -> Unit,
    onRemoveCity: (SavedCityEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedCitiesPillTab by remember { mutableStateOf(0) }

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
                    .testTag("cities_pull_to_refresh_indicator")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("cities_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cities_tab_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Top Material You Segmented Pill Switcher
            item(key = "cities_segmented_pill_header") {
                MaterialYouSegmentedPillBar(
                    selectedIndex = selectedCitiesPillTab,
                    leftLabel = if (language == AppLanguage.GREEK) "Προτεινόμενα" else "Recommended",
                    rightLabel = if (language == AppLanguage.GREEK) "Όλοι οι σταθμοί" else "All stations",
                    onSelectIndex = { selectedCitiesPillTab = it },
                    leftTag = "cities_segmented_pill_recommended",
                    rightTag = "cities_segmented_pill_all"
                )
            }

            // 1. Live Global Geocoding Search Field
            item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (language == AppLanguage.GREEK) "Αναζήτηση πόλης" else "Search city",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
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
                                    contentDescription = if (language == AppLanguage.GREEK) "Αναζήτηση πόλης" else "Search city",
                                    tint = CyanGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanZephyr
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = WeatherIcons.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.52f),
                        unfocusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.44f),
                        focusedBorderColor = CyanZephyr,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.26f),
                        cursorColor = CyanZephyr
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input")
                )

                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_results_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF152C48).copy(alpha = 0.62f)
                        ),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            searchResults.forEachIndexed { index, resultCity ->
                                if (index > 0) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAddCity(resultCity) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("search_result_${resultCity.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            if (language == AppLanguage.GREEK) " • Υψόμετρο ${resultCity.elevationMeters.toInt()} m" else " • Elev ${resultCity.elevationMeters.toInt()} m"
                                        } else ""
                                        Text(
                                            text = "${resultCity.regionName}, ${resultCity.countryName}$elevLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = WeatherIcons.AddCircle,
                                        contentDescription = "Add city",
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

        // 2. Saved World Stations Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.GREEK) {
                        "Αποθηκευμένες Πόλεις (${savedCities.size})"
                    } else {
                        "Saved World Stations (${savedCities.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (language == AppLanguage.GREEK) {
                        "Πατήστε για πρόγνωση"
                    } else {
                        "Tap card to view forecast"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (savedCities.isEmpty() && suggestedCities.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cities_tab_empty_suggestions"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.48f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.GREEK) {
                                "Δεν έχετε επιλέξει τοποθεσίες ακόμη. Αναζητήστε παραπάνω ή επιλέξτε μία πόλη:"
                            } else {
                                "No locations added yet. Search above or tap a city below to add it:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        suggestedCities.forEach { suggestion ->
                            val spec = WmoWeatherMapper.resolve(suggestion.cachedWeatherCode, suggestion.cachedIsDay)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onAddCity(suggestion) }
                                    .testTag("cities_tab_suggestion_${suggestion.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AnimatedWeatherStatusIcon(
                                            visualSpec = spec,
                                            iconSize = 22.dp,
                                            contentDescription = spec.localizedLabel(language)
                                        )
                                        Column {
                                            Text(
                                                text = suggestion.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${suggestion.regionName}, ${suggestion.countryName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = WeatherIcons.AddCircle,
                                        contentDescription = "Add city",
                                        tint = CyanZephyr,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Saved City Cards
        items(savedCities, key = { it.id }) { city ->
            SavedCityWeatherCard(
                city = city,
                isSelected = city.id == selectedCityId,
                units = units,
                language = language,
                onSelect = { onSelectCity(city) },
                onTogglePin = { onTogglePinCity(city) },
                onRemove = { onRemoveCity(city) }
            )
        }
        }
    }
}

@Composable
private fun SavedCityWeatherCard(
    city: SavedCityEntity,
    isSelected: Boolean,
    units: WeatherUnits,
    language: AppLanguage,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit
) {
    val spec = WmoWeatherMapper.resolve(city.cachedWeatherCode, city.cachedIsDay)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onSelect)
            .testTag("saved_city_card_${city.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                M3PillSelectedContainer
            } else {
                M3CardSurface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CyanZephyr.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = if (language == AppLanguage.GREEK) "ΕΝΕΡΓΗ" else "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${city.regionName}, ${city.countryName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedWeatherStatusIcon(
                        visualSpec = spec,
                        iconSize = 20.dp,
                        contentDescription = spec.localizedLabel(language)
                    )
                    val elevMeters = city.elevationMeters.coerceAtLeast(1.0).toInt()
                    val elevText = if (language == AppLanguage.GREEK) {
                        "Υψόμετρο $elevMeters m"
                    } else {
                        "Elev $elevMeters m"
                    }
                    Text(
                        text = "${spec.localizedLabel(language)} • $elevText • AQI ${city.cachedAqi}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = units.formatTemp(city.cachedTempC),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "H:${units.formatTemp(city.cachedHighC, false)} L:${units.formatTemp(city.cachedLowC, false)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pin_city_${city.id}")
                    ) {
                        Icon(
                            imageVector = if (city.isPinned) WeatherIcons.PushPin else WeatherIcons.PushPinOutlined,
                            contentDescription = "Pin station",
                            tint = if (city.isPinned) AmberSol else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_city_${city.id}")
                    ) {
                        Icon(
                            imageVector = WeatherIcons.DeleteOutline,
                            contentDescription = "Remove station",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
