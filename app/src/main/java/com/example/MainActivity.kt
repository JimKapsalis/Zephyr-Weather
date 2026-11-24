package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SavedCityEntity
import com.example.model.AppLanguage
import com.example.model.WmoWeatherMapper
import com.example.ui.LoadingState
import com.example.ui.WeatherNavTab
import com.example.ui.WeatherViewModel
import com.example.ui.components.WeatherSkyBackdrop
import com.example.ui.screens.CitiesTabScreen
import com.example.ui.screens.ForecastTabScreen
import com.example.ui.screens.RadarTabScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.rememberLiveGreeceClock
import com.example.ui.theme.AmberSol
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanZephyr
import com.example.ui.theme.EmeraldBreeze
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WeatherIcons
import com.example.util.SevereWeatherNotifier
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            ZephyrWeatherApp(fusedLocationClient = fusedLocationClient)
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchCurrentCoordinatesWithFusedClient(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: WeatherViewModel
) {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        viewModel.onLocationPermissionDenied()
        return
    }

    viewModel.onFusedLocationSearchStarted()
    val priority = if (hasFine) {
        Priority.PRIORITY_HIGH_ACCURACY
    } else {
        Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }
    val cancellationTokenSource = CancellationTokenSource()
    runCatching {
        fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null && (kotlin.math.abs(location.latitude) >= 0.01 || kotlin.math.abs(location.longitude) >= 0.01)) {
                    viewModel.onGpsCoordinatesResolved(
                        context = context,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude
                    )
                } else {
                    runCatching {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null && (kotlin.math.abs(lastLoc.latitude) >= 0.01 || kotlin.math.abs(lastLoc.longitude) >= 0.01)) {
                                    viewModel.onGpsCoordinatesResolved(
                                        context = context,
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude,
                                        altitude = lastLoc.altitude
                                    )
                                } else {
                                    viewModel.detectGpsLocation(context)
                                }
                            }
                            .addOnFailureListener {
                                viewModel.detectGpsLocation(context)
                            }
                    }.onFailure {
                        viewModel.detectGpsLocation(context)
                    }
                }
            }
            .addOnFailureListener {
                viewModel.detectGpsLocation(context)
            }
    }.onFailure {
        viewModel.detectGpsLocation(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZephyrWeatherApp(
    viewModel: WeatherViewModel = viewModel(factory = WeatherViewModel.provideFactory(LocalContext.current)),
    fusedLocationClient: FusedLocationProviderClient? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeFusedClient = remember(context, fusedLocationClient) {
        fusedLocationClient ?: LocationServices.getFusedLocationProviderClient(context)
    }

    var pendingTestAlertAfterNotifPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissions(
            locationGranted = hasFine || hasCoarse,
            notificationGranted = granted
        )
        if (granted) {
            if (pendingTestAlertAfterNotifPermission) {
                pendingTestAlertAfterNotifPermission = false
                viewModel.triggerLiveSevereWeatherAlertTest(context)
            } else {
                viewModel.onNotificationPermissionGranted(context)
            }
        } else if (pendingTestAlertAfterNotifPermission) {
            pendingTestAlertAfterNotifPermission = false
            // Still update in-app alert cards and open system notification settings if blocked
            viewModel.triggerLiveSevereWeatherAlertTest(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }
        }
    }

    val triggerTestAlertWithPermission: () -> Unit = {
        val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val canPostNow = SevereWeatherNotifier.canPostNotifications(context)
        if (hasNotifPerm && canPostNow) {
            viewModel.triggerLiveSevereWeatherAlertTest(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
            pendingTestAlertAfterNotifPermission = true
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }.onFailure {
                pendingTestAlertAfterNotifPermission = false
                viewModel.triggerLiveSevereWeatherAlertTest(context)
            }
        } else {
            // Permission granted at manifest level, but disabled in Android system notification settings
            viewModel.triggerLiveSevereWeatherAlertTest(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.updatePermissions(
            locationGranted = granted,
            notificationGranted = notifGranted
        )
        if (granted) {
            fetchCurrentCoordinatesWithFusedClient(
                context = context,
                fusedLocationClient = activeFusedClient,
                viewModel = viewModel
            )
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val triggerGpsLocate: () -> Unit = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchCurrentCoordinatesWithFusedClient(
                context = context,
                fusedLocationClient = activeFusedClient,
                viewModel = viewModel
            )
        } else {
            runCatching {
                val perms = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                locationPermissionLauncher.launch(perms)
            }.onFailure {
                viewModel.detectGpsLocation(context)
            }
        }
    }

    // On launch, sync current permission states and fetch coordinates via FusedLocationProviderClient if permitted
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.updatePermissions(
            locationGranted = hasFine || hasCoarse,
            notificationGranted = hasNotif
        )
        val prefs = com.example.data.WeatherPreferences(context)
        val initialGpsDone = prefs.hasCompletedInitialGpsSetup()
        if ((hasFine || hasCoarse) && !initialGpsDone && state.savedCities.isEmpty()) {
            fetchCurrentCoordinatesWithFusedClient(
                context = context,
                fusedLocationClient = activeFusedClient,
                viewModel = viewModel
            )
        } else {
            viewModel.autoDetectLocationOnLoad(context, hasFine || hasCoarse)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForegroundChanged(true)
                Lifecycle.Event.ON_STOP -> viewModel.onAppForegroundChanged(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = state.isLocationSetupOpen) {
        viewModel.closeLocationSetup()
    }

    BackHandler(enabled = !state.isLocationSetupOpen && state.activeTab != WeatherNavTab.FORECAST) {
        viewModel.selectTab(WeatherNavTab.FORECAST)
    }

    MyApplicationTheme(themeSetting = state.selectedTheme) {
        val currentLoadingState = state.loadingState
        val activeBundle = (currentLoadingState as? LoadingState.Success)?.bundle
        val liveClock = rememberLiveGreeceClock(activeBundle?.timezoneId)
        val sunsetTime = activeBundle?.daily?.firstOrNull()?.sunsetTime ?: "18:05"
        val visualSpec = remember(activeBundle, state.selectedHourIndex, liveClock, sunsetTime) {
            if (activeBundle != null) {
                val inspectedHour = activeBundle.hourly.getOrNull(state.selectedHourIndex)
                if (state.selectedHourIndex > 0 && inspectedHour != null) {
                    val isHourNight = !inspectedHour.isDay || WmoWeatherMapper.isNightHourLabel(inspectedHour.displayHour)
                    val isHourSunset = WmoWeatherMapper.isSunsetClockTime(inspectedHour.displayHour, sunsetTime)
                    WmoWeatherMapper.resolve(
                        code = inspectedHour.weatherCode,
                        isDay = !isHourNight,
                        isSunset = isHourSunset
                    )
                } else {
                    val isLiveNight = !activeBundle.current.isDay || WmoWeatherMapper.isNightHourLabel(liveClock)
                    val isLiveSunset = WmoWeatherMapper.isSunsetClockTime(liveClock, sunsetTime)
                    WmoWeatherMapper.resolve(
                        code = activeBundle.current.weatherCode,
                        isDay = !isLiveNight,
                        isSunset = isLiveSunset
                    )
                }
            } else {
                null
            }
        }

        // Automatically dismiss any status banner once shown so it never stays stuck on screen
        LaunchedEffect(state.statusBanner) {
            if (state.statusBanner != null) {
                delay(2200)
                viewModel.dismissBanner()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07111E))
        ) {
            if (visualSpec != null) {
                WeatherSkyBackdrop(
                    visualSpec = visualSpec,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.openLocationSetup() }
                                    .testTag("app_brand_logo_header")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF102542).copy(alpha = 0.90f),
                                    border = BorderStroke(1.2.dp, CyanZephyr.copy(alpha = 0.50f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                            contentDescription = stringResource(R.string.app_name),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val topBarCityName = activeBundle?.cityName
                                        ?: state.savedCities.find { it.id == state.selectedCityId }?.name
                                        ?: state.savedCities.firstOrNull()?.name
                                    if (!topBarCityName.isNullOrBlank()) {
                                        Text(
                                            text = "$topBarCityName • $liveClock",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanGlow,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Location Setup / Search Quick Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldBreeze.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.openLocationSetup() }
                                    .testTag("top_bar_location_setup_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WeatherIcons.MyLocation,
                                        contentDescription = "Setup Location",
                                        tint = EmeraldBreeze,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            // Language Quick Toggle Pill (EN / ΕΛ)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewModel.toggleAppLanguage() }
                                    .testTag("language_toggle_button")
                            ) {
                                Text(
                                    text = state.language.shortBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanGlow,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // Unit Toggle Pill Button (°C / °F)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AmberSol.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewModel.toggleTemperatureUnit() }
                                    .testTag("unit_toggle_button")
                            ) {
                                Text(
                                    text = if (state.units.useFahrenheit) "°F" else "°C",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberSol,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        ),
                        modifier = Modifier.testTag("weather_top_bar")
                    )
                },
                bottomBar = {
                    ZephyrBottomNavBar(
                        activeTab = state.activeTab,
                        language = state.language,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (state.isLocationSetupOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("location_setup_modal_container")
                        ) {
                            LocationSetupSearchingScreen(
                                language = state.language,
                                isLocatingGps = state.isLocatingGps,
                                searchQuery = state.searchQuery,
                                searchResults = state.searchResults,
                                isSearching = state.isSearching,
                                suggestedCities = state.suggestedCities,
                                canDismiss = state.primaryBundle != null || state.savedCities.isNotEmpty(),
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onSelectCity = { viewModel.addAndSelectCity(it) },
                                onRequestGpsLocation = triggerGpsLocate,
                                onDismiss = {
                                    if (state.primaryBundle != null) {
                                        viewModel.closeLocationSetup()
                                    } else {
                                        state.savedCities.firstOrNull()?.let { viewModel.selectCity(it) }
                                    }
                                }
                            )
                        }
                    } else {
                        when (state.activeTab) {
                            WeatherNavTab.FORECAST -> {
                                when (currentLoadingState) {
                                    is LoadingState.Loading -> {
                                        if (state.isLocatingGps || state.savedCities.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("loading_empty_scaffold")
                                            ) {
                                                LocationSetupSearchingScreen(
                                                    language = state.language,
                                                    isLocatingGps = state.isLocatingGps,
                                                    searchQuery = state.searchQuery,
                                                    searchResults = state.searchResults,
                                                    isSearching = state.isSearching,
                                                    suggestedCities = state.suggestedCities,
                                                    canDismiss = state.savedCities.isNotEmpty(),
                                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                                    onSelectCity = { viewModel.addAndSelectCity(it) },
                                                    onRequestGpsLocation = triggerGpsLocate,
                                                    onDismiss = {
                                                        state.savedCities.firstOrNull()?.let { viewModel.selectCity(it) }
                                                    }
                                                )
                                            }
                                        } else {
                                            ForecastTabScreen(
                                                bundle = state.primaryBundle,
                                                savedCities = state.savedCities,
                                                suggestedCities = state.suggestedCities,
                                                selectedCityId = state.selectedCityId,
                                                selectedProvider = state.selectedProvider,
                                                chartMode = state.chartMode,
                                                selectedHourIndex = state.selectedHourIndex,
                                                expandedDayIndex = state.expandedDayIndex,
                                                units = state.units,
                                                language = state.language,
                                                skyAtmospherePreset = state.skyAtmospherePreset,
                                                isRefreshing = state.isRefreshing,
                                                isLocatingGps = state.isLocatingGps,
                                                searchQuery = state.searchQuery,
                                                searchResults = state.searchResults,
                                                isSearching = state.isSearching,
                                                animationTriggerKey = state.animationTriggerKey,
                                                activeSevereAlert = state.activeSevereAlert,
                                                activeSevereAlerts = state.activeSevereAlerts,
                                                dismissedAlertIds = state.dismissedAlertIds,
                                                onRefresh = { viewModel.refreshCurrentWeather() },
                                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                                onAddAndSelectCity = { viewModel.addAndSelectCity(it) },
                                                onRequestGpsLocation = triggerGpsLocate,
                                                onTriggerTestAlert = triggerTestAlertWithPermission,
                                                onDismissAlert = { viewModel.dismissSevereAlert(it) },
                                                onRestoreDismissedAlerts = { viewModel.restoreDismissedAlerts() },
                                                onSelectSkyPreset = { viewModel.setSkyAtmospherePreset(it) },
                                                onSelectCity = { viewModel.selectCity(it, switchToForecastTab = false) },
                                                onSelectChartMode = { viewModel.setChartMode(it) },
                                                onSelectHourIndex = { viewModel.selectHourIndex(it) },
                                                onToggleExpandDay = { viewModel.toggleExpandedDay(it) },
                                                onNavigateToRadar = { viewModel.selectTab(WeatherNavTab.RADAR) }
                                            )
                                        }
                                    }

                                    is LoadingState.Error -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("loading_error_scaffold"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentLoadingState.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        }
                                    }

                                    is LoadingState.Success -> {
                                        ForecastTabScreen(
                                            bundle = currentLoadingState.bundle,
                                            savedCities = state.savedCities,
                                            suggestedCities = state.suggestedCities,
                                            selectedCityId = state.selectedCityId,
                                            selectedProvider = state.selectedProvider,
                                            chartMode = state.chartMode,
                                            selectedHourIndex = state.selectedHourIndex,
                                            expandedDayIndex = state.expandedDayIndex,
                                            units = state.units,
                                            language = state.language,
                                            skyAtmospherePreset = state.skyAtmospherePreset,
                                            isRefreshing = state.isRefreshing,
                                            isLocatingGps = state.isLocatingGps,
                                            searchQuery = state.searchQuery,
                                            searchResults = state.searchResults,
                                            isSearching = state.isSearching,
                                            animationTriggerKey = state.animationTriggerKey,
                                            activeSevereAlert = state.activeSevereAlert,
                                            activeSevereAlerts = state.activeSevereAlerts,
                                            dismissedAlertIds = state.dismissedAlertIds,
                                            onRefresh = { viewModel.refreshCurrentWeather() },
                                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                            onAddAndSelectCity = { viewModel.addAndSelectCity(it) },
                                            onRequestGpsLocation = triggerGpsLocate,
                                            onTriggerTestAlert = triggerTestAlertWithPermission,
                                            onDismissAlert = { viewModel.dismissSevereAlert(it) },
                                            onRestoreDismissedAlerts = { viewModel.restoreDismissedAlerts() },
                                            onSelectSkyPreset = { viewModel.setSkyAtmospherePreset(it) },
                                            onSelectCity = { viewModel.selectCity(it, switchToForecastTab = false) },
                                            onSelectChartMode = { viewModel.setChartMode(it) },
                                            onSelectHourIndex = { viewModel.selectHourIndex(it) },
                                            onToggleExpandDay = { viewModel.toggleExpandedDay(it) },
                                            onNavigateToRadar = { viewModel.selectTab(WeatherNavTab.RADAR) }
                                        )
                                    }
                                }
                            }

                            WeatherNavTab.RADAR -> {
                                when (currentLoadingState) {
                                    is LoadingState.Success -> {
                                        RadarTabScreen(
                                            bundle = currentLoadingState.bundle,
                                            units = state.units,
                                            language = state.language,
                                            selectedLayer = state.radarLayer,
                                            selectedTimelineIndex = state.radarTimelineIndex,
                                            isRefreshing = state.isRefreshing,
                                            onRefresh = { viewModel.refreshCurrentWeather() },
                                            onSelectLayer = { viewModel.selectRadarLayer(it) },
                                            onSelectTimelineIndex = { viewModel.selectRadarTimelineIndex(it) }
                                        )
                                    }

                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("loading_empty_scaffold")
                                        ) {
                                            LocationSetupSearchingScreen(
                                                language = state.language,
                                                isLocatingGps = state.isLocatingGps,
                                                searchQuery = state.searchQuery,
                                                searchResults = state.searchResults,
                                                isSearching = state.isSearching,
                                                suggestedCities = state.suggestedCities,
                                                canDismiss = state.savedCities.isNotEmpty(),
                                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                                onSelectCity = { viewModel.addAndSelectCity(it) },
                                                onRequestGpsLocation = triggerGpsLocate,
                                                onDismiss = {
                                                    state.savedCities.firstOrNull()?.let { viewModel.selectCity(it) }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            WeatherNavTab.CITIES -> CitiesTabScreen(
                                savedCities = state.savedCities,
                                suggestedCities = state.suggestedCities,
                                selectedCityId = state.selectedCityId,
                                searchQuery = state.searchQuery,
                                searchResults = state.searchResults,
                                isSearching = state.isSearching,
                                isRefreshing = state.isRefreshing,
                                units = state.units,
                                language = state.language,
                                onRefresh = { viewModel.refreshCurrentWeather() },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onAddCity = { viewModel.addAndSelectCity(it) },
                                onSelectCity = { viewModel.selectCity(it, switchToForecastTab = true) },
                                onTogglePinCity = { viewModel.togglePinCity(it) },
                                onRemoveCity = { viewModel.removeCity(it) }
                            )

                            WeatherNavTab.SETTINGS -> SettingsTabScreen(
                                bundle = state.primaryBundle,
                                selectedProvider = state.selectedProvider,
                                selectedTheme = state.selectedTheme,
                                refreshInterval = state.refreshInterval,
                                batterySaverSyncEnabled = state.batterySaverSyncEnabled,
                                units = state.units,
                                language = state.language,
                                emyAlertsEnabled = state.emyAlertsEnabled,
                                rainAlertsEnabled = state.rainAlertsEnabled,
                                onSelectLanguage = { viewModel.setAppLanguage(it) },
                                onSelectProvider = { viewModel.setWeatherProvider(it) },
                                onSelectTheme = { viewModel.setAppTheme(it) },
                                onSelectRefreshInterval = { viewModel.setRefreshInterval(it) },
                                onToggleBatterySaverSync = { viewModel.toggleBatterySaverSync() },
                                onToggleTemperatureUnit = { viewModel.toggleTemperatureUnit() },
                                onToggleWindUnit = { viewModel.toggleWindUnit() },
                                onTogglePressureUnit = { viewModel.togglePressureUnit() },
                                onToggleEmyAlerts = { viewModel.toggleEmyAlerts() },
                                onToggleRainAlerts = { viewModel.toggleRainAlerts() },
                                onRequestGpsLocation = triggerGpsLocate,
                                onOpenLocationSetup = { viewModel.openLocationSetup() },
                                onTriggerTestAlert = triggerTestAlertWithPermission,
                                isCheckingUpdates = state.isCheckingUpdates,
                                appVersionLabel = state.appVersionLabel,
                                lastUpdateCheckStatus = state.lastUpdateCheckStatus,
                                onCheckForUpdates = { viewModel.checkForAppAndWeatherUpdates(context) },
                                onSendFeedbackEmail = { category, rating, message ->
                                    viewModel.sendFeedbackEmail(context, category, rating, message)
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.statusBanner != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        state.statusBanner?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF132A46).copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("status_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = WeatherIcons.LocationOn,
                                            contentDescription = null,
                                            tint = CyanZephyr,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = WeatherIcons.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
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
}

@Composable
fun ZephyrBottomNavBar(
    activeTab: WeatherNavTab,
    language: AppLanguage = AppLanguage.ENGLISH,
    onSelectTab: (WeatherNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(language) {
        listOf(
            NavigationItemData(
                tab = WeatherNavTab.FORECAST,
                label = if (language == AppLanguage.GREEK) "Πρόγνωση" else "Forecast",
                activeIcon = WeatherIcons.PartlySunny,
                inactiveIcon = WeatherIcons.PartlySunny,
                tag = "nav_tab_forecast",
                multiColorWhenActive = true
            ),
            NavigationItemData(
                tab = WeatherNavTab.RADAR,
                label = if (language == AppLanguage.GREEK) "Ραντάρ" else "Radar",
                activeIcon = WeatherIcons.NavRadarActive,
                inactiveIcon = WeatherIcons.CompassCalibration,
                tag = "nav_tab_radar",
                multiColorWhenActive = true
            ),
            NavigationItemData(
                tab = WeatherNavTab.CITIES,
                label = if (language == AppLanguage.GREEK) "Πόλεις" else "Cities",
                activeIcon = WeatherIcons.NavCitiesActive,
                inactiveIcon = WeatherIcons.PublicOutlined,
                tag = "nav_tab_cities",
                multiColorWhenActive = true
            ),
            NavigationItemData(
                tab = WeatherNavTab.SETTINGS,
                label = if (language == AppLanguage.GREEK) "Ρυθμίσεις" else "Settings",
                activeIcon = WeatherIcons.NavSettingsActive,
                inactiveIcon = WeatherIcons.SettingsOutlined,
                tag = "nav_tab_settings",
                multiColorWhenActive = true
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("weather_bottom_bar")
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0A1628).copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        CyanZephyr.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.16f),
                        AmberSol.copy(alpha = 0.34f)
                    )
                )
            ),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = activeTab == item.tab
                    val activeAccent = when (item.tab) {
                        WeatherNavTab.FORECAST -> AmberSol
                        WeatherNavTab.RADAR -> CyanZephyr
                        WeatherNavTab.CITIES -> CyanGlow
                        WeatherNavTab.SETTINGS -> CyanZephyr
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isSelected) {
                            activeAccent.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, activeAccent.copy(alpha = 0.45f))
                        } else {
                            null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectTab(item.tab) }
                            .testTag(item.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                contentDescription = item.label,
                                tint = when {
                                    isSelected && item.multiColorWhenActive -> Color.Unspecified
                                    item.tab == WeatherNavTab.FORECAST -> Color.Unspecified
                                    isSelected -> activeAccent
                                    else -> Color.White.copy(alpha = 0.68f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 16.dp else 0.dp, height = 3.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeAccent else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavigationItemData(
    val tab: WeatherNavTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val tag: String,
    val multiColorWhenActive: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationSetupSearchingScreen(
    language: AppLanguage,
    isLocatingGps: Boolean,
    searchQuery: String,
    searchResults: List<SavedCityEntity>,
    isSearching: Boolean,
    suggestedCities: List<SavedCityEntity>,
    canDismiss: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (SavedCityEntity) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "locationSetupRadar")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("location_setup_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (canDismiss) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDismiss() }
                        .testTag("setup_close_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = WeatherIcons.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.GREEK) "Κλείσιμο" else "Close",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Animated Radar / GPS Searching Indicator
        Box(
            modifier = Modifier
                .size(128.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                val r1 = maxR * (0.35f + 0.65f * pulseProgress)
                val r2 = maxR * (0.35f + 0.65f * ((pulseProgress + 0.5f) % 1f))
                drawCircle(
                    color = CyanZephyr.copy(alpha = (1f - pulseProgress) * 0.45f),
                    radius = r1
                )
                drawCircle(
                    color = EmeraldBreeze.copy(alpha = (1f - ((pulseProgress + 0.5f) % 1f)) * 0.40f),
                    radius = r2
                )
                drawCircle(
                    color = Color(0xFF102A49),
                    radius = maxR * 0.42f
                )
            }
            Surface(
                shape = CircleShape,
                color = Color(0xFF133357),
                border = BorderStroke(1.5.dp, CyanGlow),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = WeatherIcons.MyLocation,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Text(
            text = if (language == AppLanguage.GREEK) {
                if (isLocatingGps) "Εντοπισμός Τοποθεσίας GPS…" else "Επιλέξτε Πόλη ή Χωριό"
            } else {
                if (isLocatingGps) "Locating via GPS…" else "Choose Your City or Village"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Text(
            text = if (language == AppLanguage.GREEK) {
                if (isLocatingGps) {
                    "Εντοπισμός συντεταγμένων GPS σε εξέλιξη… Ή αναζητήστε την πόλη ή το χωριό σας παρακάτω."
                } else {
                    "Χρησιμοποιήστε το GPS της συσκευής σας ή αναζητήστε μόνοι σας οποιαδήποτε πόλη ή χωριό."
                }
            } else {
                if (isLocatingGps) {
                    "Scanning GPS satellites… Or search for your city or village below."
                } else {
                    "Use your device's GPS Location Services or search for any city, town, or village below."
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.76f)
        )

        // Auto-Detect GPS Button
        Button(
            onClick = onRequestGpsLocation,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldBreeze.copy(alpha = 0.24f)
            ),
            border = BorderStroke(1.2.dp, EmeraldBreeze),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_detect_gps_button")
        ) {
            Icon(
                imageVector = WeatherIcons.MyLocation,
                contentDescription = null,
                tint = EmeraldBreeze,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.GREEK) {
                    "Αυτόματος Εντοπισμός Τοποθεσίας (GPS)"
                } else {
                    "Auto-Detect My Location (GPS)"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Search Location Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = if (language == AppLanguage.GREEK) {
                        "Αναζήτηση πόλης ή χωριού (π.χ. Κάτω Παλαιοκαρυά)..."
                    } else {
                        "Search any city, town, or village..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = WeatherIcons.Search,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(20.dp)
                )
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
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.95f),
                unfocusedContainerColor = Color(0xFF0E223B).copy(alpha = 0.88f),
                focusedBorderColor = CyanZephyr,
                unfocusedBorderColor = CyanZephyr.copy(alpha = 0.38f),
                cursorColor = CyanZephyr
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_location_search_input")
        )

        // Live Search Results
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_search_results_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF132A46)
                ),
                border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    searchResults.forEachIndexed { idx, city ->
                        if (idx > 0) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("setup_search_result_${city.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                val elevLabel = if (city.elevationMeters > 0.0) {
                                    if (language == AppLanguage.GREEK) " • Υψόμετρο ${city.elevationMeters.toInt()} m" else " • Elev ${city.elevationMeters.toInt()} m"
                                } else ""
                                Text(
                                    text = "${city.regionName}, ${city.countryName}$elevLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }
                            Icon(
                                imageVector = WeatherIcons.AddCircle,
                                contentDescription = "Select location",
                                tint = CyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Popular Quick-Select Cities
        if (suggestedCities.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.GREEK) {
                        "Δημοφιλείς Τοποθεσίες (Άμεση Επιλογή):"
                    } else {
                        "Popular Locations (Quick Select):"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanGlow,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedCities.forEach { city ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF102542).copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectCity(city) }
                                .testTag("setup_quick_city_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIcons.LocationOn,
                                    contentDescription = null,
                                    tint = CyanGlow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
