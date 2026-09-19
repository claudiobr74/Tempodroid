package com.tempodroid.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tempodroid.workers.TimelineUpdateWorker
import com.tempodroid.workers.WeatherUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WidgetUpdateCoordinator {
    const val UNIQUE_PERIODIC = "solar_weather_widget_periodic"
    const val UNIQUE_TIMELINE_PERIODIC = "solar_weather_widget_timeline_periodic"
    const val UNIQUE_ONCE_ALL = "solar_weather_widget_once_all"
    const val UNIQUE_ASTRONOMY_ONCE = "solar_weather_widget_astronomy_once"
    const val KEY_APP_WIDGET_ID = "app_widget_id"

    fun ensurePeriodic(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val weatherRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            weatherRequest,
        )

        /*
         * Migração da versão anterior: PeriodicWorkRequest de 15 min não é
         * adequado para um relógio visual porque o Android pode adiá-lo por
         * horas. Cancela a agenda antiga e usa o alarme local one-shot.
         */
        workManager.cancelUniqueWork(UNIQUE_TIMELINE_PERIODIC)
        AstronomyRefreshScheduler.scheduleNext(context)
    }

    fun refreshAstronomyNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<TimelineUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ASTRONOMY_ONCE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        AstronomyRefreshScheduler.scheduleNext(context)
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONCE_ALL,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        refreshAstronomyNow(context)
        ensurePeriodic(context)
    }

    fun refreshWidget(context: Context, appWidgetId: Int) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
            .setInputData(workDataOf(KEY_APP_WIDGET_ID to appWidgetId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "solar_weather_widget_once_$appWidgetId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        refreshAstronomyNow(context)
        ensurePeriodic(context)
    }

    fun cancelIfNoWidgets(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(SolarWeatherWidget::class.java)
            if (ids.isEmpty()) {
                val workManager = WorkManager.getInstance(context)
                workManager.cancelUniqueWork(UNIQUE_PERIODIC)
                workManager.cancelUniqueWork(UNIQUE_TIMELINE_PERIODIC)
                workManager.cancelUniqueWork(UNIQUE_ONCE_ALL)
                workManager.cancelUniqueWork(UNIQUE_ASTRONOMY_ONCE)
                AstronomyRefreshScheduler.cancel(context)
            }
        }
    }
}

class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> WidgetUpdateCoordinator.refreshNow(context)

            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> {
                WidgetUpdateCoordinator.refreshAstronomyNow(context)
                WidgetUpdateCoordinator.ensurePeriodic(context)
            }
        }
    }
}
