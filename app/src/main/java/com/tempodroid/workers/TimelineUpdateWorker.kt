package com.tempodroid.workers

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tempodroid.widget.SolarWeatherWidget

/**
 * Atualização visual leve do widget.
 *
 * Não acessa rede nem atualiza o clima. Apenas força um novo provideGlance para
 * que a timeline use a hora atual e reposicione o marcador "Agora".
 */
class TimelineUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(SolarWeatherWidget::class.java)

        ids.forEach { glanceId ->
            runCatching {
                SolarWeatherWidget().update(applicationContext, glanceId)
            }
        }

        return Result.success()
    }
}
