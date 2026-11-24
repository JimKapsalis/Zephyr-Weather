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

            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val severeChannel = NotificationChannel(
                CHANNEL_SEVERE_ALERTS,
                "Severe Weather & Thunderstorm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time GPS alerts for thunderstorms, heavy rain downpours, and EMY gale warnings."
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 300L, 150L, 300L)
                enableLights(true)
                if (defaultSoundUri != null) {
                    setSound(defaultSoundUri, audioAttributes)
                }
            }

            val rainChannel = NotificationChannel(
                CHANNEL_RAIN_ALERTS,
                "Imminent Rain & Weather Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notifications when rain or severe weather is detected at your GPS coordinates."
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 250L, 120L, 250L)
                enableLights(true)
                if (defaultSoundUri != null) {
                    setSound(defaultSoundUri, audioAttributes)
                }
            }

            manager.createNotificationChannel(severeChannel)
            manager.createNotificationChannel(rainChannel)
        }
    }

    /**
     * Evaluates the live [CityWeatherBundle] fetched for the user's GPS coordinates
     * to detect active or approaching thunderstorms, heavy rain, gale winds, or rain onset.
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
        val next6Hours = bundle.hourly.take(6)

        val thunderCodes = setOf(95, 96, 99)
        val heavyRainCodes = setOf(63, 65, 67, 81, 82)
        val anyRainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)

        val upcomingThunder = next6Hours.firstOrNull { it.weatherCode in thunderCodes }
        val isThunderNow = current.weatherCode in thunderCodes

        val upcomingHeavyRain = next6Hours.firstOrNull {
            it.weatherCode in heavyRainCodes || it.precipMm >= 3.5 || it.precipProbability >= 70
        }
        val isHeavyRainNow = current.weatherCode in heavyRainCodes || current.precipitationMm >= 3.5

        val upcomingRain = next6Hours.firstOrNull {
            it.weatherCode in anyRainCodes || it.precipProbability >= 45 || it.precipMm >= 0.4
        }
        val isRainingNow = current.weatherCode in anyRainCodes || current.precipitationMm >= 0.2

        val peakGusts = maxOf(
            current.windGustsKmh,
            next6Hours.maxOfOrNull { it.windSpeedKmh * 1.25 } ?: current.windSpeedKmh
        )
        val isGaleWarning = current.windSpeedKmh >= 42.0 || peakGusts >= 55.0

        val peakProb = maxOf(
            next6Hours.maxOfOrNull { it.precipProbability } ?: 0,
            bundle.rainOutlook.peakPrecipProbability
        )
        val peakMm = maxOf(
            current.precipitationMm,
            next6Hours.maxOfOrNull { it.precipMm } ?: 0.0
        )

        // 1. Thunderstorm Alert (Highest Priority)
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
                "Ανιχνεύθηκε ηλεκτρική δραστηριότητα και ισχυρή βροχή ($prob% πιθανότητα, ριπές ${units.formatWind(peakGusts)}) για την τοποθεσία σας ($onset). Αποφύγετε ανοιχτούς χώρους."
            } else {
                "Thunderstorm cell detected near your GPS location ($onset • $prob% rain chance, gusts ${units.formatWind(peakGusts)}). Seek indoor shelter."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_thunder_$onset",
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

        // 2. Heavy Rain / Downpour Alert
        if ((emyAlertsEnabled || rainAlertsEnabled) && (isHeavyRainNow || upcomingHeavyRain != null)) {
            val onset = if (isHeavyRainNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingHeavyRain?.displayHour ?: "Soon"
            }
            val prob = upcomingHeavyRain?.precipProbability ?: maxOf(peakProb, 75)
            val mmFormatted = String.format(Locale.US, "%.1f mm", peakMm.coerceAtLeast(3.8))
            val title = if (isGreek) {
                "🌧️ Έντονη Βροχόπτωση • ${bundle.cityName}"
            } else {
                "🌧️ Heavy Rain Warning • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Ισχυρή βροχόπτωση ($prob% πιθανότητα, $mmFormatted) στην τοποθεσία GPS σας ($onset). Άνεμος ${units.formatWind(current.windSpeedKmh)}."
            } else {
                "Heavy rainfall ($prob% chance, $mmFormatted) approaching your GPS location ($onset). Sustained wind ${units.formatWind(current.windSpeedKmh)}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_heavy_rain_$onset",
                type = SevereAlertType.HEAVY_RAIN,
                severity = SevereAlertSeverity.WARNING,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = prob,
                precipMm = peakMm.coerceAtLeast(3.8),
                windGustsKmh = peakGusts,
                onsetHourLabel = onset
            )
        }

        // 3. Gale Wind Alert
        if (emyAlertsEnabled && isGaleWarning) {
            val title = if (isGreek) {
                "💨 Θυελλώδεις Άνεμοι ΕΜΥ • ${bundle.cityName}"
            } else {
                "💨 High Wind & Gale Advisory • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Ισχυροί άνεμοι ${units.formatWind(current.windSpeedKmh)} (${current.windCardinal}) με ριπές έως ${units.formatWind(peakGusts)} στην περιοχή σας."
            } else {
                "Strong ${current.windCardinal} winds at ${units.formatWind(current.windSpeedKmh)} with gusts reaching ${units.formatWind(peakGusts)}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_gale_${current.windSpeedKmh.roundToInt()}",
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

        // 4. Imminent Rain Onset Notification
        if (rainAlertsEnabled && (isRainingNow || upcomingRain != null)) {
            val onset = if (isRainingNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingRain?.displayHour ?: "Soon"
            }
            val prob = upcomingRain?.precipProbability ?: maxOf(peakProb, 50)
            val title = if (isGreek) {
                "☔ Ειδοποίηση Βροχής • ${bundle.cityName}"
            } else {
                "☔ Rain Approaching • ${bundle.cityName}"
            }
            val msg = if (isGreek) {
                "Αναμένεται βροχή ($prob% πιθανότητα) γύρω στις $onset στην τοποθεσία σας. Θερμοκρασία ${units.formatTemp(current.tempC)}."
            } else {
                "Light rain expected ($prob% probability) around $onset at your location. Current temp ${units.formatTemp(current.tempC)}."
            }
            return SevereWeatherAlertEvent(
                id = "${bundle.cityId}_rain_$onset",
                type = SevereAlertType.RAIN_ONSET,
                severity = SevereAlertSeverity.ADVISORY,
                title = title,
                message = msg,
                cityName = bundle.cityName,
                latitude = bundle.latitude,
                longitude = bundle.longitude,
                precipProbability = prob,
                precipMm = peakMm.coerceAtLeast(0.8),
                windGustsKmh = peakGusts,
                onsetHourLabel = onset
            )
        }

        // 5. All Clear / Active Weather Monitoring Status
        val clearTitle = if (isGreek) {
            "Ζωντανή Παρακολούθηση Καιρού • ${bundle.cityName}"
        } else {
            "Live Weather Guard Active • ${bundle.cityName}"
        }
        val clearMsg = if (isGreek) {
            "Δεν υπάρχουν ακραία φαινόμενα (${units.formatTemp(current.tempC)}). Μέγιστη πιθανότητα βροχής ${peakProb}% • Άνεμος ${units.formatWind(current.windSpeedKmh)}."
        } else {
            "No severe weather threats detected (${units.formatTemp(current.tempC)}). Peak rain chance ${peakProb}% • Wind ${units.formatWind(current.windSpeedKmh)}."
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
     * Posts a real Android system notification if permission is granted and the alert is new
     * (or if [forceDispatch] is true, such as when the user triggers a live test alert).
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
            if (!alert.isSevereOrRainActive) return false
            val now = System.currentTimeMillis()
            if (alert.id == lastDispatchedSignature && (now - lastDispatchedEpochMs) < 15 * 60 * 1000L) {
                return false
            }
            lastDispatchedSignature = alert.id
            lastDispatchedEpochMs = now
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

        val priority = if (forceDispatch || alert.severity == SevereAlertSeverity.CRITICAL) {
            NotificationCompat.PRIORITY_MAX
        } else {
            when (alert.severity) {
                SevereAlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
                SevereAlertSeverity.WARNING -> NotificationCompat.PRIORITY_HIGH
                SevereAlertSeverity.ADVISORY -> NotificationCompat.PRIORITY_HIGH
            }
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0L, 300L, 150L, 300L))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(!forceDispatch)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return runCatching {
            val notifId = if (forceDispatch) NOTIFICATION_ID_TEST else NOTIFICATION_ID_SEVERE
            val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (forceDispatch) {
                // Cancel first so Android 13-17 always pops up a fresh heads-up banner
                sysManager?.cancel(notifId)
            }
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
     * Evaluates the bundle and returns all distinct active weather alerts as a list of
     * individual alert events (e.g., Thunderstorm + Heavy Rain + High Wind) so each can
     * be rendered as a distinct, independently dismissible card on the dashboard.
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
        val next6Hours = bundle.hourly.take(6)
        val alerts = mutableListOf<SevereWeatherAlertEvent>()

        val thunderCodes = setOf(95, 96, 99)
        val heavyRainCodes = setOf(63, 65, 67, 81, 82)
        val anyRainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)

        val upcomingThunder = next6Hours.firstOrNull { it.weatherCode in thunderCodes }
        val isThunderNow = current.weatherCode in thunderCodes

        val upcomingHeavyRain = next6Hours.firstOrNull {
            it.weatherCode in heavyRainCodes || it.precipMm >= 3.5 || it.precipProbability >= 70
        }
        val isHeavyRainNow = current.weatherCode in heavyRainCodes || current.precipitationMm >= 3.5

        val upcomingRain = next6Hours.firstOrNull {
            it.weatherCode in anyRainCodes || it.precipProbability >= 45 || it.precipMm >= 0.4
        }
        val isRainingNow = current.weatherCode in anyRainCodes || current.precipitationMm >= 0.2

        val peakGusts = maxOf(
            current.windGustsKmh,
            next6Hours.maxOfOrNull { it.windSpeedKmh * 1.25 } ?: current.windSpeedKmh
        )
        val isGaleWarning = current.windSpeedKmh >= 38.0 || peakGusts >= 50.0

        val peakProb = maxOf(
            next6Hours.maxOfOrNull { it.precipProbability } ?: 0,
            bundle.rainOutlook.peakPrecipProbability
        )
        val peakMm = maxOf(
            current.precipitationMm,
            next6Hours.maxOfOrNull { it.precipMm } ?: 0.0
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
                        "Ηλεκτρική δραστηριότητα και ισχυρή καταιγίδα ($prob% πιθανότητα, ριπές ${units.formatWind(peakGusts)}) στην περιοχή σας ($onset)."
                    } else {
                        "Thunderstorm cell detected near ${bundle.cityName} ($onset • $prob% rain chance, gusts ${units.formatWind(peakGusts)}). Seek indoor shelter."
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
            val prob = upcomingHeavyRain?.precipProbability ?: maxOf(peakProb, 75)
            val mmFormatted = String.format(Locale.US, "%.1f mm", peakMm.coerceAtLeast(3.8))
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_heavy_rain",
                    type = SevereAlertType.HEAVY_RAIN,
                    severity = SevereAlertSeverity.WARNING,
                    title = if (isGreek) "🌧️ Έντονη Βροχόπτωση • ${bundle.cityName}" else "🌧️ Heavy Rain Warning • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Ισχυρή βροχόπτωση ($prob% πιθανότητα, $mmFormatted) στην περιοχή σας ($onset). Άνεμος ${units.formatWind(current.windSpeedKmh)}."
                    } else {
                        "Heavy rainfall ($prob% chance, $mmFormatted) approaching ${bundle.cityName} ($onset). Sustained wind ${units.formatWind(current.windSpeedKmh)}."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = prob,
                    precipMm = peakMm.coerceAtLeast(3.8),
                    windGustsKmh = peakGusts,
                    onsetHourLabel = onset
                )
            )
        } else if (rainAlertsEnabled && (isRainingNow || upcomingRain != null)) {
            val onset = if (isRainingNow) {
                if (isGreek) "Τώρα" else "Right Now"
            } else {
                upcomingRain?.displayHour ?: "Soon"
            }
            val prob = upcomingRain?.precipProbability ?: maxOf(peakProb, 50)
            alerts.add(
                SevereWeatherAlertEvent(
                    id = "${bundle.cityId}_rain_onset",
                    type = SevereAlertType.RAIN_ONSET,
                    severity = SevereAlertSeverity.ADVISORY,
                    title = if (isGreek) "☔ Έναρξη Βροχής • ${bundle.cityName}" else "☔ Rain Onset Advisory • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Αναμένεται βροχή ($prob% πιθανότητα) γύρω στις $onset στην τοποθεσία σας. Θερμοκρασία ${units.formatTemp(current.tempC)}."
                    } else {
                        "Light rain expected ($prob% probability) around $onset in ${bundle.cityName}. Current temp ${units.formatTemp(current.tempC)}."
                    },
                    cityName = bundle.cityName,
                    latitude = bundle.latitude,
                    longitude = bundle.longitude,
                    precipProbability = prob,
                    precipMm = peakMm.coerceAtLeast(0.8),
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
                    title = if (isGreek) "💨 Θυελλώδεις Άνεμοι ΕΜΥ • ${bundle.cityName}" else "💨 High Wind & Gale Advisory • ${bundle.cityName}",
                    message = if (isGreek) {
                        "Ισχυροί άνεμοι ${units.formatWind(current.windSpeedKmh)} (${current.windCardinal}) με ριπές έως ${units.formatWind(peakGusts)} στην περιοχή σας."
                    } else {
                        "Strong ${current.windCardinal} winds at ${units.formatWind(current.windSpeedKmh)} with gusts reaching ${units.formatWind(peakGusts)}."
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

        if (alerts.isEmpty()) {
            alerts.add(
                evaluateBundleForAlerts(
                    bundle = bundle,
                    units = units,
                    language = language,
                    emyAlertsEnabled = emyAlertsEnabled,
                    rainAlertsEnabled = rainAlertsEnabled
                )
            )
        }

        if (alerts.size == 1) {
            val firstType = alerts.first().type
            if (firstType != SevereAlertType.GALE_WIND) {
                alerts.add(
                    SevereWeatherAlertEvent(
                        id = "${bundle.cityId}_wind_advisory",
                        type = SevereAlertType.GALE_WIND,
                        severity = if (peakGusts >= 40.0) SevereAlertSeverity.WARNING else SevereAlertSeverity.ADVISORY,
                        title = if (isGreek) {
                            "💨 Δελτίο Ανέμων & Ριπών • ${bundle.cityName}"
                        } else {
                            "💨 Wind & Gusts Telemetry • ${bundle.cityName}"
                        },
                        message = if (isGreek) {
                            "Άνεμοι ${current.windCardinal} ${units.formatWind(current.windSpeedKmh)} με μέγιστες ριπές ${units.formatWind(peakGusts)}."
                        } else {
                            "${current.windCardinal} winds at ${units.formatWind(current.windSpeedKmh)} with peak gusts up to ${units.formatWind(peakGusts)}."
                        },
                        cityName = bundle.cityName,
                        latitude = bundle.latitude,
                        longitude = bundle.longitude,
                        precipProbability = peakProb,
                        precipMm = peakMm,
                        windGustsKmh = peakGusts,
                        onsetHourLabel = if (isGreek) "Ζωντανά" else "Live"
                    )
                )
            } else {
                alerts.add(
                    SevereWeatherAlertEvent(
                        id = "${bundle.cityId}_rain_horizon",
                        type = SevereAlertType.RAIN_ONSET,
                        severity = SevereAlertSeverity.ADVISORY,
                        title = if (isGreek) {
                            "☔ Ορίζοντας Βροχόπτωσης • ${bundle.cityName}"
                        } else {
                            "☔ Precipitation Horizon • ${bundle.cityName}"
                        },
                        message = if (isGreek) {
                            "Μέγιστη πιθανότητα βροχής $peakProb% το επόμενο 6ωρο (${bundle.rainOutlook.nextRainOnsetLabel})."
                        } else {
                            "Peak precipitation probability $peakProb% over the next 6 hours (${bundle.rainOutlook.nextRainOnsetLabel})."
                        },
                        cityName = bundle.cityName,
                        latitude = bundle.latitude,
                        longitude = bundle.longitude,
                        precipProbability = peakProb,
                        precipMm = peakMm,
                        windGustsKmh = peakGusts,
                        onsetHourLabel = if (isGreek) "6ωρο" else "6h Outlook"
                    )
                )
            }
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
