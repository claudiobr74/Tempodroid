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

/**
 * Cache do widget isolado por appWidgetId.
 *
 * As chaves globais antigas continuam sendo lidas como fallback de migração, mas toda nova
 * configuração/atualização de widget deve gravar com appWidgetId.
 */
class WidgetCacheRepository(private val context: Context) {
    private object Names {
        const val configured = "configured"
        const val city = "city"
        const val lat = "lat"
        const val lon = "lon"
        const val timezone = "timezone"
        const val source = "location_source"
        const val temperature = "temperature"
        const val condition = "condition"
        const val high = "high"
        const val low = "low"
        const val rain = "rain"
        const val cloud = "cloud"
        const val precipitation = "precipitation"
        const val weatherCode = "weather_code"
        const val isDay = "is_day"
        const val weatherAt = "weather_at"
        const val sunrise = "sunrise"
        const val sunset = "sunset"
        const val solarNoon = "solar_noon"
        const val moonrise = "moonrise"
        const val moonset = "moonset"
        const val moonPhase = "moon_phase"
        const val illumination = "illumination"
        const val hasError = "has_error"
        const val dateLabel = "date_label"

        val all = listOf(
            configured, city, lat, lon, timezone, source, temperature, condition, high, low, rain,
            cloud, precipitation, weatherCode, isDay, weatherAt, sunrise, sunset, solarNoon,
            moonrise, moonset, moonPhase, illumination, hasError, dateLabel,
        )
    }

    private fun scoped(name: String, appWidgetId: Int?): String =
        if (appWidgetId == null) name else "widget_${appWidgetId}_$name"

    private fun stringKey(name: String, appWidgetId: Int? = null) = stringPreferencesKey(scoped(name, appWidgetId))
    private fun intKey(name: String, appWidgetId: Int? = null) = intPreferencesKey(scoped(name, appWidgetId))
    private fun longKey(name: String, appWidgetId: Int? = null) = longPreferencesKey(scoped(name, appWidgetId))
    private fun doubleKey(name: String, appWidgetId: Int? = null) = doublePreferencesKey(scoped(name, appWidgetId))
    private fun boolKey(name: String, appWidgetId: Int? = null) = booleanPreferencesKey(scoped(name, appWidgetId))

    suspend fun isConfigured(appWidgetId: Int): Boolean =
        context.dataStore.data.first()[boolKey(Names.configured, appWidgetId)] == true

    suspend fun saveLocation(location: ResolvedLocation, appWidgetId: Int? = null) {
        context.dataStore.edit {
            it[stringKey(Names.city, appWidgetId)] = location.city
            it[doubleKey(Names.lat, appWidgetId)] = location.latitude
            it[doubleKey(Names.lon, appWidgetId)] = location.longitude
            it[stringKey(Names.timezone, appWidgetId)] = location.timezoneId
            it[stringKey(Names.source, appWidgetId)] = location.source.name
            if (appWidgetId != null) it[boolKey(Names.configured, appWidgetId)] = true
        }
    }

    suspend fun savedLocationExact(appWidgetId: Int): ResolvedLocation? =
        locationFrom(context.dataStore.data.first(), appWidgetId)

    suspend fun savedLocation(appWidgetId: Int? = null): ResolvedLocation? {
        val prefs = context.dataStore.data.first()
        return locationFrom(prefs, appWidgetId)
            ?: if (appWidgetId != null) locationFrom(prefs, null) else null
    }

    private fun locationFrom(prefs: Preferences, appWidgetId: Int?): ResolvedLocation? {
        val lat = prefs[doubleKey(Names.lat, appWidgetId)] ?: return null
        val lon = prefs[doubleKey(Names.lon, appWidgetId)] ?: return null
        return ResolvedLocation(
            city = prefs[stringKey(Names.city, appWidgetId)] ?: LocationFallback.goiania.city,
            latitude = lat,
            longitude = lon,
            timezoneId = prefs[stringKey(Names.timezone, appWidgetId)] ?: LocationFallback.goiania.timezoneId,
            source = prefs[stringKey(Names.source, appWidgetId)]
                ?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
                ?: LocationSource.SAVED_COORDINATES,
        )
    }

