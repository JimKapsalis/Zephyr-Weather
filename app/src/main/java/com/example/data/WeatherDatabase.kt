package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.AppLanguage
import com.example.model.AppThemeSetting
import com.example.model.AutoRefreshInterval
import com.example.model.WeatherProvider
import com.example.model.WeatherUnits
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_cities")
data class SavedCityEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val regionName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 15.0,
    val timezoneId: String = "Europe/Athens",
    val isPinned: Boolean = false,
    val isCurrentGps: Boolean = false,
    val sortOrder: Int = 0,
    val cachedTempC: Double = 18.0,
    val cachedHighC: Double = 21.0,
    val cachedLowC: Double = 11.0,
    val cachedWeatherCode: Int = 2,
    val cachedIsDay: Boolean = true,
    val cachedAqi: Int = 18,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface CityDao {
    @Query("SELECT * FROM saved_cities ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    fun observeAllCities(): Flow<List<SavedCityEntity>>

    @Query("SELECT * FROM saved_cities ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    suspend fun getAllCitiesOnce(): List<SavedCityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCity(city: SavedCityEntity)

    @Query("DELETE FROM saved_cities WHERE id = :cityId")
    suspend fun deleteCityById(cityId: Long)

    @Query("DELETE FROM saved_cities")
    suspend fun deleteAllCities()

    @Query("UPDATE saved_cities SET isPinned = :pinned WHERE id = :cityId")
    suspend fun setCityPinned(cityId: Long, pinned: Boolean)
}

@Database(
    entities = [SavedCityEntity::class],
    version = 8,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getInstance(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext ?: context
                runCatching {
                    appCtx.deleteDatabase("zephyr_weather_v5.db")
                    appCtx.deleteDatabase("zephyr_weather_v6.db")
                    appCtx.deleteDatabase("zephyr_weather_v7.db")
                }
                val instance = Room.databaseBuilder(
                    appCtx,
                    WeatherDatabase::class.java,
                    "zephyr_weather_v8.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WeatherPreferences(context: Context) {
    private val prefs: SharedPreferences = (context.applicationContext ?: context).getSharedPreferences(
        "zephyr_weather_prefs",
        Context.MODE_PRIVATE
    )

    fun loadUnits(): WeatherUnits {
        return runCatching {
            WeatherUnits(
                useFahrenheit = prefs.getBoolean(KEY_USE_FAHRENHEIT, false),
                useMph = prefs.getBoolean(KEY_USE_MPH, false),
                useInHg = prefs.getBoolean(KEY_USE_INHG, false)
            )
        }.getOrDefault(WeatherUnits())
    }

    fun saveUseFahrenheit(useFahrenheit: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_USE_FAHRENHEIT, useFahrenheit) }
    }

    fun saveUseMph(useMph: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_USE_MPH, useMph) }
    }

    fun saveUseInHg(useInHg: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_USE_INHG, useInHg) }
    }

    fun loadLanguage(): AppLanguage {
        val code = runCatching {
            prefs.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code)
        }.getOrNull() ?: AppLanguage.ENGLISH.code
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit(commit = true) { putString(KEY_APP_LANGUAGE, language.code) }
    }

    fun loadProvider(): WeatherProvider {
        val id = runCatching {
            prefs.getString(KEY_PROVIDER, WeatherProvider.BEST_MATCH.id)
        }.getOrNull()
        return WeatherProvider.entries.find { it.id == id } ?: WeatherProvider.BEST_MATCH
    }

    fun saveProvider(provider: WeatherProvider) {
        prefs.edit(commit = true) { putString(KEY_PROVIDER, provider.id) }
    }

    fun loadTheme(): AppThemeSetting {
        val id = runCatching {
            prefs.getString(KEY_THEME, AppThemeSetting.AEGEAN_NIGHT.id)
        }.getOrNull()
        return AppThemeSetting.entries.find { it.id == id } ?: AppThemeSetting.AEGEAN_NIGHT
    }

    fun saveTheme(theme: AppThemeSetting) {
        prefs.edit(commit = true) { putString(KEY_THEME, theme.id) }
    }

    fun loadRefreshInterval(): AutoRefreshInterval {
        val id = runCatching {
            prefs.getString(KEY_REFRESH_INTERVAL, AutoRefreshInterval.EVERY_15_MIN.id)
        }.getOrNull()
        return AutoRefreshInterval.entries.find { it.id == id } ?: AutoRefreshInterval.EVERY_15_MIN
    }

    fun saveRefreshInterval(interval: AutoRefreshInterval) {
        prefs.edit(commit = true) { putString(KEY_REFRESH_INTERVAL, interval.id) }
    }

    fun loadBatterySaverSync(): Boolean = runCatching {
        prefs.getBoolean(KEY_BATTERY_SAVER, true)
    }.getOrDefault(true)

    fun saveBatterySaverSync(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_BATTERY_SAVER, enabled) }
    }

    fun loadEmyAlerts(): Boolean = runCatching {
        prefs.getBoolean(KEY_EMY_ALERTS, true)
    }.getOrDefault(true)

    fun saveEmyAlerts(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_EMY_ALERTS, enabled) }
    }

    fun loadRainAlerts(): Boolean = runCatching {
        prefs.getBoolean(KEY_RAIN_ALERTS, true)
    }.getOrDefault(true)

    fun saveRainAlerts(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_RAIN_ALERTS, enabled) }
    }

    fun loadSelectedCityId(): Long {
        val savedId = runCatching {
            prefs.getLong(KEY_SELECTED_CITY_ID, -1L)
        }.getOrDefault(-1L)
        return if (savedId == 9036755L || savedId <= 0L) {
            -1L
        } else {
            savedId
        }
    }

    fun saveSelectedCityId(cityId: Long) {
        if (cityId == 9036755L) return
        prefs.edit(commit = true) { putLong(KEY_SELECTED_CITY_ID, cityId) }
    }

    fun hasClearedAutoSeededCities(): Boolean = runCatching {
        prefs.getBoolean(KEY_CLEARED_AUTO_SEEDED_V6, false)
    }.getOrDefault(false)

    fun markAutoSeededCitiesCleared() {
        prefs.edit(commit = true) { putBoolean(KEY_CLEARED_AUTO_SEEDED_V6, true) }
    }

    fun hasCompletedInitialGpsSetup(): Boolean = runCatching {
        prefs.getBoolean(KEY_INITIAL_GPS_DONE_V2, false)
    }.getOrDefault(false)

    fun markInitialGpsSetupCompleted() {
        prefs.edit(commit = true) { putBoolean(KEY_INITIAL_GPS_DONE_V2, true) }
    }

    fun loadDeletedCityIds(): Set<Long> {
        val raw = runCatching {
            prefs.getStringSet(KEY_DELETED_CITY_IDS, emptySet())
        }.getOrNull().orEmpty()
        return raw.mapNotNull { it.toLongOrNull() }.toSet() + 9036755L
    }

    fun addDeletedCityId(cityId: Long) {
        val updated = (loadDeletedCityIds() + cityId).map { it.toString() }.toSet()
        prefs.edit(commit = true) { putStringSet(KEY_DELETED_CITY_IDS, updated) }
    }

    fun removeDeletedCityId(cityId: Long) {
        val updated = (loadDeletedCityIds() - cityId).map { it.toString() }.toSet()
        prefs.edit(commit = true) { putStringSet(KEY_DELETED_CITY_IDS, updated) }
    }

    fun loadDeletedCityNames(): Set<String> {
        return runCatching {
            prefs.getStringSet(KEY_DELETED_CITY_NAMES, emptySet())
        }.getOrNull().orEmpty()
            .map { it.lowercase().trim() }
            .toSet()
    }

    fun addDeletedCityName(name: String) {
        val clean = name.lowercase().trim()
        if (clean.isBlank()) return
        val extras = if (clean == "athens" || clean == "αθήνα" || clean == "αθηνα") {
            setOf("athens", "αθήνα", "αθηνα")
        } else {
            setOf(clean)
        }
        val updated = loadDeletedCityNames() + extras
        prefs.edit(commit = true) { putStringSet(KEY_DELETED_CITY_NAMES, updated) }
    }

    fun removeDeletedCityName(name: String) {
        val clean = name.lowercase().trim()
        val toRemove = if (clean == "athens" || clean == "αθήνα" || clean == "αθηνα") {
            setOf("athens", "αθήνα", "αθηνα")
        } else {
            setOf(clean)
        }
        val updated = loadDeletedCityNames() - toRemove
        prefs.edit(commit = true) { putStringSet(KEY_DELETED_CITY_NAMES, updated) }
    }

    fun loadGitHubRepoSlug(): String {
        val raw = runCatching {
            prefs.getString(KEY_GITHUB_REPO_SLUG, DEFAULT_GITHUB_REPO_SLUG)
        }.getOrNull()?.trim().orEmpty()
        return if (raw.contains("/") && raw.length >= 3 && !raw.equals("zephyr-weather/zephyr-weather", ignoreCase = true)) {
            raw
        } else {
            DEFAULT_GITHUB_REPO_SLUG
        }
    }

    fun saveGitHubRepoSlug(slug: String) {
        val cleaned = slug.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
        if (cleaned.contains("/")) {
            prefs.edit(commit = true) { putString(KEY_GITHUB_REPO_SLUG, cleaned) }
        }
    }

    companion object {
        const val DEFAULT_LARISSA_CITY_ID = 258576L
        const val DEFAULT_GITHUB_REPO_SLUG = "JimKapsalis/Zephyr-Weather"
        private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
        private const val KEY_USE_MPH = "use_mph"
        private const val KEY_USE_INHG = "use_inhg"
        private const val KEY_APP_LANGUAGE = "app_language_v3"
        private const val KEY_PROVIDER = "weather_provider_multimodel_default_v5"
        private const val KEY_THEME = "app_theme"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval"
        private const val KEY_BATTERY_SAVER = "battery_saver_sync"
        private const val KEY_EMY_ALERTS = "emy_alerts_enabled"
        private const val KEY_RAIN_ALERTS = "rain_alerts_enabled"
        private const val KEY_SELECTED_CITY_ID = "selected_city_id_v5"
        private const val KEY_CLEARED_AUTO_SEEDED_V6 = "cleared_auto_seeded_v6"
        private const val KEY_INITIAL_GPS_DONE_V2 = "initial_gps_setup_done_v2"
        private const val KEY_DELETED_CITY_IDS = "deleted_city_ids_v2"
        private const val KEY_DELETED_CITY_NAMES = "deleted_city_names_v1"
        private const val KEY_GITHUB_REPO_SLUG = "github_repo_slug_v1"
    }
}
