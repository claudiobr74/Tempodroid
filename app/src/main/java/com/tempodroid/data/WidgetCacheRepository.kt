package com.tempodroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tempodroid.astronomy.AstronomyData
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.location.LocationFallback
import com.tempodroid.location.LocationSource
import com.tempodroid.location.ResolvedLocation
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.weather.WeatherData
import com.tempodroid.widget.SolarWeatherWidgetState
import com.tempodroid.widget.WidgetStateMapper
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "tempodroid_widget")

class PreferencesRepository(private val context: Context) {
    private val cityKey = stringPreferencesKey("manual_city")
    private val useManualKey = booleanPreferencesKey("use_manual_city")

    suspend fun setManualCity(city: String) {
        context.dataStore.edit {
            it[cityKey] = city
            it[useManualKey] = city.isNotBlank()
        }
    }

    suspend fun manualCity(): String? {
        val prefs = context.dataStore.data.first()
        if (prefs[useManualKey] != true) return null
        return prefs[cityKey]
    }
}

class WidgetCacheRepository(private val context: Context) {
    private object Keys {
        val city = stringPreferencesKey("city")
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
        val timezone = stringPreferencesKey("timezone")
        val source = stringPreferencesKey("location_source")
        val temperature = intPreferencesKey("temperature")
        val condition = stringPreferencesKey("condition")
        val high = intPreferencesKey("high")
        val low = intPreferencesKey("low")
        val rain = intPreferencesKey("rain")
        val cloud = intPreferencesKey("cloud")
        val precipitation = doublePreferencesKey("precipitation")
        val weatherCode = intPreferencesKey("weather_code")
        val isDay = booleanPreferencesKey("is_day")
        val weatherAt = longPreferencesKey("weather_at")
        val sunrise = stringPreferencesKey("sunrise")
        val sunset = stringPreferencesKey("sunset")
        val solarNoon = stringPreferencesKey("solar_noon")
        val moonrise = stringPreferencesKey("moonrise")
        val moonset = stringPreferencesKey("moonset")
        val moonPhase = stringPreferencesKey("moon_phase")
        val illumination = doublePreferencesKey("illumination")
        val hasError = booleanPreferencesKey("has_error")
        val dateLabel = stringPreferencesKey("date_label")
    }

    suspend fun saveLocation(location: ResolvedLocation) {
        context.dataStore.edit {
            it[Keys.city] = location.city
            it[Keys.lat] = location.latitude
            it[Keys.lon] = location.longitude
            it[Keys.timezone] = location.timezoneId
            it[Keys.source] = location.source.name
        }
    }

    suspend fun savedLocation(): ResolvedLocation? {
        val prefs = context.dataStore.data.first()
        val lat = prefs[Keys.lat] ?: return null
        val lon = prefs[Keys.lon] ?: return null
        return ResolvedLocation(
            city = prefs[Keys.city] ?: LocationFallback.goiania.city,
            latitude = lat,
            longitude = lon,
            timezoneId = prefs[Keys.timezone] ?: LocationFallback.goiania.timezoneId,
            source = prefs[Keys.source]?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
                ?: LocationSource.SAVED_COORDINATES,
        )
    }

    suspend fun saveWeather(weather: WeatherData) {
        context.dataStore.edit {
            weather.temperature?.let { value -> it[Keys.temperature] = value }
            it[Keys.condition] = weather.condition.name
            weather.high?.let { value -> it[Keys.high] = value }
            weather.low?.let { value -> it[Keys.low] = value }
            weather.rainChance?.let { value -> it[Keys.rain] = value }
            weather.cloudCover?.let { value -> it[Keys.cloud] = value }
            weather.precipitation?.let { value -> it[Keys.precipitation] = value }
            weather.weatherCode?.let { value -> it[Keys.weatherCode] = value }
            it[Keys.isDay] = weather.isDay
            weather.timestamp?.let { value -> it[Keys.weatherAt] = value.toEpochMilli() }
            weather.timezoneId?.let { value -> it[Keys.timezone] = value }
            it[Keys.hasError] = false
        }
    }