    suspend fun saveWeather(weather: WeatherData, appWidgetId: Int? = null) {
        context.dataStore.edit {
            weather.temperature?.let { value -> it[intKey(Names.temperature, appWidgetId)] = value }
            it[stringKey(Names.condition, appWidgetId)] = weather.condition.name
            weather.high?.let { value -> it[intKey(Names.high, appWidgetId)] = value }
            weather.low?.let { value -> it[intKey(Names.low, appWidgetId)] = value }
            weather.rainChance?.let { value -> it[intKey(Names.rain, appWidgetId)] = value }
            weather.cloudCover?.let { value -> it[intKey(Names.cloud, appWidgetId)] = value }
            weather.precipitation?.let { value -> it[doubleKey(Names.precipitation, appWidgetId)] = value }
            weather.weatherCode?.let { value -> it[intKey(Names.weatherCode, appWidgetId)] = value }
            it[boolKey(Names.isDay, appWidgetId)] = weather.isDay
            weather.timestamp?.let { value -> it[longKey(Names.weatherAt, appWidgetId)] = value.toEpochMilli() }
            weather.timezoneId?.let { value -> it[stringKey(Names.timezone, appWidgetId)] = value }
            it[boolKey(Names.hasError, appWidgetId)] = false
        }
    }

    suspend fun clearWeather(appWidgetId: Int) {
        context.dataStore.edit {
            listOf(
                intKey(Names.temperature, appWidgetId),
                stringKey(Names.condition, appWidgetId),
                intKey(Names.high, appWidgetId),
                intKey(Names.low, appWidgetId),
                intKey(Names.rain, appWidgetId),
                intKey(Names.cloud, appWidgetId),
                doubleKey(Names.precipitation, appWidgetId),
                intKey(Names.weatherCode, appWidgetId),
                boolKey(Names.isDay, appWidgetId),
                longKey(Names.weatherAt, appWidgetId),
                boolKey(Names.hasError, appWidgetId),
            ).forEach { key -> it.remove(key) }
        }
    }

    suspend fun markWeatherError(appWidgetId: Int? = null) {
        context.dataStore.edit { it[boolKey(Names.hasError, appWidgetId)] = true }
    }

    suspend fun saveAstronomy(data: AstronomyData, appWidgetId: Int? = null) {
        context.dataStore.edit {
            it[stringKey(Names.sunrise, appWidgetId)] = data.sunrise.toString()
            it[stringKey(Names.sunset, appWidgetId)] = data.sunset.toString()
            data.solarNoon?.let { value -> it[stringKey(Names.solarNoon, appWidgetId)] = value.toString() }
                ?: it.remove(stringKey(Names.solarNoon, appWidgetId))
            data.moonrise?.let { value -> it[stringKey(Names.moonrise, appWidgetId)] = value.toString() }
                ?: it.remove(stringKey(Names.moonrise, appWidgetId))
            data.moonset?.let { value -> it[stringKey(Names.moonset, appWidgetId)] = value.toString() }
                ?: it.remove(stringKey(Names.moonset, appWidgetId))
            it[stringKey(Names.moonPhase, appWidgetId)] = data.moonPhase.name
            it[doubleKey(Names.illumination, appWidgetId)] = data.moonIllumination
        }
    }

    suspend fun saveDateLabel(label: String, appWidgetId: Int? = null) {
        context.dataStore.edit { it[stringKey(Names.dateLabel, appWidgetId)] = label }
    }

