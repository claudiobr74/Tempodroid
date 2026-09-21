package com.tempodroid.weather

import java.time.Instant
import java.time.LocalDate

/**
 * Condição atmosférica interna. Providers externos devem mapear para este enum.
 */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    STORM,
    FOG
}

data class HourlyForecast(
    val time: Instant,
    val temperature: Int?,
    val rainChance: Int?,
    val condition: WeatherCondition,
)

data class DailyForecast(
    val date: LocalDate,
    val high: Int?,
    val low: Int?,
    val rainChance: Int?,
    val condition: WeatherCondition,
)
