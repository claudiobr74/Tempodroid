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
    const val KEY_APP_WIDGET_ID = "app_widget_id"

    fun ensurePeriodic(context: Context) {
        val weatherRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            weatherRequest,
        )

        // Redraw visual sem rede para manter o marcador "Agora" acompanhando o relógio.
        val timelineRequest = PeriodicWorkRequestBuilder<TimelineUpdateWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_TIMELINE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            timelineRequest,
        )
    }

    /** Atualiza todas as instâncias ativas usando um único Worker. */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONCE_ALL,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        ensurePeriodic(context)
    }

    /** Atualiza somente a instância informada, sem criar job periódico por widget. */
    fun refreshWidget(context: Context, appWidgetId: Int) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
            .setInputData(workDataOf(KEY_APP_WIDGET_ID to appWidgetId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "solar_weather_widget_once_$appWidgetId",
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
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_TIMELINE_PERIODIC)
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_ONCE_ALL)
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
