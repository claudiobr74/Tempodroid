package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCodeMapper
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.weather.OpenMeteoResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyPaletteAndWeatherTest {

    @Test
    fun palettesDifferAcrossSkyStates() {
        val clearDay = SkyPalette.resolve(DayPeriod.DAY, WeatherCondition.CLEAR, MoonPhase.WAXING_CRESCENT, 0.2)
        val cloudy = SkyPalette.resolve(DayPeriod.DAY, WeatherCondition.CLOUDY, MoonPhase.WAXING_CRESCENT, 0.2)
        val rain = SkyPalette.resolve(DayPeriod.DAY, WeatherCondition.RAIN, MoonPhase.WAXING_GIBBOUS, 0.8)
        val sunrise = SkyPalette.resolve(DayPeriod.DAWN, WeatherCondition.CLEAR, MoonPhase.WANING_CRESCENT, 0.2)
        val sunset = SkyPalette.resolve(DayPeriod.SUNSET, WeatherCondition.CLEAR, MoonPhase.FULL_MOON, 1.0)
        val night = SkyPalette.resolve(DayPeriod.NIGHT, WeatherCondition.CLEAR, MoonPhase.FULL_MOON, 1.0)
        val nightNew = SkyPalette.resolve(DayPeriod.NIGHT, WeatherCondition.CLEAR, MoonPhase.NEW_MOON, 0.02)
        assertNotEquals(clearDay.zenith, cloudy.zenith)
        assertNotEquals(clearDay.zenith, rain.zenith)
        assertNotEquals(sunrise.horizon, sunset.horizon)
        assertNotEquals(night.starAlpha, nightNew.starAlpha)
        assertTrue(nightNew.starAlpha > night.starAlpha)
        assertTrue(rain.zenith != clearDay.glow)
    }

    @Test
    fun stormIsDarkerThanClear() {
        val clear = SkyPalette.resolve(DayPeriod.DAY, WeatherCondition.CLEAR, MoonPhase.FULL_MOON, 1.0)
        val storm = SkyPalette.resolve(DayPeriod.DAY, WeatherCondition.STORM, MoonPhase.FULL_MOON, 1.0)
        val clearBright = (clear.mid shr 16) and 0xFF
        val stormBright = (storm.mid shr 16) and 0xFF
        assertTrue("clear=$clearBright storm=$stormBright", stormBright < clearBright)
    }

    @Test
    fun openMeteoCodesMapToInternalEnum() {
        assertEquals(WeatherCondition.CLEAR, WeatherCodeMapper.fromOpenMeteo(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCodeMapper.fromOpenMeteo(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherCodeMapper.fromOpenMeteo(3))
        assertEquals(WeatherCondition.FOG, WeatherCodeMapper.fromOpenMeteo(45))
        assertEquals(WeatherCondition.RAIN, WeatherCodeMapper.fromOpenMeteo(61))
        assertEquals(WeatherCondition.HEAVY_RAIN, WeatherCodeMapper.fromOpenMeteo(65))
        assertEquals(WeatherCondition.STORM, WeatherCodeMapper.fromOpenMeteo(95))
    }

    @Test
    fun openMeteoParserReadsCurrentAndDaily() {
        val json = """
            {
              "timezone": "America/Sao_Paulo",
              "current": {
                "time": "2026-09-17T15:30",
                "temperature_2m": 33.2,
                "weather_code": 0,
                "cloud_cover": 8,
                "precipitation": 0.0,
                "is_day": 1
              },
              "daily": {
                "temperature_2m_max": [35.1],
                "temperature_2m_min": [19.4],
                "precipitation_probability_max": [0]
              }
            }
        """.trimIndent()
        val data = OpenMeteoResponseParser.parse(json)
        assertEquals(33, data.temperature)
        assertEquals(WeatherCondition.CLEAR, data.condition)
        assertEquals(35, data.high)
        assertEquals(19, data.low)
        assertEquals(0, data.rainChance)
        assertEquals(8, data.cloudCover)
        assertTrue(data.isDay)
    }
}