    private fun weatherFromExact(prefs: Preferences, appWidgetId: Int?): WeatherData? {
        val condition = prefs[stringKey(Names.condition, appWidgetId)] ?: return null
        return WeatherData(
            temperature = prefs[intKey(Names.temperature, appWidgetId)],
            condition = runCatching { WeatherCondition.valueOf(condition) }.getOrDefault(WeatherCondition.CLEAR),
            high = prefs[intKey(Names.high, appWidgetId)],
            low = prefs[intKey(Names.low, appWidgetId)],
            rainChance = prefs[intKey(Names.rain, appWidgetId)],
            cloudCover = prefs[intKey(Names.cloud, appWidgetId)],
            precipitation = prefs[doubleKey(Names.precipitation, appWidgetId)],
            weatherCode = prefs[intKey(Names.weatherCode, appWidgetId)],
            isDay = prefs[boolKey(Names.isDay, appWidgetId)] ?: true,
            timestamp = prefs[longKey(Names.weatherAt, appWidgetId)]?.let { Instant.ofEpochMilli(it) },
            timezoneId = prefs[stringKey(Names.timezone, appWidgetId)],
        )
    }

    fun weatherFrom(prefs: Preferences, appWidgetId: Int? = null): WeatherData? =
        weatherFromExact(prefs, appWidgetId)
            ?: if (appWidgetId != null) weatherFromExact(prefs, null) else null

    suspend fun loadState(appWidgetId: Int? = null, now: Instant = Instant.now()): SolarWeatherWidgetState {
        val prefs = context.dataStore.data.first()
        val location = locationFrom(prefs, appWidgetId)
            ?: if (appWidgetId != null) locationFrom(prefs, null) else null
            ?: LocationFallback.goiania
        val zone = runCatching { ZoneId.of(location.timezoneId) }
            .getOrDefault(ZoneId.of(LocationFallback.goiania.timezoneId))
        val local = now.atZone(zone)
        val weather = weatherFrom(prefs, appWidgetId)

        fun scopedString(name: String): String? = prefs[stringKey(name, appWidgetId)]
            ?: if (appWidgetId != null) prefs[stringKey(name, null)] else null
        fun scopedDouble(name: String): Double? = prefs[doubleKey(name, appWidgetId)]
            ?: if (appWidgetId != null) prefs[doubleKey(name, null)] else null
        fun scopedBool(name: String): Boolean? = prefs[boolKey(name, appWidgetId)]
            ?: if (appWidgetId != null) prefs[boolKey(name, null)] else null

        val astronomy = AstronomyData(
            sunrise = scopedString(Names.sunrise).toLocalTimeOr(LocalTime.of(6, 10)),
            sunset = scopedString(Names.sunset).toLocalTimeOr(LocalTime.of(18, 5)),
            solarNoon = scopedString(Names.solarNoon).toLocalTimeOrNull(),
            moonrise = scopedString(Names.moonrise).toLocalTimeOrNull(),
            moonset = scopedString(Names.moonset).toLocalTimeOrNull(),
            moonPhase = scopedString(Names.moonPhase)
                ?.let { runCatching { MoonPhase.valueOf(it) }.getOrNull() }
                ?: MoonPhase.WAXING_CRESCENT,
            moonIllumination = scopedDouble(Names.illumination) ?: 0.18,
            sunPosition = null,
            moonPosition = null,
            hemisphere = if (location.latitude < 0) Hemisphere.SOUTH else Hemisphere.NORTH,
        )
        val dateLabel = scopedString(Names.dateLabel)
            ?: DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
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
            hasWeatherError = scopedBool(Names.hasError) == true,
            clock = now,
        )
    }

    suspend fun deleteWidgetState(appWidgetId: Int) {
        context.dataStore.edit { prefs ->
            Names.all.forEach { name ->
                prefs.asMap().keys
                    .filter { it.name == scoped(name, appWidgetId) }
                    .forEach { prefs.remove(it) }
            }
        }
    }
}

private fun String?.toLocalTimeOr(fallback: LocalTime): LocalTime =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: fallback

private fun String?.toLocalTimeOrNull(): LocalTime? =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
