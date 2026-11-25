package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.SavedCityEntity
import com.example.data.WeatherDatabase
import com.example.data.WeatherPreferences
import com.example.data.WeatherRepository
import com.example.model.AppLanguage
import com.example.model.RadarLayerMode
import com.example.model.WeatherUnits
import com.example.ui.theme.WeatherIcons
import com.example.util.SevereAlertSeverity
import com.example.util.SevereAlertType
import com.example.util.SevereWeatherNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zephyr Weather", appName)
    }

    @Test
    fun `verify WMO weather mapper clear sky label in English and Greek`() {
        val spec = com.example.model.WmoWeatherMapper.resolve(0, isDay = true)
        assertEquals("Clear Sky", spec.localizedLabel(AppLanguage.ENGLISH))
        assertEquals("Αίθριος Ουρανός", spec.localizedLabel(AppLanguage.GREEK))
    }

    @Test
    fun `verify weather units fahrenheit conversion and persistent storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = WeatherPreferences(context)
        prefs.saveUseFahrenheit(true)
        val loadedUnits = prefs.loadUnits()
        assertTrue(loadedUnits.useFahrenheit)
        assertEquals("68°F", loadedUnits.formatTemp(20.0))

        prefs.saveUseFahrenheit(false)
        assertFalse(prefs.loadUnits().useFahrenheit)
        assertEquals("20°C", prefs.loadUnits().formatTemp(20.0))
    }

    @Test
    fun `verify initial default language is English and language preference persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = WeatherPreferences(context)
        assertEquals(AppLanguage.ENGLISH, prefs.loadLanguage())

        prefs.saveLanguage(AppLanguage.GREEK)
        assertEquals(AppLanguage.GREEK, prefs.loadLanguage())

        prefs.saveLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, prefs.loadLanguage())
    }

    @Test
    fun `verify radar layer modes localized titles`() {
        assertEquals(4, RadarLayerMode.entries.size)
        assertEquals("Ραντάρ Βροχής", RadarLayerMode.RAIN.localizedTitle(AppLanguage.GREEK))
        assertEquals("Rain Radar", RadarLayerMode.RAIN.localizedTitle(AppLanguage.ENGLISH))
    }

    @Test
    fun `verify severe weather notifier detects thunderstorm and heavy rain`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = WeatherRepository(WeatherDatabase.getInstance(context).cityDao())
        val stormCity = SavedCityEntity(
            id = 258576L,
            name = "Larissa",
            regionName = "Thessaly",
            countryName = "Greece",
            latitude = 39.6390,
            longitude = 22.4191,
            cachedWeatherCode = 95,
            cachedTempC = 19.0
        )
        val stormBundle = repo.buildInstantPreviewBundle(stormCity)
        val alert = SevereWeatherNotifier.evaluateBundleForAlerts(
            bundle = stormBundle,
            units = WeatherUnits(),
            language = AppLanguage.ENGLISH
        )
        assertEquals(SevereAlertType.THUNDERSTORM, alert.type)
        assertEquals(SevereAlertSeverity.CRITICAL, alert.severity)
        assertTrue(alert.isSevereOrRainActive)
        assertEquals(39.6390, alert.latitude, 0.001)
        assertEquals(22.4191, alert.longitude, 0.001)
    }

    @Test
    fun `verify weather providers count and model queries`() {
        val providers = com.example.model.WeatherProvider.entries
        assertEquals(4, providers.size)
        assertEquals(null, com.example.model.WeatherProvider.BEST_MATCH.modelQuery)
        assertEquals("ecmwf_ifs025", com.example.model.WeatherProvider.ECMWF.modelQuery)
        assertEquals("icon_seamless", com.example.model.WeatherProvider.DWD_ICON.modelQuery)
        assertEquals("gfs_seamless", com.example.model.WeatherProvider.NOAA_GFS.modelQuery)
    }

    @Test
    fun `verify all weather codes map strictly to the multi-color weather icons including Partly Cloudy, Foggy, Snow Clouds, and Snow Storm`() {
        val allowedIcons = setOf(
            WeatherIcons.WbSunny,
            WeatherIcons.PartlySunny,
            WeatherIcons.Foggy,
            WeatherIcons.Grain,
            WeatherIcons.SnowCloud,
            WeatherIcons.SnowStorm,
            WeatherIcons.Thunderstorm
        )
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 61, 65, 71, 73, 75, 77, 80, 85, 86, 95, 99)
        codes.forEach { code ->
            val spec = com.example.model.WmoWeatherMapper.resolve(code, isDay = true)
            assertTrue("Code $code should map to one of the multi-color weather icons", spec.icon in allowedIcons)
            assertEquals(Color.Unspecified, spec.resolvedIconTint())
        }
        val partlyCloudySpec = com.example.model.WmoWeatherMapper.resolve(2, isDay = true)
        assertEquals(WeatherIcons.PartlySunny, partlyCloudySpec.icon)
        assertEquals(com.example.model.SkyParticleEffect.CLOUDS, partlyCloudySpec.particleEffect)

        val foggySpec = com.example.model.WmoWeatherMapper.resolve(45, isDay = true)
        assertEquals(WeatherIcons.Foggy, foggySpec.icon)
        assertEquals(com.example.model.SkyParticleEffect.FOG, foggySpec.particleEffect)

        val snowCloudsSpec = com.example.model.WmoWeatherMapper.resolve(73, isDay = true)
        assertEquals(WeatherIcons.SnowCloud, snowCloudsSpec.icon)
        assertEquals(com.example.model.SkyParticleEffect.SNOW, snowCloudsSpec.particleEffect)

        val snowStormSpec = com.example.model.WmoWeatherMapper.resolve(75, isDay = true)
        assertEquals(WeatherIcons.SnowStorm, snowStormSpec.icon)
        assertEquals(com.example.model.SkyParticleEffect.SNOW_STORM, snowStormSpec.particleEffect)
    }

    @Test
    fun `verify auto refresh interval defaults to 15 minutes and battery saver is enabled by default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = WeatherPreferences(context)
        val intervals = com.example.model.AutoRefreshInterval.entries
        assertEquals(4, intervals.size)
        assertEquals(com.example.model.AutoRefreshInterval.EVERY_15_MIN, prefs.loadRefreshInterval())
        assertTrue(prefs.loadBatterySaverSync())
    }

    @Test
    fun `verify distinct severe weather alert cards and no swipe instructions in alert messages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = WeatherRepository(WeatherDatabase.getInstance(context).cityDao())
        val stormCity = SavedCityEntity(
            id = 258576L,
            name = "Larissa",
            regionName = "Thessaly",
            countryName = "Greece",
            latitude = 39.6390,
            longitude = 22.4191,
            cachedWeatherCode = 95,
            cachedTempC = 19.0
        )
        val stormBundle = repo.buildInstantPreviewBundle(stormCity)
        val alerts = SevereWeatherNotifier.evaluateAllActiveAlerts(
            bundle = stormBundle,
            units = WeatherUnits(),
            language = AppLanguage.GREEK
        )
        assertTrue(alerts.isNotEmpty())
        assertEquals(alerts.size, alerts.map { it.id }.distinct().size)
        alerts.forEach { alert ->
            assertFalse("Should not contain Σύρετε δίπλα", alert.message.contains("Σύρετε δίπλα"))
        }

        val liveTestCards = SevereWeatherNotifier.buildLiveTestAlertList(
            bundle = stormBundle,
            units = WeatherUnits(),
            language = AppLanguage.GREEK
        )
        assertEquals(2, liveTestCards.size)
        assertNotEquals(liveTestCards[0].id, liveTestCards[1].id)
        assertEquals(SevereAlertType.THUNDERSTORM, liveTestCards[0].type)
        assertEquals(SevereAlertType.HEAVY_RAIN, liveTestCards[1].type)
    }

    @Test
    fun `verify sky atmosphere presets resolve live, sunset, snow clouds, snow storm, and night moon background weather conditions`() {
        val presets = com.example.model.SkyAtmospherePreset.entries
        assertEquals(9, presets.size)

        val liveSpec = com.example.model.SkyAtmospherePreset.LIVE_GPS.resolveVisualSpec(liveCode = 65, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.RAIN, liveSpec.particleEffect)

        val sunsetSpec = com.example.model.SkyAtmospherePreset.SUNSET_TWILIGHT.resolveVisualSpec(liveCode = 0, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.SUNSET_DUSK, sunsetSpec.particleEffect)

        val stormSpec = com.example.model.SkyAtmospherePreset.THUNDERSTORM.resolveVisualSpec(liveCode = 0, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.THUNDER, stormSpec.particleEffect)

        val snowSpec = com.example.model.SkyAtmospherePreset.SNOW_CRYSTALS.resolveVisualSpec(liveCode = 0, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.SNOW, snowSpec.particleEffect)
        assertEquals(WeatherIcons.SnowCloud, snowSpec.icon)

        val snowStormSpec = com.example.model.SkyAtmospherePreset.SNOW_STORM.resolveVisualSpec(liveCode = 0, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.SNOW_STORM, snowStormSpec.particleEffect)
        assertEquals(WeatherIcons.SnowStorm, snowStormSpec.icon)

        val nightSpec = com.example.model.SkyAtmospherePreset.STARLIT_NIGHT.resolveVisualSpec(liveCode = 0, liveIsDay = true)
        assertEquals(com.example.model.SkyParticleEffect.STARFIELD, nightSpec.particleEffect)
        assertEquals(WeatherIcons.NightsStay, nightSpec.icon)
    }

    @Test
    fun `verify weather icons mapping for sun, sun with clouds, foggy mist, clouds with rain, thunderstorm, and night moon`() {
        val clearSpec = com.example.model.WmoWeatherMapper.resolve(0, isDay = true)
        val mostlySunnySpec = com.example.model.WmoWeatherMapper.resolve(1, isDay = true)
        val partlySunnySpec = com.example.model.WmoWeatherMapper.resolve(2, isDay = true)
        val cloudySpec = com.example.model.WmoWeatherMapper.resolve(3, isDay = true)
        val fogSpec = com.example.model.WmoWeatherMapper.resolve(48, isDay = true)
        val rainSpec = com.example.model.WmoWeatherMapper.resolve(61, isDay = true)
        val thunderSpec = com.example.model.WmoWeatherMapper.resolve(95, isDay = true)
        val clearNightSpec = com.example.model.WmoWeatherMapper.resolve(0, isDay = false)
        val cloudyNightSpec = com.example.model.WmoWeatherMapper.resolve(2, isDay = false)

        assertEquals(WeatherIcons.WbSunny, clearSpec.icon)
        assertEquals(WeatherIcons.WbSunny, mostlySunnySpec.icon)
        assertEquals(WeatherIcons.PartlySunny, partlySunnySpec.icon)
        assertEquals(WeatherIcons.PartlySunny, cloudySpec.icon)
        assertEquals(WeatherIcons.Foggy, fogSpec.icon)
        assertEquals(WeatherIcons.Grain, rainSpec.icon)
        assertEquals(WeatherIcons.Thunderstorm, thunderSpec.icon)
        assertEquals(WeatherIcons.NightsStay, clearNightSpec.icon)
        assertEquals(WeatherIcons.NightsStay, cloudyNightSpec.icon)
        assertTrue(com.example.model.WmoWeatherMapper.isNightHourLabel("22:00"))
        assertTrue(com.example.model.WmoWeatherMapper.isNightHourLabel("03:00"))
        assertFalse(com.example.model.WmoWeatherMapper.isNightHourLabel("11:58"))
        assertEquals(Color.Unspecified, partlySunnySpec.resolvedIconTint())
        assertEquals(Color.Unspecified, fogSpec.resolvedIconTint())
        assertEquals(Color.Unspecified, rainSpec.resolvedIconTint())
        assertEquals(Color.Unspecified, thunderSpec.resolvedIconTint())
        assertEquals(Color.Unspecified, clearNightSpec.resolvedIconTint())
    }

    @Test
    fun `verify home screen widgets update and persist bundle state including night moon icon and Greece timezone`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = WeatherRepository(WeatherDatabase.getInstance(context).cityDao())
        val city = SavedCityEntity(
            id = 258576L,
            name = "Larissa",
            regionName = "Thessaly",
            countryName = "Greece",
            latitude = 39.6390,
            longitude = 22.4191,
            cachedWeatherCode = 0,
            cachedTempC = 24.0
        )
        val bundle = repo.buildInstantPreviewBundle(city)
        assertEquals("Europe/Athens", bundle.timezoneId)
        com.example.widget.ZephyrWeatherWidgetProvider.updateAllWidgetsFromBundle(
            context = context,
            bundle = bundle,
            units = WeatherUnits(),
            language = AppLanguage.ENGLISH,
            alert = null
        )
        assertEquals(R.drawable.ic_widget_sun, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(0, isDay = true))
        assertEquals(R.drawable.ic_widget_moon, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(0, isDay = false))
        assertEquals(R.drawable.ic_widget_moon, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(2, isDay = false))
        assertEquals(R.drawable.ic_widget_partly_sunny, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(2, isDay = true))
        assertEquals(R.drawable.ic_widget_fog, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(45))
        assertEquals(R.drawable.ic_widget_rain, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(61))
        assertEquals(R.drawable.ic_widget_thunderstorm, com.example.widget.ZephyrWeatherWidgetProvider.iconResForCode(95))
    }

    @Test
    fun `verify pull to refresh resets selected hour index and no default city is forced on startup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)
        val viewModel = com.example.ui.WeatherViewModel(repo, prefs, context.applicationContext)

        assertTrue("Initial savedCities should be empty so no hardcoded default city is forced", viewModel.uiState.value.savedCities.isEmpty())
        viewModel.selectHourIndex(5)
        assertEquals(5, viewModel.uiState.value.selectedHourIndex)

        val initialKey = viewModel.uiState.value.animationTriggerKey
        viewModel.refreshCurrentWeather()

        assertEquals(0, viewModel.uiState.value.selectedHourIndex)
        assertTrue(viewModel.uiState.value.animationTriggerKey > initialKey)
    }

    @Test
    fun `verify MainActivity launches and renders without crashing`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            assertTrue(activity != null)
        }
    }

    @Test
    fun `verify weather particle system builds valid particle specs for all weather conditions`() {
        com.example.model.SkyParticleEffect.entries.forEach { effect ->
            val fullParticles = com.example.ui.components.buildParticlesForEffect(effect, isCompactOverlay = false)
            val compactParticles = com.example.ui.components.buildParticlesForEffect(effect, isCompactOverlay = true)
            assertTrue("Full particles for $effect should not be empty", fullParticles.isNotEmpty())
            assertTrue("Compact particles for $effect should not be empty", compactParticles.isNotEmpty())
            assertTrue(fullParticles.size >= compactParticles.size)
            fullParticles.forEach { p ->
                assertTrue(p.baseX in 0f..1f)
                assertTrue(p.baseY in 0f..1f)
                assertTrue(p.speed > 0f)
                assertTrue(p.baseAlpha > 0f)
            }
        }
    }

    @Test
    fun `verify AdMob banner and app id string resources are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appId = context.getString(R.string.admob_app_id)
        val bannerId = context.getString(R.string.admob_banner_unit_id)
        assertTrue(appId.startsWith("ca-app-pub-"))
        assertTrue(bannerId.startsWith("ca-app-pub-"))
    }

    @Test
    fun `verify worldwide timezones and IP geolocation city detection resolve accurately`() = kotlinx.coroutines.test.runTest {
        assertEquals("America/New_York", WeatherRepository.resolveEffectiveTimezoneId("America/New_York"))
        assertEquals("Asia/Tokyo", WeatherRepository.resolveEffectiveTimezoneId("Asia/Tokyo"))
        assertEquals("Europe/London", WeatherRepository.resolveEffectiveTimezoneId("Europe/London"))
        assertEquals("Europe/Athens", WeatherRepository.resolveEffectiveTimezoneId("auto"))
        assertEquals("Europe/Athens", WeatherRepository.resolveEffectiveTimezoneId(""))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeGeoService = object : com.example.network.GeoLocationLookupService {
            override suspend fun detectIpLocation() = com.example.network.GeoIpLocationResponse(
                city = "Tokyo",
                region = "Tokyo",
                country = "Japan",
                latitude = "35.6895",
                longitude = "139.6917",
                timezone = "Asia/Tokyo"
            )

            override suspend fun detectIpLocationBackup() = com.example.network.IpWhoIsResponse(
                success = true,
                city = "Tokyo",
                region = "Tokyo",
                country = "Japan",
                latitude = 35.6895,
                longitude = 139.6917,
                timezone = com.example.network.IpWhoIsTimezoneDto(id = "Asia/Tokyo")
            )

            override suspend fun reverseGeocode(latitude: Double, longitude: Double, language: String) =
                com.example.network.ReverseGeocodeResponse(
                    city = "Tokyo",
                    locality = "Shinjuku",
                    principalSubdivision = "Tokyo",
                    countryName = "Japan"
                )

            override suspend fun searchVillagesAndTowns(
                query: String,
                format: String,
                addressDetails: Int,
                limit: Int,
                acceptLanguage: String,
                userAgent: String
            ) = emptyList<com.example.network.NominatimPlaceDto>()

            override suspend fun getLatestGitHubRelease(
                owner: String,
                repo: String,
                accept: String,
                userAgent: String
            ) = com.example.network.GitHubReleaseResponse(tagName = "v2.7.0")
        }
        val repo = WeatherRepository(
            cityDao = WeatherDatabase.getInstance(context).cityDao(),
            geoLocationLookupService = fakeGeoService
        )
        val detected = repo.detectCurrentCityByIp("en")
        assertTrue(detected != null)
        assertEquals("Tokyo", detected?.name)
        assertEquals("Japan", detected?.countryName)
        assertEquals("Asia/Tokyo", detected?.timezoneId)
        assertTrue(detected?.isCurrentGps == true)
    }

    @Test
    fun `verify ViewModel detectGpsLocation executes safely without forcing a default city`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)
        val viewModel = com.example.ui.WeatherViewModel(repo, prefs, context.applicationContext)

        viewModel.detectGpsLocation(context)
        assertTrue(viewModel.uiState.value.suggestedCities.isNotEmpty())
    }

    @Test
    fun `verify initial startup state starts with LoadingState Loading and delays weather fetch until GPS callback`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)

        // Default provider must be Multi-Model + Live Radar (BEST_MATCH)
        assertEquals(com.example.model.WeatherProvider.BEST_MATCH, prefs.loadProvider())

        val viewModel = com.example.ui.WeatherViewModel(repo, prefs, context.applicationContext)
        val initialState = viewModel.uiState.value
        assertEquals(com.example.model.WeatherProvider.BEST_MATCH, initialState.selectedProvider)
        assertTrue(
            "Initial loadingState must be LoadingState.Loading so MainActivity starts with an empty scaffold rather than hardcoded weather values",
            initialState.loadingState is com.example.ui.LoadingState.Loading
        )
        assertEquals(null, initialState.primaryBundle)
    }

    @Test
    fun `verify selected weather model persists across sessions and updates active bundle badge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)
        val viewModel = com.example.ui.WeatherViewModel(repo, prefs, context.applicationContext)

        viewModel.setWeatherProvider(com.example.model.WeatherProvider.NOAA_GFS)
        assertEquals(com.example.model.WeatherProvider.NOAA_GFS, viewModel.uiState.value.selectedProvider)
        assertEquals(com.example.model.WeatherProvider.NOAA_GFS, prefs.loadProvider())

        viewModel.setWeatherProvider(com.example.model.WeatherProvider.BEST_MATCH)
        assertEquals(com.example.model.WeatherProvider.BEST_MATCH, viewModel.uiState.value.selectedProvider)
        assertEquals(com.example.model.WeatherProvider.BEST_MATCH, prefs.loadProvider())
    }

    @Test
    fun `verify severe weather notifier posts real system notification to NotificationManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val testAlert = SevereWeatherNotifier.buildLiveTestAlert(
            bundle = null,
            units = WeatherUnits(),
            language = AppLanguage.ENGLISH
        )
        val posted = SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
            context = context,
            alert = testAlert,
            forceDispatch = true
        )
        assertTrue("Notification should be posted when POST_NOTIFICATIONS is granted", posted)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
        assertTrue("Active notifications in NotificationManager should not be empty", shadowNotificationManager.allNotifications.isNotEmpty())
    }

    @Test
    fun `verify 24-hour hourly forecast list provides 24 hours of temperatures and condition icons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = WeatherRepository(WeatherDatabase.getInstance(context).cityDao())
        val city = repo.getSuggestedCitiesForSelection().first()
        val bundle = repo.buildInstantPreviewBundle(city)

        assertEquals(24, bundle.hourly.size)
        assertEquals("Now", bundle.hourly.first().displayHour)
        bundle.hourly.forEach { hourItem ->
            assertTrue(hourItem.displayHour.isNotBlank())
            assertTrue(hourItem.tempC.isFinite())
            val isNight = !hourItem.isDay || (hourItem.displayHour != "Now" && com.example.model.WmoWeatherMapper.isNightHourLabel(hourItem.displayHour))
            val spec = com.example.model.WmoWeatherMapper.resolve(hourItem.weatherCode, isDay = !isNight)
            assertTrue(spec.localizedLabel(AppLanguage.ENGLISH).isNotBlank())
            assertEquals(Color.Unspecified, spec.resolvedIconTint())
        }
    }

    @Test
    fun `verify Dimos Larisa and municipality prefixes are stripped and removed`() {
        assertEquals("", WeatherRepository.sanitizeCityDisplayName("Dimos Larisa"))
        assertEquals("", WeatherRepository.sanitizeCityDisplayName("Dimos Larissa"))
        assertEquals("", WeatherRepository.sanitizeCityDisplayName("Δήμος Λαρισαίων"))
        assertEquals("Larissa", WeatherRepository.sanitizeCityDisplayName("Larisa"))
        assertEquals("Larissa", WeatherRepository.sanitizeCityDisplayName("Larissa"))
        assertEquals("Λάρισα", WeatherRepository.sanitizeCityDisplayName("Λάρισα"))
        assertEquals("Athens", WeatherRepository.sanitizeCityDisplayName("Dimos Athens"))
        assertEquals("Volos", WeatherRepository.sanitizeCityDisplayName("Municipality of Volos"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = WeatherRepository(WeatherDatabase.getInstance(context).cityDao())
        val nearestLarissa = repo.findNearestKnownCity(39.6390, 22.4191, "en")
        assertEquals("Larissa", nearestLarissa?.first)
        val nearestLarissaEl = repo.findNearestKnownCity(39.6390, 22.4191, "el")
        assertEquals("Λάρισα", nearestLarissaEl?.first)
    }

    @Test
    fun `verify Kato palaiokarua and Καωτ παλαιοκαρθα village search, no Larissa in suggested defaults, and deleted Athens persistence`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = WeatherPreferences(context)
        val offlineGeocoding = object : com.example.network.OpenMeteoGeocodingService {
            override suspend fun searchCities(query: String, count: Int, language: String, format: String) =
                com.example.network.GeocodingSearchResponse(emptyList())
        }
        val offlineGeoLookup = object : com.example.network.GeoLocationLookupService {
            override suspend fun detectIpLocation() = com.example.network.GeoIpLocationResponse()
            override suspend fun detectIpLocationBackup() = com.example.network.IpWhoIsResponse(success = false)
            override suspend fun reverseGeocode(latitude: Double, longitude: Double, language: String) =
                com.example.network.ReverseGeocodeResponse()
            override suspend fun searchVillagesAndTowns(
                query: String,
                format: String,
                addressDetails: Int,
                limit: Int,
                acceptLanguage: String,
                userAgent: String
            ) = emptyList<com.example.network.NominatimPlaceDto>()

            override suspend fun getLatestGitHubRelease(
                owner: String,
                repo: String,
                accept: String,
                userAgent: String
            ) = com.example.network.GitHubReleaseResponse(tagName = "v2.7.0")
        }
        val repo = WeatherRepository(
            cityDao = WeatherDatabase.getInstance(context).cityDao(),
            geocodingService = offlineGeocoding,
            geoLocationLookupService = offlineGeoLookup
        )

        assertFalse(
            "Suggested default quick-select cities must not include Larissa",
            repo.getSuggestedCitiesForSelection().any { it.name.equals("Larissa", ignoreCase = true) }
        )

        val resultsGreeklish = repo.searchGlobalCities("Kato palaiokarua")
        assertTrue(
            "Searching 'Kato palaiokarua' must find Κάτω Παλαιοκαρυά",
            resultsGreeklish.any { it.name.contains("Παλαιοκαρυά", ignoreCase = true) || it.name.contains("Palaiokarya", ignoreCase = true) }
        )

        val resultsTypo = repo.searchGlobalCities("Καωτ παλαιοκαρθα")
        assertTrue(
            "Searching 'Καωτ παλαιοκαρθα' must find Κάτω Παλαιοκαρυά",
            resultsTypo.any { it.name.contains("Κάτω Παλαιοκαρυά", ignoreCase = true) }
        )

        val resultsGreek = repo.searchGlobalCities("Κάτω Παλαιοκαρυά")
        assertTrue(
            "Searching 'Κάτω Παλαιοκαρυά' must find Κάτω Παλαιοκαρυά",
            resultsGreek.any { it.name.contains("Παλαιοκαρυά", ignoreCase = true) }
        )

        val village = resultsTypo.first { it.name.contains("Κάτω Παλαιοκαρυά", ignoreCase = true) }
        assertEquals(450.0, village.elevationMeters, 1.0)
        val bundle = repo.buildInstantPreviewBundle(village)
        assertEquals(10, bundle.daily.size)
        bundle.daily.forEach { dayItem ->
            assertTrue("Each day in 10-day forecast must have hourly items for expanded daily hourly forecast window", dayItem.hourly.isNotEmpty())
        }

        prefs.addDeletedCityName("Athens")
        assertTrue(prefs.loadDeletedCityNames().contains("athens"))
        prefs.markInitialGpsSetupCompleted()
        assertTrue(prefs.hasCompletedInitialGpsSetup())
    }

    @Test
    fun `verify location setup screen state and privacy-safe feedback submission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)
        val viewModel = com.example.ui.WeatherViewModel(repo, prefs, context.applicationContext)

        assertEquals("", com.example.ui.WeatherViewModel.FEEDBACK_RECIPIENT_EMAIL)

        viewModel.openLocationSetup()
        assertTrue(viewModel.uiState.value.isLocationSetupOpen)

        viewModel.closeLocationSetup()
        assertFalse(viewModel.uiState.value.isLocationSetupOpen)

        viewModel.sendFeedbackEmail(
            context = context,
            category = "Feature Request",
            rating = 5,
            message = "Great weather app and background alerts!"
        )
        assertTrue(viewModel.uiState.value.statusBanner?.isNotBlank() == true)
        assertFalse(viewModel.uiState.value.statusBanner?.contains("@") == true)
    }

    @Test
    fun `verify background weather sync scheduler does not insert default city when empty`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(db.cityDao())
        val prefs = WeatherPreferences(context)

        com.example.util.WeatherBackgroundSyncScheduler.scheduleBackgroundSync(context, 15 * 60 * 1000L)
        // When no city is saved, background sync must not insert any default city
        val syncedWhenEmpty = com.example.util.WeatherBackgroundSyncScheduler.performBackgroundWeatherCheckAndNotify(
            context = context,
            forceNotification = true
        )
        assertFalse(syncedWhenEmpty)
        assertTrue(repo.ensureDefaultCitiesSeeded(prefs).isEmpty())
    }

    @Test
    fun `verify Web Mercator satellite tile computation and town satellite drawable resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.img_satellite_town)
        assertTrue("Bundled satellite town photo drawable should exist", drawable != null)

        val tile = com.example.util.SatelliteTileLoader.computeWebMercatorTile(
            lat = 37.9838,
            lon = 23.7275,
            zoom = 13
        )
        assertEquals(13, tile.zoom)
        assertTrue(tile.centerTileX > 0)
        assertTrue(tile.centerTileY > 0)
        assertTrue(tile.offsetPxX in 0f..256f)
        assertTrue(tile.offsetPxY in 0f..256f)

        // Verify Frosted Glass 2x2 Time & Weather widget formatting
        assertEquals("Afternoon", com.example.widget.ZephyrWeatherWidgetProvider.formatDayPeriodLabel(15, isGreek = false))
        assertEquals("Απόγευμα", com.example.widget.ZephyrWeatherWidgetProvider.formatDayPeriodLabel(15, isGreek = true))
        assertEquals("Clear", com.example.widget.ZephyrWeatherWidgetProvider.formatShortGlassCondition(0, isGreek = false))
        assertEquals("Αίθριος", com.example.widget.ZephyrWeatherWidgetProvider.formatShortGlassCondition(0, isGreek = true))
        assertEquals("West", com.example.widget.ZephyrWeatherWidgetProvider.formatFullCardinalDirection("W", isGreek = false))
        assertEquals("4km/h", com.example.widget.ZephyrWeatherWidgetProvider.formatCompactWindSpeed(4.0, WeatherUnits()))
    }

    @Test
    fun `export debug apk and offline html installer to project root for zip download`() {
        val candidates = listOf(
            java.io.File("/app/applet/app/build/outputs/apk/debug/app-debug.apk"),
            java.io.File("/app/applet/.build-outputs/app-debug.apk"),
            java.io.File("build/outputs/apk/debug/app-debug.apk"),
            java.io.File("../.build-outputs/app-debug.apk")
        )
        val sourceApk = candidates.firstOrNull { it.exists() && it.length() > 100_000L }
        if (sourceApk != null) {
            val apkBytes = sourceApk.readBytes()
            val rootDir = java.io.File("/app/applet").takeIf { it.exists() } ?: java.io.File("..").canonicalFile
            java.io.File(rootDir, "ZephyrWeather.apk").delete()
            val rootApk = java.io.File(rootDir, "ZephyrWeather.1.apk")
            rootApk.writeBytes(apkBytes)
            assertTrue(rootApk.exists() && rootApk.length() > 100_000L)
        }
    }
}


