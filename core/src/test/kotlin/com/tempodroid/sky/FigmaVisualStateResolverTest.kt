package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class FigmaVisualStateResolverTest {

    @Test
    fun `matriz canonica possui 120 variantes`() {
        val variants = mutableSetOf<Triple<FigmaWeatherState, FigmaMatrixPeriod, MoonPhase>>()
        FigmaWeatherState.values().forEach { weather ->
            FigmaMatrixPeriod.values().forEach { period ->
                MoonPhase.values().forEach { phase ->
                    variants += Triple(weather, period, phase)
                }
            }
        }
        assertEquals(120, variants.size)
    }

    @Test
    fun `condicoes extras do provider convergem para os cinco estados do Figma`() {
        assertEquals(FigmaWeatherState.CLOUDY, FigmaVisualStateResolver.canonicalWeather(WeatherCondition.FOG))
        assertEquals(FigmaWeatherState.RAIN, FigmaVisualStateResolver.canonicalWeather(WeatherCondition.HEAVY_RAIN))
    }

    @Test
    fun `todos os dez backgrounds do Figma sao alcancaveis`() {
        val states = setOf(
            FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.CLEAR),
            FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.PARTLY_CLOUDY),
            FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.CLOUDY),
            FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.RAIN),
            FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.STORM),
            FigmaVisualStateResolver.background(DayPeriod.DAWN, WeatherCondition.CLEAR),
            FigmaVisualStateResolver.background(DayPeriod.SUNSET, WeatherCondition.CLEAR),
            FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.CLEAR),
            FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.CLOUDY),
            FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.RAIN),
        )
        assertEquals(FigmaSkyState.values().toSet(), states)
    }
}
