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
 * MoonWeatherSky possui 5 condições × 3 períodos × 8 fases lunares = 120
 * variantes. DynamicSkyBackground possui dez rasters compartilhados.
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
            DayPeriod.DAWN -> when (weather) {
                FigmaWeatherState.CLEAR, FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.SUNRISE
                FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY
                FigmaWeatherState.RAIN -> FigmaSkyState.RAIN
                FigmaWeatherState.STORM -> FigmaSkyState.STORM
            }

            DayPeriod.MORNING, DayPeriod.DAY, DayPeriod.AFTERNOON -> dayBackground(weather)
            DayPeriod.SUNSET, DayPeriod.TWILIGHT -> twilightBackground(weather)
            DayPeriod.NIGHT -> nightBackground(weather)
        }
    }

    /**
     * Opacidades lidas diretamente das 120 variantes MoonWeatherSky.
     * Abaixo do horizonte o chamador ainda deve ocultar completamente a Lua.
     */
    fun moonOpacity(period: DayPeriod, condition: WeatherCondition): Float {
        val weather = canonicalWeather(condition)
        return when (matrixPeriod(period)) {
            FigmaMatrixPeriod.DAY -> when (weather) {
                FigmaWeatherState.CLEAR -> 0.34f
                FigmaWeatherState.PARTLY_CLOUDY -> 0.26f
                FigmaWeatherState.CLOUDY -> 0.14f
                FigmaWeatherState.RAIN -> 0.07f
                FigmaWeatherState.STORM -> 0.00f
            }

            FigmaMatrixPeriod.TWILIGHT -> when (weather) {
                FigmaWeatherState.CLEAR -> 0.70f
                FigmaWeatherState.PARTLY_CLOUDY -> 0.55f
                FigmaWeatherState.CLOUDY -> 0.28f
                FigmaWeatherState.RAIN -> 0.12f
                FigmaWeatherState.STORM -> 0.02f
            }

            FigmaMatrixPeriod.NIGHT -> when (weather) {
                FigmaWeatherState.CLEAR -> 1.00f
                FigmaWeatherState.PARTLY_CLOUDY -> 0.82f
                FigmaWeatherState.CLOUDY -> 0.42f
                FigmaWeatherState.RAIN -> 0.18f
                FigmaWeatherState.STORM -> 0.04f
            }
        }
    }

    /**
     * Tamanho da instância MoonVisual em relação à altura de referência 160px:
     * DAY 48px, TWILIGHT 58px e NIGHT 66px.
     */
    fun moonComponentSizeFraction(period: DayPeriod): Float = when (matrixPeriod(period)) {
        FigmaMatrixPeriod.DAY -> 48f / 160f
        FigmaMatrixPeriod.TWILIGHT -> 58f / 160f
        FigmaMatrixPeriod.NIGHT -> 66f / 160f
    }

    private fun dayBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.CLEAR_DAY
        FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.PARTLY_CLOUDY
        FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY
        FigmaWeatherState.RAIN -> FigmaSkyState.RAIN
        FigmaWeatherState.STORM -> FigmaSkyState.STORM
    }

    private fun twilightBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR, FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.SUNSET
        FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY_NIGHT
        FigmaWeatherState.RAIN -> FigmaSkyState.RAIN_NIGHT
        FigmaWeatherState.STORM -> FigmaSkyState.STORM
    }

    private fun nightBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.CLEAR_NIGHT
        FigmaWeatherState.PARTLY_CLOUDY, FigmaWeatherState.CLOUDY -> FigmaSkyState.CLOUDY_NIGHT
        FigmaWeatherState.RAIN -> FigmaSkyState.RAIN_NIGHT
        FigmaWeatherState.STORM -> FigmaSkyState.STORM
    }
}
