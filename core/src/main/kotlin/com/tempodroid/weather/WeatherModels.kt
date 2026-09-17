package com.tempodroid.weather

/**
 * Condição atmosférica interna. Providers externos devem mapear para este enum.
 */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    HEAVY_RAIN,
    STORM,
    FOG
}
