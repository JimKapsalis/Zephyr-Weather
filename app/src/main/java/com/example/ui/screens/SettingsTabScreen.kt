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
    appVersionLabel: String = "v1.3.1 (Build 14)",
    lastUpdateCheckStatus: String? = null,
    githubRepoSlug: String = "JimKapsalis/Zephyr-Weather",
    latestReleaseTag: String = "v1.3.1",
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
            .background(M3DarkBackground.copy(alpha = 0.88f))
            .testTag("settings_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Segmented Pill Switcher ("Recommended" | "All settings") matching the reference UI
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

        // 2. 2-Column Material You Bento Cards Grid (6 cards in 3 rows of 2)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: App Updater & Discord Server
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.Refresh,
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

                // Row 2: Forecast Models & GPS Location Hub
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

                // Row 3: Units & Language + Weather Alerts & Sync
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MaterialYouBentoServiceCard(
                        icon = WeatherIcons.Public,
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

        // Top Section 1: Official Discord Community Server Card (Matte Material You surface)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_discord_server_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = M3CardSurface
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
                        Icon(
                            imageVector = WeatherIcons.Send,
                            contentDescription = "Discord Server",
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Επίσημος Discord Server" else "Official Discord Community",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = M3TitleText
                            )
                            Text(
                                text = discordInviteUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = M3PeriwinkleAction
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
                        color = M3BodyText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isGreek) "Είσοδος στον Discord Server" else "Join Discord Server",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = M3PeriwinkleAction,
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

        // Top Section 2: GitHub Releases App Updater & 1.8k+ (1,803) Downloads Counter
        item {
            var repoInput by remember(githubRepoSlug) { mutableStateOf(githubRepoSlug) }
            val defaultChangelog = if (isGreek) {
                "• Material You Dark UI με 2-στηλο Bento Grid & Segmented Pill Switcher\n" +
                    "• Έξυπνη Διατήρηση Πόλεων (Αθήνα + GPS τοποθεσία χωρίς διαγραφή)\n" +
                    "• 60fps Κινούμενα Εικονίδια Καιρού & Νυχτερινό Φόντο με Βροχή & Σύννεφα\n" +
                    "• Επίσημος Discord Server (discord.gg/aHynb4QKC) & 1.8k+ (1,803) Λήψεις APK"
            } else {
                "• Material You Dark UI with 2-Column Bento Cards & Segmented Pill Switcher\n" +
                    "• Smart City Deduplication & Preserved Saved Cities (Athens + GPS location stay intact)\n" +
                    "• 60fps Animated Weather Icons & Night Moon Sky Backdrop with Live Rain & Clouds\n" +
                    "• Official Discord Server (discord.gg/aHynb4QKC) & 1.8k+ (1,803) Total APK Downloads"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_app_updater_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = M3CardSurface
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
                                imageVector = WeatherIcons.Refresh,
                                contentDescription = null,
                                tint = M3IconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Zephyr Weather $appVersionLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = M3TitleText
                                )
                                Text(
                                    text = if (isGreek) {
                                        "GitHub Release: $latestReleaseTag • $formattedDownloads Συνολικές Λήψεις APK"
                                    } else {
                                        "GitHub Release: $latestReleaseTag • $formattedDownloads Total APK Downloads"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = M3PeriwinkleAction
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = M3PillSelectedContainer
                        ) {
                            Text(
                                text = when {
                                    isCheckingUpdates -> if (isGreek) "Έλεγχος…" else "Checking…"
                                    isNewGitHubReleaseAvailable -> if (isGreek) "Νέα $latestReleaseTag" else "Update $latestReleaseTag"
                                    else -> if (isGreek) "Ενημερωμένο" else "Up to date"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = M3TitleText,
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
                        color = M3BodyText
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = M3DarkBackground.copy(alpha = 0.65f),
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
                                    text = if (isGreek) "Αρχείο Αλλαγών ($latestReleaseTag)" else "Release Changelog ($latestReleaseTag)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = M3TitleText
                                )
                                Text(
                                    text = if (isGreek) "Λήψη APK" else "Download APK",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = M3PeriwinkleAction
                                )
                            }
                            Text(
                                text = latestReleaseNotes?.takeIf { it.isNotBlank() } ?: defaultChangelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = M3BodyText,
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
                                text = if (isGreek) "Αποθετήριο GitHub (owner/repo)" else "GitHub Repository (owner/repo)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = M3TitleText,
                            unfocusedTextColor = M3TitleText,
                            focusedContainerColor = M3DarkBackground.copy(alpha = 0.65f),
                            unfocusedContainerColor = M3DarkBackground.copy(alpha = 0.50f),
                            focusedBorderColor = M3PeriwinkleAction,
                            unfocusedBorderColor = M3SubtleBorder,
                            focusedLabelColor = M3PeriwinkleAction,
                            unfocusedLabelColor = M3BodyText,
                            cursorColor = M3PeriwinkleAction
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
                            color = M3BodyText,
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
                                fontWeight = FontWeight.SemiBold,
                                color = M3PeriwinkleAction,
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
                                fontWeight = FontWeight.SemiBold,
                                color = M3PeriwinkleAction,
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
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
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Γλώσσα Εφαρμογής" else "App Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TitleText
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
                                color = if (isSelected) M3PillSelectedContainer else M3DarkBackground.copy(alpha = 0.55f),
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
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) M3TitleText else M3BodyText,
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
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
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Αυτόματη Τοποθεσία GPS" else "Automatic GPS Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TitleText
                        )
                    }

                    Text(
                        text = if (isGreek) {
                            "Εντοπίστε αυτόματα την τρέχουσα πόλη και το υψόμετρό σας ή ανοίξτε την αναζήτηση τοποθεσίας."
                        } else {
                            "Auto-detect your current city, coordinates, and altitude or open the location search hub."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = M3BodyText
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
                            color = M3BodyText,
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
                            fontWeight = FontWeight.SemiBold,
                            color = M3PeriwinkleAction,
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Speed,
                            contentDescription = null,
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Μονάδες Μέτρησης" else "Measurement Units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TitleText
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
                                color = M3TitleText
                            )
                            Text(
                                text = if (units.useFahrenheit) {
                                    if (isGreek) "Fahrenheit (°F)" else "Fahrenheit (°F)"
                                } else {
                                    if (isGreek) "Κελσίου (°C)" else "Celsius (°C)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = M3PeriwinkleAction
                            )
                        }
                        Switch(
                            checked = units.useFahrenheit,
                            onCheckedChange = { onToggleTemperatureUnit() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = M3TitleText,
                                checkedTrackColor = M3PillSelectedContainer,
                                uncheckedThumbColor = M3TitleText,
                                uncheckedTrackColor = M3DarkBackground
                            ),
                            modifier = Modifier.testTag("celsius_fahrenheit_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.07f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    UnitToggleRow(
                        title = if (isGreek) "Ταχύτητα Ανέμου" else "Wind Speed Unit",
                        currentValue = if (units.useMph) "mph" else "km/h",
                        onToggle = onToggleWindUnit,
                        testTag = "toggle_wind_unit"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.07f),
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

        // Section 6: Weather Forecast Models (2-Column Grid of Model Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isGreek) "Μετεωρολογικά Μοντέλα Πρόγνωσης" else "Weather Forecast Models",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = M3TitleText,
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
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable { onSelectProvider(provider) }
                                    .testTag("provider_card_${provider.id}"),
                                shape = RoundedCornerShape(26.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) M3PillSelectedContainer else M3CardSurface
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
                                                tint = if (isSelected) M3PeriwinkleAction else M3IconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = provider.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = M3TitleText,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = provider.localizedSubtitle(language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = M3BodyText,
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
                                            fontWeight = FontWeight.SemiBold,
                                            color = M3PeriwinkleAction
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
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
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Αυτόματη Ανανέωση & Μπαταρία" else "Auto-Refresh & Battery Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TitleText
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
                                color = if (isSelected) M3PillSelectedContainer else M3DarkBackground.copy(alpha = 0.55f),
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
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) M3TitleText else M3BodyText,
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
                                color = M3TitleText
                            )
                            Text(
                                text = if (isGreek) {
                                    "Παύση χρονιστών όταν η οθόνη είναι κλειστή."
                                } else {
                                    "Pauses rapid timers when screen is off to save battery."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = M3BodyText
                            )
                        }
                        Switch(
                            checked = batterySaverSyncEnabled,
                            onCheckedChange = { onToggleBatterySaverSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = M3TitleText,
                                checkedTrackColor = M3PillSelectedContainer,
                                uncheckedThumbColor = M3BodyText,
                                uncheckedTrackColor = M3DarkBackground
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.NotificationsActive,
                            contentDescription = null,
                            tint = M3IconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isGreek) "Ειδοποιήσεις ΕΜΥ & Βροχής" else "EMY & Rain Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TitleText
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
                                color = M3TitleText
                            )
                            Text(
                                text = if (isGreek) {
                                    "Ειδοποιήσεις της Εθνικής Μετεωρολογικής Υπηρεσίας."
                                } else {
                                    "Hellenic National Meteorological Service emergency bulletins."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = M3BodyText
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
                                checkedThumbColor = M3TitleText,
                                checkedTrackColor = M3PillSelectedContainer,
                                uncheckedThumbColor = M3BodyText,
                                uncheckedTrackColor = M3DarkBackground
                            ),
                            modifier = Modifier.testTag("emy_alerts_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.07f),
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
                                color = M3TitleText
                            )
                            Text(
                                text = if (isGreek) {
                                    "Άμεση ειδοποίηση πριν την ανίχνευση βροχής στο ραντάρ."
                                } else {
                                    "Instant notification before precipitation is detected on radar."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = M3BodyText
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
                                checkedThumbColor = M3TitleText,
                                checkedTrackColor = M3PillSelectedContainer,
                                uncheckedThumbColor = M3BodyText,
                                uncheckedTrackColor = M3DarkBackground
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
                colors = CardDefaults.cardColors(containerColor = M3CardSurface)
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
                                tint = M3IconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isGreek) "Σχόλια & Υποστήριξη" else "In-App Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = M3TitleText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (star <= rating) M3PillSelectedContainer else M3DarkBackground,
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
                                            color = if (star <= rating) M3PeriwinkleAction else M3BodyText.copy(alpha = 0.45f)
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
                                color = if (isSelected) M3PillSelectedContainer else M3DarkBackground.copy(alpha = 0.55f),
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
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) M3TitleText else M3BodyText,
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
                                color = M3BodyText.copy(alpha = 0.65f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = M3TitleText,
                            unfocusedTextColor = M3TitleText,
                            focusedContainerColor = M3DarkBackground.copy(alpha = 0.65f),
                            unfocusedContainerColor = M3DarkBackground.copy(alpha = 0.50f),
                            focusedBorderColor = M3PeriwinkleAction,
                            unfocusedBorderColor = M3SubtleBorder,
                            cursorColor = M3PeriwinkleAction
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_feedback_input")
                    )

                    if (feedbackSentConfirmation) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = M3PillSelectedContainer,
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
                                    tint = M3PeriwinkleAction,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isGreek) {
                                        "Τα σχόλιά σας καταχωρήθηκαν επιτυχώς!"
                                    } else {
                                        "Your feedback has been submitted!"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = M3TitleText
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
                            fontWeight = FontWeight.SemiBold,
                            color = M3PeriwinkleAction,
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
            color = if (isLeftSelected) M3PillSelectedContainer else M3PillUnselectedContainer,
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
                    fontWeight = if (isLeftSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isLeftSelected) M3TitleText else M3BodyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val isRightSelected = selectedIndex == 1
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isRightSelected) M3PillSelectedContainer else M3PillUnselectedContainer,
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
                    fontWeight = if (isRightSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isRightSelected) M3TitleText else M3BodyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2-Column Material You Bento Service Card matching the user's reference screenshot:
 * - Matte dark slate surface (#282832) with 26.dp rounded corners
 * - Icon on top-left beside a 1-2 line title
 * - Conversational multi-line description in muted lavender-gray (#C7C5D0)
 * - Bottom-right periwinkle action text (#B5C4FF)
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onCardClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = M3CardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = M3IconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        color = M3TitleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = M3BodyText,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = M3PeriwinkleAction,
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
            color = M3TitleText
        )
        Text(
            text = currentValue,
            style = MaterialTheme.typography.labelLarge,
            color = M3PeriwinkleAction,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
