package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double? = 0.0,
    val timezone: String? = "UTC",
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = 0,
    val current: CurrentDto? = null,
    @Json(name = "minutely_15") val minutely15: Minutely15Dto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    val time: String? = "",
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "dew_point_2m") val dewPoint2m: Double? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "is_day") val isDay: Int? = 1,
    val precipitation: Double? = 0.0,
    val rain: Double? = null,
    val showers: Double? = null,
    val snowfall: Double? = null,
    @Json(name = "snow_depth") val snowDepth: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = 0,
    @Json(name = "cloud_cover") val cloudCover: Int? = 15,
    @Json(name = "pressure_msl") val pressureMsl: Double? = 1013.0,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = 10.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = 180,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null,
    val visibility: Double? = null
)

@JsonClass(generateAdapter = true)
data class Minutely15Dto(
    val time: List<String> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    val rain: List<Double?> = emptyList(),
    val snowfall: List<Double?>? = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    val time: List<String>? = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?>? = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?>? = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = emptyList(),
    val precipitation: List<Double?>? = emptyList(),
    val rain: List<Double?>? = emptyList(),
    val showers: List<Double?>? = emptyList(),
    val snowfall: List<Double?>? = emptyList(),
    @Json(name = "snow_depth") val snowDepth: List<Double?>? = emptyList(),
    @Json(name = "freezing_level_height") val freezingLevelHeight: List<Double?>? = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?>? = emptyList(),
    @Json(name = "is_day") val isDay: List<Int?>? = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?>? = emptyList(),
    @Json(name = "wind_gusts_10m") val windGusts10m: List<Double?>? = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int?>? = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Int?>? = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double?>? = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?>? = emptyList(),
    val visibility: List<Double?>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    val time: List<String>? = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?>? = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?>? = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?>? = emptyList(),
    @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double?>? = emptyList(),
    @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double?>? = emptyList(),
    val sunrise: List<String?>? = emptyList(),
    val sunset: List<String?>? = emptyList(),
    @Json(name = "daylight_duration") val daylightDuration: List<Double?>? = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = emptyList(),
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?>? = emptyList(),
    @Json(name = "snowfall_sum") val snowfallSum: List<Double?>? = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = emptyList(),
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val current: AirQualityCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentDto(
    @Json(name = "us_aqi") val usAqi: Int? = 28,
    @Json(name = "european_aqi") val europeanAqi: Int? = 22,
    @Json(name = "pm2_5") val pm25: Double? = 6.4,
    val pm10: Double? = 14.2,
    val ozone: Double? = 58.0,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = 9.5,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double? = 185.0
)

@JsonClass(generateAdapter = true)
data class GeocodingSearchResponse(
    val results: List<GeocodingCityDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingCityDto(
    val id: Long = 0L,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double? = 0.0,
    val country: String? = "",
    @Json(name = "country_code") val countryCode: String? = "",
    val admin1: String? = "",
    val timezone: String? = "auto"
)

interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("elevation") elevation: Double? = null,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,snow_depth,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,visibility",
        @Query("minutely_15") minutely15: String? = null,
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,precipitation_probability,precipitation,rain,showers,snowfall,snow_depth,freezing_level_height,weather_code,is_day,wind_speed_10m,wind_gusts_10m,wind_direction_10m,cloud_cover,uv_index,relative_humidity_2m,visibility",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,daylight_duration,uv_index_max,precipitation_sum,snowfall_sum,precipitation_probability_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("models") models: String? = null,
        @Query("cell_selection") cellSelection: String = "land"
    ): OpenMeteoForecastResponse
}

interface OpenMeteoAirQualityService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,ozone",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoAirQualityResponse
}

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingSearchResponse
}

@JsonClass(generateAdapter = true)
data class GeoIpLocationResponse(
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class IpWhoIsTimezoneDto(
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class IpWhoIsResponse(
    val success: Boolean? = true,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: IpWhoIsTimezoneDto? = null
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodeResponse(
    val city: String? = null,
    val locality: String? = null,
    val principalSubdivision: String? = null,
    val countryName: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    val village: String? = null,
    val town: String? = null,
    val city: String? = null,
    val hamlet: String? = null,
    val suburb: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
    @Json(name = "state_district") val stateDistrict: String? = null,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimPlaceDto(
    @Json(name = "place_id") val placeId: Long = 0L,
    val lat: String = "0.0",
    val lon: String = "0.0",
    val name: String? = null,
    @Json(name = "display_name") val displayName: String = "",
    val address: NominatimAddressDto? = null
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAssetDto(
    val name: String = "",
    @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0L,
    @Json(name = "download_count") val downloadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    val assets: List<GitHubReleaseAssetDto>? = null
)

interface GeoLocationLookupService {
    @GET("https://get.geojs.io/v1/ip/geo.json")
    suspend fun detectIpLocation(): GeoIpLocationResponse

    @GET("https://ipwho.is/")
    suspend fun detectIpLocationBackup(): IpWhoIsResponse

    @GET("https://api.bigdatacloud.net/data/reverse-geocode-client")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("localityLanguage") language: String = "en"
    ): ReverseGeocodeResponse

    @GET("https://nominatim.openstreetmap.org/search")
    suspend fun searchVillagesAndTowns(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 12,
        @retrofit2.http.Header("Accept-Language") acceptLanguage: String = "el,en",
        @retrofit2.http.Header("User-Agent") userAgent: String = "ZephyrWeatherAndroid/2.0"
    ): List<NominatimPlaceDto>

    @GET("https://api.github.com/repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestGitHubRelease(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String,
        @retrofit2.http.Header("Accept") accept: String = "application/vnd.github+json",
        @retrofit2.http.Header("User-Agent") userAgent: String = "ZephyrWeatherAndroid/2.7"
    ): GitHubReleaseResponse

    @GET("https://api.github.com/repos/{owner}/{repo}/releases")
    suspend fun getAllGitHubReleases(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @retrofit2.http.Header("Accept") accept: String = "application/vnd.github+json",
        @retrofit2.http.Header("User-Agent") userAgent: String = "ZephyrWeatherAndroid/2.7"
    ): List<GitHubReleaseResponse> = listOf(getLatestGitHubRelease(owner, repo, accept, userAgent))
}

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder()
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val forecastService: OpenMeteoForecastService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastService::class.java)
    }

    val airQualityService: OpenMeteoAirQualityService by lazy {
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoAirQualityService::class.java)
    }

    val geocodingService: OpenMeteoGeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingService::class.java)
    }

    val geoLocationLookupService: GeoLocationLookupService by lazy {
        Retrofit.Builder()
            .baseUrl("https://get.geojs.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeoLocationLookupService::class.java)
    }
}
