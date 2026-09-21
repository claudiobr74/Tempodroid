package com.tempodroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tempodroid.weather.DailyForecast
import com.tempodroid.weather.ForecastSnapshot
import com.tempodroid.weather.HourlyForecast
import com.tempodroid.weather.WeatherCondition
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

private val Context.forecastDataStore by preferencesDataStore(name = "tempodroid_forecast")

class ForecastCacheRepository(private val context: Context) {
    private fun hourlyKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_hourly")
    private fun dailyKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_daily")

    suspend fun saveForecast(forecast: ForecastSnapshot, appWidgetId: Int) {
        context.forecastDataStore.edit { prefs ->
            prefs[hourlyKey(appWidgetId)] = encodeHourly(forecast.hourly)
            prefs[dailyKey(appWidgetId)] = encodeDaily(forecast.daily)
        }
    }

    suspend fun loadForecast(appWidgetId: Int?): ForecastSnapshot {
        if (appWidgetId == null) return ForecastSnapshot.Empty
        val prefs = context.forecastDataStore.data.first()
        return ForecastSnapshot(
            hourly = decodeHourly(prefs[hourlyKey(appWidgetId)]),
            daily = decodeDaily(prefs[dailyKey(appWidgetId)]),
        )
    }

    suspend fun deleteForecast(appWidgetId: Int) {
        context.forecastDataStore.edit { prefs ->
            prefs.remove(hourlyKey(appWidgetId))
            prefs.remove(dailyKey(appWidgetId))
        }
    }

    private fun encodeHourly(values: List<HourlyForecast>): String = values.joinToString(";") { item ->
        listOf(
            item.time.toEpochMilli().toString(),
            item.temperature?.toString() ?: NULL,
            item.rainChance?.toString() ?: NULL,
            item.condition.name,
        ).joinToString("|")
    }

    private fun decodeHourly(value: String?): List<HourlyForecast> = value
        .orEmpty()
        .split(';')
        .filter { it.isNotBlank() }
        .mapNotNull { record ->
            val fields = record.split('|')
            if (fields.size != 4) return@mapNotNull null
            val time = fields[0].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            val condition = runCatching { WeatherCondition.valueOf(fields[3]) }.getOrNull()
                ?: return@mapNotNull null
            HourlyForecast(
                time = time,
                temperature = fields[1].nullableInt(),
                rainChance = fields[2].nullableInt(),
                condition = condition,
            )
        }

    private fun encodeDaily(values: List<DailyForecast>): String = values.joinToString(";") { item ->
        listOf(
            item.date.toString(),
            item.high?.toString() ?: NULL,
            item.low?.toString() ?: NULL,
            item.rainChance?.toString() ?: NULL,
            item.condition.name,
        ).joinToString("|")
    }

    private fun decodeDaily(value: String?): List<DailyForecast> = value
        .orEmpty()
        .split(';')
        .filter { it.isNotBlank() }
        .mapNotNull { record ->
            val fields = record.split('|')
            if (fields.size != 5) return@mapNotNull null
            val date = runCatching { LocalDate.parse(fields[0]) }.getOrNull() ?: return@mapNotNull null
            val condition = runCatching { WeatherCondition.valueOf(fields[4]) }.getOrNull()
                ?: return@mapNotNull null
            DailyForecast(
                date = date,
                high = fields[1].nullableInt(),
                low = fields[2].nullableInt(),
                rainChance = fields[3].nullableInt(),
                condition = condition,
            )
        }

    private fun String.nullableInt(): Int? = if (this == NULL) null else toIntOrNull()

    private companion object {
        const val NULL = "~"
    }
}
