package com.tempodroid.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Mantém apenas a camada astronômica/visual do widget acompanhando o relógio.
 *
 * Não acessa rede. O alarme é one-shot e se auto-reagenda para evitar a
 * postergação agressiva do PeriodicWorkRequest. Em Android 12+ usa alarme exato
 * somente quando o sistema já o permite; caso contrário usa o melhor alarme
 * permitido sem solicitar uma permissão especial ao usuário.
 */
object AstronomyRefreshScheduler {
    internal const val INTERVAL_MILLIS = 5 * 60 * 1000L
    private const val REQUEST_CODE = 4109

    fun scheduleNext(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(appContext)
        val triggerAt = nextTriggerAt(nowMillis)

        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        val exactScheduled = if (exactAllowed) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }.isSuccess
        } else {
            false
        }

        if (!exactScheduled) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(appContext))
    }

    internal fun nextTriggerAt(nowMillis: Long): Long =
        ((nowMillis / INTERVAL_MILLIS) + 1L) * INTERVAL_MILLIS

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetAstronomyAlarmReceiver::class.java).apply {
            action = WidgetAstronomyAlarmReceiver.ACTION_REFRESH_ASTRONOMY
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Recebe o alarme leve, redesenha todos os widgets e agenda o próximo ciclo. */
class WidgetAstronomyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REFRESH_ASTRONOMY) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val manager = GlanceAppWidgetManager(appContext)
                val ids = manager.getGlanceIds(SolarWeatherWidget::class.java)
                if (ids.isEmpty()) {
                    AstronomyRefreshScheduler.cancel(appContext)
                    return@launch
                }

                val widget = SolarWeatherWidget()
                ids.forEach { glanceId ->
                    runCatching { widget.update(appContext, glanceId) }
                }
                AstronomyRefreshScheduler.scheduleNext(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_ASTRONOMY =
            "com.tempodroid.action.REFRESH_ASTRONOMY"
    }
}
