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
import com.tempodroid.workers.WeatherUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WidgetUpdateCoordinator {
    const val UNIQUE_PERIODIC = "solar_weather_widget_periodic"
    const val UNIQUE_ONCE = "solar_weather_widget_once"

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONCE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        ensurePeriodic(context)
    }

    fun cancelIfNoWidgets(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(SolarWeatherWidget::class.java)
            if (ids.isEmpty()) {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
            }
        }
    }
}

class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent?.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent?.action == Intent.ACTION_DATE_CHANGED
        ) {
            WidgetUpdateCoordinator.refreshNow(context)
        }
    }
}
