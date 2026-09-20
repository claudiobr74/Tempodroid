package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.minutesOf
import com.tempodroid.weather.WeatherCondition
import java.time.LocalTime

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

data class FigmaBackgroundBlend(
    val base: FigmaSkyState,
    val overlay: FigmaSkyState? = null,
    val overlayAlpha: Float = 0f,
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
        WeatherCondition.CLOUDY, WeatherCondition.FOG, WeatherCondition.SNOW -> FigmaWeatherState.CLOUDY
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
     * Faz crossfade entre os três conjuntos de fundo nas janelas próximas ao
     * nascer e ao ocaso. Isso evita a troca abrupta de JPEG quando o relógio
     * cruza um limite de período, mantendo os 15 assets como fonte visual.
     */
    fun backgroundBlend(
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
        condition: WeatherCondition,
    ): FigmaBackgroundBlend {
        val weather = canonicalWeather(condition)
        val day = dayBackground(weather)
        val twilight = sunsetBackground(weather)
        val night = nightBackground(weather)
        val nowM = minutesOf(now)
        val rise = minutesOf(sunrise)
        val set = minutesOf(sunset)

        // Cenários extremos/polares são mantidos no comportamento discreto
        // até o modelo solar fornecer explicitamente estado polar.
        if (rise >= set || set - rise < 120) {
            val period = DayPeriodCalculator.from(now, sunrise, sunset, null)
            return FigmaBackgroundBlend(background(period, condition))
        }

        val dawnStart = (rise - 40).coerceAtLeast(0)
        val dayStart = (rise + 45).coerceAtMost(set)
        val sunsetStart = (set - 50).coerceAtLeast(dayStart)
        val nightStart = (set + 40).coerceAtMost(24 * 60 - 1)

        return when {
            nowM < dawnStart -> FigmaBackgroundBlend(night)
            nowM < rise -> blend(night, twilight, progress(nowM, dawnStart, rise))
            nowM < dayStart -> blend(twilight, day, progress(nowM, rise, dayStart))
            nowM < sunsetStart -> FigmaBackgroundBlend(day)
            nowM < set -> blend(day, twilight, progress(nowM, sunsetStart, set))
            nowM < nightStart -> blend(twilight, night, progress(nowM, set, nightStart))
            else -> FigmaBackgroundBlend(night)
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

    private fun blend(base: FigmaSkyState, overlay: FigmaSkyState, raw: Float): FigmaBackgroundBlend {
        val t = raw.coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)
        return when {
            smooth <= 0.001f -> FigmaBackgroundBlend(base)
            smooth >= 0.999f -> FigmaBackgroundBlend(overlay)
            else -> FigmaBackgroundBlend(base, overlay, smooth)
        }
    }

    private fun progress(now: Int, start: Int, end: Int): Float {
        val span = (end - start).coerceAtLeast(1)
        return ((now - start).toFloat() / span).coerceIn(0f, 1f)
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
