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

/**
 * Os 15 fundos atuais do componente DynamicSkyBackground no Figma:
 * 5 condições × Dia / Entardecer / Noite.
 */
enum class FigmaSkyState {
    DAY_CLEAR,
    DAY_PARTLY_CLOUDY,
    DAY_CLOUDY,
    DAY_RAIN,
    DAY_STORM,
    SUNSET_CLEAR,
    SUNSET_PARTLY_CLOUDY,
    SUNSET_CLOUDY,
    SUNSET_RAIN,
    SUNSET_STORM,
    NIGHT_CLEAR,
    NIGHT_PARTLY_CLOUDY,
    NIGHT_CLOUDY,
    NIGHT_RAIN,
    NIGHT_STORM,
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
 * variantes. DynamicSkyBackground agora possui 15 rasters exclusivos.
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
        return when (matrixPeriod(period)) {
            FigmaMatrixPeriod.DAY -> dayBackground(weather)
            FigmaMatrixPeriod.TWILIGHT -> sunsetBackground(weather)
            FigmaMatrixPeriod.NIGHT -> nightBackground(weather)
        }
    }

    /**
     * Opacidades da matriz MoonWeatherSky. Abaixo do horizonte o chamador
     * continua ocultando completamente a Lua.
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

    fun moonComponentSizeFraction(period: DayPeriod): Float = when (matrixPeriod(period)) {
        FigmaMatrixPeriod.DAY -> 48f / 160f
        FigmaMatrixPeriod.TWILIGHT -> 58f / 160f
        FigmaMatrixPeriod.NIGHT -> 66f / 160f
    }

    private fun dayBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.DAY_CLEAR
        FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.DAY_PARTLY_CLOUDY
        FigmaWeatherState.CLOUDY -> FigmaSkyState.DAY_CLOUDY
        FigmaWeatherState.RAIN -> FigmaSkyState.DAY_RAIN
        FigmaWeatherState.STORM -> FigmaSkyState.DAY_STORM
    }

    private fun sunsetBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.SUNSET_CLEAR
        FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.SUNSET_PARTLY_CLOUDY
        FigmaWeatherState.CLOUDY -> FigmaSkyState.SUNSET_CLOUDY
        FigmaWeatherState.RAIN -> FigmaSkyState.SUNSET_RAIN
        FigmaWeatherState.STORM -> FigmaSkyState.SUNSET_STORM
    }

    private fun nightBackground(weather: FigmaWeatherState): FigmaSkyState = when (weather) {
        FigmaWeatherState.CLEAR -> FigmaSkyState.NIGHT_CLEAR
        FigmaWeatherState.PARTLY_CLOUDY -> FigmaSkyState.NIGHT_PARTLY_CLOUDY
        FigmaWeatherState.CLOUDY -> FigmaSkyState.NIGHT_CLOUDY
        FigmaWeatherState.RAIN -> FigmaSkyState.NIGHT_RAIN
        FigmaWeatherState.STORM -> FigmaSkyState.NIGHT_STORM
    }
}
