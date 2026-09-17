package com.tempodroid.weather

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OpenMeteoWeatherProviderTest {
    @Test
    fun buildsHttpsUrlWithoutUiCoupling() {
        val provider = OpenMeteoWeatherProvider(http = { error("not called") })
        val url = provider.buildUrl(-16.6869, -49.2648)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast"))
        assertTrue(url.contains("latitude=-16.6869"))
        assertTrue(url.contains("weather_code"))
        assertTrue(!url.contains("ui"))
    }

    @Test
    fun mapsProviderPayloadToDomain() = runBlocking {
        val json = """
            {"timezone":"America/Sao_Paulo",
             "current":{"temperature_2m":27.6,"weather_code":61,"cloud_cover":90,"precipitation":1.2,"is_day":1,"time":"2026-09-17T16:40"},
             "daily":{"temperature_2m_max":[28.0],"temperature_2m_min":[22.0],"precipitation_probability_max":[85]}}
        """.trimIndent()
        val provider = OpenMeteoWeatherProvider(http = { json })
        val data = provider.fetchWeather(0.0, 0.0)
        assertEquals(28, data.high)
        assertEquals(WeatherCondition.RAIN, data.condition)
        assertEquals(85, data.rainChance)
        assertEquals("America/Sao_Paulo", data.timezoneId)
        assertEquals(Instant.parse("2026-09-17T19:40:00Z"), data.timestamp)
    }
}
