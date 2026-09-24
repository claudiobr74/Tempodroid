package com.tempodroid.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val current: Current? = null,
    val hourly: Hourly? = null,
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
    data class Hourly(
        val time: List<String?>? = null,
        @SerialName("temperature_2m") val temperature: List<Double?>? = null,
        @SerialName("precipitation_probability") val rainChance: List<Int?>? = null,
        @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    )

    @Serializable
    data class Daily(
        val time: List<String?>? = null,
        @SerialName("weather_code") val weatherCode: List<Int?>? = null,
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
        val current = requireNotNull(response.current) {
            "Open-Meteo retornou payload sem bloco current"
        }
        val code = requireNotNull(current.weatherCode) {
            "Open-Meteo retornou payload sem weather_code"
        }
        val isDayRaw = requireNotNull(current.isDay) {
            "Open-Meteo retornou payload sem is_day"
        }
        require(isDayRaw == 0 || isDayRaw == 1) {
            "Open-Meteo retornou is_day inválido: $isDayRaw"
        }
        val timestamp = current.time?.let { parseInstant(it, response.timezone) }
        val hourlyForecast = parseHourly(response.hourly, response.timezone)
        val currentRainChance = rainChanceForCurrentHour(
            timestamp = timestamp,
            timezoneId = response.timezone,
            hourlyForecast = hourlyForecast,
        )
        return WeatherData(
            temperature = current.temperature?.roundToInt(),
            condition = WeatherCodeMapper.fromOpenMeteo(code),
            high = response.daily?.temperatureMax?.firstOrNull()?.roundToInt(),
            low = response.daily?.temperatureMin?.firstOrNull()?.roundToInt(),
            rainChance = currentRainChance ?: response.daily?.rainChance?.firstOrNull(),
            cloudCover = current.cloudCover,
            precipitation = current.precipitation,
            weatherCode = code,
            isDay = isDayRaw == 1,
            timestamp = timestamp,
            timezoneId = response.timezone,
            apparentTemperature = current.apparentTemperature?.roundToInt(),
            relativeHumidity = current.relativeHumidity,
            windSpeedKmh = current.windSpeed?.roundToInt(),
            surfacePressureHpa = current.surfacePressure?.roundToInt(),
            hourlyForecast = hourlyForecast,
            dailyForecast = parseDaily(response.daily),
        )
    }

    private fun rainChanceForCurrentHour(
        timestamp: Instant?,
        timezoneId: String?,
        hourlyForecast: List<HourlyForecast>,
    ): Int? {
        val candidates = hourlyForecast.filter { it.rainChance != null }
        if (candidates.isEmpty()) return null
        if (timestamp == null) return candidates.first().rainChance

        val zone = timezoneId
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.of("UTC")
        val targetHour = timestamp.atZone(zone).truncatedTo(ChronoUnit.HOURS)

        candidates.firstOrNull { item ->
            item.time.atZone(zone).truncatedTo(ChronoUnit.HOURS) == targetHour
        }?.let { return it.rainChance }

        return candidates.minByOrNull { item ->
            abs(Duration.between(timestamp, item.time).toMinutes())
        }?.rainChance
    }

    private fun parseHourly(hourly: OpenMeteoResponse.Hourly?, timezoneId: String?): List<HourlyForecast> {
        val times = hourly?.time.orEmpty()
        val temperatures = hourly?.temperature.orEmpty()
        val rainChances = hourly?.rainChance.orEmpty()
        val weatherCodes = hourly?.weatherCode.orEmpty()
        return times.mapIndexedNotNull { index, value ->
            val instant = value?.let { parseInstant(it, timezoneId) } ?: return@mapIndexedNotNull null
            val code = weatherCodes.getOrNull(index) ?: return@mapIndexedNotNull null
            HourlyForecast(
                time = instant,
                temperature = temperatures.getOrNull(index)?.roundToInt(),
                rainChance = rainChances.getOrNull(index),
                condition = WeatherCodeMapper.fromOpenMeteo(code),
            )
        }
    }

    private fun parseDaily(daily: OpenMeteoResponse.Daily?): List<DailyForecast> {
        val dates = daily?.time.orEmpty()
        val temperaturesMax = daily?.temperatureMax.orEmpty()
        val temperaturesMin = daily?.temperatureMin.orEmpty()
        val rainChances = daily?.rainChance.orEmpty()
        val weatherCodes = daily?.weatherCode.orEmpty()
        return dates.mapIndexedNotNull { index, value ->
            val date = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return@mapIndexedNotNull null
            val code = weatherCodes.getOrNull(index) ?: return@mapIndexedNotNull null
            DailyForecast(
                date = date,
                high = temperaturesMax.getOrNull(index)?.roundToInt(),
                low = temperaturesMin.getOrNull(index)?.roundToInt(),
                rainChance = rainChances.getOrNull(index),
                condition = WeatherCodeMapper.fromOpenMeteo(code),
            )
        }
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
