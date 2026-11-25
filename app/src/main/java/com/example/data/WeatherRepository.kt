package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.model.ActivitySuitability
import com.example.model.AirQualityTelemetry
import com.example.model.CityWeatherBundle
import com.example.model.CurrentWeatherTelemetry
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.SmartWeatherInsight
import com.example.model.calculateDewPointCelsius
import com.example.network.DailyDto
import com.example.network.GeocodingCityDto
import com.example.network.NetworkModule
import com.example.network.OpenMeteoAirQualityService
import com.example.network.OpenMeteoForecastResponse
import com.example.network.OpenMeteoForecastService
import com.example.network.OpenMeteoGeocodingService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class WeatherRepository(
    private val cityDao: CityDao,
    private val forecastService: OpenMeteoForecastService = NetworkModule.forecastService,
    private val airQualityService: OpenMeteoAirQualityService = NetworkModule.airQualityService,
    private val geocodingService: OpenMeteoGeocodingService = NetworkModule.geocodingService,
    private val geoLocationLookupService: com.example.network.GeoLocationLookupService = NetworkModule.geoLocationLookupService
) {
    val savedCitiesFlow: Flow<List<SavedCityEntity>> = cityDao.observeAllCities()
        .catch { emit(emptyList()) }

    fun buildInstantPreviewBundle(
        city: SavedCityEntity,
        provider: com.example.model.WeatherProvider = com.example.model.WeatherProvider.BEST_MATCH
    ): CityWeatherBundle {
        val defaultAqi = AirQualityTelemetry(
            usAqi = city.cachedAqi,
            europeanAqi = (city.cachedAqi * 0.8).roundToInt(),
            pm25 = 6.2,
            pm10 = 14.0,
            ozone = 56.0,
            nitrogenDioxide = 9.1,
            carbonMonoxide = 180.0
        )
        return buildResilientFallbackBundle(city, defaultAqi, provider)
    }

    fun findNearestKnownCity(
        lat: Double,
        lon: Double,
        languageCode: String = "en"
    ): Triple<String, String, String>? {
        val isGreek = languageCode.startsWith("el", ignoreCase = true)
        val candidates = if (isGreek) {
            localGreekCityCatalog.map { it.first } + getSuggestedCitiesForSelection()
        } else {
            getSuggestedCitiesForSelection() + localGreekCityCatalog.map { it.first }
        }
        var bestCity: SavedCityEntity? = null
        var bestDistDeg = Double.MAX_VALUE
        for (candidate in candidates) {
            val dLat = candidate.latitude - lat
            val dLon = (candidate.longitude - lon) * cos(Math.toRadians(lat))
            val distDeg = kotlin.math.sqrt(dLat * dLat + dLon * dLon)
            if (distDeg < bestDistDeg) {
                bestDistDeg = distDeg
                bestCity = candidate
            }
        }
        // ~0.25 degrees is roughly 27 km
        if (bestCity != null && bestDistDeg <= 0.25) {
            val localizedName = if (!isGreek && bestCity.id == 258576L) {
                "Larissa"
            } else if (!isGreek && bestCity.id == 2643710L) {
                "Athens"
            } else if (!isGreek && bestCity.id == 734077L) {
                "Thessaloniki"
            } else if (!isGreek && bestCity.id == 251833L) {
                "Volos"
            } else if (!isGreek && bestCity.id == 252661L) {
                "Trikala"
            } else {
                bestCity.name
            }
            return Triple(localizedName, bestCity.regionName, bestCity.countryName)
        }
        return null
    }

    suspend fun reverseGeocodeCoordinates(
        lat: Double,
        lon: Double,
        languageCode: String = "en"
    ): Triple<String, String, String>? {
        return runCatching {
            val resp = geoLocationLookupService.reverseGeocode(
                latitude = lat,
                longitude = lon,
                language = languageCode
            )
            val candidates = listOfNotNull(
                resp.locality?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("Dimos ", ignoreCase = true) && !it.startsWith("Δήμος ", ignoreCase = true) },
                resp.city?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("Dimos ", ignoreCase = true) && !it.startsWith("Δήμος ", ignoreCase = true) },
                resp.locality?.trim()?.takeIf { it.isNotBlank() },
                resp.city?.trim()?.takeIf { it.isNotBlank() }
            )
            val sanitizedName = candidates
                .map { sanitizeCityDisplayName(it) }
                .firstOrNull { it.isNotBlank() }

            val nearest = findNearestKnownCity(lat, lon, languageCode)
            val name = sanitizedName ?: nearest?.first ?: return@runCatching null
            if (name.isBlank()) return@runCatching null
            val country = resp.countryName?.trim()?.takeIf { it.isNotBlank() }
                ?: nearest?.third
                ?: ""
            val region = sanitizeCityDisplayName(resp.principalSubdivision?.trim().orEmpty())
                .ifBlank { nearest?.second.orEmpty() }
                .ifBlank { country.ifBlank { "Local" } }
            Triple(name, region, country.ifBlank { region })
        }.getOrElse {
            findNearestKnownCity(lat, lon, languageCode)
        }
    }

    suspend fun detectCurrentCityByIp(languageCode: String = "en"): SavedCityEntity? {
        val primaryIp = runCatching {
            val geo = geoLocationLookupService.detectIpLocation()
            val lat = geo.latitude?.toDoubleOrNull()
            val lon = geo.longitude?.toDoubleOrNull()
            if (lat != null && lon != null && lat.isFinite() && lon.isFinite()) {
                buildIpDetectedCity(
                    lat = lat,
                    lon = lon,
                    rawCity = geo.city,
                    rawRegion = geo.region,
                    rawCountry = geo.country,
                    rawTimezone = geo.timezone,
                    languageCode = languageCode
                )
            } else {
                null
            }
        }.getOrNull()

        if (primaryIp != null) return primaryIp

        return runCatching {
            val backup = geoLocationLookupService.detectIpLocationBackup()
            if (backup.success == false) return@runCatching null
            val lat = backup.latitude ?: return@runCatching null
            val lon = backup.longitude ?: return@runCatching null
            if (!lat.isFinite() || !lon.isFinite()) return@runCatching null
            buildIpDetectedCity(
                lat = lat,
                lon = lon,
                rawCity = backup.city,
                rawRegion = backup.region,
                rawCountry = backup.country,
                rawTimezone = backup.timezone?.id,
                languageCode = languageCode
            )
        }.getOrNull()
    }

    private suspend fun buildIpDetectedCity(
        lat: Double,
        lon: Double,
        rawCity: String?,
        rawRegion: String?,
        rawCountry: String?,
        rawTimezone: String?,
        languageCode: String
    ): SavedCityEntity? {
        if (abs(lat) < 0.01 && abs(lon) < 0.01) return null
        // Reject MaxMind US country-level fallback centroid (37.751, -97.822 in Cheney Reservoir / Ninnescah, KS)
        // and Android Emulator default Googleplex Mountain View coordinates (37.422, -122.084)
        if (lat in 37.65..37.85 && lon in -97.95..-97.70) return null
        if (lat in 37.30..37.50 && lon in -122.25..-121.95) return null

        val sanitizedCity = rawCity?.trim()?.takeIf { it.isNotBlank() }?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
        val sanitizedRegion = rawRegion?.trim()?.takeIf { it.isNotBlank() }?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
        val cleanCountry = rawCountry?.trim()?.takeIf { it.isNotBlank() }

        val reverse = if (sanitizedCity == null || cleanCountry == null) {
            reverseGeocodeCoordinates(lat, lon, languageCode)
        } else {
            null
        }
        val nearest = findNearestKnownCity(lat, lon, languageCode)

        val cityName = sanitizedCity ?: reverse?.first ?: nearest?.first ?: return null
        if (cityName.contains("Ninnescah", ignoreCase = true) || cityName.contains("Mountain View", ignoreCase = true)) {
            return null
        }
        val countryName = cleanCountry ?: reverse?.third ?: nearest?.third ?: ""
        val regionName = sanitizedRegion ?: reverse?.second ?: nearest?.second ?: countryName.ifBlank { "Local" }
        val effectiveTimezone = resolveEffectiveTimezoneId(rawTimezone)

        return SavedCityEntity(
            id = GPS_CITY_ID,
            name = cityName,
            regionName = regionName,
            countryName = countryName.ifBlank { regionName },
            latitude = lat,
            longitude = lon,
            elevationMeters = 25.0,
            timezoneId = effectiveTimezone,
            isPinned = true,
            isCurrentGps = true,
            sortOrder = -1
        )
    }

    suspend fun ensureDefaultCitiesSeeded(preferences: WeatherPreferences? = null): List<SavedCityEntity> {
        return runCatching {
            val deletedIds = preferences?.loadDeletedCityIds().orEmpty()
            val deletedNames = preferences?.loadDeletedCityNames().orEmpty()
            val obsoletePresetIds = setOf(
                9036755L // Poseidonas
            ) + deletedIds

            val shouldCleanLegacyDefaults = preferences != null && !preferences.hasClearedAutoSeededCities()
            val existing = cityDao.getAllCitiesOnce()
            for (city in existing) {
                val isBogusUsCentroid = city.latitude in 37.65..37.85 && city.longitude in -97.95..-97.70
                val lowerName = city.name.lowercase(Locale.ROOT).trim()
                val isLegacyAutoDefault = shouldCleanLegacyDefaults && (
                    city.id == WeatherPreferences.DEFAULT_LARISSA_CITY_ID ||
                        city.id == 2643710L ||
                        (city.id == GPS_CITY_ID && (lowerName == "athens" || lowerName == "αθήνα" || lowerName == "αθηνα" || lowerName == "larissa" || lowerName == "λάρισα" || lowerName == "λαρισα"))
                    )

                if (
                    isLegacyAutoDefault ||
                    city.id in obsoletePresetIds ||
                    lowerName in deletedNames ||
                    isBogusUsCentroid ||
                    city.name.contains("Ninnescah", ignoreCase = true) ||
                    city.name.contains("Dimos", ignoreCase = true) ||
                    city.name.contains("Δήμος", ignoreCase = true) ||
                    city.name.contains("Poseidon", ignoreCase = true) ||
                    city.name.contains("Ποσειδ", ignoreCase = true) ||
                    city.name.contains("Mountain View", ignoreCase = true)
                ) {
                    cityDao.deleteCityById(city.id)
                }
            }
            if (shouldCleanLegacyDefaults) {
                preferences.markAutoSeededCitiesCleared()
            }

            val remaining = cityDao.getAllCitiesOnce()
            for (c in remaining) {
                if (c.id == 258577L || c.name.contains("Κάτω Παλαιοκαρυά", ignoreCase = true) || c.name.contains("Kato Palaiokar", ignoreCase = true)) {
                    cityDao.upsertCity(
                        c.copy(
                            latitude = 39.4278,
                            longitude = 21.5714,
                            elevationMeters = 450.0,
                            timezoneId = "Europe/Athens"
                        )
                    )
                } else if (c.id == 258578L || c.name.contains("Άνω Παλαιοκαρυά", ignoreCase = true)) {
                    cityDao.upsertCity(
                        c.copy(
                            latitude = 39.4117,
                            longitude = 21.5358,
                            elevationMeters = 940.0,
                            timezoneId = "Europe/Athens"
                        )
                    )
                } else if (c.id == 258579L || c.name.contains("Μέση Παλαιοκαρυά", ignoreCase = true)) {
                    cityDao.upsertCity(
                        c.copy(
                            latitude = 39.4192,
                            longitude = 21.5528,
                            elevationMeters = 650.0,
                            timezoneId = "Europe/Athens"
                        )
                    )
                }
            }
            cityDao.getAllCitiesOnce()
        }.getOrDefault(emptyList())
    }

    fun getSuggestedCitiesForSelection(): List<SavedCityEntity> = listOf(
        SavedCityEntity(
            id = 2643710L,
            name = "Athens",
            regionName = "Attica",
            countryName = "Greece",
            latitude = 37.9838,
            longitude = 23.7275,
            elevationMeters = 70.0,
            timezoneId = "Europe/Athens",
            isPinned = true,
            sortOrder = 1,
            cachedTempC = 19.0,
            cachedHighC = 22.0,
            cachedLowC = 14.0,
            cachedWeatherCode = 2,
            cachedIsDay = true,
            cachedAqi = 24
        ),
        SavedCityEntity(
            id = 734077L,
            name = "Thessaloniki",
            regionName = "Central Macedonia",
            countryName = "Greece",
            latitude = 40.6401,
            longitude = 22.9444,
            elevationMeters = 25.0,
            timezoneId = "Europe/Athens",
            isPinned = true,
            sortOrder = 2,
            cachedTempC = 15.0,
            cachedHighC = 18.0,
            cachedLowC = 10.0,
            cachedWeatherCode = 61,
            cachedIsDay = true,
            cachedAqi = 20
        ),
        SavedCityEntity(
            id = 251833L,
            name = "Volos",
            regionName = "Thessaly",
            countryName = "Greece",
            latitude = 39.3666,
            longitude = 22.9425,
            elevationMeters = 15.0,
            timezoneId = "Europe/Athens",
            isPinned = false,
            sortOrder = 3,
            cachedTempC = 20.0,
            cachedHighC = 23.0,
            cachedLowC = 13.0,
            cachedWeatherCode = 1,
            cachedIsDay = true,
            cachedAqi = 19
        ),
        SavedCityEntity(
            id = 252661L,
            name = "Trikala",
            regionName = "Thessaly",
            countryName = "Greece",
            latitude = 39.5557,
            longitude = 21.7679,
            elevationMeters = 115.0,
            timezoneId = "Europe/Athens",
            isPinned = false,
            sortOrder = 4,
            cachedTempC = 21.0,
            cachedHighC = 24.0,
            cachedLowC = 11.0,
            cachedWeatherCode = 2,
            cachedIsDay = true,
            cachedAqi = 18
        ),
        SavedCityEntity(
            id = 2643743L,
            name = "London",
            regionName = "England",
            countryName = "United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            elevationMeters = 35.0,
            timezoneId = "Europe/London",
            isPinned = false,
            sortOrder = 5,
            cachedTempC = 16.0,
            cachedHighC = 18.0,
            cachedLowC = 11.0,
            cachedWeatherCode = 61,
            cachedIsDay = true,
            cachedAqi = 21
        )
    )

    private val localGreekCityCatalog = listOf(
        Triple(
            SavedCityEntity(
                id = 258577L,
                name = "Κάτω Παλαιοκαρυά (Kato Palaiokarya)",
                regionName = "Πύλη, Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.4278,
                longitude = 21.5714,
                elevationMeters = 450.0,
                timezoneId = "Europe/Athens",
                cachedTempC = 14.0,
                cachedHighC = 17.5,
                cachedLowC = 6.5,
                cachedWeatherCode = 2
            ),
            "κατω παλαιοκαρυα καωτ παλαιοκαρθα κατω παλαιοκαρθα παλαιοκαρθα παλαιοκαρυα kato palaiokarya kato palaiokarua kato palaiokartha kato paleokarya kato paleokarua palaiokaria paleokaria πυλη τρικαλων",
            "thessaly trikala pyli"
        ),
        Triple(
            SavedCityEntity(
                id = 258578L,
                name = "Άνω Παλαιοκαρυά (Ano Palaiokarya)",
                regionName = "Πύλη, Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.4117,
                longitude = 21.5358,
                elevationMeters = 940.0,
                timezoneId = "Europe/Athens",
                cachedTempC = 11.0,
                cachedHighC = 14.5,
                cachedLowC = 3.5,
                cachedWeatherCode = 2
            ),
            "ανω παλαιοκαρυα ανω παλαιοκαρθα παλαιοκαρυα παλαιοκαρθα ano palaiokarya ano palaiokarua ano palaiokartha ano paleokarya πυλη τρικαλων",
            "thessaly trikala pyli"
        ),
        Triple(
            SavedCityEntity(
                id = 258579L,
                name = "Μέση Παλαιοκαρυά (Mesi Palaiokarya)",
                regionName = "Πύλη, Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.4192,
                longitude = 21.5528,
                elevationMeters = 650.0,
                timezoneId = "Europe/Athens",
                cachedTempC = 12.5,
                cachedHighC = 16.0,
                cachedLowC = 5.0,
                cachedWeatherCode = 2
            ),
            "μεση παλαιοκαρυα μεση παλαιοκαρθα παλαιοκαρυα παλαιοκαρθα mesi palaiokarya mesi palaiokarua mesi palaiokartha mesi paleokarya πυλη τρικαλων",
            "thessaly trikala pyli"
        ),
        Triple(
            SavedCityEntity(
                id = 258580L,
                name = "Πύλη Τρικάλων",
                regionName = "Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.4592,
                longitude = 21.6194,
                elevationMeters = 240.0,
                cachedTempC = 19.5,
                cachedHighC = 23.0,
                cachedLowC = 10.5,
                cachedWeatherCode = 1
            ),
            "πυλη τρικαλων πυλη pyli pili trikala",
            "thessaly trikala"
        ),
        Triple(
            SavedCityEntity(
                id = 258581L,
                name = "Ελάτη Τρικάλων",
                regionName = "Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.5036,
                longitude = 21.5381,
                elevationMeters = 920.0,
                cachedTempC = 15.0,
                cachedHighC = 18.0,
                cachedLowC = 7.0,
                cachedWeatherCode = 2
            ),
            "ελατη τρικαλων ελατη elati trikala",
            "thessaly trikala"
        ),
        Triple(
            SavedCityEntity(
                id = 258582L,
                name = "Περτούλι",
                regionName = "Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.5403,
                longitude = 21.4656,
                elevationMeters = 1150.0,
                cachedTempC = 13.5,
                cachedHighC = 16.5,
                cachedLowC = 5.5,
                cachedWeatherCode = 2
            ),
            "περτουλι pertouli τρικαλα",
            "thessaly trikala"
        ),
        Triple(
            SavedCityEntity(
                id = 258583L,
                name = "Καλαμπάκα (Μετέωρα)",
                regionName = "Τρίκαλα, Θεσσαλία",
                countryName = "Ελλάδα",
                latitude = 39.7044,
                longitude = 21.6269,
                elevationMeters = 240.0,
                cachedTempC = 20.0,
                cachedHighC = 23.5,
                cachedLowC = 11.0,
                cachedWeatherCode = 0
            ),
            "καλαμπακα μετεωρα kalabaka kalampaka meteora τρικαλα",
            "thessaly trikala"
        ),
        Triple(SavedCityEntity(id = 258590L, name = "Ροπωτό Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4231, longitude = 21.5833, elevationMeters = 650.0), "ροπωτο ropoto πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258591L, name = "Στουρναραίικα", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4556, longitude = 21.4833, elevationMeters = 860.0), "στουρναραιικα στουρναρεικα stournaraiika stournareika πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258592L, name = "Βαθύρρευμα", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4394, longitude = 21.4547, elevationMeters = 900.0), "βαθυρρευμα βαθυρευμα vathyrrevma vathyrevma πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258593L, name = "Βαλκάνο", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4064, longitude = 21.4397, elevationMeters = 980.0), "βαλκανο valkano πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258594L, name = "Μοσχόφυτο", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4028, longitude = 21.4044, elevationMeters = 1000.0), "μοσχοφυτο moschofyto mosxofyto πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258595L, name = "Μυρόφυλλο", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3739, longitude = 21.3183, elevationMeters = 750.0), "μυροφυλλο myrofyllo mirofillo πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258596L, name = "Νεραϊδοχώρι", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5467, longitude = 21.4258, elevationMeters = 1140.0), "νεραιδοχωρι neraidochori neraidoxori περτουλι τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258597L, name = "Πύρρα Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5206, longitude = 21.3983, elevationMeters = 1100.0), "πυρρα pyrra pirra τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258598L, name = "Δέση Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5067, longitude = 21.3647, elevationMeters = 1080.0), "δεση desi τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258599L, name = "Γαρδίκι Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5419, longitude = 21.3578, elevationMeters = 1050.0), "γαρδικι gardiki τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258600L, name = "Αθαμανία Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5322, longitude = 21.3756, elevationMeters = 980.0), "αθαμανια athamania τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258601L, name = "Παλαιομονάστηρο", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4850, longitude = 21.6525, elevationMeters = 180.0), "παλαιομοναστηρο palaiomonastiro paleomonastiro πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258602L, name = "Γόμφοι Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4614, longitude = 21.6914, elevationMeters = 140.0), "γομφοι gomfoi gomfi πυλη τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258603L, name = "Πηγή Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5092, longitude = 21.6997, elevationMeters = 125.0), "πηγη pigi τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258604L, name = "Φήκη Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5178, longitude = 21.6442, elevationMeters = 145.0), "φηκη fiki τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258605L, name = "Πιαλεία Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5142, longitude = 21.6083, elevationMeters = 175.0), "πιαλεια pialeia pialia τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258606L, name = "Ελευθεροχώρι Τρικάλων", regionName = "Πύλη, Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5275, longitude = 21.6258, elevationMeters = 160.0), "ελευθεροχωρι eleftherochori eleftheroxori τρικαλα", "thessaly trikala pyli"),
        Triple(SavedCityEntity(id = 258607L, name = "Φαρκαδόνα", regionName = "Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5889, longitude = 22.0706, elevationMeters = 95.0), "φαρκαδονα farkadona τρικαλα", "thessaly trikala"),
        Triple(SavedCityEntity(id = 258608L, name = "Οιχαλία Τρικάλων", regionName = "Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.6075, longitude = 21.9781, elevationMeters = 105.0), "οιχαλια oichalia oixalia τρικαλα", "thessaly trikala"),
        Triple(SavedCityEntity(id = 258609L, name = "Καστράκι (Μετέωρα)", regionName = "Τρίκαλα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.7167, longitude = 21.6167, elevationMeters = 270.0), "καστρακι kastraki μετεωρα τρικαλα", "thessaly trikala"),
        Triple(SavedCityEntity(id = 258610L, name = "Λίμνη Πλαστήρα (Καλύβια Πεζούλας)", regionName = "Καρδίτσα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3081, longitude = 21.7169, elevationMeters = 800.0), "λιμνη πλαστηρα καλυβια πεζουλας limni plastira kalyvia pezoulas καρδιτσα", "thessaly karditsa"),
        Triple(SavedCityEntity(id = 258611L, name = "Νεοχώρι Λίμνης Πλαστήρα", regionName = "Καρδίτσα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.2764, longitude = 21.7322, elevationMeters = 950.0), "νεοχωρι λιμνης πλαστηρα neochori plastira καρδιτσα", "thessaly karditsa"),
        Triple(SavedCityEntity(id = 258612L, name = "Παλαμάς Καρδίτσας", regionName = "Καρδίτσα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4667, longitude = 22.0833, elevationMeters = 95.0), "παλαμας palamas καρδιτσα", "thessaly karditsa"),
        Triple(SavedCityEntity(id = 258613L, name = "Σοφάδες Καρδίτσας", regionName = "Καρδίτσα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3333, longitude = 22.1000, elevationMeters = 110.0), "σοφαδες sofades καρδιτσα", "thessaly karditsa"),
        Triple(SavedCityEntity(id = 258614L, name = "Συκούριο Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.7319, longitude = 22.5700, elevationMeters = 140.0), "συκουριο sykourio sikourio λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258615L, name = "Γόννοι Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.8617, longitude = 22.4767, elevationMeters = 90.0), "γοννοι gonnoi gonni λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258616L, name = "Ραψάνη Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.9014, longitude = 22.5494, elevationMeters = 500.0), "ραψανη rapsani λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258617L, name = "Αμπελώνας Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.7469, longitude = 22.3611, elevationMeters = 75.0), "αμπελωνας ampelonas abelonas λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258618L, name = "Τσαριτσάνη Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.8825, longitude = 22.2261, elevationMeters = 310.0), "τσαριτσανη tsaritsani λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258619L, name = "Πλατύκαμπος Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.6228, longitude = 22.5314, elevationMeters = 65.0), "πλατυκαμπος platykampos platikampos λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258620L, name = "Νίκαια Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5658, longitude = 22.4675, elevationMeters = 90.0), "νικαια nikaia nikea λαρισα", "thessaly larissa"),
        Triple(SavedCityEntity(id = 258621L, name = "Πορταριά Πηλίου", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3908, longitude = 22.9992, elevationMeters = 610.0), "πορταρια πηλιο portaria pelion βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258622L, name = "Μακρινίτσα Πηλίου", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4017, longitude = 22.9875, elevationMeters = 630.0), "μακρινιτσα πηλιο makrinitsa pelion βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258623L, name = "Ζαγορά Πηλίου", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4422, longitude = 23.1031, elevationMeters = 460.0), "ζαγορα πηλιο zagora pelion βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258624L, name = "Τσαγκαράδα Πηλίου", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3878, longitude = 23.1731, elevationMeters = 490.0), "τσαγκαραδα πηλιο tsagarada tsagkarada pelion βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258625L, name = "Μηλιές Πηλίου", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3278, longitude = 23.1508, elevationMeters = 400.0), "μηλιες πηλιο milies pelion βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258626L, name = "Αλμυρός Μαγνησίας", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.1811, longitude = 22.7594, elevationMeters = 70.0), "αλμυρος almyros almiros βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258627L, name = "Βελεστίνο", regionName = "Μαγνησία, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3842, longitude = 22.7428, elevationMeters = 120.0), "βελεστινο velestino βολος", "thessaly magnesia"),
        Triple(SavedCityEntity(id = 258628L, name = "Μέτσοβο", regionName = "Ιωάννινα, Ήπειρος", countryName = "Ελλάδα", latitude = 39.7697, longitude = 21.1828, elevationMeters = 1160.0), "μετσοβο metsovo ηπειρος", "epirus ioannina"),
        Triple(SavedCityEntity(id = 258629L, name = "Αράχωβα", regionName = "Βοιωτία, Στερεά Ελλάδα", countryName = "Ελλάδα", latitude = 38.4800, longitude = 22.5839, elevationMeters = 970.0), "αραχωβα αραχοβα arachova arahova παρνασσος", "central greece"),
        Triple(SavedCityEntity(id = 258630L, name = "Καλάβρυτα", regionName = "Αχαΐα, Πελοπόννησος", countryName = "Ελλάδα", latitude = 38.0322, longitude = 22.1111, elevationMeters = 750.0), "καλαβρυτα kalavryta kalavrita", "achaia"),
        Triple(SavedCityEntity(id = 258631L, name = "Καρπενήσι", regionName = "Ευρυτανία, Στερεά Ελλάδα", countryName = "Ελλάδα", latitude = 38.9136, longitude = 21.7911, elevationMeters = 960.0), "καρπενησι karpenisi", "central greece"),
        Triple(SavedCityEntity(id = 258632L, name = "Νυμφαίο Φλώρινας", regionName = "Φλώρινα, Δυτική Μακεδονία", countryName = "Ελλάδα", latitude = 40.6458, longitude = 21.4944, elevationMeters = 1350.0), "νυμφαιο nymfaio nimfeon φλωρινα", "macedonia"),
        Triple(SavedCityEntity(id = 258633L, name = "Δημητσάνα", regionName = "Αρκαδία, Πελοπόννησος", countryName = "Ελλάδα", latitude = 37.5950, longitude = 22.0400, elevationMeters = 950.0), "δημητσανα dimitsana αρκαδια", "arcadia"),
        Triple(SavedCityEntity(id = 258634L, name = "Βυτίνα", regionName = "Αρκαδία, Πελοπόννησος", countryName = "Ελλάδα", latitude = 37.6694, longitude = 22.1825, elevationMeters = 1033.0), "βυτινα vytina vitina αρκαδια", "arcadia"),
        Triple(SavedCityEntity(id = 258576L, name = "Λάρισα", regionName = "Θεσσαλία", countryName = "Ελλάδα", latitude = 39.6390, longitude = 22.4191, elevationMeters = 72.0, isPinned = true, cachedTempC = 18.0, cachedHighC = 21.0, cachedLowC = 11.0, cachedWeatherCode = 2), "λαρισα larissa larisa θεσσαλια", "thessaly"),
        Triple(SavedCityEntity(id = 258584L, name = "Τύρναβος", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.7442, longitude = 22.2881, elevationMeters = 90.0), "τυρναβος tyrnavos tirnavos λαρισα", "thessaly"),
        Triple(SavedCityEntity(id = 258585L, name = "Ελασσόνα", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.8947, longitude = 22.1886, elevationMeters = 300.0), "ελασσονα elassona λαρισα", "thessaly"),
        Triple(SavedCityEntity(id = 258586L, name = "Φάρσαλα", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.2947, longitude = 22.3847, elevationMeters = 160.0), "φαρσαλα farsala λαρισα", "thessaly"),
        Triple(SavedCityEntity(id = 258587L, name = "Αγιά Λάρισας", regionName = "Λάρισα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.7192, longitude = 22.7583, elevationMeters = 200.0), "αγια λαρισας agia larissa", "thessaly"),
        Triple(SavedCityEntity(id = 2643710L, name = "Αθήνα", regionName = "Αττική", countryName = "Ελλάδα", latitude = 37.9838, longitude = 23.7275, elevationMeters = 70.0), "αθηνα athens athina", "attica"),
        Triple(SavedCityEntity(id = 734077L, name = "Θεσσαλονίκη", regionName = "Κεντρική Μακεδονία", countryName = "Ελλάδα", latitude = 40.6401, longitude = 22.9444, elevationMeters = 25.0), "θεσσαλονικη thessaloniki salonika", "macedonia"),
        Triple(SavedCityEntity(id = 251833L, name = "Βόλος", regionName = "Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3666, longitude = 22.9425, elevationMeters = 15.0), "βολος volos", "thessaly"),
        Triple(SavedCityEntity(id = 252661L, name = "Τρίκαλα", regionName = "Θεσσαλία", countryName = "Ελλάδα", latitude = 39.5557, longitude = 21.7679, elevationMeters = 115.0), "τρικαλα trikala", "thessaly"),
        Triple(SavedCityEntity(id = 260114L, name = "Καρδίτσα", regionName = "Θεσσαλία", countryName = "Ελλάδα", latitude = 39.3640, longitude = 21.9214, elevationMeters = 108.0), "καρδιτσα karditsa", "thessaly"),
        Triple(SavedCityEntity(id = 258588L, name = "Μουζάκι", regionName = "Καρδίτσα, Θεσσαλία", countryName = "Ελλάδα", latitude = 39.4319, longitude = 21.6636, elevationMeters = 180.0), "μουζακι mouzaki καρδιτσα τρικαλα", "thessaly"),
        Triple(SavedCityEntity(id = 255683L, name = "Πάτρα", regionName = "Δυτική Ελλάδα", countryName = "Ελλάδα", latitude = 38.2466, longitude = 21.7346, elevationMeters = 20.0), "πατρα patra patras", "achaia"),
        Triple(SavedCityEntity(id = 261745L, name = "Ηράκλειο", regionName = "Κρήτη", countryName = "Ελλάδα", latitude = 35.3387, longitude = 25.1442, elevationMeters = 35.0), "ηρακλειο heraklion irakleio", "crete"),
        Triple(SavedCityEntity(id = 261779L, name = "Ιωάννινα", regionName = "Ήπειρος", countryName = "Ελλάδα", latitude = 39.6650, longitude = 20.8537, elevationMeters = 480.0), "ιωαννινα γιαννενα ioannina", "epirus"),
        Triple(SavedCityEntity(id = 260413L, name = "Χανιά", regionName = "Κρήτη", countryName = "Ελλάδα", latitude = 35.5138, longitude = 24.0180, elevationMeters = 20.0), "χανια chania", "crete"),
        Triple(SavedCityEntity(id = 259656L, name = "Κατερίνη", regionName = "Κεντρική Μακεδονία", countryName = "Ελλάδα", latitude = 40.2723, longitude = 22.5024, elevationMeters = 35.0), "κατερινη katerini", "pieria"),
        Triple(SavedCityEntity(id = 259285L, name = "Κοζάνη", regionName = "Δυτική Μακεδονία", countryName = "Ελλάδα", latitude = 40.3006, longitude = 21.7889, elevationMeters = 710.0), "κοζανη kozani", "macedonia"),
        Triple(SavedCityEntity(id = 258700L, name = "Λαμία", regionName = "Στερεά Ελλάδα", countryName = "Ελλάδα", latitude = 38.8995, longitude = 22.4335, elevationMeters = 100.0), "λαμια lamia", "fthiotida"),
        Triple(SavedCityEntity(id = 259820L, name = "Καλαμάτα", regionName = "Πελοπόννησος", countryName = "Ελλάδα", latitude = 37.0389, longitude = 22.1142, elevationMeters = 15.0), "καλαματα kalamata", "messinia"),
        Triple(SavedCityEntity(id = 400666L, name = "Ρόδος", regionName = "Νότιο Αιγαίο", countryName = "Ελλάδα", latitude = 36.4341, longitude = 28.2176, elevationMeters = 20.0), "ροδος rhodes rodos", "aegean"),
        Triple(SavedCityEntity(id = 2463679L, name = "Κέρκυρα", regionName = "Ιόνια Νησιά", countryName = "Ελλάδα", latitude = 39.6243, longitude = 19.9217, elevationMeters = 18.0), "κερκυρα corfu kerkyra", "ionian")
    )

    private fun normalizeGreekSearch(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace('ά', 'α')
            .replace('έ', 'ε')
            .replace('ή', 'η')
            .replace('ί', 'ι')
            .replace('ϊ', 'ι')
            .replace('ΐ', 'ι')
            .replace('ό', 'ο')
            .replace('ύ', 'υ')
            .replace('ϋ', 'υ')
            .replace('ΰ', 'υ')
            .replace('ώ', 'ω')
            .replace('ς', 'σ')
            .replace("καωτ", "κατω")
            .replace("kawt", "kato")
            .replace("kaot", "kato")
            .replace("παλαιοκαρθα", "παλαιοκαρυα")
            .replace("καρθα", "καρυα")
            .replace("palaiokartha", "palaiokarya")
            .replace("paleokartha", "paleokarya")
            .trim()
    }

    /**
     * Converts any Greek, English, or Greeklish string (e.g., "Kato palaiokarua", "Kato Paleokarya",
     * "Κάτω Παλαιοκαρυά") into a unified phonetic Latin key so any spelling variation matches.
     */
    private fun toPhoneticGreeklishKey(input: String): String {
        var s = normalizeGreekSearch(input)
        val digraphs = listOf(
            "ου" to "u",
            "αι" to "e",
            "ει" to "i",
            "οι" to "i",
            "υι" to "i",
            "μπ" to "b",
            "ντ" to "d",
            "γκ" to "g",
            "γγ" to "g",
            "τσ" to "ts",
            "τζ" to "tz",
            "θ" to "th",
            "χ" to "ch",
            "ψ" to "ps",
            "ξ" to "ks"
        )
        for ((gr, lat) in digraphs) {
            s = s.replace(gr, lat)
        }
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                'α' -> sb.append('a')
                'β' -> sb.append('v')
                'γ' -> sb.append('g')
                'δ' -> sb.append('d')
                'ε' -> sb.append('e')
                'ζ' -> sb.append('z')
                'η', 'ι', 'υ', 'y', 'u' -> sb.append('i')
                'κ', 'c', 'q' -> sb.append('k')
                'λ' -> sb.append('l')
                'μ' -> sb.append('m')
                'ν' -> sb.append('n')
                'ο', 'ω', 'w' -> sb.append('o')
                'π' -> sb.append('p')
                'ρ' -> sb.append('r')
                'σ' -> sb.append('s')
                'τ' -> sb.append('t')
                'φ' -> sb.append('f')
                'h', 'x' -> sb.append("ch")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace("ai", "e")
            .replace("ei", "i")
            .replace("oi", "i")
            .replace("ou", "i")
            .replace("ua", "ia")
            .replace("ya", "ia")
            .replace("ch", "k")
            .replace(Regex("(.)\\1+"), "$1")
            .trim()
    }

    /**
     * Transliterates Latin Greeklish queries (including 'u' for 'υ' like "Kato palaiokarua")
     * into Greek script so Open-Meteo and Nominatim find small Greek villages.
     */
    private fun greeklishToGreekText(input: String): String {
        var s = input.lowercase(Locale.ROOT).trim()
        val multi = listOf(
            "kato" to "κάτω",
            "ano" to "άνω",
            "mesi" to "μέση",
            "palaiokarua" to "παλαιοκαρυά",
            "palaiokarya" to "παλαιοκαρυά",
            "paleokarya" to "παλαιοκαρυά",
            "paleokarua" to "παλαιοκαρυά",
            "palaiokaria" to "παλαιοκαρυά",
            "paleokaria" to "παλαιοκαρυά",
            "palaio" to "παλαιο",
            "paleo" to "παλαιο",
            "karua" to "καρυά",
            "karya" to "καρυά",
            "karia" to "καρυά",
            "chori" to "χώρι",
            "xori" to "χώρι",
            "th" to "θ",
            "ch" to "χ",
            "ps" to "ψ",
            "ks" to "ξ",
            "ou" to "ου",
            "ai" to "αι",
            "ei" to "ει",
            "oi" to "οι",
            "mp" to "μπ",
            "nt" to "ντ",
            "gk" to "γκ",
            "ts" to "τσ",
            "tz" to "τζ"
        )
        for ((lat, gr) in multi) {
            s = s.replace(lat, gr)
        }
        val out = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                'a' -> out.append('α')
                'b' -> out.append("μπ")
                'v' -> out.append('β')
                'g' -> out.append('γ')
                'd' -> out.append('δ')
                'e' -> out.append('ε')
                'z' -> out.append('ζ')
                'h' -> out.append('η')
                'i' -> out.append('ι')
                'k', 'c' -> out.append('κ')
                'l' -> out.append('λ')
                'm' -> out.append('μ')
                'n' -> out.append('ν')
                'x' -> out.append('χ')
                'o' -> out.append('ο')
                'p' -> out.append('π')
                'r' -> out.append('ρ')
                's' -> out.append('σ')
                't' -> out.append('τ')
                'y', 'u' -> out.append('υ')
                'f' -> out.append('φ')
                'w' -> out.append('ω')
                else -> out.append(ch)
            }
        }
        return out.toString().replace(Regex("σ(?=\\s|$)"), "ς")
    }

    private val bundleCache = java.util.concurrent.ConcurrentHashMap<String, CityWeatherBundle>()
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes in-memory cache to minimize network & battery usage

    suspend fun searchGlobalCities(query: String): List<SavedCityEntity> = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@coroutineScope emptyList()
        val normalized = normalizeGreekSearch(trimmed)
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val phoneticQuery = toPhoneticGreeklishKey(trimmed)
        val phoneticTokens = phoneticQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val hasGreekChars = trimmed.any { it in '\u0370'..'\u03FF' || it in '\u1F00'..'\u1FFF' }
        val searchLanguage = if (hasGreekChars) "el" else "en"

        val localMatches = localGreekCityCatalog.filter { (entity, keywords, regionKey) ->
            val searchableBlob = "${normalizeGreekSearch(entity.name)} ${normalizeGreekSearch(entity.regionName)} $keywords $regionKey"
            val phoneticBlob = toPhoneticGreeklishKey(searchableBlob)
            searchableBlob.contains(normalized) ||
                (tokens.isNotEmpty() && tokens.all { token -> searchableBlob.contains(token) }) ||
                (phoneticQuery.length >= 3 && phoneticBlob.contains(phoneticQuery)) ||
                (phoneticTokens.isNotEmpty() && phoneticTokens.all { pt -> pt.length >= 2 && phoneticBlob.contains(pt) })
        }.map { it.first }

        // Build query variations so Greeklish (e.g. "Kato palaiokarua") finds villages on Open-Meteo & Nominatim
        val latinNormalizedVariant = trimmed
            .replace(Regex("ua\\b", RegexOption.IGNORE_CASE), "ya")
            .replace(Regex("palaiokarua", RegexOption.IGNORE_CASE), "Palaiokarya")
            .replace(Regex("paleokarua", RegexOption.IGNORE_CASE), "Paleokarya")
        val greekTransliterated = if (!hasGreekChars) greeklishToGreekText(trimmed) else null

        val primaryOpenMeteoDeferred = async {
            runCatching {
                geocodingService.searchCities(
                    query = latinNormalizedVariant,
                    count = 15,
                    language = searchLanguage
                ).results.orEmpty()
            }.getOrDefault(emptyList())
        }

        val greekOpenMeteoDeferred = async {
            if (!greekTransliterated.isNullOrBlank() && greekTransliterated != trimmed) {
                runCatching {
                    geocodingService.searchCities(
                        query = greekTransliterated,
                        count = 12,
                        language = "el"
                    ).results.orEmpty()
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        }

        // OpenStreetMap Nominatim lookup for small villages, hamlets, and towns
        val nominatimDeferred = async {
            runCatching {
                val primaryNom = geoLocationLookupService.searchVillagesAndTowns(
                    query = latinNormalizedVariant,
                    limit = 10,
                    acceptLanguage = "$searchLanguage,el,en"
                )
                if (primaryNom.isNotEmpty() || greekTransliterated.isNullOrBlank()) {
                    primaryNom
                } else {
                    geoLocationLookupService.searchVillagesAndTowns(
                        query = greekTransliterated,
                        limit = 10,
                        acceptLanguage = "el,en"
                    )
                }
            }.getOrDefault(emptyList())
        }

        val remoteOpenMeteo = (primaryOpenMeteoDeferred.await() + greekOpenMeteoDeferred.await())
            .filter {
                !it.name.contains("Poseidon", ignoreCase = true) &&
                    !it.name.contains("Ποσειδ", ignoreCase = true) &&
                    !it.name.equals("Dimos Larisa", ignoreCase = true) &&
                    !it.name.equals("Dimos Larissa", ignoreCase = true)
            }
            .map { dto -> dto.toSavedCityEntity() }

        val remoteNominatim = nominatimDeferred.await()
            .mapNotNull { place -> place.toSavedCityEntity() }
            .filter {
                !it.name.contains("Poseidon", ignoreCase = true) &&
                    !it.name.contains("Ποσειδ", ignoreCase = true) &&
                    !it.name.equals("Dimos Larisa", ignoreCase = true) &&
                    !it.name.equals("Dimos Larissa", ignoreCase = true)
            }

        val combined = mutableListOf<SavedCityEntity>()
        combined.addAll(localMatches)
        for (remote in (remoteOpenMeteo + remoteNominatim)) {
            if (combined.none { kotlin.math.abs(it.latitude - remote.latitude) < 0.015 && kotlin.math.abs(it.longitude - remote.longitude) < 0.015 }) {
                combined.add(remote)
            }
        }
        combined.take(15)
    }

    suspend fun saveCity(city: SavedCityEntity) {
        runCatching { cityDao.upsertCity(city) }
    }

    suspend fun deleteCity(cityId: Long) {
        runCatching { cityDao.deleteCityById(cityId) }
    }

    suspend fun togglePinCity(city: SavedCityEntity) {
        runCatching { cityDao.setCityPinned(city.id, !city.isPinned) }
    }

    suspend fun fetchWeatherForCity(
        city: SavedCityEntity,
        provider: com.example.model.WeatherProvider = com.example.model.WeatherProvider.BEST_MATCH,
        forceRefresh: Boolean = false
    ): CityWeatherBundle = coroutineScope {
        val localDateKey = formatLiveDateIsoInTimezone(city.timezoneId)
        val cacheKey = "${city.id}_${provider.id}_${localDateKey}_${String.format(Locale.US, "%.3f_%.3f", city.latitude, city.longitude)}"
        if (!forceRefresh) {
            val cached = bundleCache[cacheKey]
            if (cached != null && (System.currentTimeMillis() - cached.fetchedAtEpochMs) < cacheTtlMs) {
                return@coroutineScope cached.copy(
                    cityId = city.id,
                    cityName = city.name,
                    regionName = city.regionName,
                    countryName = city.countryName,
                    provider = provider
                )
            }
        }

        val queryElevation = city.elevationMeters.takeIf { it > 0.0 }
        val ecmwfCurrentParams = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
        val ecmwfHourlyParams = "temperature_2m,apparent_temperature,precipitation,rain,showers,snowfall,weather_code,is_day,wind_speed_10m,wind_gusts_10m,wind_direction_10m,cloud_cover,relative_humidity_2m"
        val ecmwfDailyParams = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,snowfall_sum,wind_speed_10m_max"
        val minutely15NowcastParams = "precipitation,rain,snowfall,weather_code"

        val forecastDeferred = async {
            val isSpecificModel = provider != com.example.model.WeatherProvider.BEST_MATCH && provider.modelQuery != null
            val primary = runCatching {
                if (isSpecificModel) {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        current = ecmwfCurrentParams,
                        minutely15 = null,
                        hourly = ecmwfHourlyParams,
                        daily = ecmwfDailyParams,
                        models = provider.modelQuery,
                        cellSelection = "nearest"
                    )
                } else {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        minutely15 = minutely15NowcastParams,
                        models = null,
                        cellSelection = "nearest"
                    )
                }
            }.getOrNull()
            if (primary?.current != null && primary.daily != null) {
                primary
            } else {
                runCatching {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        minutely15 = null,
                        models = null,
                        cellSelection = "nearest"
                    )
                }.getOrNull()
            }
        }
        val secondaryEcmwfDeferred = async {
            if (provider == com.example.model.WeatherProvider.BEST_MATCH) {
                runCatching {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        current = ecmwfCurrentParams,
                        minutely15 = null,
                        hourly = ecmwfHourlyParams,
                        daily = ecmwfDailyParams,
                        models = "ecmwf_ifs025",
                        cellSelection = "nearest"
                    )
                }.getOrNull()
            } else {
                runCatching {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        minutely15 = minutely15NowcastParams,
                        models = null,
                        cellSelection = "nearest"
                    )
                }.getOrNull()
            }
        }
        val tertiaryIconDeferred = async {
            if (provider == com.example.model.WeatherProvider.BEST_MATCH) {
                runCatching {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        current = ecmwfCurrentParams,
                        minutely15 = null,
                        hourly = ecmwfHourlyParams,
                        daily = ecmwfDailyParams,
                        models = "icon_seamless",
                        cellSelection = "nearest"
                    )
                }.getOrNull()
            } else {
                null
            }
        }
        val quaternaryGfsDeferred = async {
            if (provider == com.example.model.WeatherProvider.BEST_MATCH) {
                runCatching {
                    forecastService.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        elevation = queryElevation,
                        current = ecmwfCurrentParams,
                        minutely15 = null,
                        hourly = ecmwfHourlyParams,
                        daily = ecmwfDailyParams,
                        models = "gfs_seamless",
                        cellSelection = "nearest"
                    )
                }.getOrNull()
            } else {
                null
            }
        }
        val aqiDeferred = async {
            runCatching {
                airQualityService.getAirQuality(
                    latitude = city.latitude,
                    longitude = city.longitude
                )
            }.getOrNull()
        }

        val forecastResponse = forecastDeferred.await()
        val secondaryResponse = secondaryEcmwfDeferred.await()
        val tertiaryResponse = tertiaryIconDeferred.await()
        val quaternaryResponse = quaternaryGfsDeferred.await()
        val aqiResponse = aqiDeferred.await()

        val airQuality = if (aqiResponse?.current != null) {
            val c = aqiResponse.current
            AirQualityTelemetry(
                usAqi = (c.usAqi ?: city.cachedAqi).coerceAtLeast(5),
                europeanAqi = (c.europeanAqi ?: 20).coerceAtLeast(5),
                pm25 = c.pm25 ?: 5.8,
                pm10 = c.pm10 ?: 13.4,
                ozone = c.ozone ?: 54.0,
                nitrogenDioxide = c.nitrogenDioxide ?: 8.2,
                carbonMonoxide = c.carbonMonoxide ?: 175.0
            )
        } else {
            AirQualityTelemetry(
                usAqi = city.cachedAqi,
                europeanAqi = (city.cachedAqi * 0.8).roundToInt(),
                pm25 = 6.2,
                pm10 = 14.0,
                ozone = 56.0,
                nitrogenDioxide = 9.1,
                carbonMonoxide = 180.0
            )
        }

        val activePrimary = forecastResponse?.takeIf { it.current != null && it.daily != null }
            ?: secondaryResponse?.takeIf { it.current != null && it.daily != null }
            ?: tertiaryResponse?.takeIf { it.current != null && it.daily != null }
            ?: quaternaryResponse?.takeIf { it.current != null && it.daily != null }

        val bundle = if (activePrimary != null) {
            mapLiveResponseToBundle(
                city = city,
                response = activePrimary,
                secondaryResponse = secondaryResponse,
                tertiaryResponse = tertiaryResponse,
                quaternaryResponse = quaternaryResponse,
                airQuality = airQuality,
                provider = provider
            )
        } else {
            buildResilientFallbackBundle(city, airQuality, provider)
        }

        bundleCache[cacheKey] = bundle

        // Update Room cache with latest live values
        val updatedEntity = city.copy(
            cachedTempC = bundle.current.tempC,
            cachedHighC = bundle.current.highC,
            cachedLowC = bundle.current.lowC,
            cachedWeatherCode = bundle.current.weatherCode,
            cachedIsDay = bundle.current.isDay,
            cachedAqi = bundle.airQuality.usAqi,
            timezoneId = bundle.timezoneId,
            elevationMeters = bundle.elevationMeters,
            lastUpdatedEpochMs = System.currentTimeMillis()
        )
        runCatching { cityDao.upsertCity(updatedEntity) }

        bundle
    }

    private fun resolveBestSnowAndWeatherCode(
        primaryCode: Int,
        secondaryCode: Int?,
        tertiaryCode: Int? = null,
        quaternaryCode: Int? = null,
        tempC: Double,
        precipMm: Double,
        snowfallCm: Double,
        precipProb: Int,
        windSpeedKmh: Double,
        cloudCover: Int,
        freezingLevelM: Double?,
        elevationM: Double
    ): Int {
        val snowCodes = setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)
        val stormSnowCodes = setOf(57, 67, 75, 86)
        val isBelowFreezingLevel = freezingLevelM != null && elevationM > 0.0 && freezingLevelM <= (elevationM + 140.0)
        val isSnowCondition = primaryCode in snowCodes ||
            (secondaryCode != null && secondaryCode in snowCodes) ||
            (tertiaryCode != null && tertiaryCode in snowCodes) ||
            (quaternaryCode != null && quaternaryCode in snowCodes) ||
            snowfallCm >= 0.04 ||
            (precipMm >= 0.15 && (tempC <= 1.8 || isBelowFreezingLevel))

        if (isSnowCondition) {
            val effectiveSnowCm = maxOf(
                snowfallCm,
                if (tempC <= 1.8 || isBelowFreezingLevel) precipMm * 0.75 else 0.0
            )
            return when {
                primaryCode in stormSnowCodes ||
                    (secondaryCode != null && secondaryCode in stormSnowCodes) ||
                    (tertiaryCode != null && tertiaryCode in stormSnowCodes) ||
                    (quaternaryCode != null && quaternaryCode in stormSnowCodes) ||
                    effectiveSnowCm >= 1.1 ||
                    (effectiveSnowCm >= 0.40 && windSpeedKmh >= 26.0) -> {
                    if (primaryCode == 86 || secondaryCode == 86 || tertiaryCode == 86 || quaternaryCode == 86) 86 else 75
                }
                effectiveSnowCm >= 0.22 || primaryCode == 73 || secondaryCode == 73 || tertiaryCode == 73 || quaternaryCode == 73 -> 73
                primaryCode in snowCodes -> primaryCode
                secondaryCode != null && secondaryCode in snowCodes -> secondaryCode
                tertiaryCode != null && tertiaryCode in snowCodes -> tertiaryCode
                quaternaryCode != null && quaternaryCode in snowCodes -> quaternaryCode
                else -> 71
            }
        }

        // Preserve thunderstorm codes from any ensemble model
        if (primaryCode in 95..99) return primaryCode
        if (secondaryCode != null && secondaryCode in 95..99 && precipProb >= 35) return secondaryCode
        if (tertiaryCode != null && tertiaryCode in 95..99 && precipProb >= 35) return tertiaryCode
        if (quaternaryCode != null && quaternaryCode in 95..99 && precipProb >= 35) return quaternaryCode

        var resolved = primaryCode
        if ((resolved in 51..65 || resolved in 80..82) && precipMm < 0.08 && precipProb < 35) {
            if (secondaryCode != null && (secondaryCode in 51..67 || secondaryCode in 80..82) && precipMm >= 0.05) {
                return secondaryCode
            }
            if (tertiaryCode != null && (tertiaryCode in 51..67 || tertiaryCode in 80..82) && precipMm >= 0.05) {
                return tertiaryCode
            }
            if (quaternaryCode != null && (quaternaryCode in 51..67 || quaternaryCode in 80..82) && precipMm >= 0.05) {
                return quaternaryCode
            }
            resolved = when {
                cloudCover >= 75 -> 3
                cloudCover >= 25 -> 2
                else -> 1
            }
        } else if (resolved < 51 && precipMm >= 0.30) {
            resolved = if (tempC <= 1.8) {
                if (precipMm >= 1.8) 73 else 71
            } else {
                if (precipMm >= 2.5) 63 else 61
            }
        }
        return resolved
    }

    /**
     * 4-Model Inverse-Spread Ensemble Consensus Fusion:
     * Combines HR-LAM Best-Match (weight 0.42), ECMWF IFS 0.25° (weight 0.28),
     * DWD ICON Seamless (weight 0.18), and NOAA GFS Seamless (weight 0.12),
     * down-weighting any model that deviates from the ensemble median by > 2.5 units.
     */
    private fun blendMetric(
        primary: Double?,
        secondary: Double?,
        defaultVal: Double,
        useBlend: Boolean,
        tertiary: Double? = null,
        quaternary: Double? = null
    ): Double {
        val p = primary?.takeIf { it.isFinite() }
        val s = secondary?.takeIf { it.isFinite() }
        val t = tertiary?.takeIf { it.isFinite() }
        val q = quaternary?.takeIf { it.isFinite() }
        if (!useBlend) {
            return p ?: s ?: t ?: q ?: defaultVal
        }
        val samples = mutableListOf<Pair<Double, Double>>()
        if (p != null) samples.add(p to 0.42)
        if (s != null) samples.add(s to 0.28)
        if (t != null) samples.add(t to 0.18)
        if (q != null) samples.add(q to 0.12)
        if (samples.isEmpty()) return defaultVal
        if (samples.size == 1) return samples.first().first

        val sortedVals = samples.map { it.first }.sorted()
        val median = if (sortedVals.size % 2 == 1) {
            sortedVals[sortedVals.size / 2]
        } else {
            (sortedVals[sortedVals.size / 2 - 1] + sortedVals[sortedVals.size / 2]) / 2.0
        }

        var weightedSum = 0.0
        var totalWeight = 0.0
        for ((value, baseW) in samples) {
            val deviation = abs(value - median)
            // Inverse-variance outlier dampening when a model diverges from ensemble median
            val spreadDampening = if (deviation > 3.0) 1.0 / (1.0 + (deviation - 3.0) * 0.65) else 1.0
            val w = baseW * spreadDampening
            weightedSum += value * w
            totalWeight += w
        }
        return if (totalWeight > 0.0) weightedSum / totalWeight else median
    }

    private fun mapLiveResponseToBundle(
        city: SavedCityEntity,
        response: OpenMeteoForecastResponse,
        secondaryResponse: OpenMeteoForecastResponse?,
        tertiaryResponse: OpenMeteoForecastResponse? = null,
        quaternaryResponse: OpenMeteoForecastResponse? = null,
        airQuality: AirQualityTelemetry,
        provider: com.example.model.WeatherProvider
    ): CityWeatherBundle {
        val currentDto = response.current!!
        val dailyDto = response.daily!!
        val hourlyDto = response.hourly
        val minutely15Dto = response.minutely15 ?: secondaryResponse?.minutely15 ?: tertiaryResponse?.minutely15
        val secondaryCurrent = secondaryResponse?.current
        val secondaryHourly = secondaryResponse?.hourly
        val secondaryDaily = secondaryResponse?.daily
        val tertiaryCurrent = tertiaryResponse?.current
        val tertiaryHourly = tertiaryResponse?.hourly
        val tertiaryDaily = tertiaryResponse?.daily
        val quaternaryCurrent = quaternaryResponse?.current
        val quaternaryHourly = quaternaryResponse?.hourly
        val quaternaryDaily = quaternaryResponse?.daily
        val useEnsembleBlend = provider == com.example.model.WeatherProvider.BEST_MATCH &&
            (secondaryResponse != null || tertiaryResponse != null || quaternaryResponse != null)

        val effectiveElevation = (
            city.elevationMeters.takeIf { it >= 50.0 }
                ?: response.elevation?.takeIf { it > 0.0 }
                ?: city.elevationMeters
            ).coerceAtLeast(5.0)
        // Microclimate adiabatic lapse-rate correction (-0.0065 °C/m) if coarse model grid cell elevation differs from village elevation
        val modelGridElev = response.elevation?.takeIf { it > 0.0 } ?: effectiveElevation
        val elevationDeltaM = (effectiveElevation - modelGridElev).coerceIn(-650.0, 650.0)
        val lapseRateCorrectionC = if (abs(elevationDeltaM) >= 65.0) -0.0065 * elevationDeltaM else 0.0

        val currentTempC = blendMetric(
            currentDto.temperature2m,
            secondaryCurrent?.temperature2m,
            city.cachedTempC,
            useEnsembleBlend,
            tertiaryCurrent?.temperature2m,
            quaternaryCurrent?.temperature2m
        ) + lapseRateCorrectionC
        val currentFeelsC = blendMetric(
            currentDto.apparentTemperature,
            secondaryCurrent?.apparentTemperature,
            currentTempC,
            useEnsembleBlend,
            tertiaryCurrent?.apparentTemperature,
            quaternaryCurrent?.apparentTemperature
        ) + lapseRateCorrectionC
        val currentHumidity = if (useEnsembleBlend) {
            val hValues = listOfNotNull(
                currentDto.relativeHumidity2m,
                secondaryCurrent?.relativeHumidity2m,
                tertiaryCurrent?.relativeHumidity2m,
                quaternaryCurrent?.relativeHumidity2m
            )
            if (hValues.isNotEmpty()) hValues.average().roundToInt().coerceIn(5, 100) else 58
        } else {
            (currentDto.relativeHumidity2m ?: secondaryCurrent?.relativeHumidity2m ?: 58).coerceIn(5, 100)
        }
        val currentWindSpeed = blendMetric(
            currentDto.windSpeed10m,
            secondaryCurrent?.windSpeed10m,
            10.0,
            useEnsembleBlend,
            tertiaryCurrent?.windSpeed10m,
            quaternaryCurrent?.windSpeed10m
        )
        val currentWindGusts = maxOf(
            currentDto.windGusts10m ?: (currentWindSpeed * 1.35),
            if (useEnsembleBlend) (secondaryCurrent?.windGusts10m ?: 0.0) else 0.0,
            if (useEnsembleBlend) (tertiaryCurrent?.windGusts10m ?: 0.0) else 0.0,
            if (useEnsembleBlend) (quaternaryCurrent?.windGusts10m ?: 0.0) else 0.0
        )
        val currentWindDir = currentDto.windDirection10m
            ?: secondaryCurrent?.windDirection10m
            ?: tertiaryCurrent?.windDirection10m
            ?: quaternaryCurrent?.windDirection10m
            ?: 180
        val currentCloudCover = if (useEnsembleBlend) {
            val cValues = listOfNotNull(
                currentDto.cloudCover,
                secondaryCurrent?.cloudCover,
                tertiaryCurrent?.cloudCover,
                quaternaryCurrent?.cloudCover
            )
            if (cValues.isNotEmpty()) cValues.average().roundToInt().coerceIn(0, 100) else 20
        } else {
            (currentDto.cloudCover ?: secondaryCurrent?.cloudCover ?: 20).coerceIn(0, 100)
        }
        val currentPressure = blendMetric(
            currentDto.pressureMsl?.takeIf { it > 0.0 } ?: currentDto.surfacePressure?.takeIf { it > 0.0 },
            secondaryCurrent?.pressureMsl?.takeIf { it > 0.0 },
            1013.25,
            useEnsembleBlend,
            tertiaryCurrent?.pressureMsl?.takeIf { it > 0.0 },
            quaternaryCurrent?.pressureMsl?.takeIf { it > 0.0 }
        )

        val rawTodayHigh = blendMetric(
            dailyDto.temperature2mMax?.firstOrNull(),
            secondaryDaily?.temperature2mMax?.firstOrNull(),
            currentTempC + 3.0,
            useEnsembleBlend,
            tertiaryDaily?.temperature2mMax?.firstOrNull(),
            quaternaryDaily?.temperature2mMax?.firstOrNull()
        ) + lapseRateCorrectionC
        val rawTodayLow = blendMetric(
            dailyDto.temperature2mMin?.firstOrNull(),
            secondaryDaily?.temperature2mMin?.firstOrNull(),
            currentTempC - 3.0,
            useEnsembleBlend,
            tertiaryDaily?.temperature2mMin?.firstOrNull(),
            quaternaryDaily?.temperature2mMin?.firstOrNull()
        ) + lapseRateCorrectionC
        val todayHigh = maxOf(rawTodayHigh, currentTempC)
        val todayLow = minOf(rawTodayLow, currentTempC)

        val effectiveTimezone = resolveEffectiveTimezoneId(response.timezone?.takeIf { it.isNotBlank() } ?: city.timezoneId)
        val liveClockLabel = formatLiveClockInTimezone(effectiveTimezone)
        val liveDateIso = formatLiveDateIsoInTimezone(effectiveTimezone)

        val todaySunriseClock = extractClockTime(dailyDto.sunrise?.firstOrNull().orEmpty(), "06:30")
        val todaySunsetClock = extractClockTime(dailyDto.sunset?.firstOrNull().orEmpty(), "18:00")

        val tz = TimeZone.getTimeZone(effectiveTimezone)
        val nowCalendar = Calendar.getInstance(tz)
        val currentMinuteOfHour = nowCalendar.get(Calendar.MINUTE).coerceIn(0, 59)
        val quarterMinute = (currentMinuteOfHour / 15) * 15
        val liveHourPrefix = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).apply { timeZone = tz }
            .format(nowCalendar.time)
        val liveQuarterPrefix = String.format(Locale.US, "%s:%02d", liveHourPrefix, quarterMinute)
        val currentTimeStr = currentDto.time.orEmpty()

        val hourlyTimes = hourlyDto?.time.orEmpty()
        val startIndex = hourlyTimes.indexOfFirst { it.startsWith(liveHourPrefix) }
            .let { found ->
                if (found != -1) found
                else hourlyTimes.indexOfFirst { currentTimeStr.length >= 13 && it.startsWith(currentTimeStr.take(13)) }.let { if (it == -1) 0 else it }
            }

        val resolvedCurrentIsDay = (currentDto.isDay ?: 1) == 1

        // Assimilate exact 15-minute high-resolution nowcast slice (:00, :15, :30, :45)
        val minutelyTimes = minutely15Dto?.time.orEmpty()
        val minutelyIndex = minutelyTimes.indexOfFirst { it.startsWith(liveQuarterPrefix) }
            .takeIf { it >= 0 }
            ?: minutelyTimes.indexOfFirst { it.startsWith(liveHourPrefix) }.takeIf { it >= 0 }
            ?: 0
        val nowcastPrecipMm = minutely15Dto?.precipitation?.getOrNull(minutelyIndex) ?: 0.0
        val nowcastSnowCm = minutely15Dto?.snowfall?.getOrNull(minutelyIndex) ?: 0.0
        val nowcastCode = minutely15Dto?.weatherCode?.getOrNull(minutelyIndex)

        val currentSnowfallCm = maxOf(
            currentDto.snowfall ?: 0.0,
            nowcastSnowCm,
            if (useEnsembleBlend) (secondaryCurrent?.snowfall ?: 0.0) else 0.0,
            if (useEnsembleBlend) (tertiaryCurrent?.snowfall ?: 0.0) else 0.0,
            if (useEnsembleBlend) (quaternaryCurrent?.snowfall ?: 0.0) else 0.0,
            hourlyDto?.snowfall?.getOrNull(startIndex) ?: 0.0,
            if (useEnsembleBlend) (secondaryHourly?.snowfall?.getOrNull(startIndex) ?: 0.0) else 0.0
        )
        val fusedCurrentPrecipMm = maxOf(
            currentDto.precipitation ?: 0.0,
            currentDto.rain ?: 0.0,
            currentDto.showers ?: 0.0,
            nowcastPrecipMm,
            if (useEnsembleBlend) (secondaryCurrent?.precipitation ?: 0.0) else 0.0,
            if (useEnsembleBlend) (tertiaryCurrent?.precipitation ?: 0.0) else 0.0,
            if (useEnsembleBlend) (quaternaryCurrent?.precipitation ?: 0.0) else 0.0,
            currentSnowfallCm * 1.0
        )

        val rawCurrentProb = (hourlyDto?.precipitationProbability?.getOrNull(startIndex)
            ?: secondaryHourly?.precipitationProbability?.getOrNull(startIndex)
            ?: tertiaryHourly?.precipitationProbability?.getOrNull(startIndex)
            ?: quaternaryHourly?.precipitationProbability?.getOrNull(startIndex)
            ?: 0).coerceIn(0, 100)
        val currentFreezingLevelM = hourlyDto?.freezingLevelHeight?.getOrNull(startIndex)
            ?: secondaryHourly?.freezingLevelHeight?.getOrNull(startIndex)
            ?: tertiaryHourly?.freezingLevelHeight?.getOrNull(startIndex)
            ?: quaternaryHourly?.freezingLevelHeight?.getOrNull(startIndex)

        val fusedCurrentCode = resolveBestSnowAndWeatherCode(
            primaryCode = currentDto.weatherCode ?: nowcastCode ?: 0,
            secondaryCode = if (useEnsembleBlend) (secondaryCurrent?.weatherCode ?: nowcastCode) else nowcastCode,
            tertiaryCode = if (useEnsembleBlend) tertiaryCurrent?.weatherCode else null,
            quaternaryCode = if (useEnsembleBlend) quaternaryCurrent?.weatherCode else null,
            tempC = currentTempC,
            precipMm = fusedCurrentPrecipMm,
            snowfallCm = currentSnowfallCm,
            precipProb = rawCurrentProb,
            windSpeedKmh = currentWindSpeed,
            cloudCover = currentCloudCover,
            freezingLevelM = currentFreezingLevelM,
            elevationM = effectiveElevation
        )
        val fusedCurrentProb = if (fusedCurrentCode in setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)) {
            maxOf(rawCurrentProb, if (fusedCurrentCode in setOf(75, 86)) 82 else 65)
        } else {
            rawCurrentProb
        }

        val hourlyList = mutableListOf<HourlyForecastItem>()
        val count = minOf(24, (hourlyTimes.size - startIndex).coerceAtLeast(0))
        for (i in 0 until count) {
            val idx = startIndex + i
            val rawTime = hourlyTimes.getOrNull(idx).orEmpty()
            val rawHourClock = formatIsoHourLabel(rawTime)
            val hourLabel = if (i == 0) "Now" else rawHourClock
            val apiIsDay = (hourlyDto?.isDay?.getOrNull(idx) ?: currentDto.isDay ?: 1) == 1
            val resolvedIsDay = if (i == 0) resolvedCurrentIsDay else apiIsDay

            val hTemp = if (i == 0) {
                currentTempC
            } else {
                blendMetric(
                    hourlyDto?.temperature2m?.getOrNull(idx),
                    secondaryHourly?.temperature2m?.getOrNull(idx),
                    currentTempC,
                    useEnsembleBlend,
                    tertiaryHourly?.temperature2m?.getOrNull(idx),
                    quaternaryHourly?.temperature2m?.getOrNull(idx)
                ) + lapseRateCorrectionC
            }
            val hFeels = if (i == 0) {
                currentFeelsC
            } else {
                blendMetric(
                    hourlyDto?.apparentTemperature?.getOrNull(idx),
                    secondaryHourly?.apparentTemperature?.getOrNull(idx),
                    hTemp,
                    useEnsembleBlend,
                    tertiaryHourly?.apparentTemperature?.getOrNull(idx),
                    quaternaryHourly?.apparentTemperature?.getOrNull(idx)
                ) + lapseRateCorrectionC
            }

            val hSnowCm = maxOf(
                hourlyDto?.snowfall?.getOrNull(idx) ?: 0.0,
                if (useEnsembleBlend) (secondaryHourly?.snowfall?.getOrNull(idx) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (tertiaryHourly?.snowfall?.getOrNull(idx) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (quaternaryHourly?.snowfall?.getOrNull(idx) ?: 0.0) else 0.0
            )
            val primaryHProb = (hourlyDto?.precipitationProbability?.getOrNull(idx)
                ?: secondaryHourly?.precipitationProbability?.getOrNull(idx)
                ?: tertiaryHourly?.precipitationProbability?.getOrNull(idx)
                ?: quaternaryHourly?.precipitationProbability?.getOrNull(idx)
                ?: 0).coerceIn(0, 100)
            val primaryHPrecip = maxOf(
                hourlyDto?.precipitation?.getOrNull(idx) ?: 0.0,
                hourlyDto?.rain?.getOrNull(idx) ?: 0.0,
                hourlyDto?.showers?.getOrNull(idx) ?: 0.0,
                if (useEnsembleBlend) (secondaryHourly?.precipitation?.getOrNull(idx) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (tertiaryHourly?.precipitation?.getOrNull(idx) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (quaternaryHourly?.precipitation?.getOrNull(idx) ?: 0.0) else 0.0,
                hSnowCm * 1.0
            )

            val hWindSpeed = if (i == 0) {
                currentWindSpeed
            } else {
                blendMetric(
                    hourlyDto?.windSpeed10m?.getOrNull(idx),
                    secondaryHourly?.windSpeed10m?.getOrNull(idx),
                    currentWindSpeed,
                    useEnsembleBlend,
                    tertiaryHourly?.windSpeed10m?.getOrNull(idx),
                    quaternaryHourly?.windSpeed10m?.getOrNull(idx)
                )
            }
            val hWindGusts = if (i == 0) {
                currentWindGusts
            } else {
                maxOf(
                    hourlyDto?.windGusts10m?.getOrNull(idx) ?: (hWindSpeed * 1.35),
                    if (useEnsembleBlend) (secondaryHourly?.windGusts10m?.getOrNull(idx) ?: 0.0) else 0.0,
                    if (useEnsembleBlend) (tertiaryHourly?.windGusts10m?.getOrNull(idx) ?: 0.0) else 0.0,
                    if (useEnsembleBlend) (quaternaryHourly?.windGusts10m?.getOrNull(idx) ?: 0.0) else 0.0
                )
            }
            val hWindDir = if (i == 0) currentWindDir else (hourlyDto?.windDirection10m?.getOrNull(idx) ?: currentWindDir)
            val hCloud = if (i == 0) {
                currentCloudCover
            } else {
                (hourlyDto?.cloudCover?.getOrNull(idx) ?: secondaryHourly?.cloudCover?.getOrNull(idx) ?: currentCloudCover).coerceIn(0, 100)
            }
            val hFreezingLevel = hourlyDto?.freezingLevelHeight?.getOrNull(idx)
                ?: secondaryHourly?.freezingLevelHeight?.getOrNull(idx)
                ?: tertiaryHourly?.freezingLevelHeight?.getOrNull(idx)
                ?: quaternaryHourly?.freezingLevelHeight?.getOrNull(idx)

            val hPrecipMm = if (i == 0) fusedCurrentPrecipMm else primaryHPrecip
            val hCode = if (i == 0) {
                fusedCurrentCode
            } else {
                resolveBestSnowAndWeatherCode(
                    primaryCode = hourlyDto?.weatherCode?.getOrNull(idx) ?: fusedCurrentCode,
                    secondaryCode = if (useEnsembleBlend) secondaryHourly?.weatherCode?.getOrNull(idx) else null,
                    tertiaryCode = if (useEnsembleBlend) tertiaryHourly?.weatherCode?.getOrNull(idx) else null,
                    quaternaryCode = if (useEnsembleBlend) quaternaryHourly?.weatherCode?.getOrNull(idx) else null,
                    tempC = hTemp,
                    precipMm = hPrecipMm,
                    snowfallCm = hSnowCm,
                    precipProb = primaryHProb,
                    windSpeedKmh = hWindSpeed,
                    cloudCover = hCloud,
                    freezingLevelM = hFreezingLevel,
                    elevationM = effectiveElevation
                )
            }
            val hProb = if (i == 0) {
                fusedCurrentProb
            } else if (hCode in setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)) {
                maxOf(primaryHProb, if (hCode in setOf(75, 86)) 82 else 65)
            } else {
                primaryHProb
            }

            hourlyList.add(
                HourlyForecastItem(
                    isoTime = rawTime,
                    displayHour = hourLabel,
                    tempC = hTemp,
                    feelsLikeC = hFeels,
                    precipProbability = hProb,
                    precipMm = hPrecipMm,
                    weatherCode = hCode,
                    isDay = resolvedIsDay,
                    windSpeedKmh = hWindSpeed,
                    uvIndex = hourlyDto?.uvIndex?.getOrNull(idx)
                        ?: secondaryHourly?.uvIndex?.getOrNull(idx)
                        ?: (currentDto.uvIndex ?: (if (resolvedIsDay) 3.0 else 0.0)),
                    humidity = if (i == 0) currentHumidity else (hourlyDto?.relativeHumidity2m?.getOrNull(idx) ?: currentHumidity),
                    windGustsKmh = hWindGusts,
                    windDirectionDeg = hWindDir,
                    cloudCoverPercent = hCloud
                )
            )
        }

        val dailyList = mutableListOf<DailyForecastItem>()
        val dailyDates = dailyDto.time.orEmpty()
        for (i in dailyDates.indices) {
            val dateStr = dailyDates[i]
            val sunriseRaw = dailyDto.sunrise?.getOrNull(i) ?: secondaryDaily?.sunrise?.getOrNull(i) ?: ""
            val sunsetRaw = dailyDto.sunset?.getOrNull(i) ?: secondaryDaily?.sunset?.getOrNull(i) ?: ""
            val daylightSecs = dailyDto.daylightDuration?.getOrNull(i)
                ?: secondaryDaily?.daylightDuration?.getOrNull(i)
                ?: 41400.0
            val dMax = if (i == 0) {
                todayHigh
            } else {
                blendMetric(
                    dailyDto.temperature2mMax?.getOrNull(i),
                    secondaryDaily?.temperature2mMax?.getOrNull(i),
                    todayHigh,
                    useEnsembleBlend,
                    tertiaryDaily?.temperature2mMax?.getOrNull(i),
                    quaternaryDaily?.temperature2mMax?.getOrNull(i)
                ) + lapseRateCorrectionC
            }
            val dMin = if (i == 0) {
                todayLow
            } else {
                blendMetric(
                    dailyDto.temperature2mMin?.getOrNull(i),
                    secondaryDaily?.temperature2mMin?.getOrNull(i),
                    todayLow,
                    useEnsembleBlend,
                    tertiaryDaily?.temperature2mMin?.getOrNull(i),
                    quaternaryDaily?.temperature2mMin?.getOrNull(i)
                ) + lapseRateCorrectionC
            }
            val rawDCode = dailyDto.weatherCode?.getOrNull(i) ?: fusedCurrentCode
            val secondaryDCode = if (useEnsembleBlend) secondaryDaily?.weatherCode?.getOrNull(i) else null
            val tertiaryDCode = if (useEnsembleBlend) tertiaryDaily?.weatherCode?.getOrNull(i) else null
            val quaternaryDCode = if (useEnsembleBlend) quaternaryDaily?.weatherCode?.getOrNull(i) else null
            val rawDProb = (dailyDto.precipitationProbabilityMax?.getOrNull(i)
                ?: secondaryDaily?.precipitationProbabilityMax?.getOrNull(i)
                ?: tertiaryDaily?.precipitationProbabilityMax?.getOrNull(i)
                ?: quaternaryDaily?.precipitationProbabilityMax?.getOrNull(i)
                ?: 0).coerceIn(0, 100)
            val rawDSumMm = maxOf(
                dailyDto.precipitationSum?.getOrNull(i) ?: 0.0,
                if (useEnsembleBlend) (secondaryDaily?.precipitationSum?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (tertiaryDaily?.precipitationSum?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (quaternaryDaily?.precipitationSum?.getOrNull(i) ?: 0.0) else 0.0
            )
            val dailySnowSumCm = maxOf(
                dailyDto.snowfallSum?.getOrNull(i) ?: 0.0,
                if (useEnsembleBlend) (secondaryDaily?.snowfallSum?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (tertiaryDaily?.snowfallSum?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (quaternaryDaily?.snowfallSum?.getOrNull(i) ?: 0.0) else 0.0
            )
            val dWindMax = maxOf(
                dailyDto.windMaxKmh(i),
                if (useEnsembleBlend) (secondaryDaily?.windSpeed10mMax?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (tertiaryDaily?.windSpeed10mMax?.getOrNull(i) ?: 0.0) else 0.0,
                if (useEnsembleBlend) (quaternaryDaily?.windSpeed10mMax?.getOrNull(i) ?: 0.0) else 0.0
            )
            val dUvMax = dailyDto.uvIndexMax?.getOrNull(i)
                ?: secondaryDaily?.uvIndexMax?.getOrNull(i)
                ?: 4.0

            val dayHourly = mutableListOf<HourlyForecastItem>()
            var dayHourlySnowHours = 0
            var dayHourlyStormSnowHours = 0
            var dayHourlyMaxProb = rawDProb
            var dayHourlyTotalSnowCm = 0.0

            for (hIdx in hourlyTimes.indices) {
                val rawTime = hourlyTimes[hIdx]
                if (rawTime.startsWith(dateStr)) {
                    val rawHourClock = formatIsoHourLabel(rawTime)
                    val isCurrentLiveHour = (i == 0 && hIdx == startIndex)
                    val displayLabel = if (isCurrentLiveHour) "Now" else rawHourClock
                    val apiIsDay = (hourlyDto?.isDay?.getOrNull(hIdx) ?: 1) == 1
                    val hTemp = blendMetric(
                        hourlyDto?.temperature2m?.getOrNull(hIdx),
                        secondaryHourly?.temperature2m?.getOrNull(hIdx),
                        (dMax + dMin) / 2.0,
                        useEnsembleBlend,
                        tertiaryHourly?.temperature2m?.getOrNull(hIdx),
                        quaternaryHourly?.temperature2m?.getOrNull(hIdx)
                    ) + lapseRateCorrectionC
                    val hFeels = blendMetric(
                        hourlyDto?.apparentTemperature?.getOrNull(hIdx),
                        secondaryHourly?.apparentTemperature?.getOrNull(hIdx),
                        hTemp,
                        useEnsembleBlend,
                        tertiaryHourly?.apparentTemperature?.getOrNull(hIdx),
                        quaternaryHourly?.apparentTemperature?.getOrNull(hIdx)
                    ) + lapseRateCorrectionC
                    val hSnowCm = maxOf(
                        hourlyDto?.snowfall?.getOrNull(hIdx) ?: 0.0,
                        if (useEnsembleBlend) (secondaryHourly?.snowfall?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (tertiaryHourly?.snowfall?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (quaternaryHourly?.snowfall?.getOrNull(hIdx) ?: 0.0) else 0.0
                    )
                    dayHourlyTotalSnowCm += hSnowCm
                    val rawHProb = (hourlyDto?.precipitationProbability?.getOrNull(hIdx)
                        ?: secondaryHourly?.precipitationProbability?.getOrNull(hIdx)
                        ?: tertiaryHourly?.precipitationProbability?.getOrNull(hIdx)
                        ?: quaternaryHourly?.precipitationProbability?.getOrNull(hIdx)
                        ?: rawDProb).coerceIn(0, 100)
                    val hPrecipMm = maxOf(
                        hourlyDto?.precipitation?.getOrNull(hIdx) ?: 0.0,
                        hourlyDto?.rain?.getOrNull(hIdx) ?: 0.0,
                        hourlyDto?.showers?.getOrNull(hIdx) ?: 0.0,
                        if (useEnsembleBlend) (secondaryHourly?.precipitation?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (tertiaryHourly?.precipitation?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (quaternaryHourly?.precipitation?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        hSnowCm * 1.0
                    )
                    val hWindSpeed = blendMetric(
                        hourlyDto?.windSpeed10m?.getOrNull(hIdx),
                        secondaryHourly?.windSpeed10m?.getOrNull(hIdx),
                        dWindMax * 0.72,
                        useEnsembleBlend,
                        tertiaryHourly?.windSpeed10m?.getOrNull(hIdx),
                        quaternaryHourly?.windSpeed10m?.getOrNull(hIdx)
                    )
                    val hWindGusts = maxOf(
                        hourlyDto?.windGusts10m?.getOrNull(hIdx) ?: (hWindSpeed * 1.35),
                        if (useEnsembleBlend) (secondaryHourly?.windGusts10m?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (tertiaryHourly?.windGusts10m?.getOrNull(hIdx) ?: 0.0) else 0.0,
                        if (useEnsembleBlend) (quaternaryHourly?.windGusts10m?.getOrNull(hIdx) ?: 0.0) else 0.0
                    )
                    val hWindDir = hourlyDto?.windDirection10m?.getOrNull(hIdx) ?: currentWindDir
                    val hCloud = (hourlyDto?.cloudCover?.getOrNull(hIdx) ?: secondaryHourly?.cloudCover?.getOrNull(hIdx) ?: currentCloudCover).coerceIn(0, 100)
                    val hFreezingLevel = hourlyDto?.freezingLevelHeight?.getOrNull(hIdx)
                        ?: secondaryHourly?.freezingLevelHeight?.getOrNull(hIdx)
                        ?: tertiaryHourly?.freezingLevelHeight?.getOrNull(hIdx)
                        ?: quaternaryHourly?.freezingLevelHeight?.getOrNull(hIdx)

                    val hCode = resolveBestSnowAndWeatherCode(
                        primaryCode = hourlyDto?.weatherCode?.getOrNull(hIdx) ?: rawDCode,
                        secondaryCode = if (useEnsembleBlend) secondaryHourly?.weatherCode?.getOrNull(hIdx) else null,
                        tertiaryCode = if (useEnsembleBlend) tertiaryHourly?.weatherCode?.getOrNull(hIdx) else null,
                        quaternaryCode = if (useEnsembleBlend) quaternaryHourly?.weatherCode?.getOrNull(hIdx) else null,
                        tempC = hTemp,
                        precipMm = hPrecipMm,
                        snowfallCm = hSnowCm,
                        precipProb = rawHProb,
                        windSpeedKmh = hWindSpeed,
                        cloudCover = hCloud,
                        freezingLevelM = hFreezingLevel,
                        elevationM = effectiveElevation
                    )
                    val hProb = if (hCode in setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)) {
                        dayHourlySnowHours++
                        if (hCode in setOf(57, 67, 75, 86)) dayHourlyStormSnowHours++
                        maxOf(rawHProb, if (hCode in setOf(75, 86)) 82 else 65)
                    } else {
                        rawHProb
                    }
                    dayHourlyMaxProb = maxOf(dayHourlyMaxProb, hProb)

                    dayHourly.add(
                        HourlyForecastItem(
                            isoTime = rawTime,
                            displayHour = displayLabel,
                            tempC = hTemp,
                            feelsLikeC = hFeels,
                            precipProbability = hProb,
                            precipMm = hPrecipMm,
                            weatherCode = hCode,
                            isDay = apiIsDay,
                            windSpeedKmh = hWindSpeed,
                            uvIndex = hourlyDto?.uvIndex?.getOrNull(hIdx)
                                ?: secondaryHourly?.uvIndex?.getOrNull(hIdx)
                                ?: (if (apiIsDay) dUvMax * 0.7 else 0.0),
                            humidity = hourlyDto?.relativeHumidity2m?.getOrNull(hIdx) ?: currentHumidity,
                            windGustsKmh = hWindGusts,
                            windDirectionDeg = hWindDir,
                            cloudCoverPercent = hCloud
                        )
                    )
                }
            }

            val totalDaySnowCm = maxOf(dailySnowSumCm, dayHourlyTotalSnowCm)
            val dCode = when {
                dayHourlyStormSnowHours >= 1 || totalDaySnowCm >= 3.5 ||
                    rawDCode in setOf(57, 67, 75, 86) ||
                    secondaryDCode in setOf(57, 67, 75, 86) ||
                    tertiaryDCode in setOf(57, 67, 75, 86) -> {
                    if (rawDCode == 86 || secondaryDCode == 86 || tertiaryDCode == 86) 86 else 75
                }
                dayHourlySnowHours >= 1 || totalDaySnowCm >= 0.15 ||
                    rawDCode in setOf(56, 66, 71, 73, 77, 85) ||
                    secondaryDCode in setOf(56, 66, 71, 73, 77, 85) ||
                    tertiaryDCode in setOf(56, 66, 71, 73, 77, 85) -> {
                    if (totalDaySnowCm >= 1.0 || rawDCode == 73 || secondaryDCode == 73 || tertiaryDCode == 73) 73 else 71
                }
                else -> rawDCode
            }
            val dProb = if (dCode in setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)) {
                maxOf(dayHourlyMaxProb, if (dCode in setOf(75, 86)) 85 else 68)
            } else {
                dayHourlyMaxProb
            }
            val dSumMm = maxOf(rawDSumMm, totalDaySnowCm * 1.0)

            if (dayHourly.isEmpty()) {
                dayHourly.addAll(
                    synthesizeDailyHourlyItems(
                        dateStr = dateStr,
                        minTempC = dMin,
                        maxTempC = dMax,
                        weatherCode = dCode,
                        maxPrecipProb = dProb,
                        precipSumMm = dSumMm,
                        windMaxKmh = dWindMax,
                        uvMax = dUvMax
                    )
                )
            }

            dailyList.add(
                DailyForecastItem(
                    dateIso = dateStr,
                    dayLabel = if (i == 0) "Today" else formatDayOfWeek(dateStr),
                    fullDateLabel = formatMonthDay(dateStr),
                    weatherCode = dCode,
                    maxTempC = dMax,
                    minTempC = dMin,
                    apparentMaxC = dailyDto.apparentTemperatureMax?.getOrNull(i) ?: dMax,
                    apparentMinC = dailyDto.apparentTemperatureMin?.getOrNull(i) ?: dMin,
                    maxPrecipProb = dProb,
                    precipSumMm = dSumMm,
                    sunriseTime = extractClockTime(sunriseRaw, todaySunriseClock),
                    sunsetTime = extractClockTime(sunsetRaw, todaySunsetClock),
                    daylightHours = daylightSecs / 3600.0,
                    uvIndexMax = dUvMax,
                    windMaxKmh = dWindMax,
                    hourly = dayHourly
                )
            )
        }

        val currentVisibilityMeters = currentDto.visibility
            ?: hourlyDto?.visibility?.getOrNull(startIndex)
            ?: when {
                fusedCurrentCode in setOf(45, 48) -> 1800.0
                fusedCurrentCode >= 71 -> 3800.0
                fusedCurrentCode >= 51 -> 8500.0
                currentHumidity >= 90 -> 9500.0
                else -> (24000.0 - airQuality.pm25 * 140.0).coerceIn(10000.0, 24000.0)
            }
        val currentUv = currentDto.uvIndex
            ?: hourlyDto?.uvIndex?.getOrNull(startIndex)
            ?: (if (resolvedCurrentIsDay) 4.2 else 0.0)
        val exactDewPointC = ((currentDto.dewPoint2m?.let { it + lapseRateCorrectionC })
            ?: calculateDewPointCelsius(currentTempC, currentHumidity))
            .coerceAtMost(currentTempC)

        val currentTelemetry = CurrentWeatherTelemetry(
            tempC = currentTempC,
            feelsLikeC = currentFeelsC,
            highC = todayHigh,
            lowC = todayLow,
            weatherCode = fusedCurrentCode,
            isDay = resolvedCurrentIsDay,
            humidity = currentHumidity,
            dewPointC = exactDewPointC,
            windSpeedKmh = currentWindSpeed,
            windGustsKmh = currentWindGusts,
            windDirectionDeg = currentWindDir,
            pressureHpa = currentPressure,
            visibilityKm = (currentVisibilityMeters / 1000.0).coerceIn(0.1, 80.0),
            cloudCoverPercent = currentCloudCover,
            uvIndex = currentUv.coerceAtLeast(0.0),
            precipitationMm = fusedCurrentPrecipMm
        )

        val sunProgress = computeSunProgress(
            currentTimeStr = "${liveDateIso}T$liveClockLabel",
            sunriseClock = dailyList.firstOrNull()?.sunriseTime ?: todaySunriseClock,
            sunsetClock = dailyList.firstOrNull()?.sunsetTime ?: todaySunsetClock
        )

        val (activities, bestWindow) = evaluateActivities(currentTelemetry, hourlyList, airQuality)
        val insights = buildSmartInsights(currentTelemetry, hourlyList, dailyList, airQuality)
        val rainOutlook = buildRainTimingOutlook(currentTelemetry, hourlyList, dailyList)

        return CityWeatherBundle(
            cityId = city.id,
            cityName = city.name,
            regionName = city.regionName,
            countryName = city.countryName,
            latitude = city.latitude,
            longitude = city.longitude,
            elevationMeters = effectiveElevation,
            timezoneId = effectiveTimezone,
            currentLocalTimeLabel = liveClockLabel,
            sunProgressFraction = sunProgress,
            current = currentTelemetry,
            hourly = hourlyList,
            daily = dailyList,
            airQuality = airQuality,
            rainOutlook = rainOutlook,
            insights = insights,
            activities = activities,
            bestOutdoorWindow = bestWindow,
            fetchedAtEpochMs = System.currentTimeMillis(),
            isLiveNetwork = true,
            provider = provider
        )
    }

    private fun buildResilientFallbackBundle(
        city: SavedCityEntity,
        airQuality: AirQualityTelemetry,
        provider: com.example.model.WeatherProvider
    ): CityWeatherBundle {
        val effectiveTimezone = resolveEffectiveTimezoneId(city.timezoneId)
        val liveClockLabel = formatLiveClockInTimezone(effectiveTimezone)
        val startHour = getCurrentHourInTimezone(effectiveTimezone)
        val isCurrentlyDay = startHour in 7..17
        val effectiveWeatherCode = city.cachedWeatherCode
        val baseTemp = city.cachedTempC
        val high = city.cachedHighC
        val low = city.cachedLowC
        val hourlyList = (0 until 24).map { h ->
            val hourNum = (startHour + h) % 24
            val isHourDay = hourNum in 7..17
            val wave = sin((hourNum - 6) * Math.PI / 12.0)
            val t = ((high + low) / 2.0) + ((high - low) / 2.0) * wave
            val hourCode = if (effectiveWeatherCode >= 45) {
                if (h <= 6) effectiveWeatherCode else if (h % 3 == 0) 61 else 2
            } else {
                when (h % 4) {
                    0, 1 -> 0
                    2 -> 1
                    else -> 2
                }
            }
            val hProb = if (hourCode >= 51) (78 - (h * 4).coerceAtMost(35)) else (8 + (h * 2) % 14)
            val hPrecip = if (hourCode >= 51) (2.2 - (h * 0.2).coerceAtMost(1.4)) else 0.0
            val hWind = 14.0 + (h % 6) * 1.8
            HourlyForecastItem(
                isoTime = String.format(Locale.US, "2026-11-14T%02d:00", hourNum),
                displayHour = if (h == 0) "Now" else String.format(Locale.US, "%02d:00", hourNum),
                tempC = t,
                feelsLikeC = t - 0.8,
                precipProbability = hProb,
                precipMm = hPrecip,
                weatherCode = hourCode,
                isDay = isHourDay,
                windSpeedKmh = hWind,
                uvIndex = if (isHourDay) (5.0 * sin(((hourNum - 6).coerceAtLeast(1)) * Math.PI / 12.0)).coerceAtLeast(1.0) else 0.0,
                humidity = if (hourCode >= 51) 84 else (55 - (wave * 14).roundToInt()).coerceIn(30, 88),
                windGustsKmh = hWind * 1.4,
                windDirectionDeg = 245,
                cloudCoverPercent = if (hourCode >= 51) 88 else 35
            )
        }

        val tz = TimeZone.getTimeZone(effectiveTimezone)
        val cal = Calendar.getInstance(tz)
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
        val sdfDay = SimpleDateFormat("EEE", Locale.US).apply { timeZone = tz }
        val sdfMonthDay = SimpleDateFormat("MMM d", Locale.US).apply { timeZone = tz }

        val dailyList = (0 until 10).map { d ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, d) }
            val dMax = high + sin(d * 0.9) * 2.8
            val dMin = low + cos(d * 0.8) * 2.2
            val code = if (effectiveWeatherCode >= 45 && d == 0) {
                effectiveWeatherCode
            } else {
                when (d % 4) {
                    0, 1 -> 0
                    2 -> 1
                    else -> 2
                }
            }
            val dateIsoStr = sdfIso.format(dayCal.time)
            val dProb = if (code >= 51) 82 else if (code == 2) 15 else 5
            val dSumMm = if (code >= 51) 6.4 else 0.0
            DailyForecastItem(
                dateIso = dateIsoStr,
                dayLabel = if (d == 0) "Today" else sdfDay.format(dayCal.time),
                fullDateLabel = sdfMonthDay.format(dayCal.time),
                weatherCode = code,
                maxTempC = dMax,
                minTempC = dMin,
                apparentMaxC = dMax - 0.5,
                apparentMinC = dMin - 1.0,
                maxPrecipProb = dProb,
                precipSumMm = dSumMm,
                sunriseTime = "06:44",
                sunsetTime = "17:29",
                daylightHours = 10.75,
                uvIndexMax = 5.5,
                windMaxKmh = 22.0,
                hourly = synthesizeDailyHourlyItems(
                    dateStr = dateIsoStr,
                    minTempC = dMin,
                    maxTempC = dMax,
                    weatherCode = code,
                    maxPrecipProb = dProb,
                    precipSumMm = dSumMm,
                    windMaxKmh = 22.0,
                    uvMax = 5.5
                )
            )
        }

        val isRainFallback = effectiveWeatherCode >= 51
        val currentHumidity = if (isRainFallback) 84 else 58
        val currentTelemetry = CurrentWeatherTelemetry(
            tempC = baseTemp,
            feelsLikeC = baseTemp - 0.7,
            highC = high,
            lowC = low,
            weatherCode = effectiveWeatherCode,
            isDay = isCurrentlyDay,
            humidity = currentHumidity,
            dewPointC = calculateDewPointCelsius(baseTemp, currentHumidity),
            windSpeedKmh = if (isRainFallback) 16.5 else 13.5,
            windGustsKmh = if (isRainFallback) 28.0 else 21.0,
            windDirectionDeg = 245,
            pressureHpa = if (isRainFallback) 1009.0 else 1016.0,
            visibilityKm = if (isRainFallback) 9.5 else 18.0,
            cloudCoverPercent = if (effectiveWeatherCode == 0) 8 else if (isRainFallback) 88 else 42,
            uvIndex = if (isCurrentlyDay) 3.2 else 0.0,
            precipitationMm = if (isRainFallback) 2.4 else 0.0
        )

        val sunProgress = computeSunProgress(
            currentTimeStr = "2026-11-14T$liveClockLabel",
            sunriseClock = "06:44",
            sunsetClock = "17:29"
        )

        val (activities, bestWindow) = evaluateActivities(currentTelemetry, hourlyList, airQuality)
        val insights = buildSmartInsights(currentTelemetry, hourlyList, dailyList, airQuality)
        val rainOutlook = buildRainTimingOutlook(currentTelemetry, hourlyList, dailyList)

        return CityWeatherBundle(
            cityId = city.id,
            cityName = city.name,
            regionName = city.regionName,
            countryName = city.countryName,
            latitude = city.latitude,
            longitude = city.longitude,
            elevationMeters = city.elevationMeters,
            timezoneId = effectiveTimezone,
            currentLocalTimeLabel = liveClockLabel,
            sunProgressFraction = sunProgress,
            current = currentTelemetry,
            hourly = hourlyList,
            daily = dailyList,
            airQuality = airQuality,
            rainOutlook = rainOutlook,
            insights = insights,
            activities = activities,
            bestOutdoorWindow = bestWindow,
            fetchedAtEpochMs = System.currentTimeMillis(),
            isLiveNetwork = false,
            provider = provider
        )
    }

    private fun buildRainTimingOutlook(
        current: CurrentWeatherTelemetry,
        hourly: List<HourlyForecastItem>,
        daily: List<DailyForecastItem>
    ): com.example.model.RainTimingOutlook {
        val rainPoints = hourly.take(48).map { item ->
            com.example.model.HourlyRainPoint(
                hourLabel = item.displayHour,
                probabilityPercent = item.precipProbability,
                precipMm = item.precipMm,
                tempC = item.tempC,
                windKmh = item.windSpeedKmh,
                isRaining = item.precipProbability >= 35 || item.precipMm > 0.05 || item.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
            )
        }

        val firstRain = rainPoints.firstOrNull { it.isRaining || it.probabilityPercent >= 40 }
        val peakRain = rainPoints.maxByOrNull { it.probabilityPercent }
        val peakProb = peakRain?.probabilityPercent ?: 0
        val totalRain7Days = daily.take(7).sumOf { it.precipSumMm }

        val nextRainOnsetLabel = if (current.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)) {
            "Precipitation active right now"
        } else if (firstRain != null) {
            "Rain expected around ${firstRain.hourLabel} (${firstRain.probabilityPercent}% chance, ${firstRain.tempC.roundToInt()}°C, wind ${firstRain.windKmh.roundToInt()} km/h)"
        } else {
            "No precipitation expected in the next 48 hours"
        }

        val peakRainWindow = if (peakRain != null && peakProb >= 25) {
            "Peak rain chance: ${peakRain.hourLabel} at ${peakProb}% (${String.format(Locale.US, "%.1f", peakRain.precipMm)} mm, wind ${peakRain.windKmh.roundToInt()} km/h)"
        } else {
            "Clear, dry skies across the upcoming 7–10 day forecast horizon"
        }

        val riskLevel = when {
            peakProb >= 65 -> "High Rain Chance"
            peakProb >= 35 -> "Moderate Chance of Showers"
            peakProb >= 15 -> "Low / Spotty Showers"
            else -> "Dry Clear Horizon"
        }

        val rainAdvice = when {
            peakProb >= 60 -> "Rain showers likely. Expect reduced visibility and gusty surface winds."
            peakProb >= 30 -> "Scattered light showers possible. Check hourly rain radar before heading outdoors."
            else -> "Dry atmospheric conditions with clear skies and stable barometric pressure."
        }

        return com.example.model.RainTimingOutlook(
            hasRainInNext48h = firstRain != null,
            nextRainOnsetLabel = nextRainOnsetLabel,
            peakRainWindowLabel = peakRainWindow,
            totalRainNext7DaysMm = totalRain7Days,
            peakPrecipProbability = peakProb,
            rainRiskLevel = riskLevel,
            rainAdvice = rainAdvice,
            hourlyRainTimeline = rainPoints.take(24)
        )
    }

    private fun buildSmartInsights(
        current: CurrentWeatherTelemetry,
        hourly: List<HourlyForecastItem>,
        daily: List<DailyForecastItem>,
        aqi: AirQualityTelemetry
    ): List<SmartWeatherInsight> {
        val insights = mutableListOf<SmartWeatherInsight>()

        // 1. Precipitation alert or clear window
        val peakRainHour = hourly.take(12).maxByOrNull { it.precipProbability }
        if (peakRainHour != null && peakRainHour.precipProbability >= 45) {
            insights.add(
                SmartWeatherInsight(
                    headline = "Rain Likely Around ${peakRainHour.displayHour} (${peakRainHour.precipProbability}%)",
                    detail = "Pack a waterproof shell or umbrella before heading out. Expect wet pavement and reduced visibility.",
                    tag = "PRECIPITATION"
                )
            )
        } else {
            insights.add(
                SmartWeatherInsight(
                    headline = "Dry Horizon Over the Next 12 Hours",
                    detail = "Precipitation probability stays below ${(peakRainHour?.precipProbability ?: 10)}%. Great conditions for commuting and outdoor errands.",
                    tag = "OUTLOOK"
                )
            )
        }

        // 2. Thermal & Clothing guidance
        val clothingAdvice = when {
            current.feelsLikeC < 8.0 -> "Chilly thermal feel (${current.feelsLikeC.roundToInt()}°C). Wear an insulated jacket and windproof outer layer."
            current.feelsLikeC < 16.0 -> "Crisp mild air. A breathable mid-layer or light sweater keeps you comfortable."
            current.feelsLikeC < 25.0 -> "Optimal thermal comfort zone. Light casual layers are ideal all day."
            else -> "Warm atmospheric conditions. Choose breathable fabrics and stay hydrated."
        }
        insights.add(
            SmartWeatherInsight(
                headline = "Thermal Comfort & Wardrobe",
                detail = clothingAdvice,
                tag = "FEELS LIKE"
            )
        )

        // 3. UV / Air Quality / Wind highlight
        if (current.uvIndex >= 6.0) {
            insights.add(
                SmartWeatherInsight(
                    headline = "High UV Radiation (Index ${String.format(Locale.US, "%.1f", current.uvIndex)})",
                    detail = "Apply broad-spectrum SPF 30+ sunscreen and wear UV-blocking sunglasses during midday hours.",
                    tag = "SOLAR UV"
                )
            )
        } else {
            insights.add(
                SmartWeatherInsight(
                    headline = "Air Quality is ${aqi.categoryLabel} (US AQI ${aqi.usAqi})",
                    detail = aqi.healthAdvice,
                    tag = "AIR QUALITY"
                )
            )
        }

        return insights
    }

    private fun evaluateActivities(
        current: CurrentWeatherTelemetry,
        hourly: List<HourlyForecastItem>,
        aqi: AirQualityTelemetry
    ): Pair<List<ActivitySuitability>, String> {
        val next12 = hourly.take(12).ifEmpty {
            listOf(
                HourlyForecastItem(
                    isoTime = "",
                    displayHour = "Now",
                    tempC = current.tempC,
                    feelsLikeC = current.feelsLikeC,
                    precipProbability = 10,
                    precipMm = 0.0,
                    weatherCode = current.weatherCode,
                    isDay = current.isDay,
                    windSpeedKmh = current.windSpeedKmh,
                    uvIndex = current.uvIndex,
                    humidity = current.humidity
                )
            )
        }

        // Find best 2-hour daytime outdoor window
        val bestHour = next12.minByOrNull { h ->
            val tempPenalty = abs(h.feelsLikeC - 19.0) * 2.2
            val rainPenalty = h.precipProbability * 0.9
            val windPenalty = (h.windSpeedKmh - 12.0).coerceAtLeast(0.0) * 1.4
            tempPenalty + rainPenalty + windPenalty + (if (!h.isDay) 15.0 else 0.0)
        } ?: next12.first()

        val bestWindowLabel = if (bestHour.displayHour == "Now") {
            "Right Now (Next 2 Hours)"
        } else {
            "Around ${bestHour.displayHour} (${bestHour.tempC.roundToInt()}°C, ${bestHour.precipProbability}% rain)"
        }

        val rainPenalty = (bestHour.precipProbability * 0.6).roundToInt()
        val aqiPenalty = ((aqi.usAqi - 35).coerceAtLeast(0) * 0.35).roundToInt()

        val runScore = (96 - abs(current.feelsLikeC - 16.0).roundToInt() * 2 - rainPenalty - aqiPenalty)
            .coerceIn(18, 99)
        val cycleScore = (94 - abs(current.feelsLikeC - 19.0).roundToInt() * 2 - (current.windSpeedKmh * 0.9).roundToInt() - rainPenalty)
            .coerceIn(15, 98)
        val photoScore = (92 - (if (current.cloudCoverPercent > 85) 20 else 0) - rainPenalty / 2)
            .coerceIn(25, 99)
        val stargazeScore = (98 - current.cloudCoverPercent - rainPenalty)
            .coerceIn(10, 99)
        val hikeScore = (95 - abs(current.feelsLikeC - 18.0).roundToInt() * 2 - rainPenalty - aqiPenalty)
            .coerceIn(20, 98)
        val patioScore = (95 - abs(current.feelsLikeC - 22.0).roundToInt() * 3 - (current.windSpeedKmh * 0.7).roundToInt() - rainPenalty)
            .coerceIn(15, 98)

        val list = listOf(
            ActivitySuitability(
                id = "running",
                name = "Outdoor Running",
                score = runScore,
                statusLabel = scoreLabel(runScore),
                recommendation = "Dew point ${current.dewPointC.roundToInt()}°C and ${current.windCardinal} breeze (${current.windSpeedKmh.roundToInt()} km/h) provide ${scoreLabel(runScore).lowercase()} pacing conditions.",
                bestTimeWindow = bestWindowLabel,
                accentColor = scoreColor(runScore)
            ),
            ActivitySuitability(
                id = "cycling",
                name = "Road & Gravel Cycling",
                score = cycleScore,
                statusLabel = scoreLabel(cycleScore),
                recommendation = "Sustained wind at ${current.windSpeedKmh.roundToInt()} km/h with gusts to ${current.windGustsKmh.roundToInt()} km/h from ${current.windDirectionDeg}° (${current.windCardinal}).",
                bestTimeWindow = bestWindowLabel,
                accentColor = scoreColor(cycleScore)
            ),
            ActivitySuitability(
                id = "photography",
                name = "Golden Hour Photography",
                score = photoScore,
                statusLabel = scoreLabel(photoScore),
                recommendation = "${current.cloudCoverPercent}% cloud cover creates dramatic diffused sky contrast with ${current.visibilityKm.roundToInt()} km visibility.",
                bestTimeWindow = "1 hr before Sunset",
                accentColor = scoreColor(photoScore)
            ),
            ActivitySuitability(
                id = "hiking",
                name = "Trail Hiking & Parks",
                score = hikeScore,
                statusLabel = scoreLabel(hikeScore),
                recommendation = "Air quality index is ${aqi.usAqi} (${aqi.categoryLabel}) with peak UV around ${String.format(Locale.US, "%.1f", current.uvIndex)}.",
                bestTimeWindow = bestWindowLabel,
                accentColor = scoreColor(hikeScore)
            ),
            ActivitySuitability(
                id = "stargazing",
                name = "Night Sky & Stargazing",
                score = stargazeScore,
                statusLabel = scoreLabel(stargazeScore),
                recommendation = "Sky transparency depends on ${current.cloudCoverPercent}% overhead cloud deck and ${current.humidity}% relative humidity.",
                bestTimeWindow = "After 21:30 Local",
                accentColor = scoreColor(stargazeScore)
            ),
            ActivitySuitability(
                id = "patio",
                name = "Al Fresco Dining & Patio",
                score = patioScore,
                statusLabel = scoreLabel(patioScore),
                recommendation = "Feels like ${current.feelsLikeC.roundToInt()}°C outside with ${bestHour.precipProbability}% chance of rain.",
                bestTimeWindow = bestWindowLabel,
                accentColor = scoreColor(patioScore)
            )
        )

        return list to bestWindowLabel
    }

    private fun scoreLabel(score: Int): String = when {
        score >= 82 -> "Ideal"
        score >= 65 -> "Good"
        score >= 45 -> "Fair"
        else -> "Poor"
    }

    private fun scoreColor(score: Int): Color = when {
        score >= 82 -> Color(0xFF34D399)
        score >= 65 -> Color(0xFF38BDF8)
        score >= 45 -> Color(0xFFFBBF24)
        else -> Color(0xFFFB7185)
    }

    private fun DailyDto.windMaxKmh(index: Int): Double {
        return this.windSpeed10mMax?.getOrNull(index) ?: 15.0
    }

    private fun synthesizeDailyHourlyItems(
        dateStr: String,
        minTempC: Double,
        maxTempC: Double,
        weatherCode: Int,
        maxPrecipProb: Int,
        precipSumMm: Double,
        windMaxKmh: Double,
        uvMax: Double
    ): List<HourlyForecastItem> {
        return (0 until 24 step 2).map { hourNum ->
            val isHourDay = hourNum in 7..18
            val wave = sin((hourNum - 6) * Math.PI / 12.0)
            val temp = ((maxTempC + minTempC) / 2.0) + ((maxTempC - minTempC) / 2.0) * wave
            val hProb = if (weatherCode >= 51) {
                (maxPrecipProb * (0.65 + 0.35 * abs(sin(hourNum * 0.4)))).roundToInt().coerceIn(20, 100)
            } else {
                maxPrecipProb
            }
            val hWind = (windMaxKmh * (0.60 + 0.35 * maxOf(0.0, wave))).coerceAtLeast(4.0)
            HourlyForecastItem(
                isoTime = String.format(Locale.US, "%sT%02d:00", dateStr, hourNum),
                displayHour = String.format(Locale.US, "%02d:00", hourNum),
                tempC = temp,
                feelsLikeC = temp - 0.6,
                precipProbability = hProb,
                precipMm = if (weatherCode >= 51) (precipSumMm / 12.0) else 0.0,
                weatherCode = weatherCode,
                isDay = isHourDay,
                windSpeedKmh = hWind,
                uvIndex = if (isHourDay) (uvMax * maxOf(0.15, wave)) else 0.0,
                humidity = if (weatherCode >= 51) 82 else 55,
                windGustsKmh = hWind * 1.35,
                windDirectionDeg = 240,
                cloudCoverPercent = if (weatherCode >= 51) 85 else if (weatherCode >= 2) 45 else 12
            )
        }
    }

    private fun GeocodingCityDto.toSavedCityEntity(): SavedCityEntity {
        val cleanName = sanitizeCityDisplayName(name).ifBlank { name }
        val cleanAdmin = admin1?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() }
        val deterministicId = if (id != 0L) id else abs("${cleanName}_${latitude}_${longitude}".hashCode().toLong())
        return SavedCityEntity(
            id = deterministicId,
            name = cleanName,
            regionName = cleanAdmin ?: (country ?: "Global"),
            countryName = country?.takeIf { it.isNotBlank() } ?: (countryCode ?: ""),
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevation?.takeIf { it > 0.0 } ?: -1.0,
            timezoneId = timezone ?: "auto"
        )
    }

    private fun com.example.network.NominatimPlaceDto.toSavedCityEntity(): SavedCityEntity? {
        val latVal = lat.toDoubleOrNull() ?: return null
        val lonVal = lon.toDoubleOrNull() ?: return null
        if (!latVal.isFinite() || !lonVal.isFinite()) return null
        val addr = address
        val rawPlaceName = listOfNotNull(
            name?.trim()?.takeIf { it.isNotBlank() },
            addr?.village?.trim()?.takeIf { it.isNotBlank() },
            addr?.hamlet?.trim()?.takeIf { it.isNotBlank() },
            addr?.town?.trim()?.takeIf { it.isNotBlank() },
            addr?.suburb?.trim()?.takeIf { it.isNotBlank() },
            addr?.city?.trim()?.takeIf { it.isNotBlank() },
            displayName.substringBefore(",").trim().takeIf { it.isNotBlank() }
        ).firstOrNull() ?: return null

        val cleanName = sanitizeCityDisplayName(rawPlaceName).ifBlank { rawPlaceName }
        val regionParts = listOfNotNull(
            addr?.municipality?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() && !it.equals(cleanName, ignoreCase = true) },
            addr?.county?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() && !it.equals(cleanName, ignoreCase = true) },
            addr?.state?.let { sanitizeCityDisplayName(it) }?.takeIf { it.isNotBlank() && !it.equals(cleanName, ignoreCase = true) }
        ).distinct()
        val country = addr?.country?.trim()?.takeIf { it.isNotBlank() }
            ?: displayName.substringAfterLast(",").trim()
        val region = regionParts.joinToString(", ").ifBlank { country.ifBlank { "Local" } }
        val isGreece = addr?.countryCode.equals("gr", ignoreCase = true) ||
            country.contains("Ελλάδα", ignoreCase = true) ||
            country.contains("Greece", ignoreCase = true) ||
            (latVal in 34.8..41.8 && lonVal in 19.3..29.7)
        val matchedCatalogCity = localGreekCityCatalog.firstOrNull { (cat, _, _) ->
            abs(cat.latitude - latVal) < 0.015 && abs(cat.longitude - lonVal) < 0.015
        }?.first
        val idVal = matchedCatalogCity?.id ?: placeId.takeIf { it != 0L } ?: abs("${cleanName}_${latVal}_${lonVal}".hashCode().toLong())
        return SavedCityEntity(
            id = idVal,
            name = cleanName,
            regionName = region,
            countryName = country.ifBlank { if (isGreece) "Greece" else "Global" },
            latitude = latVal,
            longitude = lonVal,
            elevationMeters = matchedCatalogCity?.elevationMeters ?: -1.0,
            timezoneId = if (isGreece) "Europe/Athens" else "auto"
        )
    }

    private fun formatIsoHourLabel(isoTime: String): String {
        val tIndex = isoTime.indexOf('T')
        if (tIndex != -1 && isoTime.length >= tIndex + 6) {
            return isoTime.substring(tIndex + 1, tIndex + 6)
        }
        return isoTime
    }

    private fun extractClockTime(isoDateTime: String, fallback: String): String {
        val tIndex = isoDateTime.indexOf('T')
        if (tIndex != -1 && isoDateTime.length >= tIndex + 6) {
            return isoDateTime.substring(tIndex + 1, tIndex + 6)
        }
        return fallback
    }

    private fun computeSunProgress(currentTimeStr: String, sunriseClock: String, sunsetClock: String): Float {
        val currentMins = clockToMinutes(extractClockTime(currentTimeStr, "12:30"))
        val riseMins = clockToMinutes(sunriseClock)
        val setMins = clockToMinutes(sunsetClock)
        if (setMins <= riseMins) return 0.5f
        return ((currentMins - riseMins).toFloat() / (setMins - riseMins).toFloat()).coerceIn(0f, 1f)
    }

    private fun clockToMinutes(clock: String): Int {
        val parts = clock.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun formatDayOfWeek(dateIso: String): String {
        return try {
            val utc = TimeZone.getTimeZone("UTC")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
            val date = parser.parse(dateIso) ?: return dateIso
            SimpleDateFormat("EEE", Locale.US).apply { timeZone = utc }.format(date)
        } catch (e: Exception) {
            dateIso
        }
    }

    private fun formatMonthDay(dateIso: String): String {
        return try {
            val utc = TimeZone.getTimeZone("UTC")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
            val date = parser.parse(dateIso) ?: return dateIso
            SimpleDateFormat("MMM d", Locale.US).apply { timeZone = utc }.format(date)
        } catch (e: Exception) {
            dateIso
        }
    }

    companion object {
        const val GPS_CITY_ID: Long = 999_999_001L

        fun sanitizeCityDisplayName(raw: String): String {
            var cleaned = raw.trim()
            if (cleaned.isEmpty()) return ""
            val exactRejections = setOf(
                "dimos larisa",
                "dimos larissa",
                "dimos larisas",
                "δήμος λαρισαίων",
                "δήμος λάρισας",
                "dimos",
                "δήμος"
            )
            if (cleaned.lowercase(Locale.ROOT) in exactRejections) {
                return ""
            }
            val prefixes = listOf(
                "Dimos ",
                "dimos ",
                "Δήμος ",
                "δήμος ",
                "Municipality of ",
                "municipality of ",
                "Δημοτική Ενότητα ",
                "Regional Unit of "
            )
            for (prefix in prefixes) {
                if (cleaned.startsWith(prefix, ignoreCase = true)) {
                    cleaned = cleaned.substring(prefix.length).trim()
                }
            }
            if (cleaned.equals("Larisa", ignoreCase = true) || cleaned.equals("Larisas", ignoreCase = true)) {
                return "Larissa"
            }
            if (cleaned.equals("Λαρισαίων", ignoreCase = true) || cleaned.equals("Λάρισας", ignoreCase = true)) {
                return "Λάρισα"
            }
            return cleaned
        }

        fun resolveEffectiveTimezoneId(timezoneId: String?): String {
            val clean = timezoneId?.trim().orEmpty()
            return if (clean.isBlank() || clean.equals("auto", ignoreCase = true) || clean.equals("GMT", ignoreCase = true)) {
                "Europe/Athens"
            } else {
                clean
            }
        }

        fun formatLiveClockInTimezone(timezoneId: String?, epochMs: Long = System.currentTimeMillis()): String {
            val tz = TimeZone.getTimeZone(resolveEffectiveTimezoneId(timezoneId))
            val sdf = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
            return sdf.format(java.util.Date(epochMs))
        }

        fun formatLiveDateIsoInTimezone(timezoneId: String?, epochMs: Long = System.currentTimeMillis()): String {
            val tz = TimeZone.getTimeZone(resolveEffectiveTimezoneId(timezoneId))
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            return sdf.format(java.util.Date(epochMs))
        }

        fun getCurrentHourInTimezone(timezoneId: String?, epochMs: Long = System.currentTimeMillis()): Int {
            val tz = TimeZone.getTimeZone(resolveEffectiveTimezoneId(timezoneId))
            val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMs }
            return cal.get(Calendar.HOUR_OF_DAY)
        }
    }
}
