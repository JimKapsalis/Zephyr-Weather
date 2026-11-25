package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.AppThemeSetting
import com.example.model.AutoRefreshInterval
import com.example.model.WeatherProvider
import com.example.model.WeatherUnits
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.WeatherIcons

@Composable
fun SettingsTabScreen(
    bundle: com.example.model.CityWeatherBundle? = null,
    selectedProvider: WeatherProvider,
    selectedTheme: AppThemeSetting,
    refreshInterval: AutoRefreshInterval,
    batterySaverSyncEnabled: Boolean,
    units: WeatherUnits,
    language: AppLanguage = AppLanguage.ENGLISH,
    emyAlertsEnabled: Boolean,
    rainAlertsEnabled: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onSelectProvider: (WeatherProvider) -> Unit,
    onSelectTheme: (AppThemeSetting) -> Unit,
    onSelectRefreshInterval: (AutoRefreshInterval) -> Unit,
    onToggleBatterySaverSync: () -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    onToggleWindUnit: () -> Unit,
    onTogglePressureUnit: () -> Unit,
    onToggleEmyAlerts: () -> Unit,
    onToggleRainAlerts: () -> Unit,
    onRequestGpsLocation: () -> Unit,
    onOpenLocationSetup: () -> Unit = {},
    onTriggerTestAlert: () -> Unit = {},
    isCheckingUpdates: Boolean = false,
    appVersionLabel: String = "v2.8.0 (Build 28)",
    lastUpdateCheckStatus: String? = null,
    githubRepoSlug: String = "zephyr-weather/zephyr-weather",
    latestReleaseTag: String = "v2.8.0",
    latestReleaseNotes: String? = null,
    isNewGitHubReleaseAvailable: Boolean = false,
    onUpdateGitHubRepoSlug: (String) -> Unit = {},
    onOpenGitHubReleaseDownload: (directApk: Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onSendFeedbackEmail: (category: String, rating: Int, message: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_tab_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Text(
                    text = if (language == AppLanguage.GREEK) "Ρυθμίσεις & Προτιμήσεις" else "Settings & Preferences",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (language == AppLanguage.GREEK) {
                        "Ενημερώσεις εφαρμογής, γλώσσα (ΕΛ / EN), μονάδες (°C / °F) και μοντέλα πρόγνωσης."
                    } else {
                        "App updates, language (EN / GR), temperature units (°C / °F), and forecast models."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }
        }



        // Section 0: Language Selection (English / Ελληνικά)
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.Public,
                title = if (language == AppLanguage.GREEK) "Γλώσσα Εφαρμογής / Language" else "App Language / Γλώσσα",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Επιλέξτε Αγγλικά ή Ελληνικά (αποθηκεύεται αυτόματα)"
                } else {
                    "Switch between English and Greek (Ελληνικά)"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLanguage.entries.forEach { langOption ->
                    val isSelected = langOption == language
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelectLanguage(langOption) }
                            .testTag("language_option_${langOption.code}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF153356) else Color(0xFF102238).copy(alpha = 0.88f)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = langOption.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) CyanGlow else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = langOption.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = WeatherIcons.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF07111E),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 0b: Automatic GPS Location
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.MyLocation,
                title = if (language == AppLanguage.GREEK) "Αυτόματη Τοποθεσία GPS" else "Automatic GPS Location",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Εντοπισμός της τρέχουσας πόλης σας με ακρίβεια"
                } else {
                    "Detect your current city & weather station automatically"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_location_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                ),
                border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.GREEK) "Ζωντανός Εντοπισμός GPS" else "Live GPS Weather Station",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Εντοπισμός της τρέχουσας τοποθεσίας σας ή αναζήτηση πόλης"
                                } else {
                                    "Auto-detect your current location or open the Location Setup search screen"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onRequestGpsLocation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldBreeze.copy(alpha = 0.22f)
                            ),
                            border = BorderStroke(1.dp, EmeraldBreeze),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("settings_gps_locate_button")
                        ) {
                            Icon(
                                imageVector = WeatherIcons.MyLocation,
                                contentDescription = null,
                                tint = EmeraldBreeze,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = onOpenLocationSetup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanZephyr.copy(alpha = 0.20f)
                        ),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_location_setup_button")
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Search,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.GREEK) {
                                "Άνοιγμα Οθόνης Εύρεσης & Ρύθμισης Τοποθεσίας"
                            } else {
                                "Open Location Setup & Search Screen"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section 1: Persistent Units & Temperature Scale (°C / °F)
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.Speed,
                title = if (language == AppLanguage.GREEK) "Μονάδες Μέτρησης (°C / °F)" else "Measurement Units (°C / °F)",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Εναλλαγή Κελσίου (°C) και Φαρενάιτ (°F) με μόνιμη αποθήκευση"
                } else {
                    "Toggle between Celsius (°C) and Fahrenheit (°F), saved persistently"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("units_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTemperatureUnit() }
                            .testTag("toggle_temp_unit"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.GREEK) "Μονάδα Θερμοκρασίας" else "Temperature Scale",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (units.useFahrenheit) {
                                    if (language == AppLanguage.GREEK) "Ενεργό: Fahrenheit (°F)" else "Active: Fahrenheit (°F)"
                                } else {
                                    if (language == AppLanguage.GREEK) "Ενεργό: Κελσίου (°C)" else "Active: Celsius (°C)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanGlow,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Switch(
                            checked = units.useFahrenheit,
                            onCheckedChange = { onToggleTemperatureUnit() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AmberSol,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = CyanZephyr
                            ),
                            modifier = Modifier.testTag("celsius_fahrenheit_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    UnitToggleRow(
                        title = if (language == AppLanguage.GREEK) "Ταχύτητα Ανέμου" else "Wind Speed Unit",
                        currentValue = if (units.useMph) "mph" else "km/h",
                        onToggle = onToggleWindUnit,
                        testTag = "toggle_wind_unit"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    UnitToggleRow(
                        title = if (language == AppLanguage.GREEK) "Ατμοσφαιρική Πίεση" else "Atmospheric Pressure",
                        currentValue = if (units.useInHg) "inHg" else "hPa",
                        onToggle = onTogglePressureUnit,
                        testTag = "toggle_pressure_unit"
                    )
                }
            }
        }

        // Section 3: Weather Forecast Models
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.CompassCalibration,
                title = if (language == AppLanguage.GREEK) "Μετεωρολογικά Μοντέλα Πρόγνωσης" else "Weather Forecast Models",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Επιλέξτε ανάμεσα στα 3 κορυφαία ευρωπαϊκά και παγκόσμια μοντέλα"
                } else {
                    "Choose from the top 3 global & European meteorological models"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WeatherProvider.entries.forEach { provider ->
                    val isSelected = provider == selectedProvider
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelectProvider(provider) }
                            .testTag("provider_card_${provider.id}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF153356) else Color(0xFF102238).copy(alpha = 0.88f)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) CyanGlow else Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) CyanZephyr.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = provider.resolutionBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.70f),
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = provider.localizedSubtitle(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = WeatherIcons.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF07111E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Auto-Refresh Frequency & Zero-Drain Battery Optimization
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.Refresh,
                title = if (language == AppLanguage.GREEK) "Αυτόματη Ανανέωση & Μπαταρία" else "Auto-Refresh & Battery Optimization",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Συχνότητα αυτόματης ανανέωσης χωρίς κατανάλωση μπαταρίας"
                } else {
                    "Automatic background forecast refresh without draining battery"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AutoRefreshInterval.entries.forEach { interval ->
                    val isSelected = interval == refreshInterval
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelectRefreshInterval(interval) }
                            .testTag("refresh_interval_card_${interval.id}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF153356) else Color(0xFF102238).copy(alpha = 0.88f)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) EmeraldBreeze else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = interval.localizedName(language),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) EmeraldBreeze else Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) EmeraldBreeze.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = interval.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) EmeraldBreeze else Color.White.copy(alpha = 0.70f),
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = interval.localizedSubtitle(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) EmeraldBreeze else Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = WeatherIcons.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF07111E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("battery_saver_sync_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Έξυπνος Συγχρονισμός Μηδενικής Κατανάλωσης"
                                } else {
                                    "Zero-Drain Smart Background Sync"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Παύση χρονιστών όταν η οθόνη είναι κλειστή για μηδενική κατανάλωση μπαταρίας."
                                } else {
                                    "Pauses rapid timers when the screen is off to prevent battery drain."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Switch(
                            checked = batterySaverSyncEnabled,
                            onCheckedChange = { onToggleBatterySaverSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldBreeze,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("battery_saver_sync_switch")
                        )
                    }
                }
            }
        }

        // Section 6: Meteorological Alerts (EMY & Rain)
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.NotificationsActive,
                title = if (language == AppLanguage.GREEK) "Ειδοποιήσεις ΕΜΥ & Βροχής" else "EMY & Rain Alerts",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Έκτακτα δελτία ΕΜΥ και ειδοποιήσεις επερχόμενης βροχόπτωσης"
                } else {
                    "Real-time severe warnings & imminent precipitation notifications"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alerts_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238).copy(alpha = 0.90f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIcons.Warning,
                                    contentDescription = null,
                                    tint = AmberSol,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.GREEK) "Έκτακτα Δελτία ΕΜΥ" else "EMY Severe Weather Warnings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Ειδοποιήσεις της Εθνικής Μετεωρολογικής Υπηρεσίας για θυελλώδεις ανέμους και καταιγίδες."
                                } else {
                                    "Hellenic National Meteorological Service emergency weather bulletins."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Switch(
                            checked = emyAlertsEnabled,
                            onCheckedChange = {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onToggleEmyAlerts()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanZephyr,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("emy_alerts_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIcons.WaterDrop,
                                    contentDescription = null,
                                    tint = CyanZephyr,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.GREEK) "Ειδοποίηση Έναρξης Βροχής" else "Rain Onset & Storm Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Άμεση ειδοποίηση 30 λεπτά πριν την ανίχνευση βροχής στο ραντάρ."
                                } else {
                                    "Instant heads-up notification 30 minutes before precipitation is detected on radar."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Switch(
                            checked = rainAlertsEnabled,
                            onCheckedChange = {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onToggleRainAlerts()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanZephyr,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("rain_alerts_switch")
                        )
                    }
                }
            }
        }

        // Section 7: App Feedback Sent Directly to User's Email
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.Send,
                title = if (language == AppLanguage.GREEK) "Αποστολή Σχολίων (Feedback)" else "App Feedback & Support",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Στείλτε τις προτάσεις ή αναφορές σφαλμάτων απευθείας στο email της εφαρμογής"
                } else {
                    "Send your feedback, bug reports, or ideas directly to our email"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            var selectedCategory by remember { mutableStateOf("General") }
            var rating by remember { mutableIntStateOf(5) }
            var feedbackMessage by remember { mutableStateOf("") }
            var feedbackSentConfirmation by remember { mutableStateOf(false) }

            val categories = listOf(
                "General" to (if (language == AppLanguage.GREEK) "Γενικά" else "General"),
                "Bug Report" to (if (language == AppLanguage.GREEK) "Αναφορά Σφάλματος" else "Bug Report"),
                "Weather Accuracy" to (if (language == AppLanguage.GREEK) "Ακρίβεια Καιρού" else "Weather Accuracy"),
                "Feature Request" to (if (language == AppLanguage.GREEK) "Νέα Λειτουργία" else "Feature Request")
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238).copy(alpha = 0.92f)
                ),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.GREEK) "Σχόλια & Υποστήριξη Εφαρμογής" else "In-App Feedback & Support",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.GREEK) "Μοιραστείτε προτάσεις ή αναφορές απευθείας από την εφαρμογή" else "Share feature ideas or reports directly from the app",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (star <= rating) AmberSol.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (star <= rating) AmberSol else Color.White.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { rating = star }
                                        .testTag("feedback_star_$star")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "★",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (star <= rating) AmberSol else Color.White.copy(alpha = 0.45f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (id, label) ->
                            val isSelected = selectedCategory == id
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) CyanZephyr.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanZephyr else Color.White.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedCategory = id }
                                    .testTag("feedback_category_${id.lowercase().replace(' ', '_')}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.78f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = {
                            feedbackMessage = it
                            feedbackSentConfirmation = false
                        },
                        placeholder = {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Γράψτε τα σχόλιά σας, αναφορά προβλήματος ή προτάσεις για την εφαρμογή..."
                                } else {
                                    "Write your feedback, bug report, or suggestions for the app..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0A182B),
                            unfocusedContainerColor = Color(0xFF0A182B).copy(alpha = 0.80f),
                            focusedBorderColor = CyanZephyr,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            cursorColor = CyanZephyr
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_feedback_input")
                    )

                    if (feedbackSentConfirmation) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldBreeze.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_sent_confirmation")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIcons.Check,
                                    contentDescription = null,
                                    tint = EmeraldBreeze,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.GREEK) {
                                        "Τα σχόλιά σας καταχωρήθηκαν επιτυχώς! Ευχαριστούμε!"
                                    } else {
                                        "Your feedback has been submitted! Thank you!"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSendFeedbackEmail(selectedCategory, rating, feedbackMessage)
                            feedbackSentConfirmation = true
                            feedbackMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanZephyr.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.2.dp, CyanZephyr),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_send_feedback_button")
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Send,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.GREEK) {
                                "Αποστολή Σχολίων"
                            } else {
                                "Submit Feedback"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 8 (Bottom): GitHub Releases App Updater & 4-Model Ensemble Sync
        item {
            SettingsSectionHeader(
                icon = WeatherIcons.Refresh,
                title = if (language == AppLanguage.GREEK) "Ενημέρωση Εφαρμογής (GitHub Releases)" else "GitHub Releases App Updater",
                subtitle = if (language == AppLanguage.GREEK) {
                    "Έλεγχος νέας έκδοσης APK στο GitHub Releases & συγχρονισμός 4 μοντέλων"
                } else {
                    "Check GitHub Releases for new APK versions & sync 4-model forecasts"
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            var repoInput by remember(githubRepoSlug) { mutableStateOf(githubRepoSlug) }
            val defaultChangelog = if (language == AppLanguage.GREEK) {
                "• Μέγιστη απόδοση & ομαλότητα (60Hz/90Hz/120Hz) για όλες τις συσκευές Android\n" +
                    "• Ακαριαίος εντοπισμός τοποθεσίας GPS (< 100ms) & συγχρονισμός 4 μοντέλων\n" +
                    "• Διάφανη γραμμή πλοήγησης, τρισδιάστατα εικονίδια & ελάχιστη κατανάλωση μπαταρίας"
            } else {
                "• Maximum performance & fluid 60Hz/90Hz/120Hz rendering for all Android devices\n" +
                    "• Instant sub-100ms GPS location finding & 4-model super-ensemble sync\n" +
                    "• Transparent glass navigation bar, 3D animated weather icons & ultra-low battery usage"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_app_updater_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102542).copy(alpha = 0.94f)
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (isNewGitHubReleaseAvailable) AmberSol else CyanZephyr.copy(alpha = 0.45f)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zephyr Weather $appVersionLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "GitHub Release: $latestReleaseTag • Βελτιστοποιημένο για Όλες τις Συσκευές"
                                } else {
                                    "GitHub Release: $latestReleaseTag • Optimized for All Android Devices"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isNewGitHubReleaseAvailable) {
                                AmberSol.copy(alpha = 0.20f)
                            } else {
                                EmeraldBreeze.copy(alpha = 0.16f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isNewGitHubReleaseAvailable) AmberSol else EmeraldBreeze.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = when {
                                    isCheckingUpdates -> if (language == AppLanguage.GREEK) "ΕΛΕΓΧΟΣ…" else "CHECKING…"
                                    isNewGitHubReleaseAvailable -> if (language == AppLanguage.GREEK) "ΝΕΑ ΕΚΔΟΣΗ $latestReleaseTag" else "UPDATE $latestReleaseTag"
                                    else -> if (language == AppLanguage.GREEK) "ΕΝΗΜΕΡΩΜΕΝΟ" else "LATEST"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNewGitHubReleaseAvailable) AmberSol else EmeraldBreeze,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = lastUpdateCheckStatus ?: if (language == AppLanguage.GREEK) {
                            "Ελέγχει αυτόματα το αποθετήριο GitHub Releases για νέο APK και ενημερώνει τα 4 μετεωρολογικά μοντέλα."
                        } else {
                            "Automatically checks your GitHub Releases feed for new APK builds and syncs all 4 NWP forecast models."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isNewGitHubReleaseAvailable -> AmberSol
                            lastUpdateCheckStatus != null -> EmeraldBreeze
                            else -> Color.White.copy(alpha = 0.74f)
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0A182B).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Αρχείο Αλλαγών ($latestReleaseTag)"
                                } else {
                                    "Release Changelog ($latestReleaseTag)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )
                            Text(
                                text = latestReleaseNotes?.takeIf { it.isNotBlank() } ?: defaultChangelog,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f),
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = {
                            repoInput = it
                            onUpdateGitHubRepoSlug(it)
                        },
                        singleLine = true,
                        label = {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Αποθετήριο GitHub Releases (owner/repo)"
                                } else {
                                    "GitHub Releases Repository (owner/repo)"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0A182B),
                            unfocusedContainerColor = Color(0xFF0A182B).copy(alpha = 0.80f),
                            focusedBorderColor = CyanZephyr,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            focusedLabelColor = CyanGlow,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                            cursorColor = CyanZephyr
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_repo_input")
                    )

                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdates,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldBreeze.copy(alpha = 0.24f),
                            disabledContainerColor = CyanZephyr.copy(alpha = 0.14f)
                        ),
                        border = BorderStroke(1.2.dp, if (isCheckingUpdates) CyanZephyr else EmeraldBreeze),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_check_updates_button")
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Refresh,
                            contentDescription = null,
                            tint = if (isCheckingUpdates) CyanGlow else EmeraldBreeze,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCheckingUpdates) {
                                if (language == AppLanguage.GREEK) {
                                    "Έλεγχος GitHub Releases & Μοντέλων…"
                                } else {
                                    "Checking GitHub Releases & Models…"
                                }
                            } else {
                                if (language == AppLanguage.GREEK) {
                                    "Έλεγχος για Νέα Έκδοση στο GitHub Releases"
                                } else {
                                    "Check GitHub Releases for New Update"
                                }
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onOpenGitHubReleaseDownload(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNewGitHubReleaseAvailable) {
                                    AmberSol.copy(alpha = 0.28f)
                                } else {
                                    CyanZephyr.copy(alpha = 0.22f)
                                }
                            ),
                            border = BorderStroke(
                                1.2.dp,
                                if (isNewGitHubReleaseAvailable) AmberSol else CyanZephyr
                            ),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_github_release_download_button")
                        ) {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "Λήψη Τελευταίου APK"
                                } else {
                                    "Download Latest APK"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { onOpenGitHubReleaseDownload(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.10f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_github_releases_page_button")
                        ) {
                            Text(
                                text = if (language == AppLanguage.GREEK) {
                                    "GitHub Releases"
                                } else {
                                    "GitHub Releases"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanGlow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanZephyr,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.60f)
            )
        }
    }
}

@Composable
private fun UnitToggleRow(
    title: String,
    currentValue: String,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = CyanZephyr.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.40f))
        ) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

