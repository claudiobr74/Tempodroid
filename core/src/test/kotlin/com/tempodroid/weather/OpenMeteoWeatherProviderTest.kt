package com.tempodroid.weather

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class OpenMeteoWeatherProviderTest {
    @Test
    fun buildsHttpsUrlWithoutUiCoupling() {
        val provider = OpenMeteoWeatherProvider(http = { error("not called") })
        val url = provider.buildUrl(-16.6869, -49.2648)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast"))
        assertTrue(url.contains("latitude=-16.6869"))
        assertTrue(url.contains("weather_code"))
        assertTrue(url.contains("apparent_temperature"))
        assertTrue(url.contains("wind_speed_10m"))
        assertTrue(url.contains("hourly=temperature_2m,precipitation_probability,weather_code"))
        assertTrue(url.contains("daily=weather_code"))
        assertTrue(url.contains("forecast_days=6"))
        assertTrue(!url.contains("relative_humidity_2m"))
        assertTrue(!url.contains("surface_pressure"))
        assertTrue(!url.contains("ui"))
    }

    @Test
    fun mapsProviderPayloadToDomain() = runBlocking {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":27.6,"apparent_temperature":29.2,"wind_speed_10m":11.4,"weather_code":61,"cloud_cover":90,"precipitation":1.2,"is_day":1,"time":"2026-09-17T16:40"},
             "hourly":{"time":["2026-09-17T17:00","2026-09-17T18:00"],"temperature_2m":[28.1,27.4],"precipitation_probability":[70,80],"weather_code":[61,63]},
             "daily":{"time":["2026-09-17","2026-09-18"],"weather_code":[61,3],"temperature_2m_max":[28.0,29.0],"temperature_2m_min":[22.0,21.0],"precipitation_probability_max":[85,40]}}
        """.trimIndent()
        val provider = OpenMeteoWeatherProvider(http = { json })
        val data = provider.fetchWeather(0.0, 0.0)
        assertEquals(28, data.high)
        assertEquals(WeatherCondition.RAIN, data.condition)
        assertEquals(70, data.rainChance)
        assertEquals(29, data.apparentTemperature)
        assertEquals(11, data.windSpeedKmh)
        assertEquals("America/Sao_Paulo", data.timezoneId)
        assertEquals(Instant.parse("2026-09-17T19:40:00Z"), data.timestamp)

        assertEquals(2, data.hourlyForecast.size)
        assertEquals(28, data.hourlyForecast.first().temperature)
        assertEquals(70, data.hourlyForecast.first().rainChance)
        assertEquals(WeatherCondition.RAIN, data.hourlyForecast.first().condition)
        assertEquals(Instant.parse("2026-09-17T20:00:00Z"), data.hourlyForecast.first().time)

        assertEquals(2, data.dailyForecast.size)
        assertEquals(LocalDate.parse("2026-09-18"), data.dailyForecast[1].date)
        assertEquals(29, data.dailyForecast[1].high)
        assertEquals(21, data.dailyForecast[1].low)
        assertEquals(40, data.dailyForecast[1].rainChance)
        assertEquals(WeatherCondition.CLOUDY, data.dailyForecast[1].condition)
    }

    @Test
    fun usesHourlyProbabilityForCurrentHourInsteadOfDailyMaximum() {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":23.0,"weather_code":2,"cloud_cover":55,"precipitation":0.0,"is_day":0,"time":"2026-09-23T22:22"},
             "hourly":{"time":["2026-09-23T21:00","2026-09-23T22:00","2026-09-23T23:00"],"temperature_2m":[24.0,23.0,22.0],"precipitation_probability":[18,27,42],"weather_code":[2,2,61]},
             "daily":{"time":["2026-09-23"],"weather_code":[61],"temperature_2m_max":[30.0],"temperature_2m_min":[20.0],"precipitation_probability_max":[81]}}
        """.trimIndent()

        val data = OpenMeteoResponseParser.parse(json)

        assertEquals(27, data.rainChance)
        assertEquals(81, data.dailyForecast.first().rainChance)
    }

    @Test
    fun fallsBackToDailyMaximumWhenHourlyProbabilityIsUnavailable() {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":23.0,"weather_code":2,"is_day":0,"time":"2026-09-23T22:22"},
             "daily":{"time":["2026-09-23"],"weather_code":[61],"temperature_2m_max":[30.0],"temperature_2m_min":[20.0],"precipitation_probability_max":[81]}}
        """.trimIndent()

        val data = OpenMeteoResponseParser.parse(json)

        assertEquals(81, data.rainChance)
    }

    @Test
    fun mapsSnowCodesAsSnow() {
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromOpenMeteo(71))
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromOpenMeteo(75))
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromOpenMeteo(85))
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromOpenMeteo(86))
    }

    @Test
    fun rejectsPayloadWithoutWeatherCode() {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":27.6,"is_day":1,"time":"2026-09-17T16:40"}}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            OpenMeteoResponseParser.parse(json)
        }
    }

    @Test
    fun rejectsPayloadWithoutIsDay() {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":27.6,"weather_code":0,"time":"2026-09-17T16:40"}}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            OpenMeteoResponseParser.parse(json)
        }
    }
}
