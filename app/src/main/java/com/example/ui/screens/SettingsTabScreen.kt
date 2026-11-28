package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.AppThemeSetting
import com.example.model.AutoRefreshInterval
import com.example.model.WeatherProvider
import com.example.model.WeatherUnits
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CoralWarmth
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.M3BodyText
import com.example.ui.theme.M3CardSurface
import com.example.ui.theme.M3CardSurfaceElevated
import com.example.ui.theme.M3DarkBackground
import com.example.ui.theme.M3IconTint
import com.example.ui.theme.M3PeriwinkleAction
import com.example.ui.theme.M3PillSelectedContainer
import com.example.ui.theme.M3PillUnselectedContainer
import com.example.ui.theme.M3SubtleBorder
import com.example.ui.theme.M3TitleText
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
    appVersionLabel: String = "v1.4.2 (Build 15)",
    lastUpdateCheckStatus: String? = null,
    githubRepoSlug: String = "JimKapsalis/Zephyr-Weather",
    latestReleaseTag: String = "v1.4.2",
    latestReleaseNotes: String? = null,
    isNewGitHubReleaseAvailable: Boolean = false,
    totalApkDownloads: Int = 1803,
    onUpdateGitHubRepoSlug: (String) -> Unit = {},
    onOpenGitHubReleaseDownload: (directApk: Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onSendFeedbackEmail: (category: String, rating: Int, message: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGreek = language == AppLanguage.GREEK
    val discordInviteUrl = "https://discord.gg/aHynb4QKC"

    // Top Segmented Pill Tab state: 0 = "Recommended", 1 = "All settings"
    var selectedTopTab by remember { mutableIntStateOf(0) }

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

    val formattedDownloads = if (totalApkDownloads >= 1000) {
        String.format(java.util.Locale.US, "%.1fk+ (%,d)", totalApkDownloads / 1000.0, totalApkDownloads)
    } else {
        totalApkDownloads.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Segmented Pill Switcher ("Recommended" | "All settings")
        item {
            MaterialYouSegmentedPillBar(
                leftLabel = if (isGreek) "Προτεινόμενα" else "Recommended",
                rightLabel = if (isGreek) "Όλες οι ρυθμίσεις" else "All settings",
                selectedIndex = selectedTopTab,
                onSelectIndex = { selectedTopTab = it },
                leftTag = "settings_pill_recommended",
                rightTag = "settings_pill_all_services"
            )
        }

        // 2. 2-Column Colorful Material You Bento Cards Grid (6 cards in 3 rows of 2)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: App Updater (Emerald Green) & Discord Server (Vibrant Indigo/Cyan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.Refresh,
                        accent = EmeraldBreeze,
                        title = if (isGreek) "Ενημέρωση\nΕφαρμογής" else "App\nUpdater",
                        description = if (isGreek) {
                            "Έκδοση $latestReleaseTag με $formattedDownloads λήψεις APK. Έλεγχος GitHub Releases για νέες εκδόσεις."
                        } else {
                            "Version $latestReleaseTag with $formattedDownloads APK downloads. Check GitHub Releases for new builds."
                        },
                        actionLabel = if (isCheckingUpdates) {
                            if (isGreek) "Έλεγχος…" else "Checking…"
                        } else if (isNewGitHubReleaseAvailable) {
                            if (isGreek) "Λήψη APK" else "Install APK"
                        } else {
                            if (isGreek) "Έλεγχος" else "Check update"
                        },
                        onCardClick = { onCheckForUpdates() },
                        onActionClick = {
                            if (isNewGitHubReleaseAvailable) {
                                onOpenGitHubReleaseDownload(true)
                            } else {
                                onCheckForUpdates()
                            }
                        },
                        testTag = "bento_card_app_updater",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.Send,
                        accent = Color(0xFF818CF8),
                        title = if (isGreek) "Κοινότητα\nDiscord" else "Discord\nServer",
                        description = if (isGreek) {
                            "Μπείτε στον επίσημο server discord.gg/aHynb4QKC για ζωντανές αναφορές καιρού και νέα APK."
                        } else {
                            "Join our community at discord.gg/aHynb4QKC for live weather updates, chat, and new APK builds."
                        },
                        actionLabel = if (isGreek) "Είσοδος" else "Join Discord",
                        onCardClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordInviteUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onActionClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordInviteUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        testTag = "bento_card_discord_server",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 2: Forecast Models (Cyan Glow) & GPS Location Hub (Amber Gold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val nextProvider = remember(selectedProvider) {
                        val all = WeatherProvider.entries
                        all[(all.indexOf(selectedProvider) + 1) % all.size]
                    }
                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.CompassCalibration,
                        accent = CyanGlow,
                        title = if (isGreek) "Μοντέλα\nΠρόγνωσης" else "Forecast\nModels",
                        description = if (isGreek) {
                            "Ενεργό: ${selectedProvider.displayName} (${selectedProvider.resolutionBadge}). Συνδυασμός 4 μοντέλων ακριβείας."
                        } else {
                            "Active: ${selectedProvider.displayName} (${selectedProvider.resolutionBadge}). 4-model NWP super-ensemble."
                        },
                        actionLabel = if (isGreek) "Αλλαγή" else "Switch model",
                        onCardClick = { onSelectProvider(nextProvider) },
                        onActionClick = { onSelectProvider(nextProvider) },
                        testTag = "bento_card_forecast_models",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.MyLocation,
                        accent = AmberSol,
                        title = if (isGreek) "Εύρεση\nΤοποθεσίας" else "Location\nHub",
                        description = if (isGreek) {
                            "Αυτόματος εντοπισμός συντεταγμένων GPS & υψομέτρου ή αναζήτηση πόλης και χωριού."
                        } else {
                            "Auto-detect your GPS coordinates and elevation or open search to locate any city or village."
                        },
                        actionLabel = if (isGreek) "Εντοπισμός" else "Locate GPS",
                        onCardClick = onOpenLocationSetup,
                        onActionClick = onRequestGpsLocation,
                        testTag = "bento_card_location_hub",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 3: Units & Language (Sky Cyan) + Weather Alerts & Sync (Coral Orange)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.Public,
                        accent = CyanZephyr,
                        title = if (isGreek) "Μονάδες &\nΓλώσσα" else "Units &\nLanguage",
                        description = if (isGreek) {
                            "Κλίμακα: ${if (units.useFahrenheit) "Fahrenheit (°F)" else "Κελσίου (°C)"}, ${if (units.useMph) "mph" else "km/h"} • Γλώσσα: ${language.displayName}."
                        } else {
                            "Scale: ${if (units.useFahrenheit) "Fahrenheit (°F)" else "Celsius (°C)"}, ${if (units.useMph) "mph" else "km/h"} • Language: ${language.displayName}."
                        },
                        actionLabel = if (isGreek) "Εναλλαγή" else "Continue",
                        onCardClick = onToggleTemperatureUnit,
                        onActionClick = {
                            val nextLang = if (language == AppLanguage.ENGLISH) AppLanguage.GREEK else AppLanguage.ENGLISH
                            onSelectLanguage(nextLang)
                        },
                        testTag = "bento_card_units_language",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.NotificationsActive,
                        accent = Color(0xFFFB7185),
                        title = if (isGreek) "Ειδοποιήσεις\nΚαιρού" else "Weather\nAlerts",
                        description = if (isGreek) {
                            "Δελτία ΕΜΥ: ${if (emyAlertsEnabled) "ΕΝΕΡΓΑ" else "ΚΛΕΙΣΤΑ"} • Βροχή: ${if (rainAlertsEnabled) "ΕΝΕΡΓΗ" else "ΚΛΕΙΣΤΗ"} • Ανανέωση: ${refreshInterval.badge}."
                        } else {
                            "EMY warnings: ${if (emyAlertsEnabled) "ON" else "OFF"} • Rain onset: ${if (rainAlertsEnabled) "ON" else "OFF"} • Sync: ${refreshInterval.badge}."
                        },
                        actionLabel = if (isGreek) "Ρυθμίσεις" else "Settings",
                        onCardClick = { selectedTopTab = 1 },
                        onActionClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            onToggleRainAlerts()
                        },
                        testTag = "bento_card_weather_alerts",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Top Section 1: Official Discord Community Server Card (Vibrant Indigo Accent)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_discord_server_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)
                ),
                border = BorderStroke(
                    width = 1.1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF818CF8).copy(alpha = 0.65f),
                            CyanZephyr.copy(alpha = 0.35f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF818CF8).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.55f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WeatherIcons.Send,
                                    contentDescription = "Discord Server",
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Επίσημος Discord Server" else "Official Discord Community",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = discordInviteUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }

                    Text(
                        text = if (isGreek) {
                            "Συζητήστε για τον καιρό στην Ελλάδα, αναφέρετε τοπικά φαινόμενα (βροχή, χιόνι, καταιγίδες) και κατεβάστε πρώτοι τις νέες εκδόσεις APK."
                        } else {
                            "Chat about live weather, report local rain or snow conditions, suggest new features, and get instant notifications for new APK builds."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isGreek) "Είσοδος στον Discord Server" else "Join Discord Server",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordInviteUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("settings_join_discord_button")
                        )
                    }
                }
            }
        }

        // Top Section 2: GitHub Releases App Updater & 1.8k+ (1,803) Downloads Counter (Emerald Green Accent)
        item {
            var repoInput by remember(githubRepoSlug) { mutableStateOf(githubRepoSlug) }
            val defaultChangelog = if (isGreek) {
                "🚀 ΜΕΓΑΛΗ ΕΝΗΜΕΡΩΣΗ v1.4.2 (Build 15):\n" +
                    "• Νέες 2-στηλες Material You Bento Κάρτες για Δείκτη UV, Άνεμο & Ριπές, Υγρασία & Δρόσο, Πίεση & Ορατότητα\n" +
                    "• Ανανεωμένη Σειρά Πρόγνωσης: 10ήμερη Πρόγνωση Πρώτα, 24ωρη Καμπύλη & Τροχιά Ηλίου (Ανατολή/Δύση)\n" +
                    "• Εξάλειψη Ψευδούς Βροχής στη Λάρισα & Θεσσαλία με διασταύρωση 3+ μοντέλων\n" +
                    "• Πλήρως Πολύχρωμο Glassmorphic UI με Ζωντανό Ουρανό, Κινούμενα Εικονίδια & Discord (discord.gg/aHynb4QKC)"
            } else {
                "🚀 BIG UPDATE v1.4.2 (Build 15) CHANGELOG:\n" +
                    "• New 2-Column Material You Bento Cards for UV Index, Wind & Gusts, Humidity & Dew, and Pressure & Visibility\n" +
                    "• Redesigned Forecast Flow: 10-Day Forecast First, 24-Hour Horizon Curve, Bento Metrics & Sunset/Sunrise Arc\n" +
                    "• Multi-Model Anti-False-Rain Engine for Larissa & Greece (requires 3+ model agreement for active rain)\n" +
                    "• Full Colorful Glassmorphic UI over Live Animated Sky Backdrop & Discord (discord.gg/aHynb4QKC)"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_app_updater_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)
                ),
                border = BorderStroke(
                    width = 1.1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldBreeze.copy(alpha = 0.65f),
                            CyanZephyr.copy(alpha = 0.35f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                color = EmeraldBreeze.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.55f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WeatherIcons.Refresh,
                                        contentDescription = null,
                                        tint = EmeraldBreeze,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Zephyr Weather $appVersionLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isGreek) {
                                        "GitHub Release: $latestReleaseTag • $formattedDownloads Συνολικές Λήψεις APK"
                                    } else {
                                        "GitHub Release: $latestReleaseTag • $formattedDownloads Total APK Downloads"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldBreeze
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldBreeze.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = when {
                                    isCheckingUpdates -> if (isGreek) "Έλεγχος…" else "Checking…"
                                    isNewGitHubReleaseAvailable -> if (isGreek) "Νέα $latestReleaseTag" else "Update $latestReleaseTag"
                                    else -> if (isGreek) "Ενημερωμένο" else "Up to date"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBreeze,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = lastUpdateCheckStatus ?: if (isGreek) {
                            "Ελέγχει αυτόματα το αποθετήριο GitHub Releases για νέο APK και ενημερώνει τα 4 μετεωρολογικά μοντέλα."
                        } else {
                            "Automatically checks your GitHub Releases feed for new APK builds and syncs all 4 NWP forecast models."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0A192F).copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenGitHubReleaseDownload(true) }
                            .testTag("settings_changelog_download_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isGreek) "Big Update Changelog ($latestReleaseTag)" else "Big Update Changelog ($latestReleaseTag)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isGreek) "Λήψη APK" else "Download APK",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBreeze
                                )
                            }
                            Text(
                                text = latestReleaseNotes?.takeIf { it.isNotBlank() } ?: defaultChangelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 7,
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
                                text = if (isGreek) "Αποθετήριο GitHub (owner/repo)" else "GitHub Repository (owner/repo)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0A192F).copy(alpha = 0.65f),
                            unfocusedContainerColor = Color(0xFF0A192F).copy(alpha = 0.50f),
                            focusedBorderColor = EmeraldBreeze,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                            focusedLabelColor = EmeraldBreeze,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.70f),
                            cursorColor = EmeraldBreeze
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_repo_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GitHub Releases",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = CyanGlow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenGitHubReleaseDownload(false) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("settings_github_releases_page_button")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (isGreek) "Λήψη APK" else "Download APK",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBreeze,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenGitHubReleaseDownload(true) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("settings_github_release_download_button")
                            )

                            Text(
                                text = if (isCheckingUpdates) {
                                    if (isGreek) "Έλεγχος…" else "Checking…"
                                } else {
                                    if (isGreek) "Έλεγχος ενημέρωσης" else "Check for update"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isCheckingUpdates) { onCheckForUpdates() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("settings_check_updates_button")
                            )
                        }
                    }
                }
            }
        }

        // Section 3: App Language Segmented Pills (English / Ελληνικά)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Public,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Γλώσσα Εφαρμογής" else "App Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { langOption ->
                            val isSelected = langOption == language
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) CyanZephyr.copy(alpha = 0.28f) else Color(0xFF0A192F).copy(alpha = 0.55f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanGlow else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectLanguage(langOption) }
                                    .testTag("language_option_${langOption.code}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 13.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = "${langOption.displayName} (${langOption.shortBadge})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
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

        // Section 4: Automatic GPS Location & Search Setup
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_location_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.MyLocation,
                            contentDescription = null,
                            tint = AmberSol,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Αυτόματη Τοποθεσία GPS" else "Automatic GPS Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = if (isGreek) {
                            "Εντοπίστε αυτόματα την τρέχουσα πόλη και το υψόμετρό σας ή ανοίξτε την αναζήτηση τοποθεσίας."
                        } else {
                            "Auto-detect your current city, coordinates, and altitude or open the location search hub."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGreek) "Αναζήτηση πόλης" else "Search location",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = CyanGlow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenLocationSetup() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("settings_open_location_setup_button")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGreek) "Εντοπισμός GPS" else "Detect GPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AmberSol,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onRequestGpsLocation() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("settings_gps_locate_button")
                        )
                    }
                }
            }
        }

        // Section 5: Measurement Units (°C / °F, Wind, Pressure)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("units_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Speed,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Μονάδες Μέτρησης" else "Measurement Units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                text = if (isGreek) "Μονάδα Θερμοκρασίας" else "Temperature Scale",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = if (units.useFahrenheit) {
                                    if (isGreek) "Fahrenheit (°F)" else "Fahrenheit (°F)"
                                } else {
                                    if (isGreek) "Κελσίου (°C)" else "Celsius (°C)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanGlow
                            )
                        }
                        Switch(
                            checked = units.useFahrenheit,
                            onCheckedChange = { onToggleTemperatureUnit() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AmberSol.copy(alpha = 0.75f),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = CyanZephyr.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.testTag("celsius_fahrenheit_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    UnitToggleRow(
                        title = if (isGreek) "Ταχύτητα Ανέμου" else "Wind Speed Unit",
                        currentValue = if (units.useMph) "mph" else "km/h",
                        onToggle = onToggleWindUnit,
                        testTag = "toggle_wind_unit"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    UnitToggleRow(
                        title = if (isGreek) "Ατμοσφαιρική Πίεση" else "Atmospheric Pressure",
                        currentValue = if (units.useInHg) "inHg" else "hPa",
                        onToggle = onTogglePressureUnit,
                        testTag = "toggle_pressure_unit"
                    )
                }
            }
        }

        // Section 6: Weather Forecast Models (2-Column Grid of Colorful Model Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isGreek) "Μετεωρολογικά Μοντέλα Πρόγνωσης" else "Weather Forecast Models",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                val providers = WeatherProvider.entries
                providers.chunked(2).forEach { rowProviders ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowProviders.forEach { provider ->
                            val isSelected = provider == selectedProvider
                            val modelAccent = if (isSelected) AmberSol else CyanGlow
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable { onSelectProvider(provider) }
                                    .testTag("provider_card_${provider.id}"),
                                shape = RoundedCornerShape(26.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF14355B).copy(alpha = 0.78f) else Color(0xFF0E223A).copy(alpha = 0.65f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.3.dp else 1.dp,
                                    color = if (isSelected) AmberSol else CyanZephyr.copy(alpha = 0.40f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = WeatherIcons.CompassCalibration,
                                                contentDescription = null,
                                                tint = modelAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = provider.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = provider.localizedSubtitle(language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.82f),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = if (isSelected) {
                                                if (isGreek) "Ενεργό (${provider.resolutionBadge})" else "Active (${provider.resolutionBadge})"
                                            } else {
                                                if (isGreek) "Επιλογή" else "Select"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = modelAccent
                                        )
                                    }
                                }
                            }
                        }
                        if (rowProviders.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Section 7: Auto-Refresh & Zero-Drain Battery Sync
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_saver_sync_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Refresh,
                            contentDescription = null,
                            tint = EmeraldBreeze,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Αυτόματη Ανανέωση & Μπαταρία" else "Auto-Refresh & Battery Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoRefreshInterval.entries.forEach { interval ->
                            val isSelected = interval == refreshInterval
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) EmeraldBreeze.copy(alpha = 0.25f) else Color(0xFF0A192F).copy(alpha = 0.55f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) EmeraldBreeze else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectRefreshInterval(interval) }
                                    .testTag("refresh_interval_card_${interval.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = interval.badge,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Συγχρονισμός Μηδενικής Κατανάλωσης" else "Zero-Drain Background Sync",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = if (isGreek) {
                                    "Παύση χρονιστών όταν η οθόνη είναι κλειστή."
                                } else {
                                    "Pauses rapid timers when screen is off to save battery."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                        }
                        Switch(
                            checked = batterySaverSyncEnabled,
                            onCheckedChange = { onToggleBatterySaverSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldBreeze.copy(alpha = 0.75f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.80f),
                                uncheckedTrackColor = Color(0xFF0A192F)
                            ),
                            modifier = Modifier.testTag("battery_saver_sync_switch")
                        )
                    }
                }
            }
        }

        // Section 8: Meteorological Alerts (EMY & Rain)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alerts_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(
                    1.1.dp,
                    Brush.horizontalGradient(listOf(CoralWarmth.copy(alpha = 0.60f), AmberSol.copy(alpha = 0.35f)))
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.NotificationsActive,
                            contentDescription = null,
                            tint = CoralWarmth,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Ειδοποιήσεις ΕΜΥ & Βροχής" else "EMY & Rain Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Έκτακτα Δελτία ΕΜΥ" else "EMY Severe Weather Warnings",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = if (isGreek) {
                                    "Ειδοποιήσεις της Εθνικής Μετεωρολογικής Υπηρεσίας."
                                } else {
                                    "Hellenic National Meteorological Service emergency bulletins."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
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
                                checkedTrackColor = CoralWarmth,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.80f),
                                uncheckedTrackColor = Color(0xFF0A192F)
                            ),
                            modifier = Modifier.testTag("emy_alerts_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Ειδοποίηση Έναρξης Βροχής" else "Rain Onset & Storm Alerts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = if (isGreek) {
                                    "Άμεση ειδοποίηση πριν την ανίχνευση βροχής στο ραντάρ."
                                } else {
                                    "Instant notification before precipitation is detected on radar."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
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
                                checkedTrackColor = CyanGlow,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.80f),
                                uncheckedTrackColor = Color(0xFF0A192F)
                            ),
                            modifier = Modifier.testTag("rain_alerts_switch")
                        )
                    }
                }
            }
        }

        // Section 9: In-App Feedback & Support (No email client required)
        item {
            var selectedCategory by remember { mutableStateOf("General") }
            var rating by remember { mutableIntStateOf(5) }
            var feedbackMessage by remember { mutableStateOf("") }
            var feedbackSentConfirmation by remember { mutableStateOf(false) }

            val categories = listOf(
                "General" to (if (isGreek) "Γενικά" else "General"),
                "Bug Report" to (if (isGreek) "Σφάλμα" else "Bug Report"),
                "Weather Accuracy" to (if (isGreek) "Ακρίβεια" else "Accuracy"),
                "Feature Request" to (if (isGreek) "Πρόταση" else "Feature")
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_settings_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)),
                border = BorderStroke(
                    1.1.dp,
                    Brush.horizontalGradient(listOf(CyanGlow.copy(alpha = 0.60f), AmberSol.copy(alpha = 0.35f)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = WeatherIcons.Send,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isGreek) "Σχόλια & Υποστήριξη" else "In-App Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (star <= rating) AmberSol.copy(alpha = 0.25f) else Color(0xFF0A192F).copy(alpha = 0.70f),
                                    border = BorderStroke(1.dp, if (star <= rating) AmberSol else Color.White.copy(alpha = 0.16f)),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (id, label) ->
                            val isSelected = selectedCategory == id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF3F436E) else Color(0xFF0A192F).copy(alpha = 0.65f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanGlow else Color.White.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedCategory = id }
                                    .testTag("feedback_category_${id.lowercase().replace(' ', '_')}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
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
                                text = if (isGreek) {
                                    "Γράψτε τα σχόλιά σας ή προτάσεις για την εφαρμογή..."
                                } else {
                                    "Write your feedback, bug report, or suggestions..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0A192F).copy(alpha = 0.75f),
                            unfocusedContainerColor = Color(0xFF0A192F).copy(alpha = 0.60f),
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                            cursorColor = CyanGlow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_feedback_input")
                    )

                    if (feedbackSentConfirmation) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldBreeze.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, EmeraldBreeze),
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
                                    text = if (isGreek) {
                                        "Τα σχόλιά σας καταχωρήθηκαν επιτυχώς!"
                                    } else {
                                        "Your feedback has been submitted!"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isGreek) "Αποστολή Σχολίων" else "Submit Feedback",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSendFeedbackEmail(selectedCategory, rating, feedbackMessage)
                                    feedbackSentConfirmation = true
                                    feedbackMessage = ""
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("settings_send_feedback_button")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Two-option Material You Segmented Pill Bar ("Recommended" | "All services")
 * styled after Android 15 / Google Settings dark theme.
 */
@Composable
fun MaterialYouSegmentedPillBar(
    leftLabel: String,
    rightLabel: String,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    leftTag: String,
    rightTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val isLeftSelected = selectedIndex == 0
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isLeftSelected) Color(0xFF3F436E) else Color(0xFF0E223A).copy(alpha = 0.62f),
            border = BorderStroke(
                width = 1.1.dp,
                color = if (isLeftSelected) CyanGlow else Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelectIndex(0) }
                .testTag(leftTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = leftLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isLeftSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLeftSelected) Color.White else Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val isRightSelected = selectedIndex == 1
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isRightSelected) Color(0xFF3F436E) else Color(0xFF0E223A).copy(alpha = 0.62f),
            border = BorderStroke(
                width = 1.1.dp,
                color = if (isRightSelected) AmberSol else Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelectIndex(1) }
                .testTag(rightTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isRightSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isRightSelected) Color.White else Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2-Column Colorful Material You Bento Service Card:
 * - Translucent glassmorphic surface over the live sky backdrop with 26.dp rounded corners
 * - Glowing accent border & colored icon badge on top-left beside a 1-2 line title
 * - Conversational multi-line description
 * - Bottom-right colored action text
 */
@Composable
fun MaterialYouBentoServiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    accent: Color = CyanGlow
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onCardClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E223A).copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = 1.1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.22f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 21.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onActionClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
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
        Text(
            text = currentValue,
            style = MaterialTheme.typography.labelLarge,
            color = AmberSol,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
