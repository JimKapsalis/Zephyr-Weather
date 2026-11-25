package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.WeatherDatabase
import com.example.data.WeatherPreferences
import com.example.data.WeatherRepository
import com.example.widget.ZephyrWeatherWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ultra-low-power background weather synchronization using Android's Doze-aware [JobScheduler]
 * without any CPU wakelocks or [AlarmManager.ELAPSED_REALTIME_WAKEUP] alarms.
 *
 * Engineered for all Android devices to prevent background keep-awake drain while still
 * delivering severe weather notifications during standard OS maintenance windows.
 */
object WeatherBackgroundSyncScheduler {

    const val ACTION_BACKGROUND_WEATHER_SYNC = "com.example.action.BACKGROUND_WEATHER_SYNC"
    private const val JOB_ID_WEATHER_SYNC = 4901
    private const val ALARM_REQUEST_CODE = 4902
    private const val MIN_BACKGROUND_FETCH_GAP_MS = 25 * 60 * 1000L

    fun scheduleBackgroundSync(
        context: Context,
        intervalMs: Long? = null,
        forceReschedule: Boolean = false
    ) {
        val appContext = context.applicationContext ?: context
        SevereWeatherNotifier.ensureChannelsCreated(appContext)
        val prefs = WeatherPreferences(appContext)
        val batterySaver = prefs.loadBatterySaverSync()
        val requestedInterval = intervalMs ?: prefs.loadRefreshInterval().intervalMs
        val minAllowedInterval = if (batterySaver) 30 * 60 * 1000L else 15 * 60 * 1000L
        val effectiveIntervalMs = requestedInterval.coerceAtLeast(minAllowedInterval)

        // 1. Always cancel any legacy AlarmManager wakeup alarms so the phone never holds CPU wakelocks
        cancelLegacyWakeupAlarms(appContext)

        // 2. Schedule Doze-batched JobScheduler Periodic Sync (only if not already registered with same interval)
        runCatching {
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return@runCatching
            val existingJob = jobScheduler.allPendingJobs.firstOrNull { it.id == JOB_ID_WEATHER_SYNC }
            if (!forceReschedule && existingJob != null && existingJob.intervalMillis == effectiveIntervalMs) {
                return@runCatching
            }

            val component = ComponentName(appContext, WeatherBackgroundJobService::class.java)
            val builder = JobInfo.Builder(JOB_ID_WEATHER_SYNC, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(effectiveIntervalMs)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresBatteryNotLow(true)
            }

            jobScheduler.schedule(builder.build())
        }
    }

    private fun cancelLegacyWakeupAlarms(appContext: Context) {
        runCatching {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return@runCatching
            val intent = Intent(appContext, WeatherAlarmReceiver::class.java).apply {
                action = ACTION_BACKGROUND_WEATHER_SYNC
            }
            val pendingFlags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                ALARM_REQUEST_CODE,
                intent,
                pendingFlags
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /**
     * Executes a single bounded background weather fetch + alert evaluation + system notification dispatch.
     */
    suspend fun performBackgroundWeatherCheckAndNotify(
        context: Context,
        forceNotification: Boolean = false
    ): Boolean {
        val appContext = context.applicationContext ?: context
        return withTimeoutOrNull(10_000L) {
            runCatching {
                SevereWeatherNotifier.ensureChannelsCreated(appContext)
                val prefs = WeatherPreferences(appContext)
                val emyEnabled = prefs.loadEmyAlerts()
                val rainEnabled = prefs.loadRainAlerts()
                val batterySaver = prefs.loadBatterySaverSync()

                // If user disabled all background alerts and battery saver is active, skip background radio wake
                if (!forceNotification && batterySaver && !emyEnabled && !rainEnabled) {
                    return@runCatching false
                }

                val units = prefs.loadUnits()
                val language = prefs.loadLanguage()
                val provider = prefs.loadProvider()

                val db = WeatherDatabase.getInstance(appContext)
                val repo = WeatherRepository(db.cityDao())
                val cities = repo.ensureDefaultCitiesSeeded(prefs)
                val selectedId = prefs.loadSelectedCityId()
                val targetCity = cities.find { it.id == selectedId }
                    ?: cities.firstOrNull()
                    ?: return@runCatching false

                val now = System.currentTimeMillis()
                if (!forceNotification && targetCity.lastUpdatedEpochMs > 0L &&
                    (now - targetCity.lastUpdatedEpochMs) < MIN_BACKGROUND_FETCH_GAP_MS
                ) {
                    return@runCatching false
                }

                val bundle = repo.fetchWeatherForCity(
                    city = targetCity,
                    provider = provider,
                    forceRefresh = false
                )

                val alerts = SevereWeatherNotifier.evaluateAllActiveAlerts(
                    bundle = bundle,
                    units = units,
                    language = language,
                    emyAlertsEnabled = emyEnabled,
                    rainAlertsEnabled = rainEnabled
                )
                val primaryAlert = alerts.firstOrNull {
                    it.type == SevereAlertType.THUNDERSTORM ||
                        it.type == SevereAlertType.HEAVY_RAIN ||
                        it.type == SevereAlertType.RAIN_ONSET ||
                        it.type == SevereAlertType.GALE_WIND
                } ?: alerts.firstOrNull()

                ZephyrWeatherWidgetProvider.updateAllWidgetsFromBundle(
                    context = appContext,
                    bundle = bundle,
                    units = units,
                    language = language,
                    alert = primaryAlert
                )

                if (primaryAlert != null && (emyEnabled || rainEnabled || forceNotification)) {
                    SevereWeatherNotifier.dispatchSystemNotificationIfPermitted(
                        context = appContext,
                        alert = primaryAlert,
                        forceDispatch = forceNotification || primaryAlert.isSevereOrRainActive
                    )
                } else {
                    false
                }
            }.getOrDefault(false)
        } ?: false
    }
}

/**
 * Doze-friendly [JobService] that releases its job wakelock immediately after a bounded check
 * without recursively re-scheduling itself.
 */
class WeatherBackgroundJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            try {
                WeatherBackgroundSyncScheduler.performBackgroundWeatherCheckAndNotify(
                    context = applicationContext,
                    forceNotification = false
                )
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * Boot/Broadcast receiver that ensures periodic [JobScheduler] sync is registered after reboot
 * without scheduling any wakeup alarms.
 */
class WeatherAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext ?: context
        WeatherBackgroundSyncScheduler.scheduleBackgroundSync(
            context = appContext,
            forceReschedule = false
        )
    }
}
