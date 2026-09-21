package com.tempodroid.workers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tempodroid.di.AppContainer
import com.tempodroid.weather.ForecastSnapshot
import com.tempodroid.widget.SolarWeatherWidget
import com.tempodroid.widget.WidgetUpdateCoordinator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val glanceManager = GlanceAppWidgetManager(applicationContext)
        val requestedId = inputData.getInt(
            WidgetUpdateCoordinator.KEY_APP_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val appWidgetIds = if (requestedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (WidgetUpdateCoordinator.isInstalledWidget(applicationContext, requestedId)) {
                listOf(requestedId)
            } else {
                emptyList()
            }
        } else {
            WidgetUpdateCoordinator.installedWidgetIds(applicationContext).toList()
        }

        if (appWidgetIds.isEmpty()) {
            WidgetUpdateCoordinator.cancelAll(applicationContext)
            return Result.success()
        }

        var hadWeatherFailure = false
        appWidgetIds.distinct().forEach { appWidgetId ->
            val success = runCatching {
                refreshWidget(appWidgetId, glanceManager)
            }.getOrElse {
                hadWeatherFailure = true
                false
            }
            if (!success) hadWeatherFailure = true
        }

        WidgetUpdateCoordinator.ensurePeriodic(applicationContext)
        return if (hadWeatherFailure) Result.retry() else Result.success()
    }

    private suspend fun refreshWidget(
        appWidgetId: Int,
        glanceManager: GlanceAppWidgetManager,
    ): Boolean {
        val container = AppContainer.get(applicationContext)
        val location = container.locationRepository.resolve(
            appWidgetId = appWidgetId,
            allowFreshDeviceLocation = false,
        )
        val now = Instant.now()

        var weatherSucceeded = false
        val weather = if (isNetworkAvailable()) {
            runCatching {
                container.weatherRepository.getWeather(location.latitude, location.longitude)
            }.onSuccess {
                // Remove campos opcionais antigos antes de gravar o payload novo;
                // assim um null real da API não deixa sensação/chuva/etc. obsoletos no cache.
                container.cache.clearWeather(appWidgetId)
                container.cache.saveWeather(it, appWidgetId)
                container.forecastCache.saveForecast(
                    ForecastSnapshot(
                        hourly = it.hourlyForecast,
                        daily = it.dailyForecast,
                    ),
                    appWidgetId,
                )
                weatherSucceeded = true
            }.onFailure {
                container.cache.markWeatherError(appWidgetId)
            }.getOrNull()
        } else {
            container.cache.markWeatherError(appWidgetId)
            null
        }

        val effectiveZone = weather?.timezoneId
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: runCatching { ZoneId.of(location.timezoneId) }.getOrDefault(ZoneId.systemDefault())

        val effectiveLocation = if (effectiveZone.id != location.timezoneId) {
            location.copy(timezoneId = effectiveZone.id)
        } else {
            location
        }
        // Atualização em background não deve marcar uma instância ainda não confirmada como configurada.
        container.cache.saveLocation(
            effectiveLocation,
            appWidgetId,
            markConfigured = false,
        )

        val local = now.atZone(effectiveZone)
        val astronomy = container.astronomyRepository.getAstronomy(
            latitude = effectiveLocation.latitude,
            longitude = effectiveLocation.longitude,
            date = LocalDate.from(local),
            timezone = effectiveZone,
            at = local.toLocalTime(),
        )
        container.cache.saveAstronomy(astronomy, appWidgetId)

        val dateLabel = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
            .format(local)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        container.cache.saveDateLabel(dateLabel, appWidgetId)

        runCatching {
            val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
            SolarWeatherWidget().update(applicationContext, glanceId)
        }
        return weatherSucceeded
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
