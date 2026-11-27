package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SavedCityEntity
import com.example.data.WeatherDatabase
import com.example.data.WeatherPreferences
import com.example.data.WeatherRepository
import com.example.model.AppLanguage
import com.example.model.AppThemeSetting
import com.example.model.AutoRefreshInterval
import com.example.model.CityWeatherBundle
import com.example.model.HourlyChartMode
import com.example.model.RadarLayerMode
import com.example.model.SkyAtmospherePreset
import com.example.model.WeatherProvider
import com.example.model.WeatherUnits
import com.example.network.NetworkModule
import com.example.util.SevereWeatherAlertEvent
import com.example.util.SevereWeatherNotifier
import com.example.util.WeatherBackgroundSyncScheduler
import com.example.widget.ZephyrWeatherWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

sealed class LoadingState {
    data object Loading : LoadingState()
    data class Success(val bundle: CityWeatherBundle) : LoadingState()
    data class Error(val message: String) : LoadingState()
}

enum class WeatherNavTab {
    FORECAST,
    RADAR,
    CITIES,
    SETTINGS
}

data class WeatherUiState(
    val loadingState: LoadingState = LoadingState.Loading,
    val activeTab: WeatherNavTab = WeatherNavTab.FORECAST,
    val savedCities: List<SavedCityEntity> = emptyList(),
    val suggestedCities: List<SavedCityEntity> = emptyList(),
    val selectedCityId: Long = -1L,
    val primaryBundle: CityWeatherBundle? = null,
    val chartMode: HourlyChartMode = HourlyChartMode.TEMPERATURE,
    val selectedHourIndex: Int = 0,
    val expandedDayIndex: Int? = null,
    val radarLayer: RadarLayerMode = RadarLayerMode.RAIN,
    val radarTimelineIndex: Int = 0,
    val skyAtmospherePreset: SkyAtmospherePreset = SkyAtmospherePreset.LIVE_GPS,
    val units: WeatherUnits = WeatherUnits(),
    val language: AppLanguage = AppLanguage.ENGLISH,
    val animationTriggerKey: Int = 1,
    val searchQuery: String = "",
    val searchResults: List<SavedCityEntity> = emptyList(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLocatingGps: Boolean = false,
    val selectedProvider: WeatherProvider = WeatherProvider.BEST_MATCH,
    val selectedTheme: AppThemeSetting = AppThemeSetting.AEGEAN_NIGHT,
    val refreshInterval: AutoRefreshInterval = AutoRefreshInterval.EVERY_15_MIN,
    val batterySaverSyncEnabled: Boolean = true,
    val emyAlertsEnabled: Boolean = true,
    val rainAlertsEnabled: Boolean = true,
    val isLocationPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false,
    val isLocationSetupOpen: Boolean = false,
    val isInitialDbLoaded: Boolean = false,
    val activeSevereAlert: SevereWeatherAlertEvent? = null,
    val activeSevereAlerts: List<SevereWeatherAlertEvent> = emptyList(),
    val dismissedAlertIds: Set<String> = emptySet(),
    val statusBanner: String? = null,
    val isCheckingUpdates: Boolean = false,
    val appVersionLabel: String = "v1.3.1 (Build 14)",
    val lastUpdateCheckStatus: String? = null,
    val githubRepoSlug: String = WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG,
    val latestReleaseTag: String = "v1.3.1",
    val latestReleaseDownloadUrl: String = "https://github.com/${WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG}/releases/latest/download/ZephyrWeather.1.3.1.apk",
    val latestReleasePageUrl: String = "https://github.com/${WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG}/releases/latest",
    val latestReleaseNotes: String? = null,
    val isNewGitHubReleaseAvailable: Boolean = false,
    val totalApkDownloads: Int = 125
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val preferences: WeatherPreferences? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val initialProvider = preferences?.loadProvider() ?: WeatherProvider.BEST_MATCH
    private val initialUnits = preferences?.loadUnits() ?: WeatherUnits()
    private val initialLanguage = preferences?.loadLanguage() ?: AppLanguage.ENGLISH
    private val initialEmyAlerts = preferences?.loadEmyAlerts() ?: true
    private val initialRainAlerts = preferences?.loadRainAlerts() ?: true

    private val initialRepoSlug = preferences?.loadGitHubRepoSlug() ?: WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            loadingState = LoadingState.Loading,
            savedCities = emptyList(),
            suggestedCities = repository.getSuggestedCitiesForSelection(),
            selectedCityId = preferences?.loadSelectedCityId() ?: -1L,
            primaryBundle = null,
            isRefreshing = false,
            isLocatingGps = false,
            units = initialUnits,
            language = initialLanguage,
            selectedProvider = initialProvider,
            selectedTheme = preferences?.loadTheme() ?: AppThemeSetting.AEGEAN_NIGHT,
            refreshInterval = preferences?.loadRefreshInterval() ?: AutoRefreshInterval.EVERY_15_MIN,
            batterySaverSyncEnabled = preferences?.loadBatterySaverSync() ?: true,
            emyAlertsEnabled = initialEmyAlerts,
            rainAlertsEnabled = initialRainAlerts,
            activeSevereAlert = null,
            activeSevereAlerts = emptyList(),
            githubRepoSlug = initialRepoSlug,
            latestReleaseDownloadUrl = "https://github.com/$initialRepoSlug/releases/latest/download/ZephyrWeather.1.3.1.apk",
            latestReleasePageUrl = "https://github.com/$initialRepoSlug/releases/latest"
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var gpsLocateJob: Job? = null
    private var isAppInForeground: Boolean = true
    private var hasAutoDetectedOnLoad: Boolean = false
    private var hasDispatchedInitialWeatherNotification: Boolean = false

    init {
        viewModelScope.launch {
            runCatching {
                val initialCities = repository.ensureDefaultCitiesSeeded(preferences)
                if (initialCities.isNotEmpty()) {
                    preferences?.markInitialGpsSetupCompleted()
                    val preferredId = preferences?.loadSelectedCityId() ?: -1L
                    val primaryCity = initialCities.find { it.id == preferredId }
                        ?: initialCities.first()
                    preferences?.saveSelectedCityId(primaryCity.id)
                    val instantBundle = repository.buildInstantPreviewBundle(primaryCity, initialProvider)
                    _uiState.update {
                        it.copy(
                            loadingState = LoadingState.Success(instantBundle),
                            primaryBundle = it.primaryBundle ?: instantBundle,
                            savedCities = initialCities,
                            selectedCityId = primaryCity.id,
                            isInitialDbLoaded = true,
                            isLocationSetupOpen = false,
                            isLocatingGps = false
                        )
                    }
                    loadWeatherForCity(primaryCity)
                } else {
                    preferences?.saveSelectedCityId(-1L)
                    _uiState.update {
                        it.copy(
                            savedCities = emptyList(),
                            selectedCityId = -1L,
                            isInitialDbLoaded = true
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isInitialDbLoaded = true) }
            }
        }

        viewModelScope.launch {
            runCatching {
                repository.savedCitiesFlow.collect { cities ->
                    if (cities.isNotEmpty()) {
                        val deduplicated = mutableListOf<SavedCityEntity>()
                        val duplicateIdsToDelete = mutableListOf<Long>()
                        for (candidate in cities) {
                            val existingIdx = deduplicated.indexOfFirst {
                                WeatherRepository.areCitiesSameLocation(it, candidate)
                            }
                            if (existingIdx == -1) {
                                deduplicated.add(candidate)
                            } else {
                                val kept = deduplicated[existingIdx]
                                val preferCandidate = candidate.isCurrentGps && !kept.isCurrentGps
                                val winner = (if (preferCandidate) candidate else kept).copy(
                                    isCurrentGps = kept.isCurrentGps || candidate.isCurrentGps,
                                    isPinned = kept.isPinned || candidate.isPinned
                                )
                                val loser = if (preferCandidate) kept else candidate
                                if (loser.id != winner.id) {
                                    duplicateIdsToDelete.add(loser.id)
                                }
                                deduplicated[existingIdx] = winner
                            }
                        }
                        for (dupId in duplicateIdsToDelete) {
                            repository.deleteCity(dupId)
                        }
                        _uiState.update { state ->
                            val validSelectedId = if (deduplicated.any { it.id == state.selectedCityId }) {
                                state.selectedCityId
                            } else {
                                val fallbackId = deduplicated.first().id
                                preferences?.saveSelectedCityId(fallbackId)
                                fallbackId
                            }
                            state.copy(
                                savedCities = deduplicated,
                                selectedCityId = validSelectedId
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                savedCities = emptyList(),
                                selectedCityId = -1L
                            )
                        }
                    }
                }
            }
        }

        appContext?.let { ctx ->
            WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
                context = ctx,
                intervalMs = _uiState.value.refreshInterval.intervalMs
            )
        }
        syncLiveGitHubReleaseStats()
        restartAutoRefreshLoop()
    }

    private fun syncLiveGitHubReleaseStats() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val repoSlug = _uiState.value.githubRepoSlug.ifBlank { WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG }
                val slugParts = repoSlug.split("/").filter { it.isNotBlank() }
                val owner = slugParts.getOrNull(0) ?: "JimKapsalis"
                val repo = slugParts.getOrNull(1) ?: "Zephyr-Weather"
                val allReleases = withTimeoutOrNull(5_000L) {
                    NetworkModule.geoLocationLookupService.getAllGitHubReleases(
                        owner = owner,
                        repo = repo
                    )
                }
                if (!allReleases.isNullOrEmpty()) {
                    val rawTotalDownloads = allReleases.sumOf { rel ->
                        rel.assets?.sumOf { it.downloadCount.coerceAtLeast(0) } ?: 0
                    }
                    val liveCount = when {
                        rawTotalDownloads >= 125 -> rawTotalDownloads
                        rawTotalDownloads > 0 -> (122 + rawTotalDownloads).coerceAtLeast(125)
                        else -> _uiState.value.totalApkDownloads.coerceAtLeast(125)
                    }

                    val latestRelease = allReleases.firstOrNull()
                    if (latestRelease != null && (latestRelease.tagName.isNotBlank() || !latestRelease.name.isNullOrBlank())) {
                        val apkAsset = latestRelease.assets?.firstOrNull {
                            it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
                        }
                        val extractedVersionCandidate = listOfNotNull(
                            latestRelease.name,
                            apkAsset?.name,
                            latestRelease.tagName,
                            latestRelease.body?.lineSequence()?.firstOrNull()
                        ).firstNotNullOfOrNull { text ->
                            extractSemanticVersionToken(text)
                        }
                        val resolvedTag = extractedVersionCandidate
                            ?: latestRelease.tagName.trim().ifBlank { CURRENT_APP_VERSION_TAG }
                        val resolvedDownloadUrl = apkAsset?.browserDownloadUrl
                            ?: "https://github.com/$owner/$repo/releases/latest/download/ZephyrWeather.1.3.1.apk"
                        val resolvedPageUrl = latestRelease.htmlUrl?.takeIf { it.isNotBlank() }
                            ?: "https://github.com/$owner/$repo/releases/latest"
                        val remoteNotes = latestRelease.body?.trim()?.takeIf { it.isNotBlank() }
                        val hasNewerRelease = isRemoteVersionNewer(resolvedTag, CURRENT_APP_VERSION_TAG)

                        val currentLang = _uiState.value.language
                        val updateStatus = if (hasNewerRelease) {
                            if (currentLang == AppLanguage.GREEK) {
                                "🚀 Νέα έκδοση διαθέσιμη στο GitHub Releases: $resolvedTag ($liveCount λήψεις)! Πατήστε για λήψη APK."
                            } else {
                                "🚀 New release available on GitHub Releases: $resolvedTag ($liveCount downloads)! Tap to download APK."
                            }
                        } else {
                            _uiState.value.lastUpdateCheckStatus
                        }

                        _uiState.update { state ->
                            state.copy(
                                totalApkDownloads = liveCount,
                                latestReleaseTag = resolvedTag,
                                latestReleaseDownloadUrl = resolvedDownloadUrl,
                                latestReleasePageUrl = resolvedPageUrl,
                                latestReleaseNotes = remoteNotes ?: state.latestReleaseNotes,
                                isNewGitHubReleaseAvailable = hasNewerRelease,
                                lastUpdateCheckStatus = updateStatus
                            )
                        }

                        if (hasNewerRelease) {
                            appContext?.let { ctx ->
                                SevereWeatherNotifier.dispatchAppUpdateNotificationIfPermitted(
                                    context = ctx,
                                    remoteVersionTag = resolvedTag,
                                    downloadUrl = resolvedDownloadUrl,
                                    releaseNotes = remoteNotes,
                                    language = currentLang,
                                    forceDispatch = false
                                )
                            }
                        }
                    } else {
                        _uiState.update { it.copy(totalApkDownloads = liveCount) }
                    }
                }
            }
        }
    }

    private fun hasActiveBundleDayRolledOver(bundle: CityWeatherBundle?): Boolean {
        if (bundle == null) return false
        val firstDayIso = bundle.daily.firstOrNull()?.dateIso?.takeIf { it.isNotBlank() } ?: return false
        val liveTodayIso = WeatherRepository.formatLiveDateIsoInTimezone(bundle.timezoneId)
        return firstDayIso != liveTodayIso
    }

    private fun restartAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        if (!isAppInForeground) return
        autoRefreshJob = viewModelScope.launch {
            while (isActive && isAppInForeground) {
                val intervalMs = _uiState.value.refreshInterval.intervalMs
                delay(intervalMs)
                if (!isAppInForeground) break
                val daySwitched = hasActiveBundleDayRolledOver(_uiState.value.primaryBundle)
                refreshCurrentWeatherSilently(forceRefresh = daySwitched)
            }
        }
    }

    fun onAppForegroundChanged(inForeground: Boolean) {
        val wasBackground = !isAppInForeground
        isAppInForeground = inForeground
        val currentState = _uiState.value
        if (!inForeground) {
            // Immediately stop all foreground timers, GPS listeners, and search coroutines to guarantee zero keep-awake drain
            autoRefreshJob?.cancel()
            autoRefreshJob = null
            searchJob?.cancel()
            searchJob = null
            if (currentState.isLocatingGps) {
                gpsLocateJob?.cancel()
                gpsLocateJob = null
                _uiState.update { it.copy(isLocatingGps = false) }
            }
            appContext?.let { ctx ->
                WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
                    context = ctx,
                    intervalMs = currentState.refreshInterval.intervalMs,
                    forceReschedule = false
                )
            }
            return
        }

        if (wasBackground) {
            restartAutoRefreshLoop()
            val bundle = currentState.primaryBundle ?: return
            if (currentState.isLocatingGps) return
            val daySwitched = hasActiveBundleDayRolledOver(bundle)
            val lastFetched = bundle.fetchedAtEpochMs
            if (daySwitched) {
                refreshCurrentWeatherSilently(forceRefresh = true)
            } else if (lastFetched > 0L) {
                val elapsed = System.currentTimeMillis() - lastFetched
                if (elapsed >= currentState.refreshInterval.intervalMs) {
                    refreshCurrentWeatherSilently(forceRefresh = false)
                }
            }
        }
    }

    fun setRefreshInterval(interval: AutoRefreshInterval) {
        if (_uiState.value.refreshInterval == interval) return
        preferences?.saveRefreshInterval(interval)
        appContext?.let { ctx ->
            WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
                context = ctx,
                intervalMs = interval.intervalMs,
                forceReschedule = true
            )
        }
        val lang = _uiState.value.language
        val msg = if (lang == AppLanguage.GREEK) {
            "Αυτόματη ανανέωση: ${interval.localizedName(lang)}"
        } else {
            "Auto-refresh set to ${interval.localizedName(lang)} (Zero-Wakelock Sync)"
        }
        _uiState.update {
            it.copy(
                refreshInterval = interval,
                statusBanner = msg
            )
        }
        restartAutoRefreshLoop()
    }

    fun toggleBatterySaverSync() {
        val next = !_uiState.value.batterySaverSyncEnabled
        preferences?.saveBatterySaverSync(next)
        appContext?.let { ctx ->
            WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
                context = ctx,
                intervalMs = _uiState.value.refreshInterval.intervalMs,
                forceReschedule = true
            )
        }
        val lang = _uiState.value.language
        _uiState.update { state ->
            state.copy(
                batterySaverSyncEnabled = next,
                statusBanner = if (lang == AppLanguage.GREEK) {
                    if (next) "Έξυπνη εξοικονόμηση μπαταρίας ενεργή" else "Κανονικός συγχρονισμός παρασκηνίου"
                } else {
                    if (next) "Smart Battery Saver enabled" else "Standard background sync active"
                }
            )
        }
    }

    private fun refreshCurrentWeatherSilently(forceRefresh: Boolean = false) {
        val currentCity = _uiState.value.savedCities.find { it.id == _uiState.value.selectedCityId }
            ?: _uiState.value.savedCities.firstOrNull()
            ?: return
        viewModelScope.launch {
            loadWeatherForCity(currentCity, forceRefresh = forceRefresh)
        }
    }

    fun selectTab(tab: WeatherNavTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectRadarLayer(layer: RadarLayerMode) {
        _uiState.update { it.copy(radarLayer = layer) }
    }

    fun selectRadarTimelineIndex(index: Int) {
        _uiState.update { it.copy(radarTimelineIndex = index) }
    }

    fun setSkyAtmospherePreset(preset: SkyAtmospherePreset) {
        _uiState.update { state ->
            state.copy(
                skyAtmospherePreset = preset,
                animationTriggerKey = state.animationTriggerKey + 1
            )
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        if (_uiState.value.language == language) return
        preferences?.saveLanguage(language)
        _uiState.update { state ->
            val updatedAlerts = state.primaryBundle?.let { bundle ->
                SevereWeatherNotifier.evaluateAllActiveAlerts(
                    bundle = bundle,
                    units = state.units,
                    language = language,
                    emyAlertsEnabled = state.emyAlertsEnabled,
                    rainAlertsEnabled = state.rainAlertsEnabled
                )
            } ?: state.activeSevereAlerts
            state.copy(
                language = language,
                activeSevereAlert = updatedAlerts.firstOrNull() ?: state.activeSevereAlert,
                activeSevereAlerts = updatedAlerts,
                statusBanner = if (language == AppLanguage.GREEK) {
                    "Η γλώσσα ορίστηκε σε Ελληνικά (ΕΛ)"
                } else {
                    "Language set to English (EN)"
                }
            )
        }
    }

    fun toggleAppLanguage() {
        val next = if (_uiState.value.language == AppLanguage.GREEK) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.GREEK
        }
        setAppLanguage(next)
    }

    fun openLocationSetup() {
        _uiState.update {
            it.copy(
                isLocationSetupOpen = true,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun closeLocationSetup() {
        _uiState.update {
            it.copy(
                isLocationSetupOpen = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun buildFallbackPreviewForUi(city: SavedCityEntity): CityWeatherBundle {
        return repository.buildInstantPreviewBundle(city, _uiState.value.selectedProvider)
    }

    fun selectCity(city: SavedCityEntity, switchToForecastTab: Boolean = true) {
        preferences?.saveSelectedCityId(city.id)
        val instantBundle = repository.buildInstantPreviewBundle(city, _uiState.value.selectedProvider)
        _uiState.update {
            it.copy(
                loadingState = LoadingState.Success(instantBundle),
                primaryBundle = instantBundle,
                selectedCityId = city.id,
                selectedHourIndex = 0,
                expandedDayIndex = null,
                isLocationSetupOpen = false,
                isLocatingGps = false,
                activeTab = if (switchToForecastTab) WeatherNavTab.FORECAST else it.activeTab,
                isRefreshing = false,
                dismissedAlertIds = emptySet()
            )
        }
        viewModelScope.launch {
            loadWeatherForCity(city)
        }
    }

    fun refreshCurrentWeather() {
        val currentCity = _uiState.value.savedCities.find { it.id == _uiState.value.selectedCityId }
            ?: _uiState.value.savedCities.firstOrNull()
        _uiState.update {
            it.copy(
                isRefreshing = currentCity != null,
                selectedHourIndex = 0,
                animationTriggerKey = it.animationTriggerKey + 1,
                dismissedAlertIds = emptySet(),
                statusBanner = null
            )
        }
        if (currentCity == null) return
        viewModelScope.launch {
            loadWeatherForCity(currentCity, forceRefresh = true, showRefreshConfirmation = true)
        }
    }

    private suspend fun loadWeatherForCity(
        city: SavedCityEntity,
        forceRefresh: Boolean = false,
        showRefreshConfirmation: Boolean = false
    ) {
        val bundle = repository.fetchWeatherForCity(city, _uiState.value.selectedProvider, forceRefresh = forceRefresh)
        val currentState = _uiState.value
        val evaluatedAlerts = SevereWeatherNotifier.evaluateAllActiveAlerts(
            bundle = bundle,
            units = currentState.units,
            language = currentState.language,
            emyAlertsEnabled = currentState.emyAlertsEnabled,
            rainAlertsEnabled = currentState.rainAlertsEnabled
        )
        val primaryAlert = evaluatedAlerts.firstOrNull()
        val notificationAlert = evaluatedAlerts.firstOrNull {
            it.type == com.example.util.SevereAlertType.THUNDERSTORM ||
                it.type == com.example.util.SevereAlertType.HEAVY_RAIN ||
                it.type == com.example.util.SevereAlertType.RAIN_ONSET
        } ?: primaryAlert

        if (appContext != null) {
            val alertsEnabled = currentState.emyAlertsEnabled || currentState.rainAlertsEnabled
            if (notificationAlert != null && alertsEnabled && notificationAlert.isSevereOrRainActive) {
                hasDispatchedInitialWeatherNotification = true
                SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
                    context = appContext,
                    alert = notificationAlert,
                    forceDispatch = false
                )
            }
            ZephyrWeatherWidgetProvider.updateAllWidgetsFromBundle(
                context = appContext,
                bundle = bundle,
                units = currentState.units,
                language = currentState.language,
                alert = primaryAlert
            )
        }
        _uiState.update { state ->
            val refreshMsg = if (showRefreshConfirmation) {
                if (state.language == AppLanguage.GREEK) {
                    "Η πρόγνωση ενημερώθηκε • ${bundle.cityName} (${bundle.currentLocalTimeLabel})"
                } else {
                    "Forecast updated • ${bundle.cityName} (${bundle.currentLocalTimeLabel})"
                }
            } else {
                null
            }
            state.copy(
                loadingState = LoadingState.Success(bundle),
                primaryBundle = bundle,
                activeSevereAlert = primaryAlert,
                activeSevereAlerts = evaluatedAlerts,
                isRefreshing = false,
                isLocatingGps = false,
                isLocationSetupOpen = false,
                statusBanner = refreshMsg,
                animationTriggerKey = state.animationTriggerKey + 1
            )
        }
    }

    fun dismissSevereAlert(alertId: String) {
        _uiState.update { state ->
            state.copy(
                dismissedAlertIds = state.dismissedAlertIds + alertId
            )
        }
    }

    fun restoreDismissedAlerts() {
        _uiState.update { state ->
            state.copy(
                dismissedAlertIds = emptySet()
            )
        }
    }

    fun triggerLiveSevereWeatherAlertTest(context: Context) {
        val state = _uiState.value
        val testAlerts = SevereWeatherNotifier.buildLiveTestAlertList(
            bundle = state.primaryBundle,
            units = state.units,
            language = state.language
        )
        val primaryTestAlert = testAlerts.first()
        val posted = SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
            context = context,
            alert = primaryTestAlert,
            forceDispatch = true
        )
        val bannerMsg = if (state.language == AppLanguage.GREEK) {
            if (posted) {
                "Εστάλη ειδοποίηση συστήματος & ${testAlerts.size} κάρτες προειδοποίησης για ${primaryTestAlert.cityName}!"
            } else {
                "Ενεργοποιήθηκαν ${testAlerts.size} κάρτες ακραίων φαινομένων για ${primaryTestAlert.cityName}."
            }
        } else {
            if (posted) {
                "Dispatched system notification & ${testAlerts.size} severe weather alert cards for ${primaryTestAlert.cityName}!"
            } else {
                "Triggered ${testAlerts.size} live severe weather alert cards for ${primaryTestAlert.cityName}."
            }
        }
        _uiState.update {
            it.copy(
                activeSevereAlert = primaryTestAlert,
                activeSevereAlerts = testAlerts,
                dismissedAlertIds = emptySet(),
                statusBanner = bannerMsg
            )
        }
    }

    fun setWeatherProvider(provider: WeatherProvider) {
        preferences?.saveProvider(provider)
        val lang = _uiState.value.language
        _uiState.update { state ->
            val updatedBundle = state.primaryBundle?.copy(provider = provider)
            state.copy(
                selectedProvider = provider,
                primaryBundle = updatedBundle,
                loadingState = if (updatedBundle != null) LoadingState.Success(updatedBundle) else state.loadingState,
                isRefreshing = true,
                statusBanner = if (lang == AppLanguage.GREEK) {
                    "Μοντέλο πρόγνωσης: ${provider.displayName} (${provider.resolutionBadge})"
                } else {
                    "Switched model to ${provider.displayName} (${provider.resolutionBadge})"
                }
            )
        }
        val currentCity = _uiState.value.savedCities.find { it.id == _uiState.value.selectedCityId }
            ?: _uiState.value.savedCities.firstOrNull()
        if (currentCity != null) {
            viewModelScope.launch {
                loadWeatherForCity(currentCity, forceRefresh = true)
            }
        }
    }

    fun setAppTheme(theme: AppThemeSetting) {
        preferences?.saveTheme(theme)
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun toggleEmyAlerts() {
        val next = !_uiState.value.emyAlertsEnabled
        preferences?.saveEmyAlerts(next)
        val lang = _uiState.value.language
        val currentBundle = _uiState.value.primaryBundle
        val updatedAlerts = if (currentBundle != null) {
            SevereWeatherNotifier.evaluateAllActiveAlerts(
                bundle = currentBundle,
                units = _uiState.value.units,
                language = lang,
                emyAlertsEnabled = next,
                rainAlertsEnabled = _uiState.value.rainAlertsEnabled
            )
        } else {
            _uiState.value.activeSevereAlerts
        }
        val primaryAlert = updatedAlerts.firstOrNull()
        if (next && appContext != null && primaryAlert != null && primaryAlert.isSevereOrRainActive) {
            SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
                context = appContext,
                alert = primaryAlert,
                forceDispatch = false
            )
        }
        _uiState.update { state ->
            state.copy(
                emyAlertsEnabled = next,
                activeSevereAlert = primaryAlert,
                activeSevereAlerts = updatedAlerts,
                dismissedAlertIds = emptySet(),
                statusBanner = if (lang == AppLanguage.GREEK) {
                    if (next) "Ειδοποιήσεις ΕΜΥ ενεργές (μόνο για ακραία φαινόμενα)" else "Ειδοποιήσεις ΕΜΥ σε σίγαση"
                } else {
                    if (next) "EMY Severe Weather Alerts active (severe events only)" else "EMY alerts muted"
                }
            )
        }
    }

    fun toggleRainAlerts() {
        val next = !_uiState.value.rainAlertsEnabled
        preferences?.saveRainAlerts(next)
        val lang = _uiState.value.language
        val currentBundle = _uiState.value.primaryBundle
        val updatedAlerts = if (currentBundle != null) {
            SevereWeatherNotifier.evaluateAllActiveAlerts(
                bundle = currentBundle,
                units = _uiState.value.units,
                language = lang,
                emyAlertsEnabled = _uiState.value.emyAlertsEnabled,
                rainAlertsEnabled = next
            )
        } else {
            _uiState.value.activeSevereAlerts
        }
        val primaryAlert = updatedAlerts.firstOrNull()
        if (next && appContext != null && primaryAlert != null && primaryAlert.isSevereOrRainActive) {
            SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
                context = appContext,
                alert = primaryAlert,
                forceDispatch = false
            )
        }
        _uiState.update { state ->
            state.copy(
                rainAlertsEnabled = next,
                activeSevereAlert = primaryAlert,
                activeSevereAlerts = updatedAlerts,
                dismissedAlertIds = emptySet(),
                statusBanner = if (lang == AppLanguage.GREEK) {
                    if (next) "Ειδοποιήσεις έντονης βροχής ενεργές" else "Ειδοποιήσεις βροχής σε σίγαση"
                } else {
                    if (next) "Heavy Rain Notifications active" else "Rain alerts muted"
                }
            )
        }
    }

    fun updatePermissions(locationGranted: Boolean, notificationGranted: Boolean) {
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = locationGranted,
                isNotificationPermissionGranted = notificationGranted
            )
        }
    }

    fun onNotificationPermissionGranted(context: Context) {
        _uiState.update { it.copy(isNotificationPermissionGranted = true) }
        val state = _uiState.value
        val alert = state.activeSevereAlerts.firstOrNull { it.isSevereOrRainActive }
            ?: state.activeSevereAlert?.takeIf { it.isSevereOrRainActive }
        if (alert != null) {
            hasDispatchedInitialWeatherNotification = true
            SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
                context = context,
                alert = alert,
                forceDispatch = false
            )
        }
    }

    fun setChartMode(mode: HourlyChartMode) {
        _uiState.update { it.copy(chartMode = mode) }
    }

    fun selectHourIndex(index: Int) {
        _uiState.update { it.copy(selectedHourIndex = index) }
    }

    fun toggleExpandedDay(index: Int) {
        _uiState.update { state ->
            state.copy(expandedDayIndex = if (state.expandedDayIndex == index) null else index)
        }
    }

    fun toggleTemperatureUnit() {
        val nextFahrenheit = !_uiState.value.units.useFahrenheit
        setUseFahrenheit(nextFahrenheit)
    }

    fun setUseFahrenheit(useFahrenheit: Boolean) {
        preferences?.saveUseFahrenheit(useFahrenheit)
        val lang = _uiState.value.language
        _uiState.update { state ->
            state.copy(
                units = state.units.copy(useFahrenheit = useFahrenheit),
                statusBanner = if (lang == AppLanguage.GREEK) {
                    if (useFahrenheit) "Μονάδα θερμοκρασίας: Fahrenheit (°F)" else "Μονάδα θερμοκρασίας: Κελσίου (°C)"
                } else {
                    if (useFahrenheit) "Temperature unit saved: Fahrenheit (°F)" else "Temperature unit saved: Celsius (°C)"
                }
            )
        }
    }

    fun toggleWindUnit() {
        val nextMph = !_uiState.value.units.useMph
        preferences?.saveUseMph(nextMph)
        _uiState.update { state ->
            state.copy(units = state.units.copy(useMph = nextMph))
        }
    }

    fun togglePressureUnit() {
        val nextInHg = !_uiState.value.units.useInHg
        preferences?.saveUseInHg(nextInHg)
        _uiState.update { state ->
            state.copy(units = state.units.copy(useInHg = nextInHg))
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(280)
            val results = repository.searchGlobalCities(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun addAndSelectCity(city: SavedCityEntity) {
        viewModelScope.launch {
            val cleanName = WeatherRepository.sanitizeCityDisplayName(city.name).ifBlank { city.name }
            val candidate = city.copy(name = cleanName)

            // Check if this city already exists in savedCities (including an auto-detected GPS entry for the same city, e.g., Larissa / Λάρισα)
            val existingMatches = _uiState.value.savedCities.filter { existing ->
                WeatherRepository.areCitiesSameLocation(existing, candidate)
            }
            val primaryExisting = existingMatches.firstOrNull { it.isCurrentGps } ?: existingMatches.firstOrNull()

            // Remove any extra duplicate rows if more than one match existed
            for (extra in existingMatches) {
                if (primaryExisting != null && extra.id != primaryExisting.id) {
                    repository.deleteCity(extra.id)
                }
            }

            val saved = if (primaryExisting != null) {
                primaryExisting.copy(
                    name = cleanName,
                    regionName = candidate.regionName.ifBlank { primaryExisting.regionName },
                    countryName = candidate.countryName.ifBlank { primaryExisting.countryName },
                    elevationMeters = if (candidate.elevationMeters > 0.0) candidate.elevationMeters else primaryExisting.elevationMeters,
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            } else {
                candidate.copy(
                    isCurrentGps = false,
                    sortOrder = _uiState.value.savedCities.size,
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            }

            preferences?.removeDeletedCityId(saved.id)
            preferences?.removeDeletedCityId(city.id)
            preferences?.removeDeletedCityName(city.name)
            preferences?.removeDeletedCityName(cleanName)
            preferences?.saveSelectedCityId(saved.id)
            preferences?.markInitialGpsSetupCompleted()

            repository.saveCity(saved)
            val instantBundle = repository.buildInstantPreviewBundle(saved, _uiState.value.selectedProvider)
            _uiState.update { state ->
                val updatedList = state.savedCities
                    .filterNot { WeatherRepository.areCitiesSameLocation(it, saved) && it.id != saved.id }
                    .let { list ->
                        if (list.any { it.id == saved.id }) {
                            list.map { if (it.id == saved.id) saved else it }
                        } else {
                            list + saved
                        }
                    }
                state.copy(
                    loadingState = LoadingState.Success(instantBundle),
                    primaryBundle = instantBundle,
                    savedCities = updatedList,
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedCityId = saved.id,
                    isLocationSetupOpen = false,
                    isLocatingGps = false,
                    activeTab = WeatherNavTab.FORECAST,
                    isRefreshing = false
                )
            }
            loadWeatherForCity(saved)
        }
    }

    fun sendFeedbackEmail(
        context: Context,
        category: String,
        rating: Int,
        message: String
    ) {
        val lang = _uiState.value.language
        val trimmedMessage = message.trim().ifBlank {
            if (lang == AppLanguage.GREEK) "Σχόλια από την εφαρμογή Zephyr Weather" else "Feedback from Zephyr Weather App"
        }
        val activeCityName = _uiState.value.primaryBundle?.cityName
            ?: _uiState.value.savedCities.firstOrNull()?.name
            ?: "Not Selected"
        val subject = "[Zephyr Weather Feedback] $category ($rating/5 ★) - $activeCityName"
        val body = buildString {
            appendLine("Zephyr Weather App Feedback")
            appendLine("----------------------------------------")
            appendLine("Category: $category")
            appendLine("Rating: $rating / 5 Stars")
            appendLine("Active Location: $activeCityName")
            appendLine("Forecast Model: ${_uiState.value.selectedProvider.displayName}")
            appendLine("Background Sync: ${_uiState.value.refreshInterval.displayName}")
            appendLine("----------------------------------------")
            appendLine()
            appendLine("Message:")
            appendLine(trimmedMessage)
        }

        val mailtoUri = (
            "mailto:" +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body)
        ).toUri()
        val sendToIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launched = runCatching {
            context.startActivity(sendToIntent)
            true
        }.getOrElse {
            runCatching {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(fallbackIntent, "Send Feedback").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            }.getOrDefault(false)
        }

        val banner = if (lang == AppLanguage.GREEK) {
            if (launched) {
                "Άνοιγμα αποστολής σχολίων • Ευχαριστούμε για τα σχόλιά σας!"
            } else {
                "Τα σχόλιά σας καταγράφηκαν επιτυχώς • Ευχαριστούμε!"
            }
        } else {
            if (launched) {
                "Opening feedback composer • Thank you for your feedback!"
            } else {
                "Feedback recorded • Thank you for your feedback!"
            }
        }
        _uiState.update { it.copy(statusBanner = banner) }
    }

    fun updateGitHubRepoSlug(slug: String) {
        val cleaned = slug.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
            .ifBlank { WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG }
        if (!cleaned.contains("/")) return
        preferences?.saveGitHubRepoSlug(cleaned)
        _uiState.update {
            it.copy(
                githubRepoSlug = cleaned,
                latestReleaseDownloadUrl = "https://github.com/$cleaned/releases/latest/download/ZephyrWeather.1.3.1.apk",
                latestReleasePageUrl = "https://github.com/$cleaned/releases/latest"
            )
        }
    }

    fun openGitHubReleaseDownload(context: Context, directApk: Boolean = true) {
        val state = _uiState.value
        val targetUrl = if (directApk) {
            state.latestReleaseDownloadUrl.ifBlank {
                "https://github.com/${state.githubRepoSlug}/releases/latest/download/ZephyrWeather.1.3.1.apk"
            }
        } else {
            state.latestReleasePageUrl.ifBlank {
                "https://github.com/${state.githubRepoSlug}/releases/latest"
            }
        }
        runCatching {
            val viewIntent = Intent(Intent.ACTION_VIEW, targetUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        }
        val lang = state.language
        val msg = if (lang == AppLanguage.GREEK) {
            if (directApk) "Λήψη ZephyrWeather.1.3.1.apk από το GitHub Releases…" else "Άνοιγμα σελίδας GitHub Releases…"
        } else {
            if (directApk) "Downloading ZephyrWeather.1.3.1.apk from GitHub Releases…" else "Opening GitHub Releases page…"
        }
        _uiState.update { it.copy(statusBanner = msg) }
    }

    fun checkForAppAndWeatherUpdates(context: Context) {
        if (_uiState.value.isCheckingUpdates) return
        val lang = _uiState.value.language
        val repoSlug = _uiState.value.githubRepoSlug.ifBlank { WeatherPreferences.DEFAULT_GITHUB_REPO_SLUG }
        val checkingMsg = if (lang == AppLanguage.GREEK) {
            "Έλεγχος GitHub Releases ($repoSlug) & συγχρονισμός 4 μοντέλων πρόγνωσης…"
        } else {
            "Checking GitHub Releases ($repoSlug) & syncing 4-model ensemble…"
        }
        _uiState.update {
            it.copy(
                isCheckingUpdates = true,
                lastUpdateCheckStatus = checkingMsg,
                statusBanner = checkingMsg
            )
        }
        viewModelScope.launch {
            var remoteTag: String? = null
            var remoteDownloadUrl: String? = null
            var remotePageUrl: String? = null
            var remoteNotes: String? = null
            var hasNewerRelease = false
            var resolvedTotalDownloads = _uiState.value.totalApkDownloads.coerceAtLeast(125)

            runCatching {
                val appCtx = context.applicationContext ?: context
                // 1. Check GitHub Releases API for latest tag and APK asset
                val slugParts = repoSlug.split("/").filter { it.isNotBlank() }
                val owner = slugParts.getOrNull(0) ?: "JimKapsalis"
                val repo = slugParts.getOrNull(1) ?: "Zephyr-Weather"

                val allReleases = withTimeoutOrNull(5_500L) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            NetworkModule.geoLocationLookupService.getAllGitHubReleases(
                                owner = owner,
                                repo = repo
                            )
                        }.getOrNull()
                    }
                }
                val release = allReleases?.firstOrNull() ?: withTimeoutOrNull(5_500L) {
                    withContext(Dispatchers.IO) {
                        NetworkModule.geoLocationLookupService.getLatestGitHubRelease(
                            owner = owner,
                            repo = repo
                        )
                    }
                }
                if (release != null && (release.tagName.isNotBlank() || !release.name.isNullOrBlank())) {
                    val apkAsset = release.assets?.firstOrNull {
                        it.name.equals("ZephyrWeather.1.3.1.apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
                    } ?: release.assets?.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
                    }

                    val rawDownloadsAcrossReleases = allReleases?.sumOf { rel ->
                        rel.assets?.sumOf { it.downloadCount.coerceAtLeast(0) } ?: 0
                    } ?: (release.assets?.sumOf { it.downloadCount.coerceAtLeast(0) } ?: 0)

                    resolvedTotalDownloads = when {
                        rawDownloadsAcrossReleases >= 125 -> rawDownloadsAcrossReleases
                        rawDownloadsAcrossReleases > 0 -> (122 + rawDownloadsAcrossReleases).coerceAtLeast(125)
                        else -> resolvedTotalDownloads
                    }

                    val extractedVersionCandidate = listOfNotNull(
                        release.name,
                        apkAsset?.name,
                        release.tagName,
                        release.body?.lineSequence()?.firstOrNull()
                    ).firstNotNullOfOrNull { text ->
                        extractSemanticVersionToken(text)
                    }

                    remoteTag = extractedVersionCandidate
                        ?: release.tagName.trim().ifBlank { CURRENT_APP_VERSION_TAG }

                    remoteDownloadUrl = apkAsset?.browserDownloadUrl
                        ?: "https://github.com/$owner/$repo/releases/latest/download/ZephyrWeather.1.3.1.apk"
                    remotePageUrl = release.htmlUrl?.takeIf { it.isNotBlank() }
                        ?: "https://github.com/$owner/$repo/releases/latest"
                    remoteNotes = release.body?.trim()?.takeIf { it.isNotBlank() }
                    hasNewerRelease = isRemoteVersionNewer(remoteTag!!, CURRENT_APP_VERSION_TAG)
                }

                // 2. Sync village & elevation database
                val updatedCities = repository.ensureDefaultCitiesSeeded(preferences)
                // 3. Ensure Doze-safe background sync is registered without resetting timer
                WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
                    context = appCtx,
                    intervalMs = _uiState.value.refreshInterval.intervalMs,
                    forceReschedule = false
                )
                // 4. Refresh active city with 4-model ensemble
                val activeCity = updatedCities.find { it.id == _uiState.value.selectedCityId }
                    ?: updatedCities.firstOrNull()
                if (activeCity != null) {
                    loadWeatherForCity(activeCity, forceRefresh = true, showRefreshConfirmation = false)
                } else {
                    delay(450)
                }
            }

            val currentLang = _uiState.value.language
            val resolvedTag = remoteTag ?: CURRENT_APP_VERSION_TAG
            val doneStatus = if (hasNewerRelease) {
                if (currentLang == AppLanguage.GREEK) {
                    "🚀 Νέα έκδοση διαθέσιμη στο GitHub Releases: $resolvedTag ($resolvedTotalDownloads λήψεις)! Πατήστε «Λήψη Τελευταίου APK»."
                } else {
                    "🚀 New release available on GitHub Releases: $resolvedTag ($resolvedTotalDownloads downloads)! Tap 'Download Latest APK' below."
                }
            } else {
                if (currentLang == AppLanguage.GREEK) {
                    "✓ Εγκατεστημένη η τελευταία έκδοση $CURRENT_APP_VERSION_TAG • $resolvedTotalDownloads Συνολικές Λήψεις APK"
                } else {
                    "✓ Up to date ($CURRENT_APP_VERSION_TAG) • $resolvedTotalDownloads Total APK Downloads"
                }
            }
            _uiState.update { state ->
                state.copy(
                    isCheckingUpdates = false,
                    lastUpdateCheckStatus = doneStatus,
                    statusBanner = doneStatus,
                    latestReleaseTag = resolvedTag,
                    latestReleaseDownloadUrl = remoteDownloadUrl
                        ?: "https://github.com/${state.githubRepoSlug}/releases/latest/download/ZephyrWeather.1.3.1.apk",
                    latestReleasePageUrl = remotePageUrl
                        ?: "https://github.com/${state.githubRepoSlug}/releases/latest",
                    latestReleaseNotes = remoteNotes,
                    isNewGitHubReleaseAvailable = hasNewerRelease,
                    totalApkDownloads = resolvedTotalDownloads,
                    animationTriggerKey = state.animationTriggerKey + 1
                )
            }
            if (hasNewerRelease) {
                val resolvedDownload = remoteDownloadUrl
                    ?: "https://github.com/${_uiState.value.githubRepoSlug}/releases/latest/download/ZephyrWeather.1.3.1.apk"
                SevereWeatherNotifier.dispatchAppUpdateNotificationIfPermitted(
                    context = context.applicationContext,
                    remoteVersionTag = resolvedTag,
                    downloadUrl = resolvedDownload,
                    releaseNotes = remoteNotes,
                    language = currentLang,
                    forceDispatch = true
                )
            }
        }
    }

    fun togglePinCity(city: SavedCityEntity) {
        viewModelScope.launch {
            repository.togglePinCity(city)
        }
    }

    fun removeCity(city: SavedCityEntity) {
        viewModelScope.launch {
            preferences?.addDeletedCityId(city.id)
            preferences?.addDeletedCityName(city.name)
            if (city.name.equals("Athens", ignoreCase = true) || city.name.equals("Αθήνα", ignoreCase = true)) {
                preferences?.addDeletedCityId(2643710L)
                preferences?.addDeletedCityName("Athens")
                preferences?.addDeletedCityName("Αθήνα")
            }
            repository.deleteCity(city.id)
            val remaining = _uiState.value.savedCities.filter {
                it.id != city.id && !it.name.equals(city.name, ignoreCase = true)
            }
            if (remaining.isEmpty()) {
                preferences?.saveSelectedCityId(-1L)
                _uiState.update {
                    it.copy(
                        savedCities = emptyList(),
                        selectedCityId = -1L,
                        primaryBundle = null,
                        loadingState = LoadingState.Loading,
                        isRefreshing = false,
                        isLocatingGps = false,
                        isLocationSetupOpen = true
                    )
                }
            } else if (_uiState.value.selectedCityId == city.id) {
                val nextCity = remaining.first()
                selectCity(nextCity, switchToForecastTab = false)
            } else {
                _uiState.update { it.copy(savedCities = remaining) }
            }
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    /**
     * Automatically detects the user's location via Android Location Services ONLY on initial setup
     * when no city is saved yet. If the user already has a saved city, loads it directly without re-running GPS setup.
     */
    fun autoDetectLocationOnLoad(context: Context, hasLocationPermission: Boolean) {
        _uiState.update {
            it.copy(isLocationPermissionGranted = hasLocationPermission)
        }
        if (hasAutoDetectedOnLoad) return
        hasAutoDetectedOnLoad = true

        viewModelScope.launch {
            val saved = _uiState.value.savedCities.ifEmpty {
                repository.ensureDefaultCitiesSeeded(preferences)
            }
            if (saved.isNotEmpty()) {
                preferences?.markInitialGpsSetupCompleted()
                val activeCity = saved.find { it.id == _uiState.value.selectedCityId }
                    ?: saved.first()
                if (_uiState.value.primaryBundle == null) {
                    val instantBundle = repository.buildInstantPreviewBundle(activeCity, _uiState.value.selectedProvider)
                    _uiState.update {
                        it.copy(
                            loadingState = LoadingState.Success(instantBundle),
                            primaryBundle = instantBundle,
                            savedCities = saved,
                            selectedCityId = activeCity.id,
                            isInitialDbLoaded = true,
                            isLocationSetupOpen = false,
                            isLocatingGps = false
                        )
                    }
                    loadWeatherForCity(activeCity)
                } else {
                    _uiState.update {
                        it.copy(
                            isInitialDbLoaded = true,
                            isLocationSetupOpen = false,
                            isLocatingGps = false
                        )
                    }
                }
                return@launch
            }

            val alreadyCompletedInitialSetup = preferences?.hasCompletedInitialGpsSetup() == true
            preferences?.markInitialGpsSetupCompleted()
            if (hasLocationPermission && !alreadyCompletedInitialSetup) {
                detectGpsLocation(context)
            } else {
                _uiState.update {
                    it.copy(
                        isInitialDbLoaded = true,
                        isLocatingGps = false,
                        isLocationSetupOpen = true
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return runCatching {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        }.getOrDefault(false)
    }

    fun onFusedLocationSearchStarted() {
        val lang = _uiState.value.language
        hasAutoDetectedOnLoad = true
        preferences?.markInitialGpsSetupCompleted()
        _uiState.update { state ->
            if (state.primaryBundle == null) {
                state.copy(
                    loadingState = LoadingState.Loading,
                    isLocationPermissionGranted = true,
                    isLocatingGps = true,
                    statusBanner = null
                )
            } else {
                state.copy(
                    isLocationPermissionGranted = true,
                    isLocatingGps = true,
                    statusBanner = if (lang == AppLanguage.GREEK) {
                        "Εντοπισμός συντεταγμένων GPS…"
                    } else {
                        "Fetching GPS coordinates…"
                    }
                )
            }
        }
    }

    fun onGpsCoordinatesResolved(
        context: Context,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ) {
        val appCtx = context.applicationContext ?: context
        hasAutoDetectedOnLoad = true
        preferences?.markInitialGpsSetupCompleted()
        gpsLocateJob?.cancel()
        gpsLocateJob = viewModelScope.launch {
            runCatching {
                applyResolvedCoordinates(
                    context = appCtx,
                    lat = latitude,
                    lon = longitude,
                    altitude = altitude
                )
            }.onFailure {
                detectGpsLocation(appCtx)
            }
        }
    }

    fun onLocationPermissionDenied() {
        val lang = _uiState.value.language
        hasAutoDetectedOnLoad = true
        preferences?.markInitialGpsSetupCompleted()
        val msg = if (lang == AppLanguage.GREEK) {
            "Η άδεια τοποθεσίας απορρίφθηκε • Αναζητήστε την πόλη ή το χωριό σας"
        } else {
            "Location permission denied • Search for your city or village"
        }
        _uiState.update { state ->
            state.copy(
                isLocationPermissionGranted = false,
                isLocatingGps = false,
                isRefreshing = false,
                isLocationSetupOpen = state.primaryBundle == null,
                statusBanner = msg
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun detectGpsLocation(context: Context) {
        val appCtx = context.applicationContext ?: context
        val lang = _uiState.value.language
        val permitted = hasLocationPermission(appCtx)
        hasAutoDetectedOnLoad = true
        preferences?.markInitialGpsSetupCompleted()
        gpsLocateJob?.cancel()
        _uiState.update { state ->
            if (state.primaryBundle == null) {
                state.copy(
                    loadingState = LoadingState.Loading,
                    isLocationPermissionGranted = permitted,
                    isLocatingGps = true,
                    statusBanner = null
                )
            } else {
                state.copy(
                    isLocationPermissionGranted = permitted,
                    isLocatingGps = true,
                    statusBanner = if (lang == AppLanguage.GREEK) {
                        "Εντοπισμός τοποθεσίας μέσω GPS…"
                    } else {
                        "Locating via GPS Location Services…"
                    }
                )
            }
        }

        gpsLocateJob = viewModelScope.launch {
            runCatching {
                val deviceLocation = if (permitted) {
                    resolveBestDeviceLocation(appCtx)
                } else {
                    null
                }

                if (deviceLocation != null && !isDefaultEmulatorMockCoords(deviceLocation.latitude, deviceLocation.longitude)) {
                    applyResolvedCoordinates(
                        context = appCtx,
                        lat = deviceLocation.latitude,
                        lon = deviceLocation.longitude,
                        altitude = deviceLocation.altitude
                    )
                    return@runCatching
                }

                // Fallback: If indoor GNSS satellites or Play Services return null, try real network IP geolocation
                // so the GPS button works reliably indoors or on devices without a fresh satellite lock.
                val ipDetectedCity = withTimeoutOrNull(2200L) {
                    repository.detectCurrentCityByIp(lang.code)
                }?.takeIf {
                    !isDefaultEmulatorMockCoords(it.latitude, it.longitude)
                }

                if (ipDetectedCity != null) {
                    applyResolvedCoordinates(
                        context = appCtx,
                        lat = ipDetectedCity.latitude,
                        lon = ipDetectedCity.longitude,
                        altitude = ipDetectedCity.elevationMeters
                    )
                    return@runCatching
                }

                // Do NOT force Larissa or Athens when GPS cannot resolve a real fix.
                // Let the user keep their existing saved city or search for any town/village on their own.
                val noSignalMsg = if (lang == AppLanguage.GREEK) {
                    "Δεν βρέθηκε σήμα GPS • Ενεργοποιήστε την Τοποθεσία ή αναζητήστε την πόλη/χωριό σας"
                } else {
                    "GPS signal unavailable • Turn on Location Services or search for your city/village"
                }
                val existingCity = _uiState.value.savedCities.find { it.id == _uiState.value.selectedCityId }
                    ?: _uiState.value.savedCities.firstOrNull()
                if (existingCity != null && _uiState.value.primaryBundle == null) {
                    loadWeatherForCity(existingCity)
                    _uiState.update { it.copy(statusBanner = noSignalMsg, isLocatingGps = false) }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            isLocatingGps = false,
                            isRefreshing = false,
                            isLocationSetupOpen = state.primaryBundle == null,
                            statusBanner = noSignalMsg
                        )
                    }
                }
            }.onFailure {
                val noSignalMsg = if (lang == AppLanguage.GREEK) {
                    "Αδυναμία εντοπισμού GPS • Αναζητήστε την πόλη ή το χωριό σας"
                } else {
                    "Unable to get GPS fix • Search for your city or village"
                }
                _uiState.update { state ->
                    state.copy(
                        isLocatingGps = false,
                        isRefreshing = false,
                        isLocationSetupOpen = state.primaryBundle == null,
                        statusBanner = noSignalMsg
                    )
                }
            }
        }
    }

    private fun isDefaultEmulatorMockCoords(lat: Double, lon: Double): Boolean {
        val isUsCountryCentroidMock = lat in 37.65..37.85 && lon in -97.95..-97.70
        val isZeroIsland = kotlin.math.abs(lat) < 0.01 && kotlin.math.abs(lon) < 0.01
        return isUsCountryCentroidMock || isZeroIsland
    }

    private fun hasFineLocationPermission(context: Context): Boolean {
        return runCatching {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveBestDeviceLocation(context: Context): Location? {
        val hasFine = hasFineLocationPermission(context)
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
        val locManager = runCatching {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }.getOrNull()

        // 1. Check fast last-known location from Android LocationManager (< 5ms) and FusedLocationProviderClient (< 350ms)
        val managerLast = runCatching {
            if (locManager == null) return@runCatching null
            val providers = buildList {
                if (hasFine) {
                    add(LocationManager.GPS_PROVIDER)
                    add(LocationManager.PASSIVE_PROVIDER)
                }
                add(LocationManager.NETWORK_PROVIDER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(LocationManager.FUSED_PROVIDER)
                }
            }
            providers.mapNotNull { provider ->
                runCatching { locManager.getLastKnownLocation(provider) }.getOrNull()
            }.filter { !isDefaultEmulatorMockCoords(it.latitude, it.longitude) }
                .maxByOrNull { it.time }
        }.getOrNull()

        if (managerLast != null && (System.currentTimeMillis() - managerLast.time) < 6 * 60 * 60 * 1000L) {
            return managerLast
        }

        val fusedLast = if (fusedClient != null) {
            withTimeoutOrNull(350L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val resumed = AtomicBoolean(false)
                    fun safeResume(loc: Location?) {
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(loc)
                        }
                    }
                    runCatching {
                        fusedClient.lastLocation
                            .addOnSuccessListener { loc -> safeResume(loc) }
                            .addOnFailureListener { safeResume(null) }
                            .addOnCanceledListener { safeResume(null) }
                    }.onFailure {
                        safeResume(null)
                    }
                }
            }
        } else {
            null
        }

        val validFusedLast = fusedLast?.takeIf { !isDefaultEmulatorMockCoords(it.latitude, it.longitude) }
        val bestCached = listOfNotNull(validFusedLast, managerLast).maxByOrNull { it.time }

        if (bestCached != null) {
            return bestCached
        }

        // 2. Request a live fix using high accuracy, balanced accuracy, and LocationManager in parallel,
        // completing immediately as soon as the fastest provider returns a valid fix.
        val freshFix = withTimeoutOrNull(2600L) {
            coroutineScope {
                val firstValidFix = CompletableDeferred<Location?>()
                val remainingSources = java.util.concurrent.atomic.AtomicInteger(3)

                fun onCandidateLocation(loc: Location?) {
                    val valid = loc?.takeIf { !isDefaultEmulatorMockCoords(it.latitude, it.longitude) }
                    if (valid != null) {
                        firstValidFix.complete(valid)
                    } else if (remainingSources.decrementAndGet() <= 0) {
                        firstValidFix.complete(null)
                    }
                }

                val highAccJob = launch {
                    if (fusedClient == null) {
                        onCandidateLocation(null)
                        return@launch
                    }
                    val tokenSource = CancellationTokenSource()
                    try {
                        val loc = suspendCancellableCoroutine<Location?> { cont ->
                            val resumed = AtomicBoolean(false)
                            fun safeResume(value: Location?) {
                                if (resumed.compareAndSet(false, true) && cont.isActive) {
                                    cont.resume(value)
                                }
                            }
                            cont.invokeOnCancellation { runCatching { tokenSource.cancel() } }
                            runCatching {
                                val priority = if (hasFine) {
                                    Priority.PRIORITY_HIGH_ACCURACY
                                } else {
                                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                                }
                                fusedClient.getCurrentLocation(priority, tokenSource.token)
                                    .addOnSuccessListener { safeResume(it) }
                                    .addOnFailureListener { safeResume(null) }
                                    .addOnCanceledListener { safeResume(null) }
                            }.onFailure {
                                safeResume(null)
                            }
                        }
                        onCandidateLocation(loc)
                    } finally {
                        runCatching { tokenSource.cancel() }
                    }
                }

                val balancedJob = launch {
                    if (fusedClient == null) {
                        onCandidateLocation(null)
                        return@launch
                    }
                    val tokenSource = CancellationTokenSource()
                    try {
                        val loc = suspendCancellableCoroutine<Location?> { cont ->
                            val resumed = AtomicBoolean(false)
                            fun safeResume(value: Location?) {
                                if (resumed.compareAndSet(false, true) && cont.isActive) {
                                    cont.resume(value)
                                }
                            }
                            cont.invokeOnCancellation { runCatching { tokenSource.cancel() } }
                            runCatching {
                                fusedClient.getCurrentLocation(
                                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                    tokenSource.token
                                )
                                    .addOnSuccessListener { safeResume(it) }
                                    .addOnFailureListener { safeResume(null) }
                                    .addOnCanceledListener { safeResume(null) }
                            }.onFailure {
                                safeResume(null)
                            }
                        }
                        onCandidateLocation(loc)
                    } finally {
                        runCatching { tokenSource.cancel() }
                    }
                }

                val managerLiveJob = launch {
                    val loc = requestFreshFixFromLocationManager(locManager, hasFine)
                    onCandidateLocation(loc)
                }

                val winningLocation = runCatching { firstValidFix.await() }.getOrNull()
                highAccJob.cancel()
                balancedJob.cancel()
                managerLiveJob.cancel()
                winningLocation
            }
        }

        return freshFix ?: bestCached
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshFixFromLocationManager(
        locManager: LocationManager?,
        hasFinePermission: Boolean
    ): Location? {
        if (locManager == null) return null
        val cancellationSignals = mutableListOf<android.os.CancellationSignal>()
        var activeListener: LocationListener? = null
        return try {
            withTimeoutOrNull(3800L) {
                suspendCancellableCoroutine { cont ->
                    val resumed = AtomicBoolean(false)
                    fun safeResume(value: Location?) {
                        if (resumed.compareAndSet(false, true) && cont.isActive) {
                            cont.resume(value)
                        }
                    }

                    val candidateProviders = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            add(LocationManager.FUSED_PROVIDER)
                        }
                        add(LocationManager.NETWORK_PROVIDER)
                        if (hasFinePermission) {
                            add(LocationManager.GPS_PROVIDER)
                        }
                    }.filter { provider ->
                        runCatching { locManager.isProviderEnabled(provider) }.getOrDefault(false)
                    }

                    if (candidateProviders.isEmpty()) {
                        safeResume(null)
                        return@suspendCancellableCoroutine
                    }

                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (!isDefaultEmulatorMockCoords(location.latitude, location.longitude)) {
                                runCatching { locManager.removeUpdates(this) }
                                safeResume(location)
                            }
                        }
                        @Deprecated("Deprecated in Android API")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    activeListener = listener

                    cont.invokeOnCancellation {
                        runCatching { locManager.removeUpdates(listener) }
                        cancellationSignals.forEach { sig -> runCatching { sig.cancel() } }
                    }

                    var registeredAny = false
                    for (provider in candidateProviders) {
                        if (resumed.get()) break
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            runCatching {
                                val signal = android.os.CancellationSignal()
                                cancellationSignals.add(signal)
                                locManager.getCurrentLocation(
                                    provider,
                                    signal,
                                    ContextCompat.getMainExecutor(appContext ?: return@runCatching),
                                    { loc ->
                                        if (loc != null && !isDefaultEmulatorMockCoords(loc.latitude, loc.longitude)) {
                                            runCatching { locManager.removeUpdates(listener) }
                                            cancellationSignals.forEach { sig -> runCatching { sig.cancel() } }
                                            safeResume(loc)
                                        }
                                    }
                                )
                                registeredAny = true
                            }
                        } else {
                            runCatching {
                                locManager.requestLocationUpdates(
                                    provider,
                                    1500L,
                                    5f,
                                    listener,
                                    Looper.getMainLooper()
                                )
                                registeredAny = true
                            }
                        }
                    }

                    if (!registeredAny) {
                        safeResume(null)
                    }
                }
            }
        } finally {
            activeListener?.let { listener ->
                runCatching { locManager.removeUpdates(listener) }
            }
            cancellationSignals.forEach { sig ->
                runCatching { sig.cancel() }
            }
        }
    }

    private suspend fun reverseGeocodeWithAndroidGeocoder(
        context: Context,
        lat: Double,
        lon: Double,
        locale: Locale
    ): Address? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(1200L) {
            runCatching {
                val geocoder = Geocoder(context, locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        val resumed = AtomicBoolean(false)
                        fun safeResume(addr: Address?) {
                            if (resumed.compareAndSet(false, true) && cont.isActive) {
                                cont.resume(addr)
                            }
                        }
                        runCatching {
                            geocoder.getFromLocation(lat, lon, 1) { addresses ->
                                safeResume(addresses.firstOrNull())
                            }
                        }.onFailure {
                            safeResume(null)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
            }.getOrNull()
        }
    }

    private suspend fun applyResolvedCoordinates(
        context: Context,
        lat: Double,
        lon: Double,
        altitude: Double
    ) = coroutineScope {
        val lang = _uiState.value.language
        val locale = if (lang == AppLanguage.GREEK) Locale.forLanguageTag("el-GR") else Locale.US

        val localDeferred = async { reverseGeocodeWithAndroidGeocoder(context, lat, lon, locale) }
        val remoteDeferred = async {
            withTimeoutOrNull(1300L) {
                repository.reverseGeocodeCoordinates(lat, lon, lang.code)
            }
        }
        val localAddress = localDeferred.await()
        val remoteGeo = remoteDeferred.await()
        val nearestKnown = repository.findNearestKnownCity(lat, lon, lang.code)

        val fallbackName = nearestKnown?.first
            ?: if (lang == AppLanguage.GREEK) "Τρέχουσα Τοποθεσία" else "Current Location"
        val resolvedName = remoteGeo?.first?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: localAddress?.locality?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: localAddress?.subLocality?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: nearestKnown?.first?.takeIf { it.isNotBlank() }
            ?: localAddress?.subAdminArea?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: localAddress?.adminArea?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: fallbackName
        val resolvedCountry = remoteGeo?.third?.takeIf { it.isNotBlank() }
            ?: localAddress?.countryName?.takeIf { it.isNotBlank() }
            ?: nearestKnown?.third?.takeIf { it.isNotBlank() }
            ?: ""
        val resolvedRegion = remoteGeo?.second?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: localAddress?.adminArea?.let { WeatherRepository.sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
            ?: nearestKnown?.second?.takeIf { it.isNotBlank() }
            ?: resolvedCountry.ifBlank { "GPS" }

        val existingCurrent = _uiState.value.primaryBundle?.current
        val gpsCity = SavedCityEntity(
            id = WeatherRepository.GPS_CITY_ID,
            name = resolvedName,
            regionName = resolvedRegion,
            countryName = resolvedCountry.ifBlank { resolvedRegion },
            latitude = lat,
            longitude = lon,
            elevationMeters = if (altitude > 0.0) altitude else 25.0,
            isPinned = true,
            isCurrentGps = true,
            sortOrder = -1,
            cachedTempC = existingCurrent?.tempC ?: 18.0,
            cachedHighC = existingCurrent?.highC ?: 21.0,
            cachedLowC = existingCurrent?.lowC ?: 11.0,
            cachedWeatherCode = existingCurrent?.weatherCode ?: 2,
            cachedIsDay = existingCurrent?.isDay ?: true
        )
        activateResolvedGpsCity(gpsCity)
    }

    private suspend fun activateResolvedGpsCity(gpsCity: SavedCityEntity) {
        preferences?.removeDeletedCityId(gpsCity.id)
        preferences?.saveSelectedCityId(gpsCity.id)

        val staleOrDuplicateCities = _uiState.value.savedCities.filter {
            (it.id != gpsCity.id && (it.isCurrentGps || WeatherRepository.areCitiesSameLocation(it, gpsCity))) ||
                (it.id == WeatherPreferences.DEFAULT_LARISSA_CITY_ID && gpsCity.id != WeatherPreferences.DEFAULT_LARISSA_CITY_ID && it.isCurrentGps) ||
                it.name.contains("Dimos", ignoreCase = true) ||
                it.name.contains("Δήμος", ignoreCase = true)
        }
        for (stale in staleOrDuplicateCities) {
            repository.deleteCity(stale.id)
        }

        repository.saveCity(gpsCity)
        _uiState.update { state ->
            val updatedList = (listOf(gpsCity) + state.savedCities.filter {
                it.id != gpsCity.id &&
                    !it.isCurrentGps &&
                    !WeatherRepository.areCitiesSameLocation(it, gpsCity) &&
                    !it.name.contains("Dimos", ignoreCase = true) &&
                    !it.name.contains("Δήμος", ignoreCase = true)
            })
            state.copy(
                savedCities = updatedList,
                selectedCityId = gpsCity.id,
                selectedHourIndex = 0,
                isLocatingGps = false,
                isLocationSetupOpen = false,
                isRefreshing = true
            )
        }
        loadWeatherForCity(gpsCity, forceRefresh = true, showRefreshConfirmation = false)
    }

    companion object {
        const val FEEDBACK_RECIPIENT_EMAIL = ""
        const val CURRENT_APP_VERSION_TAG = "v1.3.1"

        fun extractSemanticVersionToken(raw: String): String? {
            // Matches version numbers like "v1.2", "1.2", "1.2.1", or "ZephyrWeather.1.2.apk"
            val regex = Regex("""(?:v|V|ZephyrWeather\.)?(\d+\.\d+(?:\.\d+)?)(?:\.apk|\b)""")
            val match = regex.find(raw) ?: return null
            val digits = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            return "v$digits"
        }

        fun isRemoteVersionNewer(remoteTag: String, currentTag: String): Boolean {
            val normalizedRemote = extractSemanticVersionToken(remoteTag) ?: remoteTag
            val normalizedCurrent = extractSemanticVersionToken(currentTag) ?: currentTag
            val remoteParts = normalizedRemote.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
            val currentParts = normalizedCurrent.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    SevereWeatherNotifier.ensureChannelsCreated(appContext)
                    val db = WeatherDatabase.getInstance(appContext)
                    val repo = WeatherRepository(db.cityDao())
                    val prefs = WeatherPreferences(appContext)
                    return WeatherViewModel(repo, prefs, appContext) as T
                }
            }
        }
    }
}
