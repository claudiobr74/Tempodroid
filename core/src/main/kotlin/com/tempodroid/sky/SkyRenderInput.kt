package com.tempodroid.sky

import com.tempodroid.astronomy.CelestialPosition
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition
import java.time.LocalTime

data class SkyRenderInput(
    val widthPx: Int,
    val heightPx: Int,
    val localTime: LocalTime,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val weatherCondition: WeatherCondition,
    val cloudCover: Int?,
    val precipitation: Double?,
    val sunPosition: CelestialPosition?,
    val moonPosition: CelestialPosition?,
    val moonPhase: MoonPhase,
    val moonIllumination: Double,
    val hemisphere: Hemisphere,
    val solarNoon: LocalTime? = null,
)
