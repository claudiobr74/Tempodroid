package com.tempodroid.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val current: Current? = null,
    val daily: Daily? = null,
) {
    @Serializable
    data class Current(
        val time: String? = null,
        @SerialName("temperature_2m") val temperature: Double? = null,
        @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
        @SerialName("relative_humidity_2m") val relativeHumidity: Int? = null,
        @SerialName("wind_speed_10m") val windSpeed: Double? = null,
        @SerialName("surface_pressure") val surfacePressure: Double? = null,
        @SerialName("weather_code") val weatherCode: Int? = null,
        @SerialName("cloud_cover") val cloudCover: Int? = null,
        val precipitation: Double? = null,
        @SerialName("is_day") val isDay: Int? = null,
    )

    @Serializable
    data class Daily(
        @SerialName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
        @SerialName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
        @SerialName("precipitation_probability_max") val rainChance: List<Int?>? = null,
        val sunrise: List<String?>? = null,
        val sunset: List<String?>? = null,
    )
}

object OpenMeteoResponseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(body: String): WeatherData {
        val response = json.decodeFromString(OpenMeteoResponse.serializer(), body)
        val current = response.current
        val code = current?.weatherCode ?: 0
        val timestamp = current?.time?.let { parseInstant(it, response.timezone) }
        return WeatherData(
            temperature = current?.temperature?.roundToInt(),
            condition = WeatherCodeMapper.fromOpenMeteo(code),
            high = response.daily?.temperatureMax?.firstOrNull()?.roundToInt(),
            low = response.daily?.temperatureMin?.firstOrNull()?.roundToInt(),
            rainChance = response.daily?.rainChance?.firstOrNull(),
            cloudCover = current?.cloudCover,
            precipitation = current?.precipitation,
            weatherCode = code,
            isDay = (current?.isDay ?: 1) == 1,
            timestamp = timestamp,
            timezoneId = response.timezone,
            apparentTemperature = current?.apparentTemperature?.roundToInt(),
            relativeHumidity = current?.relativeHumidity,
            windSpeedKmh = current?.windSpeed?.roundToInt(),
            surfacePressureHpa = current?.surfacePressure?.roundToInt(),
        )
    }

    private fun parseInstant(value: String, timezoneId: String?): Instant? {
        return try {
            if (value.endsWith("Z")) {
                Instant.parse(value)
            } else {
                val localDateTime = LocalDateTime.parse(value)
                val zone = timezoneId
                    ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                    ?: ZoneId.of("UTC")
                localDateTime.atZone(zone).toInstant()
            }
        } catch (_: Exception) {
            null
        }
    }
}
