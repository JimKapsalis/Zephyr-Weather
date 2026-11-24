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
import android.os.SystemClock
import com.example.data.WeatherDatabase
import com.example.data.WeatherPreferences
import com.example.data.WeatherRepository
import com.example.widget.ZephyrWeatherWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Coordinates background weather synchronization and push notifications using both
 * Android's [JobScheduler] ([WeatherBackgroundJobService]) and [AlarmManager] ([WeatherAlarmReceiver])
 * so severe weather and rain notifications are reliably delivered even when the app is in the background.
 */
object WeatherBackgroundSyncScheduler {

    const val ACTION_BACKGROUND_WEATHER_SYNC = "com.example.action.BACKGROUND_WEATHER_SYNC"
    private const val JOB_ID_WEATHER_SYNC = 4901
    private const val ALARM_REQUEST_CODE = 4902

    fun scheduleBackgroundSync(
        context: Context,
        intervalMs: Long? = null
    ) {
        val appContext = context.applicationContext ?: context
        SevereWeatherNotifier.ensureChannelsCreated(appContext)
        val prefs = WeatherPreferences(appContext)
        val effectiveIntervalMs = (intervalMs ?: prefs.loadRefreshInterval().intervalMs)
            .coerceAtLeast(15 * 60 * 1000L)

        // 1. Schedule JobScheduler Periodic Background Sync
        runCatching {
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            if (jobScheduler != null) {
                val component = ComponentName(appContext, WeatherBackgroundJobService::class.java)
                val jobInfo = JobInfo.Builder(JOB_ID_WEATHER_SYNC, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(effectiveIntervalMs)
                    .build()
                jobScheduler.schedule(jobInfo)
            }
        }

        // 2. Schedule AlarmManager wakeup so background notifications fire reliably during idle/background
        runCatching {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return@runCatching
            val intent = Intent(appContext, WeatherAlarmReceiver::class.java).apply {
                action = ACTION_BACKGROUND_WEATHER_SYNC
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                ALARM_REQUEST_CODE,
                intent,
                pendingFlags
            )
            val triggerAtElapsed = SystemClock.elapsedRealtime() + effectiveIntervalMs
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsed,
                pendingIntent
            )
        }
    }

    /**
     * Executes a single background weather fetch + alert evaluation + system notification dispatch.
     */
    suspend fun performBackgroundWeatherCheckAndNotify(
        context: Context,
        forceNotification: Boolean = false
    ): Boolean {
        val appContext = context.applicationContext ?: context
        return runCatching {
            SevereWeatherNotifier.ensureChannelsCreated(appContext)
            val prefs = WeatherPreferences(appContext)
            val emyEnabled = prefs.loadEmyAlerts()
            val rainEnabled = prefs.loadRainAlerts()
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

            val bundle = repo.fetchWeatherForCity(
                city = targetCity,
                provider = provider,
                forceRefresh = true
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
    }
}

/**
 * Background [JobService] that keeps weather forecasts and severe weather notifications active
 * when the application is in the background.
 */
class WeatherBackgroundJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            WeatherBackgroundSyncScheduler.performBackgroundWeatherCheckAndNotify(
                context = applicationContext,
                forceNotification = false
            )
            WeatherBackgroundSyncScheduler.scheduleBackgroundSync(applicationContext)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * [BroadcastReceiver] triggered by [AlarmManager] and device boot to fetch weather in the background
 * and post notifications when the user is outside the app.
 */
class WeatherAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext ?: context
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val isExplicitSync = intent?.action == WeatherBackgroundSyncScheduler.ACTION_BACKGROUND_WEATHER_SYNC
                WeatherBackgroundSyncScheduler.performBackgroundWeatherCheckAndNotify(
                    context = appContext,
                    forceNotification = isExplicitSync
                )
                WeatherBackgroundSyncScheduler.scheduleBackgroundSync(appContext)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}
