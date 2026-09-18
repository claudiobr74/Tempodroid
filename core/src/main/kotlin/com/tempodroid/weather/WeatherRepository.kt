package com.tempodroid.weather

import java.time.Instant

/**
 * Dado bruto de clima, independente de apresentação.
 */
data class WeatherData(
    val temperature: Int?,
    val condition: WeatherCondition,
    val high: Int?,
    val low: Int?,
    val rainChance: Int?,
    val cloudCover: Int?,
    val precipitation: Double?,
    val weatherCode: Int?,
    val isDay: Boolean,
    val timestamp: Instant?,
    val timezoneId: String? = null,
    val cityName: String? = null,
    val apparentTemperature: Int? = null,
    val relativeHumidity: Int? = null,
    val windSpeedKmh: Int? = null,
    val surfacePressureHpa: Int? = null,
)

interface WeatherProvider {
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherData
}

interface WeatherRepository {
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherData
}
