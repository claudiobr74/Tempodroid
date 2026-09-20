package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FigmaVisualStateResolverTest {

    @Test
    fun `matriz canonica possui 120 variantes`() {
        val variants = mutableSetOf<Triple<FigmaWeatherState, FigmaMatrixPeriod, MoonPhase>>()
        FigmaWeatherState.values().forEach { weather ->
            FigmaMatrixPeriod.values().forEach { period ->
                MoonPhase.values().forEach { phase -> variants += Triple(weather, period, phase) }
            }
        }
        assertEquals(120, variants.size)
    }

    @Test
    fun `condicoes extras do provider convergem para os cinco estados`() {
        assertEquals(FigmaWeatherState.CLOUDY, FigmaVisualStateResolver.canonicalWeather(WeatherCondition.FOG))
        assertEquals(FigmaWeatherState.CLOUDY, FigmaVisualStateResolver.canonicalWeather(WeatherCondition.SNOW))
        assertEquals(FigmaWeatherState.RAIN, FigmaVisualStateResolver.canonicalWeather(WeatherCondition.HEAVY_RAIN))
    }

    @Test
    fun `dia possui cinco fundos climaticos independentes`() {
        assertEquals(FigmaSkyState.DAY_CLEAR, FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.CLEAR))
        assertEquals(FigmaSkyState.DAY_PARTLY_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.PARTLY_CLOUDY))
        assertEquals(FigmaSkyState.DAY_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.CLOUDY))
        assertEquals(FigmaSkyState.DAY_RAIN, FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.RAIN))
        assertEquals(FigmaSkyState.DAY_STORM, FigmaVisualStateResolver.background(DayPeriod.DAY, WeatherCondition.STORM))
    }

    @Test
    fun `entardecer possui cinco fundos climaticos independentes`() {
        assertEquals(FigmaSkyState.SUNSET_CLEAR, FigmaVisualStateResolver.background(DayPeriod.TWILIGHT, WeatherCondition.CLEAR))
        assertEquals(FigmaSkyState.SUNSET_PARTLY_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.TWILIGHT, WeatherCondition.PARTLY_CLOUDY))
        assertEquals(FigmaSkyState.SUNSET_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.TWILIGHT, WeatherCondition.CLOUDY))
        assertEquals(FigmaSkyState.SUNSET_RAIN, FigmaVisualStateResolver.background(DayPeriod.TWILIGHT, WeatherCondition.RAIN))
        assertEquals(FigmaSkyState.SUNSET_STORM, FigmaVisualStateResolver.background(DayPeriod.TWILIGHT, WeatherCondition.STORM))
    }

    @Test
    fun `noite possui cinco fundos climaticos independentes`() {
        assertEquals(FigmaSkyState.NIGHT_CLEAR, FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.CLEAR))
        assertEquals(FigmaSkyState.NIGHT_PARTLY_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.PARTLY_CLOUDY))
        assertEquals(FigmaSkyState.NIGHT_CLOUDY, FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.CLOUDY))
        assertEquals(FigmaSkyState.NIGHT_RAIN, FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.RAIN))
        assertEquals(FigmaSkyState.NIGHT_STORM, FigmaVisualStateResolver.background(DayPeriod.NIGHT, WeatherCondition.STORM))
    }

    @Test
    fun `dawn usa a familia de entardecer definida para periodo twilight`() {
        assertEquals(FigmaMatrixPeriod.TWILIGHT, FigmaVisualStateResolver.matrixPeriod(DayPeriod.DAWN))
        assertEquals(FigmaSkyState.SUNSET_CLEAR, FigmaVisualStateResolver.background(DayPeriod.DAWN, WeatherCondition.CLEAR))
        assertEquals(FigmaSkyState.SUNSET_RAIN, FigmaVisualStateResolver.background(DayPeriod.DAWN, WeatherCondition.RAIN))
    }

    @Test
    fun `todos os quinze backgrounds do Figma sao alcancaveis e unicos`() {
        val periods = listOf(DayPeriod.DAY, DayPeriod.TWILIGHT, DayPeriod.NIGHT)
        val conditions = listOf(
            WeatherCondition.CLEAR,
            WeatherCondition.PARTLY_CLOUDY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAIN,
            WeatherCondition.STORM,
        )
        val states = periods.flatMap { period ->
            conditions.map { condition -> FigmaVisualStateResolver.background(period, condition) }
        }.toSet()
        assertEquals(15, states.size)
        assertEquals(FigmaSkyState.values().toSet(), states)
    }

    @Test
    fun `crossfade do ocaso progride sem salto`() {
        val before = FigmaVisualStateResolver.backgroundBlend(
            now = LocalTime.of(17, 20),
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 10),
            condition = WeatherCondition.CLEAR,
        )
        val middle = FigmaVisualStateResolver.backgroundBlend(
            now = LocalTime.of(17, 45),
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 10),
            condition = WeatherCondition.CLEAR,
        )
        val after = FigmaVisualStateResolver.backgroundBlend(
            now = LocalTime.of(18, 30),
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 10),
            condition = WeatherCondition.CLEAR,
        )

        assertEquals(FigmaSkyState.DAY_CLEAR, before.base)
        assertEquals(FigmaSkyState.SUNSET_CLEAR, middle.overlay)
        assertTrue(middle.overlayAlpha in 0.01f..0.99f)
        assertEquals(FigmaSkyState.SUNSET_CLEAR, after.base)
        assertEquals(FigmaSkyState.NIGHT_CLEAR, after.overlay)
    }

    @Test
    fun `opacidades lunares permanecem inalteradas`() {
        assertEquals(0.34f, FigmaVisualStateResolver.moonOpacity(DayPeriod.DAY, WeatherCondition.CLEAR), 0f)
        assertEquals(0.26f, FigmaVisualStateResolver.moonOpacity(DayPeriod.DAY, WeatherCondition.PARTLY_CLOUDY), 0f)
        assertEquals(0.14f, FigmaVisualStateResolver.moonOpacity(DayPeriod.DAY, WeatherCondition.CLOUDY), 0f)
        assertEquals(0.07f, FigmaVisualStateResolver.moonOpacity(DayPeriod.DAY, WeatherCondition.RAIN), 0f)
        assertEquals(0.00f, FigmaVisualStateResolver.moonOpacity(DayPeriod.DAY, WeatherCondition.STORM), 0f)
        assertEquals(0.70f, FigmaVisualStateResolver.moonOpacity(DayPeriod.TWILIGHT, WeatherCondition.CLEAR), 0f)
        assertEquals(0.55f, FigmaVisualStateResolver.moonOpacity(DayPeriod.TWILIGHT, WeatherCondition.PARTLY_CLOUDY), 0f)
        assertEquals(0.28f, FigmaVisualStateResolver.moonOpacity(DayPeriod.TWILIGHT, WeatherCondition.CLOUDY), 0f)
        assertEquals(0.12f, FigmaVisualStateResolver.moonOpacity(DayPeriod.TWILIGHT, WeatherCondition.RAIN), 0f)
        assertEquals(0.02f, FigmaVisualStateResolver.moonOpacity(DayPeriod.TWILIGHT, WeatherCondition.STORM), 0f)
        assertEquals(1.00f, FigmaVisualStateResolver.moonOpacity(DayPeriod.NIGHT, WeatherCondition.CLEAR), 0f)
        assertEquals(0.82f, FigmaVisualStateResolver.moonOpacity(DayPeriod.NIGHT, WeatherCondition.PARTLY_CLOUDY), 0f)
        assertEquals(0.42f, FigmaVisualStateResolver.moonOpacity(DayPeriod.NIGHT, WeatherCondition.CLOUDY), 0f)
        assertEquals(0.18f, FigmaVisualStateResolver.moonOpacity(DayPeriod.NIGHT, WeatherCondition.RAIN), 0f)
        assertEquals(0.04f, FigmaVisualStateResolver.moonOpacity(DayPeriod.NIGHT, WeatherCondition.STORM), 0f)
    }

    @Test
    fun `escala lunar continua 48 58 e 66 sobre frame 160`() {
        assertEquals(48f / 160f, FigmaVisualStateResolver.moonComponentSizeFraction(DayPeriod.DAY), 0f)
        assertEquals(58f / 160f, FigmaVisualStateResolver.moonComponentSizeFraction(DayPeriod.TWILIGHT), 0f)
        assertEquals(66f / 160f, FigmaVisualStateResolver.moonComponentSizeFraction(DayPeriod.NIGHT), 0f)
    }
}
