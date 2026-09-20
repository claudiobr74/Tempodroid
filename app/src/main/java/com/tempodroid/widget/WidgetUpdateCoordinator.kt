package com.tempodroid.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tempodroid.workers.TimelineUpdateWorker
import com.tempodroid.workers.WeatherUpdateWorker
import java.util.concurrent.TimeUnit

object WidgetUpdateCoordinator {
    const val UNIQUE_PERIODIC = "solar_weather_widget_periodic"
    const val UNIQUE_TIMELINE_PERIODIC = "solar_weather_widget_timeline_periodic"
    const val UNIQUE_ONCE_ALL = "solar_weather_widget_once_all"
    const val UNIQUE_ASTRONOMY_ONCE = "solar_weather_widget_astronomy_once"
    const val KEY_APP_WIDGET_ID = "app_widget_id"

    private const val WEATHER_BACKOFF_MINUTES = 10L

    fun ensurePeriodic(context: Context) {
        val appContext = context.applicationContext
        if (!hasInstalledWidgets(appContext)) {
            cancelAll(appContext)
            return
        }

        val workManager = WorkManager.getInstance(appContext)
        val weatherRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(weatherConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WEATHER_BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            weatherRequest,
        )

        /*
         * Migração da versão anterior: PeriodicWorkRequest de 15 min não é
         * adequado para um relógio visual porque o Android pode adiá-lo por
         * horas. Cancela a agenda antiga e usa o alarme local one-shot.
         */
        workManager.cancelUniqueWork(UNIQUE_TIMELINE_PERIODIC)
        AstronomyRefreshScheduler.scheduleNext(appContext)
    }

    fun refreshAstronomyNow(context: Context) {
        val appContext = context.applicationContext
        if (!hasInstalledWidgets(appContext)) {
            cancelAll(appContext)
            return
        }
        val request = OneTimeWorkRequestBuilder<TimelineUpdateWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_ASTRONOMY_ONCE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        AstronomyRefreshScheduler.scheduleNext(appContext)
    }

    fun refreshNow(context: Context) {
        val appContext = context.applicationContext
        if (!hasInstalledWidgets(appContext)) {
            cancelAll(appContext)
            return
        }
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
            .setConstraints(weatherConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WEATHER_BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_ONCE_ALL,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        refreshAstronomyNow(appContext)
        ensurePeriodic(appContext)
    }

    fun refreshWidget(context: Context, appWidgetId: Int) {
        val appContext = context.applicationContext
        if (!isInstalledWidget(appContext, appWidgetId)) {
            cancelIfNoWidgets(appContext)
            return
        }
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
            .setInputData(workDataOf(KEY_APP_WIDGET_ID to appWidgetId))
            .setConstraints(weatherConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WEATHER_BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "solar_weather_widget_once_$appWidgetId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        refreshAstronomyNow(appContext)
        ensurePeriodic(appContext)
    }

    fun cancelIfNoWidgets(context: Context) {
        val appContext = context.applicationContext
        if (!hasInstalledWidgets(appContext)) cancelAll(appContext)
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC)
        workManager.cancelUniqueWork(UNIQUE_TIMELINE_PERIODIC)
        workManager.cancelUniqueWork(UNIQUE_ONCE_ALL)
        workManager.cancelUniqueWork(UNIQUE_ASTRONOMY_ONCE)
        AstronomyRefreshScheduler.cancel(appContext)
    }

    internal fun hasInstalledWidgets(context: Context): Boolean =
        installedWidgetIds(context).isNotEmpty()

    internal fun isInstalledWidget(context: Context, appWidgetId: Int): Boolean =
        installedWidgetIds(context).contains(appWidgetId)

    internal fun installedWidgetIds(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, SolarWeatherWidgetReceiver::class.java)
        return manager.getAppWidgetIds(provider)
    }

    private fun weatherConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}

class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> WidgetUpdateCoordinator.refreshNow(context)

            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                WidgetUpdateCoordinator.refreshAstronomyNow(context)
                WidgetUpdateCoordinator.ensurePeriodic(context)
            }
        }
    }
}
