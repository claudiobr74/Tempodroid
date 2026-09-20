package com.tempodroid.astronomy

enum class MoonPhase {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT
}

enum class Hemisphere {
    NORTH,
    SOUTH
}

enum class MoonOrientation {
    LIGHT_FROM_LEFT,
    LIGHT_FROM_RIGHT
}

enum class MoonRenderStyle {
    SKY,
    TIMELINE
}

data class CelestialPosition(
    val normalizedX: Float,
    val normalizedY: Float,
    val aboveHorizon: Boolean,
    val altitudeDegrees: Double? = null,
    val azimuthDegrees: Double? = null,
)

data class AstronomyData(
    val sunrise: java.time.LocalTime?,
    val sunset: java.time.LocalTime?,
    val solarNoon: java.time.LocalTime?,
    val moonrise: java.time.LocalTime?,
    val moonset: java.time.LocalTime?,
    val moonPhase: MoonPhase,
    val moonIllumination: Double,
    val sunPosition: CelestialPosition?,
    val moonPosition: CelestialPosition?,
    val hemisphere: Hemisphere,
)

fun hemisphereFromLatitude(latitude: Double): Hemisphere =
    if (latitude < 0.0) Hemisphere.SOUTH else Hemisphere.NORTH
