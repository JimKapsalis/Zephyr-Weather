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
import androidx.compose.ui.graphics.graphicsLayer
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
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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

    // Fast-path #1: Check synchronous Android LocationManager lastKnownLocation (< 5ms)
    val locManager = runCatching {
        context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    }.getOrNull()
    val fastCachedLoc = runCatching {
        if (locManager == null) return@runCatching null
        val providers = buildList {
            if (hasFine) {
                add(android.location.LocationManager.GPS_PROVIDER)
            }
            add(android.location.LocationManager.PASSIVE_PROVIDER)
            add(android.location.LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(android.location.LocationManager.FUSED_PROVIDER)
            }
        }
        providers.mapNotNull { provider ->
            runCatching { locManager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { loc ->
            val isZero = kotlin.math.abs(loc.latitude) < 0.01 && kotlin.math.abs(loc.longitude) < 0.01
            val isMock = loc.latitude in 37.65..37.85 && loc.longitude in -97.95..-97.70
            !isZero && !isMock
        }.maxByOrNull { it.time }
    }.getOrNull()

    if (fastCachedLoc != null && (System.currentTimeMillis() - fastCachedLoc.time) < 12 * 60 * 60 * 1000L) {
        viewModel.onGpsCoordinatesResolved(
            context = context,
            latitude = fastCachedLoc.latitude,
            longitude = fastCachedLoc.longitude,
            altitude = fastCachedLoc.altitude
        )
        return
    }

    // Delegate to ViewModel's multi-provider GPS engine (Fused lastLocation + High Accuracy + Balanced + LocationManager + IP fallback)
    viewModel.detectGpsLocation(context)
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
        viewModel.autoDetectLocationOnLoad(context, hasFine || hasCoarse)
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
        com.example.ui.components.ProvideSharedWeatherIconAnimation {
        val currentLoadingState = state.loadingState
        val activeBundle = state.primaryBundle ?: (currentLoadingState as? LoadingState.Success)?.bundle
        val liveClock = rememberLiveGreeceClock(activeBundle?.timezoneId)
        val sunsetTime = activeBundle?.daily?.firstOrNull()?.sunsetTime ?: "18:05"
        val visualSpec = remember(activeBundle, state.selectedHourIndex, liveClock, sunsetTime, state.skyAtmospherePreset) {
            if (activeBundle != null) {
                val inspectedHour = activeBundle.hourly.getOrNull(state.selectedHourIndex)
                if (state.selectedHourIndex > 0 && inspectedHour != null) {
                    val isHourNight = !inspectedHour.isDay || WmoWeatherMapper.isNightHourLabel(inspectedHour.displayHour)
                    val isHourSunset = WmoWeatherMapper.isSunsetClockTime(inspectedHour.displayHour, sunsetTime)
                    state.skyAtmospherePreset.resolveVisualSpec(
                        liveCode = inspectedHour.weatherCode,
                        liveIsDay = !isHourNight,
                        liveIsSunset = isHourSunset
                    )
                } else {
                    val isLiveNight = !activeBundle.current.isDay || WmoWeatherMapper.isNightHourLabel(liveClock)
                    val isLiveSunset = WmoWeatherMapper.isSunsetClockTime(liveClock, sunsetTime)
                    state.skyAtmospherePreset.resolveVisualSpec(
                        liveCode = activeBundle.current.weatherCode,
                        liveIsDay = !isLiveNight,
                        liveIsSunset = isLiveSunset
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
                    val isSetupScreenVisible = state.isLocationSetupOpen ||
                        (state.isInitialDbLoaded && state.primaryBundle == null && state.savedCities.isEmpty())
                    if (!isSetupScreenVisible) {
                        ZephyrBottomNavBar(
                            activeTab = state.activeTab,
                            language = state.language,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
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
                                        val fallbackCity = state.savedCities.find { it.id == state.selectedCityId }
                                            ?: state.savedCities.firstOrNull()
                                        val resolvedBundle = state.primaryBundle
                                            ?: fallbackCity?.let { viewModel.buildFallbackPreviewForUi(it) }

                                        if (resolvedBundle != null) {
                                            ForecastTabScreen(
                                                bundle = resolvedBundle,
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
                                        } else if (state.isInitialDbLoaded && (state.isLocatingGps || state.savedCities.isEmpty())) {
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
                                            // Silent 1-frame empty box while Room DB reads saved cities on startup (< 30ms)
                                            Box(modifier = Modifier.fillMaxSize())
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
                                val fallbackCity = state.savedCities.find { it.id == state.selectedCityId }
                                    ?: state.savedCities.firstOrNull()
                                val resolvedRadarBundle = (currentLoadingState as? LoadingState.Success)?.bundle
                                    ?: state.primaryBundle
                                    ?: fallbackCity?.let { viewModel.buildFallbackPreviewForUi(it) }
                                if (resolvedRadarBundle != null) {
                                    RadarTabScreen(
                                        bundle = resolvedRadarBundle,
                                        units = state.units,
                                        language = state.language,
                                        selectedLayer = state.radarLayer,
                                        selectedTimelineIndex = state.radarTimelineIndex,
                                        isRefreshing = state.isRefreshing,
                                        onRefresh = { viewModel.refreshCurrentWeather() },
                                        onSelectLayer = { viewModel.selectRadarLayer(it) },
                                        onSelectTimelineIndex = { viewModel.selectRadarTimelineIndex(it) }
                                    )
                                } else if (state.isInitialDbLoaded) {
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
                                    Box(modifier = Modifier.fillMaxSize())
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
                                githubRepoSlug = state.githubRepoSlug,
                                latestReleaseTag = state.latestReleaseTag,
                                latestReleaseNotes = state.latestReleaseNotes,
                                isNewGitHubReleaseAvailable = state.isNewGitHubReleaseAvailable,
                                totalApkDownloads = state.totalApkDownloads,
                                onUpdateGitHubRepoSlug = { viewModel.updateGitHubRepoSlug(it) },
                                onOpenGitHubReleaseDownload = { directApk ->
                                    viewModel.openGitHubReleaseDownload(context, directApk)
                                },
                                onCheckForUpdates = { viewModel.checkForAppAndWeatherUpdates(context) },
                                onSendFeedbackEmail = { category, rating, message ->
                                    viewModel.sendFeedbackEmail(context, category, rating, message)
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        AnimatedVisibility(
                            visible = state.isNewGitHubReleaseAvailable,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF0A2E26).copy(alpha = 0.96f),
                                border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable {
                                        viewModel.openGitHubReleaseDownload(context, directApk = true)
                                    }
                                    .testTag("new_app_update_available_banner")
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
                                            imageVector = WeatherIcons.Refresh,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (state.language == AppLanguage.GREEK) {
                                                "🚀 Νέα ενημέρωση διαθέσιμη (${state.latestReleaseTag})! Πατήστε για άμεση λήψη APK"
                                            } else {
                                                "🚀 New Update Available (${state.latestReleaseTag})! Tap to download latest APK"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = if (state.language == AppLanguage.GREEK) "ΛΗΨΗ" else "GET APK",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF6EE7B7),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = state.statusBanner != null,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
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
                inactiveIcon = WeatherIcons.NavRadarActive,
                tag = "nav_tab_radar",
                multiColorWhenActive = true
            ),
            NavigationItemData(
                tab = WeatherNavTab.CITIES,
                label = if (language == AppLanguage.GREEK) "Πόλεις" else "Cities",
                activeIcon = WeatherIcons.NavCitiesActive,
                inactiveIcon = WeatherIcons.NavCitiesActive,
                tag = "nav_tab_cities",
                multiColorWhenActive = true
            ),
            NavigationItemData(
                tab = WeatherNavTab.SETTINGS,
                label = if (language == AppLanguage.GREEK) "Ρυθμίσεις" else "Settings",
                activeIcon = WeatherIcons.NavSettingsActive,
                inactiveIcon = WeatherIcons.NavSettingsActive,
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
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF0A1D35).copy(alpha = 0.28f),
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.36f),
                        CyanZephyr.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.22f),
                        AmberSol.copy(alpha = 0.34f)
                    )
                )
            ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.03f),
                                Color(0xFF061325).copy(alpha = 0.20f)
                            )
                        )
                    )
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
                            Color.White.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        },
                        border = if (isSelected) {
                            BorderStroke(
                                1.2.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.48f),
                                        activeAccent.copy(alpha = 0.55f)
                                    )
                                )
                            )
                        } else {
                            null
                        },
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectTab(item.tab) }
                            .testTag(item.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    activeAccent.copy(alpha = 0.34f),
                                                    activeAccent.copy(alpha = 0.10f),
                                                    Color.Transparent
                                                )
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.08f),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(if (isSelected) 26.dp else 23.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.68f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 18.dp else 0.dp, height = 3.dp)
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
    val isGreek = language == AppLanguage.GREEK
    val isResumed = com.example.ui.components.rememberIsAppResumed()
    val pulseProgress = if (isResumed && isLocatingGps) {
        val infiniteTransition = rememberInfiniteTransition(label = "locationSetupRadar")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseProgress"
        )
        animated
    } else {
        0.35f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDismiss() }
                        .testTag("setup_close_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                            text = if (isGreek) "Κλείσιμο" else "Close",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 1. Atmospheric Hero Onboarding Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0E223A).copy(alpha = 0.48f)
            ),
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        CyanZephyr.copy(alpha = 0.55f),
                        EmeraldBreeze.copy(alpha = 0.45f),
                        AmberSol.copy(alpha = 0.45f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_atmosphere_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(30.dp))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color(0xFF0E294B).copy(alpha = 0.36f),
                                    Color(0xFF081426).copy(alpha = 0.58f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated Radar / Weather Beacon
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxR = size.minDimension / 2f
                            val r1 = maxR * (0.38f + 0.62f * pulseProgress)
                            val r2 = maxR * (0.38f + 0.62f * ((pulseProgress + 0.5f) % 1f))
                            drawCircle(
                                color = CyanZephyr.copy(alpha = (1f - pulseProgress) * 0.42f),
                                radius = r1
                            )
                            drawCircle(
                                color = EmeraldBreeze.copy(alpha = (1f - ((pulseProgress + 0.5f) % 1f)) * 0.38f),
                                radius = r2
                            )
                            drawCircle(
                                color = Color(0xFF102A49),
                                radius = maxR * 0.46f
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF14355B),
                            border = BorderStroke(1.5.dp, CyanGlow),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLocatingGps) WeatherIcons.MyLocation else WeatherIcons.PartlySunny,
                                    contentDescription = null,
                                    tint = if (isLocatingGps) CyanGlow else Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CyanZephyr.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = if (isGreek) {
                                "ΖΩΝΤΑΝΟΣ ΚΑΙΡΟΣ • ΠΟΛΕΙΣ, ΧΩΡΙΑ & ΥΨΟΜΕΤΡΟ"
                            } else {
                                "LIVE WEATHER • CITIES, VILLAGES & ELEVATION"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanGlow,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = if (isGreek) {
                            if (isLocatingGps) "Εντοπισμός Τοποθεσίας GPS…" else "Καλώς ήρθατε στο Zephyr Weather"
                        } else {
                            if (isLocatingGps) "Locating via GPS…" else "Welcome to Zephyr Weather"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = if (isGreek) {
                            if (isLocatingGps) {
                                "Λήψη συντεταγμένων GPS & υψομέτρου σε εξέλιξη… Ή αναζητήστε την πόλη ή το χωριό σας παρακάτω."
                            } else {
                                "Εντοπίστε αυτόματα την τοποθεσία σας με GPS ή αναζητήστε οποιαδήποτε πόλη, κωμόπολη ή χωριό σε όλο τον κόσμο."
                            }
                        } else {
                            if (isLocatingGps) {
                                "Acquiring your live GPS coordinates & altitude… Or search for any city or village below."
                            } else {
                                "Detect your current coordinates via GPS or search for any city, town, or mountain village worldwide."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.80f)
                    )

                    // Primary Auto-Detect GPS Action Button inside the Hero Card
                    Button(
                        onClick = onRequestGpsLocation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldBreeze.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.3.dp, EmeraldBreeze),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_detect_gps_button")
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldBreeze
                            )
                        } else {
                            Icon(
                                imageVector = WeatherIcons.MyLocation,
                                contentDescription = null,
                                tint = EmeraldBreeze,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (isGreek) {
                                    if (isLocatingGps) "Αναζήτηση Σήματος GPS…" else "Αυτόματος Εντοπισμός Τοποθεσίας (GPS)"
                                } else {
                                    if (isLocatingGps) "Scanning GPS Satellites…" else "Auto-Detect My Location (GPS)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isGreek) {
                                    "Ακριβείς συντεταγμένες, υψόμετρο & τοπικός σταθμός"
                                } else {
                                    "Exact coordinates, elevation & local weather station"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldBreeze
                            )
                        }
                    }
                }
            }
        }

        // 2. Search Any City, Town, or Village Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10243E).copy(alpha = 0.48f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = WeatherIcons.Search,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isGreek) {
                            "Αναζήτηση Πόλης ή Χωριού"
                        } else {
                            "Search Any City, Town, or Village"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (isGreek) {
                                "Πληκτρολογήστε πόλη ή χωριό (π.χ. Κάτω Παλαιοκαρυά)..."
                            } else {
                                "Type any city, town, or village (e.g. Kato Palaiokarya)..."
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
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0A182B).copy(alpha = 0.52f),
                        unfocusedContainerColor = Color(0xFF0A182B).copy(alpha = 0.42f),
                        focusedBorderColor = CyanZephyr,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                        cursorColor = CyanZephyr
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_location_search_input")
                )

                // Live Search Results inside the Search Card
                if (searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_search_results_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0A182B).copy(alpha = 0.56f),
                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            searchResults.forEachIndexed { idx, city ->
                                if (idx > 0) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCity(city) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                        .testTag("setup_search_result_${city.id}"),
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
                                            color = CyanZephyr.copy(alpha = 0.16f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = WeatherIcons.LocationOn,
                                                    contentDescription = null,
                                                    tint = CyanGlow,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = city.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val elevLabel = if (city.elevationMeters > 0.0) {
                                                if (isGreek) " • Υψόμετρο ${city.elevationMeters.toInt()} m" else " • Elev ${city.elevationMeters.toInt()} m"
                                            } else ""
                                            Text(
                                                text = "${city.regionName}, ${city.countryName}$elevLabel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.72f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = WeatherIcons.AddCircle,
                                        contentDescription = "Select location",
                                        tint = EmeraldBreeze,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Popular Locations (Rich Interactive Cards with Weather Icons & Elevation)
        if (suggestedCities.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0E2138).copy(alpha = 0.46f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGreek) {
                                "Δημοφιλείς Προορισμοί (Άμεση Επιλογή)"
                            } else {
                                "Popular Destinations (Quick Select)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = CyanGlow,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isGreek) "Πατήστε για επιλογή" else "Tap to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.58f)
                        )
                    }

                    suggestedCities.forEach { city ->
                        val spec = WmoWeatherMapper.resolve(city.cachedWeatherCode, city.cachedIsDay)
                        val elevBadge = if (city.elevationMeters > 0.0) {
                            if (isGreek) " • Υψόμετρο ${city.elevationMeters.toInt()} m" else " • Elev ${city.elevationMeters.toInt()} m"
                        } else ""

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF132B49).copy(alpha = 0.46f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectCity(city) }
                                .testTag("setup_quick_city_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0B1A2E),
                                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f)),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = spec.icon,
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${city.regionName}, ${city.countryName}$elevBadge",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = CyanZephyr.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, CyanZephyr.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = if (isGreek) "Επιλογή" else "Select",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanGlow,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
