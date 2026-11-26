package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppLanguage
import com.example.model.CityWeatherBundle
import com.example.model.WeatherUnits
import java.util.Locale
import kotlin.math.roundToInt

enum class SevereAlertSeverity {
    ADVISORY,
    WARNING,
    CRITICAL
}

enum class SevereAlertType {
    THUNDERSTORM,
    HEAVY_RAIN,
    GALE_WIND,
    RAIN_ONSET,
    ALL_CLEAR
}

data class SevereWeatherAlertEvent(
    val id: String,
    val type: SevereAlertType,
    val severity: SevereAlertSeverity,
    val title: String,
    val message: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val precipProbability: Int,
    val precipMm: Double,
    val windGustsKmh: Double,
    val onsetHourLabel: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
) {
    val isSevereOrRainActive: Boolean
        get() = type != SevereAlertType.ALL_CLEAR
}

object SevereWeatherNotifier {

    const val CHANNEL_SEVERE_ALERTS = "zephyr_severe_weather_alerts_v3"
    const val CHANNEL_RAIN_ALERTS = "zephyr_rain_onset_alerts_v3"

    private const val NOTIFICATION_ID_SEVERE = 4101
    private const val NOTIFICATION_ID_TEST = 4102

    private const val PREFS_ALERT_COOLDOWN = "zephyr_alert_cooldown_prefs"
    private const val KEY_LAST_ALERT_SIG = "last_alert_signature"
    private const val KEY_LAST_ALERT_TIME = "last_alert_epoch_ms"
    // Minimum 6 hours between automatic notifications for the same city/condition so phones are never spammed
    private const val AUTOMATIC_ALERT_COOLDOWN_MS = 6 * 60 * 60 * 1000L

    private var lastDispatchedSignature: String? = null
    private var lastDispatchedEpochMs: Long = 0L

    fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val appNotificationsEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)
        return permissionGranted && appNotificationsEnabled
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val severeChannel = NotificationChannel(
                CHANNEL_SEVERE_ALERTS,
                "Severe Weather & Thunderstorm Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Important alerts only for severe thunderstorms, heavy downpours, and gale-force winds."
                enableVibration(false)
                enableLights(true)
            }

            val rainChannel = NotificationChannel(
                CHANNEL_RAIN_ALERTS,
                "Imminent Heavy Rain Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quiet notifications when significant rain is imminent at your location."
                enableVibration(false)
                setSound(null, null)
            }

            manager.createNotificationChannel(severeChannel)
            manager.createNotificationChannel(rainChannel)
        }
    }

    /**
     * Evaluates the live [CityWeatherBundle] strictly for truly important weather events
     * (Thunderstorms, Heavy Downpours >= 4.5 mm/h, Severe Gale Winds >= 50 km/h, or high-confidence imminent rain).
     */
    fun evaluateBundleForAlerts(
        bundle: CityWeatherBundle,
        units: WeatherUnits,
        language: AppLanguage,
        emyAlertsEnabled: Boolean = true,
        rainAlertsEnabled: Boolean = true
    ): SevereWeatherAlertEvent {
        val isGreek = language == AppLanguage.GREEK
        val current = bundle.current
        val next3Hours = bundle.hourly.take(3)

        val thunderCodes = setOf(95, 96, 99)
        val heavyRainCodes = setOf(65, 67, 75, 82, 86)
        val significantRainCodes = setOf(63, 65, 67, 73, 75, 81, 82, 86, 95, 96, 99)

        val upcomingThunder = next3Hours.firstOrNull {
            it.weatherCode in thunderCodes && it.precipProbability >= 60
        }
        val isThunderNow = current.weatherCode in thunderCodes

        val upcomingHeavyRain = next3Hours.firstOrNull {
            (it.weatherCode in heavyRainCodes && it.precipProbability >= 75) ||
                (it.precipMm >= 4.5 && it.precipProbability >= 75)
        }
        val isHeavyRainNow = current.weatherCode in heavyRainCodes || current.precipitationMm >= 4.5

        val upcomingSignificantRain = next3Hours.take(2).firstOrNull {
            it.weatherCode in significantRainCodes && it.precipProbability >= 78 && it.precipMm >= 2.0
        }
        val isSignificantRainNow = current.weatherCode in significantRainCodes && current.precipitationMm >= 2.0

        val peakGusts = maxOf(
            current.windGustsKmh,
            next3Hours.maxOfOrNull { it.windGustsKmh } ?: current.windGustsKmh
        )
        // Only trigger Gale Warning for true Beaufort 7+ gale winds (>= 50 km/h sustained or >= 65 km/h gusts)
        val isGaleWarning = current.windSpeedKmh >= 50.0 || peakGusts >= 65.0

        val peakProb = next3Hours.maxOfOrNull { it.precipProbability } ?: 0
        val peakMm = maxOf(
            current.precipitationMm,
            next3Hours.maxOfOrNull { it.precipMm } ?: 0.0
        )

        // 1. Thunderstorm Alert (Critical)
        if (emyAlertsEnabled && (isThunderNow || upcomingThunder != null)) {
            val onset = if (isThunderNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingThunder?.displayHour ?: "Soon"
            }
            val prob = upcomingThunder?.precipProbability ?: maxOf(peakProb, 85)
            val title = if (isGreek) {
                "⚡ Προειδοποίηση Καταιγίδας • ${bundle.cityName}"
            } else {
                "⚡ Severe Thunderstorm Alert • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Ισχυρή καταιγίδα ($prob% πιθανότητα, ριπές ${units.formatWind(peakGusts)}) για ${bundle.cityName} ($onset)."
            } else {
                "Severe thunderstorm ($onset • $prob% chance, gusts ${units.formatWind(peakGusts)}) near ${bundle.cityName}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_thunder",
                type = SevereAlertType.THUNDERSTORM,
                severity = SevereAlertSeverity.CRITICAL,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = prob,
                precipMm = peakMm.coerceAtLeast(4.5),
                windGustsKmh = peakGusts,
                onsetHourLabel = onset
            )
        }

        // 2. Heavy Rain / Downpour Alert (Warning)
        if ((emyAlertsEnabled || rainAlertsEnabled) && (isHeavyRainNow || upcomingHeavyRain != null)) {
            val onset = if (isHeavyRainNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingHeavyRain?.displayHour ?: "Soon"
            }
            val prob = upcomingHeavyRain?.precipProbability ?: maxOf(peakProb, 80)
            val mmFormatted = String.format(Locale.US, "%.1f mm", peakMm.coerceAtLeast(4.5))
            val title = if (isGreek) {
                "🌧️ Έντονη Βροχόπτωση • ${bundle.cityName}"
            } else {
                "🌧️ Heavy Rain Warning • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Ισχυρή βροχόπτωση ($prob% πιθανότητα, $mmFormatted) στην περιοχή ${bundle.cityName} ($onset)."
            } else {
                "Heavy rainfall ($prob% chance, $mmFormatted) approaching ${bundle.cityName} ($onset)."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_heavy_rain",
                type = SevereAlertType.HEAVY_RAIN,
                severity = SevereAlertSeverity.WARNING,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = prob,
                precipMm = peakMm.coerceAtLeast(4.5),
                windGustsKmh = peakGusts,
                onsetHourLabel = onset
            )
        }

        // 3. Severe Gale Wind Alert (Warning)
        if (emyAlertsEnabled && isGaleWarning) {
            val title = if (isGreek) {
                "💨 Θυελλώδεις Άνεμοι ΕΜΥ • ${bundle.cityName}"
            } else {
                "💨 Severe Gale Wind Warning • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Θυελλώδεις άνεμοι ${units.formatWind(current.windSpeedKmh)} (${current.windCardinal}) με ριπές έως ${units.formatWind(peakGusts)}."
            } else {
                "Gale-force ${current.windCardinal} winds at ${units.formatWind(current.windSpeedKmh)} with gusts up to ${units.formatWind(peakGusts)}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_gale",
                type = SevereAlertType.GALE_WIND,
                severity = SevereAlertSeverity.WARNING,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = peakProb,
                precipMm = peakMm,
                windGustsKmh = peakGusts,
                onsetHourLabel = if (isGreek) "Τώρα" else "Now"
            )
        }

        // 4. Significant Imminent Rain Onset (>= 78% prob and >= 2.0 mm within 2 hours)
        if (rainAlertsEnabled && (isSignificantRainNow || upcomingSignificantRain != null)) {
            val onset = if (isSignificantRainNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingSignificantRain?.displayHour ?: "Soon"
            }
            val prob = upcomingSignificantRain?.precipProbability ?: maxOf(peakProb, 78)
            val title = if (isGreek) {
                "☔ Έναρξη Βροχής • ${bundle.cityName}"
            } else {
                "☔ Rain Starting Soon • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Αξιόλογη βροχή ($prob% πιθανότητα) αναμένεται στις $onset στην τοποθεσία ${bundle.cityName}."
            } else {
                "Steady rain ($prob% probability) expected around $onset in ${bundle.cityName}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_rain_onset",
                type = SevereAlertType.RAIN_ONSET,
                severity = SevereAlertSeverity.ADVISORY,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = prob,
                precipMm = peakMm.coerceAtLeast(2.0),
                windGustsKmh = peakGusts,
                onsetHourLabel = onset
            )
        }

        // 5. All Clear (Never triggers a push notification)
        val clearTitle = if (isGreek) {
            "Καμία Προειδοποίηση Καιρού • ${bundle.cityName}"
        } else {
            "No Severe Weather Alerts • ${bundle.cityName}"
        }
        val clearMsg = if (isGreek) {
            "Δεν υπάρχουν ακραία καιρικά φαινόμενα (${units.formatTemp(current.tempC)})."
        } else {
            "No severe weather threats detected (${units.formatTemp(current.tempC)})."
        }
        return SevereWeatherAlertEvent(
            id = "${bundle.cityId}_clear",
            type = SevereAlertType.ALL_CLEAR,
            severity = SevereAlertSeverity.ADVISORY,
            title = clearTitle,
            message = clearMsg,
            cityName = bundle.cityName,
            latitude = bundle.latitude,
            longitude = bundle.longitude,
            precipProbability = peakProb,
            precipMm = peakMm,
            windGustsKmh = peakGusts,
            onsetHourLabel = "--"
        )
    }

    /**
     * Posts a system notification ONLY if [alert] is a genuinely important severe weather event
     * (or if [forceDispatch] is true when the user manually taps the "Test Alert" button),
     * enforcing a persistent 6-hour deduplication cooldown in SharedPreferences so phones are never spammed.
     */
    fun dispatchSystemNotificationIfPermitted(
        context: Context,
        alert: SevereWeatherAlertEvent,
        forceDispatch: Boolean = false
    ): Boolean {
        ensureChannelsCreated(context)

        if (!canPostNotifications(context)) {
            return false
        }

        if (!forceDispatch) {
            // Only send automatic notifications for genuinely important weather:
            // CRITICAL (Thunderstorms), WARNING (Heavy Downpours >= 4.5mm/h or Gale Winds >= 50km/h),
            // or high-certainty significant rain (>= 80% & >= 2.2mm)
            val isImportantAlert = alert.severity == SevereAlertSeverity.CRITICAL ||
                alert.severity == SevereAlertSeverity.WARNING ||
                (alert.type == SevereAlertType.RAIN_ONSET && alert.precipProbability >= 80 && alert.precipMm >= 2.2)
            if (!alert.isSevereOrRainActive || !isImportantAlert) {
                return false
            }

            val now = System.currentTimeMillis()
            val prefs = runCatching {
                context.getSharedPreferences(PREFS_ALERT_COOLDOWN, Context.MODE_PRIVATE)
            }.getOrNull()
            val savedSig = prefs?.getString(KEY_LAST_ALERT_SIG, null) ?: lastDispatchedSignature
            val savedTime = prefs?.getLong(KEY_LAST_ALERT_TIME, 0L) ?: lastDispatchedEpochMs
            val canonicalSig = "${alert.cityName}_${alert.type.name}"

            // Never spam: enforce a strict 6-hour cooldown for the same city & alert type
            if (canonicalSig == savedSig && (now - savedTime) < AUTOMATIC_ALERT_COOLDOWN_MS) {
                return false
            }

            lastDispatchedSignature = canonicalSig
            lastDispatchedEpochMs = now
            prefs?.edit()
                ?.putString(KEY_LAST_ALERT_SIG, canonicalSig)
                ?.putLong(KEY_LAST_ALERT_TIME, now)
                ?.apply()
        }

        val channelId = when (alert.type) {
            SevereAlertType.THUNDERSTORM,
            SevereAlertType.HEAVY_RAIN,
            SevereAlertType.GALE_WIND -> CHANNEL_SEVERE_ALERTS
            SevereAlertType.RAIN_ONSET,
            SevereAlertType.ALL_CLEAR -> CHANNEL_RAIN_ALERTS
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)

        val priority = when (alert.severity) {
            SevereAlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_HIGH
            SevereAlertSeverity.WARNING -> NotificationCompat.PRIORITY_DEFAULT
            SevereAlertSeverity.ADVISORY -> NotificationCompat.PRIORITY_LOW
        }

        val smallIconRes = when (alert.type) {
            SevereAlertType.THUNDERSTORM -> R.drawable.ic_widget_thunderstorm
            SevereAlertType.ALL_CLEAR -> R.drawable.ic_widget_partly_sunny
            else -> R.drawable.ic_widget_rain
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(!forceDispatch && alert.severity == SevereAlertSeverity.ADVISORY)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return runCatching {
            val notifId = if (forceDispatch) NOTIFICATION_ID_TEST else NOTIFICATION_ID_SEVERE
            val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (sysManager != null) {
                sysManager.notify(notifId, notification)
                true
            } else if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(notifId, notification)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Builds a simulated severe thunderstorm / heavy rain alert for the user's selected
     * station so the user can test and verify real-time push notifications on demand.
     */
    fun buildLiveTestAlert(
        bundle: CityWeatherBundle?,
        units: WeatherUnits,
        language: AppLanguage
    ): SevereWeatherAlertEvent {
        val isGreek = language == AppLanguage.GREEK
        val city = bundle?.cityName ?: if (isGreek) "Τρέχουσα Τοποθεσία" else "Current Location"
        val lat = bundle?.latitude ?: 38.0
        val lon = bundle?.longitude ?: 23.0
        val prob = maxOf(bundle?.rainOutlook?.peakPrecipProbability ?: 0, 88)
        val gusts = maxOf(bundle?.current?.windGustsKmh ?: 0.0, 62.0)

        val title = if (isGreek) {
            "⚡ Προειδοποίηση Καταιγίδας & Ισχυρής Βροχής • $city"
        } else {
            "⚡ Severe Thunderstorm & Heavy Rain • $city"
        }
        val message = if (isGreek) {
            "Ζωντανή πρόγνωση για $city: Ισχυρή βροχόπτωση ($prob% πιθανότητα, 6.4 mm/h) και καταιγίδα με ριπές ανέμου ${units.formatWind(gusts)}."
        } else {
            "Live update for $city: Heavy downpour ($prob% probability, 6.4 mm/h) and thunderstorm activity with gusts up to ${units.formatWind(gusts)}."
        }

        return SevereWeatherAlertEvent(
            id = "test_severe_thunder_${bundle?.cityId ?: 258576L}",
            type = SevereAlertType.THUNDERSTORM,
            severity = SevereAlertSeverity.CRITICAL,
            title = title,
            message = message,
            cityName = city,
            latitude = lat,
            longitude = lon,
            precipProbability = prob,
            precipMm = 6.4,
            windGustsKmh = gusts,
            onsetHourLabel = if (isGreek) "Σε 15 λεπτά" else "In 15 min"
        )
    }

    /**
     * Evaluates the bundle and returns ONLY genuine severe or significant active weather alerts
     * without injecting any artificial filler advisories when conditions are calm.
     */
    fun evaluateAllActiveAlerts(
        bundle: CityWeatherBundle,
        units: WeatherUnits,
        language: AppLanguage,
        emyAlertsEnabled: Boolean = true,
        rainAlertsEnabled: Boolean = true
    ): List<SevereWeatherAlertEvent> {
        val isGreek = language == AppLanguage.GREEK
        val current = bundle.current
        val next3Hours = bundle.hourly.take(3)
        val alerts = mutableListOf<SevereWeatherAlertEvent>()

        val thunderCodes = setOf(95, 96, 99)
        val heavyRainCodes = setOf(65, 67, 75, 82, 86)
        val significantRainCodes = setOf(63, 65, 67, 73, 75, 81, 82, 86, 95, 96, 99)

        val upcomingThunder = next3Hours.firstOrNull {
            it.weatherCode in thunderCodes && it.precipProbability >= 60
        }
        val isThunderNow = current.weatherCode in thunderCodes

        val upcomingHeavyRain = next3Hours.firstOrNull {
            (it.weatherCode in heavyRainCodes && it.precipProbability >= 75) ||
                (it.precipMm >= 4.5 && it.precipProbability >= 75)
        }
        val isHeavyRainNow = current.weatherCode in heavyRainCodes || current.precipitationMm >= 4.5

        val upcomingSignificantRain = next3Hours.take(2).firstOrNull {
            it.weatherCode in significantRainCodes && it.precipProbability >= 78 && it.precipMm >= 2.0
        }
        val isSignificantRainNow = current.weatherCode in significantRainCodes && current.precipitationMm >= 2.0

        val peakGusts = maxOf(
            current.windGustsKmh,
            next3Hours.maxOfOrNull { it.windGustsKmh } ?: current.windGustsKmh
        )
        val isGaleWarning = current.windSpeedKmh >= 50.0 || peakGusts >= 65.0

        val peakProb = next3Hours.maxOfOrNull { it.precipProbability } ?: 0
        val peakMm = maxOf(
            current.precipitationMm,
            next3Hours.maxOfOrNull { it.precipMm } ?: 0.0
        )

        if (emyAlertsEnabled && (isThunderNow || upcomingThunder != null)) {
            val onset = if (isThunderNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingThunder?.displayHour ?: "Soon"
            }
            val prob = upcomingThunder?.precipProbability ?: maxOf(peakProb, 85)
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_thunder",
                    type = SevereAlertType.THUNDERSTORM,
                    severity = SevereAlertSeverity.CRITICAL,
                    title = if (isGreek) "⚡ Προειδοποίηση Καταιγίδας • ${bundle.cityName}" else "⚡ Severe Thunderstorm • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Ισχυρή καταιγίδα ($prob% πιθανότητα, ριπές ${units.formatWind(peakGusts)}) στην περιοχή σας ($onset)."
                    } else {
                        "Severe thunderstorm detected near ${bundle.cityName} ($onset • $prob% chance, gusts ${units.formatWind(peakGusts)})."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = prob,
                    precipMm = peakMm.coerceAtLeast(4.5),
                    windGustsKmh = peakGusts,
                    onsetHourLabel = onset
                )
            )
        }

        if ((emyAlertsEnabled || rainAlertsEnabled) && (isHeavyRainNow || upcomingHeavyRain != null)) {
            val onset = if (isHeavyRainNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingHeavyRain?.displayHour ?: "Soon"
            }
            val prob = upcomingHeavyRain?.precipProbability ?: maxOf(peakProb, 80)
            val mmFormatted = String.format(Locale.US, "%.1f mm", peakMm.coerceAtLeast(4.5))
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_heavy_rain",
                    type = SevereAlertType.HEAVY_RAIN,
                    severity = SevereAlertSeverity.WARNING,
                    title = if (isGreek) "🌧️ Έντονη Βροχόπτωση • ${bundle.cityName}" else "🌧️ Heavy Rain Warning • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Ισχυρή βροχόπτωση ($prob% πιθανότητα, $mmFormatted) στην περιοχή σας ($onset)."
                    } else {
                        "Heavy rainfall ($prob% chance, $mmFormatted) approaching ${bundle.cityName} ($onset)."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = prob,
                    precipMm = peakMm.coerceAtLeast(4.5),
                    windGustsKmh = peakGusts,
                    onsetHourLabel = onset
                )
            )
        } else if (rainAlertsEnabled && (isSignificantRainNow || upcomingSignificantRain != null)) {
            val onset = if (isSignificantRainNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingSignificantRain?.displayHour ?: "Soon"
            }
            val prob = upcomingSignificantRain?.precipProbability ?: maxOf(peakProb, 78)
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_rain_onset",
                    type = SevereAlertType.RAIN_ONSET,
                    severity = SevereAlertSeverity.ADVISORY,
                    title = if (isGreek) "☔ Έναρξη Βροχής • ${bundle.cityName}" else "☔ Rain Onset Advisory • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Αξιόλογη βροχή ($prob% πιθανότητα) αναμένεται στις $onset στην τοποθεσία σας."
                    } else {
                        "Steady rain ($prob% probability) expected around $onset in ${bundle.cityName}."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = prob,
                    precipMm = peakMm.coerceAtLeast(2.0),
                    windGustsKmh = peakGusts,
                    onsetHourLabel = onset
                )
            )
        }

        if (emyAlertsEnabled && isGaleWarning) {
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_gale_wind",
                    type = SevereAlertType.GALE_WIND,
                    severity = SevereAlertSeverity.WARNING,
                    title = if (isGreek) "💨 Θυελλώδεις Άνεμοι ΕΜΥ • ${bundle.cityName}" else "💨 Severe Gale Wind Warning • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Θυελλώδεις άνεμοι ${units.formatWind(current.windSpeedKmh)} (${current.windCardinal}) με ριπές έως ${units.formatWind(peakGusts)}."
                    } else {
                        "Gale-force ${current.windCardinal} winds at ${units.formatWind(current.windSpeedKmh)} with gusts reaching ${units.formatWind(peakGusts)}."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = peakProb,
                    precipMm = peakMm,
                    windGustsKmh = peakGusts,
                    onsetHourLabel = if (isGreek) "Τώρα" else "Now"
                )
            )
        }

        return alerts
    }

    /**
     * Builds a set of distinct severe weather alert cards (Thunderstorm Warning + Heavy Rain Downpour)
     * so users can test swiping horizontally one-by-one and dismissing individual cards on the dashboard.
     */
    fun buildLiveTestAlertList(
        bundle: CityWeatherBundle?,
        units: WeatherUnits,
        language: AppLanguage
    ): List<SevereWeatherAlertEvent> {
        val isGreek = language == AppLanguage.GREEK
        val city = bundle?.cityName ?: if (isGreek) "Τρέχουσα Τοποθεσία" else "Current Location"
        val cityId = bundle?.cityId ?: 0L
        val lat = bundle?.latitude ?: 38.0
        val lon = bundle?.longitude ?: 23.0
        val prob = maxOf(bundle?.rainOutlook?.peakPrecipProbability ?: 0, 88)
        val gusts = maxOf(bundle?.current?.windGustsKmh ?: 0.0, 62.0)

        val thunderAlert = buildLiveTestAlert(bundle, units, language)
        val heavyRainAlert = SevereWeatherAlertEvent(
            id = "test_heavy_rain_$cityId",
            type = SevereAlertType.HEAVY_RAIN,
            severity = SevereAlertSeverity.WARNING,
            title = if (isGreek) "🌧️ Έντονη Βροχόπτωση • $city" else "🌧️ Heavy Rain Downpour • $city",
            message = if (isGreek) {
                "Ισχυρή ζώνη βροχής ($prob% πιθανότητα, 5.2 mm/h) κινείται προς $city. Περιορισμένη ορατότητα στους δρόμους."
            } else {
                "Intense rain band ($prob% probability, 5.2 mm/h) moving across $city. Expect wet roads and reduced visibility."
            },
            cityName = city,
            latitude = lat,
            longitude = lon,
            precipProbability = prob,
            precipMm = 5.2,
            windGustsKmh = gusts * 0.85,
            onsetHourLabel = if (isGreek) "Σε 30 λεπτά" else "In 30 min"
        )

        return listOf(thunderAlert, heavyRainAlert)
    }
}
