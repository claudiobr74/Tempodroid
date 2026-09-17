package com.tempodroid.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tempodroid.di.AppContainer
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
        val container = AppContainer.get(applicationContext)
        val location = container.locationRepository.resolve(allowFreshDeviceLocation = false)
        val now = Instant.now()

        val weather = if (isNetworkAvailable()) {
            runCatching {
                container.weatherRepository.getWeather(location.latitude, location.longitude)
            }.onSuccess {
                container.cache.saveWeather(it)
            }.onFailure {
                container.cache.markWeatherError()
            }.getOrNull()
        } else {
            container.cache.markWeatherError()
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
        container.cache.saveLocation(effectiveLocation)

        val local = now.atZone(effectiveZone)
        val astronomy = container.astronomyRepository.getAstronomy(
            latitude = effectiveLocation.latitude,
            longitude = effectiveLocation.longitude,
            date = LocalDate.from(local),
            timezone = effectiveZone,
            at = local.toLocalTime(),
        )
        container.cache.saveAstronomy(astronomy)

        val dateLabel = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
            .format(local)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        container.cache.saveDateLabel(dateLabel)

        SolarWeatherWidget().updateAll(applicationContext)
        WidgetUpdateCoordinator.ensurePeriodic(applicationContext)
        return Result.success()
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
