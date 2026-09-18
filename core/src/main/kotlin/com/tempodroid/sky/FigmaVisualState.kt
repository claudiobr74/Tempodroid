package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition

enum class FigmaWeatherState {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    STORM,
}

enum class FigmaMatrixPeriod {
    DAY,
    TWILIGHT,
    NIGHT,
}

enum class FigmaSkyState {
    CLEAR_DAY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    STORM,
    SUNRISE,
    SUNSET,
    CLEAR_NIGHT,
    CLOUDY_NIGHT,
    RAIN_NIGHT,
}

data class FigmaMoonWeatherVariant(
    val weather: FigmaWeatherState,
    val period: FigmaMatrixPeriod,
    val moonPhase: MoonPhase,
)

/**
 * Fonte de verdade para traduzir o estado meteorológico/astronômico do app
 * para a arquitetura visual do arquivo final do Figma.
 *
 * O componente MoonWeatherSky do Figma possui 5 condições × 3 períodos ×
 * 8 fases lunares = 120 variantes. Os dez fundos DynamicSkyBackground são
 * compartilhados entre essas variantes.
 */
object FigmaVisualStateResolver {

    fun canonicalWeather(condition: WeatherCondition): FigmaWeatherState = when (condition) {
        WeatherCondition.CLEAR -> FigmaWeatherState.CLEAR
        WeatherCondition.PARTLY_CLOUDY -> FigmaWeatherState.PARTLY_CLOUDY
        WeatherCondition.CLOUDY, WeatherCondition.FOG -> FigmaWeatherState.CLOUDY
        WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> FigmaWeatherState.RAIN
        WeatherCondition.STORM -> FigmaWeatherState.STORM
    }

    fun matrixPeriod(period: DayPeriod): FigmaMatrixPeriod = when (period) {
        DayPeriod.NIGHT -> FigmaMatrixPeriod.NIGHT
        DayPeriod.DAWN, DayPeriod.SUNSET, DayPeriod.TWILIGHT -> FigmaMatrixPeriod.TWILIGHT
        DayPeriod.MORNING, DayPeriod.DAY, DayPeriod.AFTERNOON -> FigmaMatrixPeriod.DAY
    }

    fun variant(
        period: DayPeriod,
        condition: WeatherCondition,
        moonPhase: MoonPhase,
    ): FigmaMoonWeatherVariant = FigmaMoonWeatherVariant(
        weather = canonicalWeather(condition),
        period = matrixPeriod(period),
        moonPhase = moonPhase,
    )

    fun background(period: DayPeriod, condition: WeatherCondition): FigmaSkyState {
        val weather = canonicalWeather(condition)
        return when (period) {
            DayPeriod.DAWN, DayPeriod.MORNING -> when (weather) {
                FigmaWeatherState.CLEAR, FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.SUNRISE
                FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY
                FigmaWeatherState.RAIN -> FigmaSkyState.RAIN
                FigmaWeatherState.STORM -> FigmaSkyState.STORM
            }

            DayPeriod.SUNSET -> when (weather) {
                FigmaWeatherState.CLEAR, FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.SUNSET
                FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY
                FigmaWeatherState.RAIN -> FigmaSkyState.RAIN
                FigmaWeatherState.STORM -> FigmaSkyState.STORM
            }

            DayPeriod.TWILIGHT -> when (weather) {
                FigmaWeatherState.CLEAR -> FigmaSkyState.SUNSET
                FigmaWeatherState.PARTLY_CLOUDY, FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY_NIGHT
                FigmaWeatherState.RAIN -> FigmaSkyState.RAIN_NIGHT
                FigmaWeatherState.STORM -> FigmaSkyState.STORM
            }

            DayPeriod.NIGHT -> nightBackground(weather)
            DayPeriod.DAY, DayPeriod.AFTERNOON -> dayBackground(weather)
        }
    }

    private fun dayBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.CLEAR_DAY
        FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.PARTLY_CLOUDY
        FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY
        FigmaWeatherState.RAIN -> FigmaSkyState.RAIN
        FigmaWeatherState.STORM -> FigmaSkyState.STORM
    }

    private fun nightBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.CLEAR_NIGHT
        FigmaWeatherState.PARTLY_CLOUDY, FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY_NIGHT
        FigmaWeatherState.RAIN -> FigmaSkyState.RAIN_NIGHT
        FigmaWeatherState.STORM -> FigmaSkyState.STORM
    }
}
