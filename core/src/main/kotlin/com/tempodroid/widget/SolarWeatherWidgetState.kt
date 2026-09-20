package com.tempodroid.widget

import com.tempodroid.astronomy.AstronomyData
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.hemisphereFromLatitude
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.weather.WeatherData
import java.time.Instant
import java.time.LocalTime

/**
 * Estado de apresentação do widget. Separado dos DTOs brutos de clima/astronomia.
 */
data class SolarWeatherWidgetState(
    val city: String,
    val temperature: Int?,
    val condition: WeatherCondition,
    val high: Int?,
    val low: Int?,
    val rainChance: Int?,
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val solarNoon: LocalTime?,
    val moonPhase: MoonPhase,
    val moonIllumination: Double,
    val moonrise: LocalTime?,
    val moonset: LocalTime?,
    val weatherCode: Int?,
    val cloudCover: Int?,
    val precipitation: Double?,
    val isDay: Boolean,
    val updatedAt: Instant?,
    val hasWeatherError: Boolean,
    val isStale: Boolean,
    val latitude: Double = FALLBACK_LATITUDE,
    val longitude: Double = FALLBACK_LONGITUDE,
    val timezoneId: String = FALLBACK_TIMEZONE,
    val displayTime: LocalTime? = null,
    val displayDate: String = "",
    val apparentTemperature: Int? = null,
    val relativeHumidity: Int? = null,
    val windSpeedKmh: Int? = null,
    val surfacePressureHpa: Int? = null,
) {
    val hemisphere get() = hemisphereFromLatitude(latitude)

    companion object {
        const val FALLBACK_CITY = "Goiânia"
        const val FALLBACK_LATITUDE = -16.6869
        const val FALLBACK_LONGITUDE = -49.2648
        const val FALLBACK_TIMEZONE = "America/Sao_Paulo"
        const val STALE_AFTER_HOURS = 3L

        val Empty = SolarWeatherWidgetState(
            city = FALLBACK_CITY,
            temperature = null,
            condition = WeatherCondition.CLEAR,
            high = null,
            low = null,
            rainChance = null,
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 5),
            solarNoon = LocalTime.of(12, 8),
            moonPhase = MoonPhase.WAXING_CRESCENT,
            moonIllumination = 0.18,
            moonrise = null,
            moonset = null,
            weatherCode = null,
            cloudCover = 10,
            precipitation = 0.0,
            isDay = true,
            updatedAt = null,
            hasWeatherError = false,
            isStale = false,
        )
    }
}

object WidgetStateMapper {
    fun merge(
        city: String,
        weather: WeatherData?,
        astronomy: AstronomyData,
        now: LocalTime,
        dateLabel: String,
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        hasWeatherError: Boolean,
        clock: Instant,
    ): SolarWeatherWidgetState {
        val updatedAt = weather?.timestamp
        val stale = updatedAt == null ||
            java.time.Duration.between(updatedAt, clock).toHours() >= SolarWeatherWidgetState.STALE_AFTER_HOURS
        val astronomicalDay = astronomy.sunPosition?.aboveHorizon ?: run {
            val rise = astronomy.sunrise
            val set = astronomy.sunset
            rise != null && set != null && now.isAfter(rise) && now.isBefore(set)
        }
        return SolarWeatherWidgetState(
            city = city,
            temperature = weather?.temperature,
            condition = weather?.condition ?: WeatherCondition.CLEAR,
            high = weather?.high,
            low = weather?.low,
            rainChance = weather?.rainChance,
            sunrise = astronomy.sunrise,
            sunset = astronomy.sunset,
            solarNoon = astronomy.solarNoon,
            moonPhase = astronomy.moonPhase,
            moonIllumination = astronomy.moonIllumination,
            moonrise = astronomy.moonrise,
            moonset = astronomy.moonset,
            weatherCode = weather?.weatherCode,
            cloudCover = weather?.cloudCover,
            precipitation = weather?.precipitation,
            isDay = weather?.isDay ?: astronomicalDay,
            updatedAt = updatedAt,
            hasWeatherError = hasWeatherError,
            isStale = stale && weather != null,
            latitude = latitude,
            longitude = longitude,
            timezoneId = timezoneId,
            displayTime = now,
            displayDate = dateLabel,
            apparentTemperature = weather?.apparentTemperature,
            relativeHumidity = weather?.relativeHumidity,
            windSpeedKmh = weather?.windSpeedKmh,
            surfacePressureHpa = weather?.surfacePressureHpa,
        )
    }
}
