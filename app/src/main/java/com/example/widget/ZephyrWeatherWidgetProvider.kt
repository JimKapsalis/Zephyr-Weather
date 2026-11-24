package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import com.example.MainActivity
import com.example.R
import com.example.data.WeatherDatabase
import com.example.data.WeatherPreferences
import com.example.data.WeatherRepository
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.WeatherUnits
import com.example.model.WmoWeatherMapper
import com.example.util.SevereWeatherAlertEvent
import com.example.util.SevereWeatherNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget 1 (4x3): Master Clock, Hourly Forecast (4 columns) & Daily Forecast (4 columns) Home Screen Widget.
 */
class ZephyrWeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                syncAndPushWidgetState(context.applicationContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    companion object {
        @DrawableRes
        fun iconResForCode(code: Int, isDay: Boolean = true): Int = when (code) {
            0, 1 -> if (isDay) R.drawable.ic_widget_sun else R.drawable.ic_widget_moon
            2, 3 -> if (isDay) R.drawable.ic_widget_partly_sunny else R.drawable.ic_widget_moon
            45, 48 -> R.drawable.ic_widget_fog
            95, 96, 99 -> R.drawable.ic_widget_thunderstorm
            else -> R.drawable.ic_widget_rain
        }

        fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            return runCatching {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
                val provider = ComponentName(context, providerClass)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }.getOrDefault(false)
        }

        fun syncAndPushWidgetState(context: Context) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    val appContext = context.applicationContext
                    val prefs = WeatherPreferences(appContext)
                    val db = WeatherDatabase.getInstance(appContext)
                    val repo = WeatherRepository(db.cityDao())
                    val cities = repo.ensureDefaultCitiesSeeded(prefs)
                    val selectedId = prefs.loadSelectedCityId()
                    val activeCity = cities.find { it.id == selectedId }
                        ?: cities.firstOrNull()
                        ?: return@runCatching

                    val bundle = repo.fetchWeatherForCity(activeCity, prefs.loadProvider())
                    val units = prefs.loadUnits()
                    val lang = prefs.loadLanguage()
                    val alerts = SevereWeatherNotifier.evaluateAllActiveAlerts(
                        bundle = bundle,
                        units = units,
                        language = lang,
                        emyAlertsEnabled = prefs.loadEmyAlerts(),
                        rainAlertsEnabled = prefs.loadRainAlerts()
                    )
                    val primaryAlert = alerts.firstOrNull()
                    updateAllWidgetsFromBundle(
                        context = appContext,
                        bundle = bundle,
                        units = units,
                        language = lang,
                        alert = primaryAlert
                    )
                }
            }
        }

        private fun createLaunchPendingIntent(context: Context): PendingIntent {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
        }

        private fun localizeDayShort(raw: String, isGreek: Boolean, isToday: Boolean): String {
            if (isToday) return if (isGreek) "Σήμερα" else "Today"
            if (!isGreek) return raw
            return when (raw) {
                "Mon" -> "Δευ"
                "Tue" -> "Τρί"
                "Wed" -> "Τετ"
                "Thu" -> "Πέμ"
                "Fri" -> "Παρ"
                "Sat" -> "Σάβ"
                "Sun" -> "Κυρ"
                else -> raw
            }
        }

        fun updateAllWidgetsFromBundle(
            context: Context,
            bundle: CityWeatherBundle,
            units: WeatherUnits,
            language: AppLanguage,
            alert: SevereWeatherAlertEvent?
        ) {
            runCatching {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val pendingIntent = createLaunchPendingIntent(context)
                val isGreek = language == AppLanguage.GREEK
                val effectiveZone = bundle.timezoneId.takeIf {
                    it.isNotBlank() && !it.equals("auto", true) && !it.equals("GMT", true) && !it.equals("UTC", true)
                } ?: "Europe/Athens"
                val spec = WmoWeatherMapper.resolve(bundle.current.weatherCode, bundle.current.isDay)
                val currentIconRes = iconResForCode(bundle.current.weatherCode, isDay = bundle.current.isDay)

                val highLowLine = if (isGreek) {
                    "Μέγ:${units.formatTemp(bundle.current.highC, false)} Ελάχ:${units.formatTemp(bundle.current.lowC, false)}"
                } else {
                    "H:${units.formatTemp(bundle.current.highC, false)} L:${units.formatTemp(bundle.current.lowC, false)}"
                }

                // 1. Update Master 4x3 Widget (Clock + 4 Hourly + 4 Daily)
                val masterIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ZephyrWeatherWidgetProvider::class.java)
                )
                if (masterIds.isNotEmpty()) {
                    val hTimeIds = intArrayOf(
                        R.id.widget_hour_1_time,
                        R.id.widget_hour_2_time,
                        R.id.widget_hour_3_time,
                        R.id.widget_hour_4_time
                    )
                    val hIconIds = intArrayOf(
                        R.id.widget_hour_1_icon,
                        R.id.widget_hour_2_icon,
                        R.id.widget_hour_3_icon,
                        R.id.widget_hour_4_icon
                    )
                    val hTempIds = intArrayOf(
                        R.id.widget_hour_1_temp,
                        R.id.widget_hour_2_temp,
                        R.id.widget_hour_3_temp,
                        R.id.widget_hour_4_temp
                    )
                    val dNameIds = intArrayOf(
                        R.id.widget_day_1_name,
                        R.id.widget_day_2_name,
                        R.id.widget_day_3_name,
                        R.id.widget_day_4_name
                    )
                    val dIconIds = intArrayOf(
                        R.id.widget_day_1_icon,
                        R.id.widget_day_2_icon,
                        R.id.widget_day_3_icon,
                        R.id.widget_day_4_icon
                    )
                    val dTempIds = intArrayOf(
                        R.id.widget_day_1_temp,
                        R.id.widget_day_2_temp,
                        R.id.widget_day_3_temp,
                        R.id.widget_day_4_temp
                    )

                    for (widgetId in masterIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_zephyr_weather).apply {
                            setString(R.id.widget_live_clock, "setTimeZone", effectiveZone)
                            setString(R.id.widget_live_date, "setTimeZone", effectiveZone)
                            setTextViewText(R.id.widget_city_name, "📍 ${bundle.cityName}, ${bundle.countryName}")
                            setImageViewResource(R.id.widget_hero_icon, currentIconRes)
                            setTextViewText(R.id.widget_temp_text, units.formatTemp(bundle.current.tempC))
                            setTextViewText(R.id.widget_condition_text, spec.localizedLabel(language))
                            setTextViewText(R.id.widget_high_low_text, highLowLine)

                            bundle.hourly.take(4).forEachIndexed { idx, h ->
                                val hourLbl = if (idx == 0) {
                                    if (isGreek) "Τώρα" else "Now"
                                } else {
                                    h.displayHour
                                }
                                val isHourDay = h.isDay && (idx == 0 || !WmoWeatherMapper.isNightHourLabel(h.displayHour))
                                setTextViewText(hTimeIds[idx], hourLbl)
                                setImageViewResource(hIconIds[idx], iconResForCode(h.weatherCode, isDay = isHourDay))
                                setTextViewText(hTempIds[idx], units.formatTemp(h.tempC, false))
                            }

                            bundle.daily.take(4).forEachIndexed { idx, d ->
                                val dayLbl = localizeDayShort(d.dayLabel, isGreek, idx == 0)
                                setTextViewText(dNameIds[idx], dayLbl)
                                setImageViewResource(dIconIds[idx], iconResForCode(d.weatherCode, isDay = true))
                                setTextViewText(
                                    dTempIds[idx],
                                    "${units.formatTemp(d.maxTempC, false)}/${units.formatTemp(d.minTempC, false)}"
                                )
                            }

                            setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)
                        }
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }

                // 2. Update 4x2 Clock & 5-Hour Strip Widget
                val hourlyIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ZephyrHourlyClockWidgetProvider::class.java)
                )
                if (hourlyIds.isNotEmpty()) {
                    val slotTimeIds = intArrayOf(
                        R.id.hourly_slot_1_time,
                        R.id.hourly_slot_2_time,
                        R.id.hourly_slot_3_time,
                        R.id.hourly_slot_4_time,
                        R.id.hourly_slot_5_time
                    )
                    val slotIconIds = intArrayOf(
                        R.id.hourly_slot_1_icon,
                        R.id.hourly_slot_2_icon,
                        R.id.hourly_slot_3_icon,
                        R.id.hourly_slot_4_icon,
                        R.id.hourly_slot_5_icon
                    )
                    val slotTempIds = intArrayOf(
                        R.id.hourly_slot_1_temp,
                        R.id.hourly_slot_2_temp,
                        R.id.hourly_slot_3_temp,
                        R.id.hourly_slot_4_temp,
                        R.id.hourly_slot_5_temp
                    )

                    for (widgetId in hourlyIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_zephyr_hourly_clock).apply {
                            setString(R.id.hourly_widget_clock, "setTimeZone", effectiveZone)
                            setString(R.id.hourly_widget_date, "setTimeZone", effectiveZone)
                            setTextViewText(R.id.hourly_widget_city, "📍 ${bundle.cityName}, ${bundle.countryName}")
                            setImageViewResource(R.id.hourly_widget_current_icon, currentIconRes)
                            setTextViewText(R.id.hourly_widget_current_temp, units.formatTemp(bundle.current.tempC))
                            setTextViewText(
                                R.id.hourly_widget_condition,
                                "${spec.localizedLabel(language)} • $highLowLine"
                            )

                            bundle.hourly.take(5).forEachIndexed { idx, h ->
                                val hourLbl = if (idx == 0) {
                                    if (isGreek) "Τώρα" else "Now"
                                } else {
                                    h.displayHour
                                }
                                val isHourDay = h.isDay && (idx == 0 || !WmoWeatherMapper.isNightHourLabel(h.displayHour))
                                setTextViewText(slotTimeIds[idx], hourLbl)
                                setImageViewResource(slotIconIds[idx], iconResForCode(h.weatherCode, isDay = isHourDay))
                                setTextViewText(slotTempIds[idx], units.formatTemp(h.tempC, false))
                            }

                            setOnClickPendingIntent(R.id.hourly_widget_root, pendingIntent)
                        }
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }

                // 3. Update 4x2 5-Day Outlook Widget
                val dailyIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ZephyrDailyForecastWidgetProvider::class.java)
                )
                if (dailyIds.isNotEmpty()) {
                    val slotDayIds = intArrayOf(
                        R.id.daily_slot_1_day,
                        R.id.daily_slot_2_day,
                        R.id.daily_slot_3_day,
                        R.id.daily_slot_4_day,
                        R.id.daily_slot_5_day
                    )
                    val slotIconIds = intArrayOf(
                        R.id.daily_slot_1_icon,
                        R.id.daily_slot_2_icon,
                        R.id.daily_slot_3_icon,
                        R.id.daily_slot_4_icon,
                        R.id.daily_slot_5_icon
                    )
                    val slotTempIds = intArrayOf(
                        R.id.daily_slot_1_temp,
                        R.id.daily_slot_2_temp,
                        R.id.daily_slot_3_temp,
                        R.id.daily_slot_4_temp,
                        R.id.daily_slot_5_temp
                    )

                    for (widgetId in dailyIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_zephyr_daily_outlook).apply {
                            setString(R.id.daily_widget_clock, "setTimeZone", effectiveZone)
                            setString(R.id.daily_widget_date, "setTimeZone", effectiveZone)
                            setTextViewText(R.id.daily_widget_city, "📍 ${bundle.cityName}, ${bundle.countryName}")
                            setTextViewText(
                                R.id.daily_widget_condition,
                                "${spec.localizedLabel(language)} • $highLowLine"
                            )
                            setImageViewResource(R.id.daily_widget_current_icon, currentIconRes)
                            setTextViewText(R.id.daily_widget_current_temp, units.formatTemp(bundle.current.tempC))

                            bundle.daily.take(5).forEachIndexed { idx, d ->
                                val dayLbl = localizeDayShort(d.dayLabel, isGreek, idx == 0)
                                setTextViewText(slotDayIds[idx], dayLbl)
                                setImageViewResource(slotIconIds[idx], iconResForCode(d.weatherCode))
                                setTextViewText(
                                    slotTempIds[idx],
                                    "${units.formatTemp(d.maxTempC, false)}/${units.formatTemp(d.minTempC, false)}"
                                )
                            }

                            setOnClickPendingIntent(R.id.daily_widget_root, pendingIntent)
                        }
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }

                // 4. Update 2x2 Frosted Glass Time Widget
                val glassTimeIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ZephyrGlassTimeWidgetProvider::class.java)
                )
                if (glassTimeIds.isNotEmpty()) {
                    val currentHour = WeatherRepository.getCurrentHourInTimezone(effectiveZone)
                    val periodLabel = formatDayPeriodLabel(currentHour, isGreek)
                    val progressPercent = ((currentHour * 60 + 30) * 100 / 1440).coerceIn(15, 96)
                    for (widgetId in glassTimeIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_zephyr_glass_time).apply {
                            setString(R.id.glass_time_day_of_week, "setTimeZone", effectiveZone)
                            setString(R.id.glass_time_clock_digits, "setTimeZone", effectiveZone)
                            setString(R.id.glass_time_full_date, "setTimeZone", effectiveZone)
                            setTextViewText(R.id.glass_time_period_label, periodLabel)
                            setTextViewText(
                                R.id.glass_time_theme_subtitle,
                                if (isGreek) "Θέμα ${bundle.cityName}" else "Theme ${bundle.cityName}"
                            )
                            setProgressBar(R.id.glass_time_day_progress, 100, progressPercent, false)
                            setOnClickPendingIntent(R.id.glass_time_widget_root, pendingIntent)
                        }
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }

                // 5. Update 2x2 Frosted Glass Weather Widget
                val glassWeatherIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ZephyrGlassWeatherWidgetProvider::class.java)
                )
                if (glassWeatherIds.isNotEmpty()) {
                    val shortCondition = formatShortGlassCondition(bundle.current.weatherCode, isGreek)
                    val glassIconRes = if (bundle.current.weatherCode in 0..1 && bundle.current.isDay) {
                        R.drawable.ic_widget_white_sun
                    } else {
                        currentIconRes
                    }
                    val fullCardinal = formatFullCardinalDirection(bundle.current.windCardinal, isGreek)
                    val windCompact = formatCompactWindSpeed(bundle.current.windSpeedKmh, units)
                    for (widgetId in glassWeatherIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_zephyr_glass_weather).apply {
                            setTextViewText(R.id.glass_weather_city_name, bundle.cityName)
                            setTextViewText(R.id.glass_weather_temp, units.formatTemp(bundle.current.tempC, includeUnit = false))
                            setTextViewText(R.id.glass_weather_condition, shortCondition)
                            setImageViewResource(R.id.glass_weather_icon, glassIconRes)
                            setTextViewText(R.id.glass_weather_high, "↑ ${units.formatTemp(bundle.current.highC, includeUnit = false)}")
                            setTextViewText(R.id.glass_weather_low, "↓ ${units.formatTemp(bundle.current.lowC, includeUnit = false)}")
                            setTextViewText(R.id.glass_weather_aqi, "AQI: ${bundle.airQuality.usAqi}")
                            setTextViewText(R.id.glass_weather_wind_pill, "$fullCardinal,$windCompact")
                            setOnClickPendingIntent(R.id.glass_weather_widget_root, pendingIntent)
                        }
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }

        fun formatDayPeriodLabel(hourOfDay: Int, isGreek: Boolean): String = when (hourOfDay) {
            in 5..11 -> if (isGreek) "Πρωί" else "Morning"
            in 12..16 -> if (isGreek) "Απόγευμα" else "Afternoon"
            in 17..20 -> if (isGreek) "Βράδυ" else "Evening"
            else -> if (isGreek) "Νύχτα" else "Night"
        }

        fun formatShortGlassCondition(weatherCode: Int, isGreek: Boolean): String = when (weatherCode) {
            0, 1 -> if (isGreek) "Αίθριος" else "Clear"
            2 -> if (isGreek) "Λίγα Σύννεφα" else "Partly Cloudy"
            3 -> if (isGreek) "Συννεφιά" else "Cloudy"
            45, 48 -> if (isGreek) "Ομίχλη" else "Fog"
            in 51..67, in 80..82 -> if (isGreek) "Βροχή" else "Rain"
            in 71..77, 85, 86 -> if (isGreek) "Χιόνι" else "Snow"
            95, 96, 99 -> if (isGreek) "Καταιγίδα" else "Storm"
            else -> if (isGreek) "Αίθριος" else "Clear"
        }

        fun formatFullCardinalDirection(cardinal: String, isGreek: Boolean): String = when (cardinal.uppercase()) {
            "N" -> if (isGreek) "Βοριάς" else "North"
            "S" -> if (isGreek) "Νοτιάς" else "South"
            "E" -> if (isGreek) "Ανατολικός" else "East"
            "W" -> if (isGreek) "Δυτικός" else "West"
            "NE" -> if (isGreek) "ΒΑ" else "NorthEast"
            "NW" -> if (isGreek) "ΒΔ" else "NorthWest"
            "SE" -> if (isGreek) "ΝΑ" else "SouthEast"
            "SW" -> if (isGreek) "ΝΔ" else "SouthWest"
            else -> cardinal
        }

        fun formatCompactWindSpeed(speedKmh: Double, units: WeatherUnits): String {
            return if (units.useMph) {
                val mph = (speedKmh * 0.621371).toInt().coerceAtLeast(1)
                "${mph}mph"
            } else {
                val kmh = speedKmh.toInt().coerceAtLeast(1)
                "${kmh}km/h"
            }
        }
    }
}

/**
 * Widget 2 (4x2): Live Clock & 5-Hour Weather Strip Home Screen Widget.
 */
class ZephyrHourlyClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ZephyrWeatherWidgetProvider.syncAndPushWidgetState(context.applicationContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}

/**
 * Widget 3 (4x2): 5-Day Weather Outlook Home Screen Widget.
 */
class ZephyrDailyForecastWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ZephyrWeatherWidgetProvider.syncAndPushWidgetState(context.applicationContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}

/**
 * Widget 4 (2x2): Frosted Glass Time / Digital Clock Home Screen Widget.
 */
class ZephyrGlassTimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ZephyrWeatherWidgetProvider.syncAndPushWidgetState(context.applicationContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}

/**
 * Widget 5 (2x2): Frosted Glass Atmospheric Weather Home Screen Widget.
 */
class ZephyrGlassWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ZephyrWeatherWidgetProvider.syncAndPushWidgetState(context.applicationContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}
