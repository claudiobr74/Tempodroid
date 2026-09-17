package com.tempodroid.astronomy

/**
 * Geometria do terminador lunar (máscara esférica, não recorte retangular).
 *
 * A parte iluminada é a união/interseção de um semicírculo com uma elipse
 * cujo semi-eixo horizontal = |2e - 1| · r, onde e é a iluminação 0..1.
 */
object MoonTerminator {
    fun isGibbous(illumination: Double): Boolean = illumination >= 0.5

    fun ellipseHalfWidth(illumination: Double, radius: Float): Float {
        val scale = kotlin.math.abs(illumination.coerceIn(0.0, 1.0) * 2.0 - 1.0)
        return (scale * radius).toFloat()
    }

    fun isWaxing(phase: MoonPhase): Boolean = when (phase) {
        MoonPhase.NEW_MOON,
        MoonPhase.WAXING_CRESCENT,
        MoonPhase.FIRST_QUARTER,
        MoonPhase.WAXING_GIBBOUS,
        MoonPhase.FULL_MOON,
        -> true
        MoonPhase.WANING_GIBBOUS,
        MoonPhase.LAST_QUARTER,
        MoonPhase.WANING_CRESCENT,
        -> false
    }

    fun nightAmbientBoost(phase: MoonPhase, illumination: Double): Float = when (phase) {
        MoonPhase.NEW_MOON -> 0f
        MoonPhase.WAXING_CRESCENT, MoonPhase.WANING_CRESCENT -> 0.12f
        MoonPhase.FIRST_QUARTER, MoonPhase.LAST_QUARTER -> 0.28f
        MoonPhase.WAXING_GIBBOUS, MoonPhase.WANING_GIBBOUS -> 0.48f
        MoonPhase.FULL_MOON -> 0.72f
    } * illumination.coerceIn(0.0, 1.0).toFloat()

    fun skyVisibility(
        aboveHorizon: Boolean,
        isDay: Boolean,
        weather: com.tempodroid.weather.WeatherCondition,
    ): Float {
        if (!aboveHorizon) return 0f
        val weatherFactor = when (weather) {
            com.tempodroid.weather.WeatherCondition.CLEAR -> 1f
            com.tempodroid.weather.WeatherCondition.PARTLY_CLOUDY -> 0.78f
            com.tempodroid.weather.WeatherCondition.CLOUDY -> 0.35f
            com.tempodroid.weather.WeatherCondition.FOG -> 0.15f
            com.tempodroid.weather.WeatherCondition.RAIN -> 0.12f
            com.tempodroid.weather.WeatherCondition.HEAVY_RAIN -> 0.06f
            com.tempodroid.weather.WeatherCondition.STORM -> 0f
        }
        val dayFactor = if (isDay) 0.28f else 1f
        return (weatherFactor * dayFactor).coerceIn(0f, 1f)
    }
}