    suspend fun markWeatherError() {
        context.dataStore.edit { it[Keys.hasError] = true }
    }

    suspend fun saveAstronomy(data: AstronomyData) {
        context.dataStore.edit {
            it[Keys.sunrise] = data.sunrise.toString()
            it[Keys.sunset] = data.sunset.toString()
            data.solarNoon?.let { value -> it[Keys.solarNoon] = value.toString() }
            data.moonrise?.let { value -> it[Keys.moonrise] = value.toString() } ?: it.remove(Keys.moonrise)
            data.moonset?.let { value -> it[Keys.moonset] = value.toString() } ?: it.remove(Keys.moonset)
            it[Keys.moonPhase] = data.moonPhase.name
            it[Keys.illumination] = data.moonIllumination
        }
    }

    suspend fun saveDateLabel(label: String) {
        context.dataStore.edit { it[Keys.dateLabel] = label }
    }

    fun weatherFrom(prefs: Preferences): WeatherData? {
        val condition = prefs[Keys.condition] ?: return null
        return WeatherData(
            temperature = prefs[Keys.temperature],
            condition = runCatching { WeatherCondition.valueOf(condition) }.getOrDefault(WeatherCondition.CLEAR),
            high = prefs[Keys.high],
            low = prefs[Keys.low],
            rainChance = prefs[Keys.rain],
            cloudCover = prefs[Keys.cloud],
            precipitation = prefs[Keys.precipitation],
            weatherCode = prefs[Keys.weatherCode],
            isDay = prefs[Keys.isDay] ?: true,
            timestamp = prefs[Keys.weatherAt]?.let { Instant.ofEpochMilli(it) },
            timezoneId = prefs[Keys.timezone],
        )
    }

    suspend fun loadState(now: Instant = Instant.now()): SolarWeatherWidgetState {
        val prefs = context.dataStore.data.first()
        val location = savedLocation() ?: LocationFallback.goiania
        val zone = runCatching { ZoneId.of(location.timezoneId) }.getOrDefault(ZoneId.of("America/Sao_Paulo"))
        val local = now.atZone(zone)
        val weather = weatherFrom(prefs)
        val astronomy = AstronomyData(
            sunrise = prefs[Keys.sunrise].toLocalTimeOr(LocalTime.of(6, 10)),
            sunset = prefs[Keys.sunset].toLocalTimeOr(LocalTime.of(18, 5)),
            solarNoon = prefs[Keys.solarNoon].toLocalTimeOrNull(),
            moonrise = prefs[Keys.moonrise].toLocalTimeOrNull(),
            moonset = prefs[Keys.moonset].toLocalTimeOrNull(),
            moonPhase = prefs[Keys.moonPhase]?.let { runCatching { MoonPhase.valueOf(it) }.getOrNull() }
                ?: MoonPhase.WAXING_CRESCENT,
            moonIllumination = prefs[Keys.illumination] ?: 0.18,
            sunPosition = null,
            moonPosition = null,
            hemisphere = if (location.latitude < 0) Hemisphere.SOUTH else Hemisphere.NORTH,
        )
        val dateLabel = prefs[Keys.dateLabel] ?: DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
            .format(local)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        return WidgetStateMapper.merge(
            city = location.city,
            weather = weather,
            astronomy = astronomy,
            now = local.toLocalTime(),
            dateLabel = dateLabel,
            latitude = location.latitude,
            longitude = location.longitude,
            timezoneId = location.timezoneId,
            hasWeatherError = prefs[Keys.hasError] == true,
            clock = now,
        )
    }
}

private fun String?.toLocalTimeOr(fallback: LocalTime): LocalTime =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: fallback

private fun String?.toLocalTimeOrNull(): LocalTime? =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
