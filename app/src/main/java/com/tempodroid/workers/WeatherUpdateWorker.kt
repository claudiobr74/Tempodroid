package com.tempodroid.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tempodroid.di.AppContainer
import com.tempodroid.widget.SolarWeatherWidget
import com.tempodroid.widget.WidgetUpdateCoordinator
import androidx.glance.appwidget.updateAll
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
        val container = AppContainer.get(applicationContext)
        val location = container.locationRepository.resolve(allowFreshDeviceLocation = false)
        val zone = runCatching { ZoneId.of(location.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val now = Instant.now()
        val local = now.atZone(zone)
        val astronomy = container.astronomyRepository.getAstronomy(
            latitude = location.latitude,
            longitude = location.longitude,
            date = LocalDate.from(local),
            timezone = zone,
            at = local.toLocalTime(),
        )
        container.cache.saveLocation(location)
        container.cache.saveAstronomy(astronomy)
        val dateLabel = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
            .format(local)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        container.cache.saveDateLabel(dateLabel)
        runCatching {
            val weather = container.weatherRepository.getWeather(location.latitude, location.longitude)
            container.cache.saveWeather(weather)
        }.onFailure {
            container.cache.markWeatherError()
        }
        SolarWeatherWidget().updateAll(applicationContext)
        WidgetUpdateCoordinator.ensurePeriodic(applicationContext)
        return Result.success()
    }
}
