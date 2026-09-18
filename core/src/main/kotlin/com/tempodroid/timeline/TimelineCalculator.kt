package com.tempodroid.timeline

import com.tempodroid.astronomy.CelestialPosition
import com.tempodroid.astronomy.MoonPhase
import java.time.LocalTime

data class TimelineModels(
    val currentPosition: Float,
    val sunrisePosition: Float,
    val sunsetPosition: Float,
    val solarNoonPosition: Float,
    val moonrisePosition: Float?,
    val moonsetPosition: Float?,
    val nowLabel: String,
    val sunrise: LocalTime,
    val sunset: LocalTime,
)

object TimelineCalculator {
    fun timeToNormalizedPosition(time: LocalTime): Float {
        val minutes = time.hour * 60 + time.minute
        return (minutes / 1440f).coerceIn(0f, 1f)
    }

    fun fromMinutes(totalMinutes: Int): Float =
        (totalMinutes.coerceIn(0, 1439) / 1440f).coerceIn(0f, 1f)

    fun build(
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
        solarNoon: LocalTime?,
        moonrise: LocalTime?,
        moonset: LocalTime?,
    ): TimelineModels = TimelineModels(
        currentPosition = timeToNormalizedPosition(now),
        sunrisePosition = timeToNormalizedPosition(sunrise),
        sunsetPosition = timeToNormalizedPosition(sunset),
        solarNoonPosition = timeToNormalizedPosition(solarNoon ?: LocalTime.NOON),
        moonrisePosition = moonrise?.let { timeToNormalizedPosition(it) },
        moonsetPosition = moonset?.let { timeToNormalizedPosition(it) },
        nowLabel = formatClock(now),
        sunrise = sunrise,
        sunset = sunset,
    )

    fun formatClock(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)
}

data class TimelineMarker(
    val position: Float,
    val title: String,
    val subtitle: String,
)

object TimelineLabels {
    /**
     * A timeline refinada possui apenas as duas âncoras astronômicas fixas.
     * O terceiro marcador ("agora") é dinâmico e renderizado separadamente na
     * sua posição cronológica real.
     */
    fun defaultMarkers(sunrise: LocalTime, sunset: LocalTime): List<TimelineMarker> = listOf(
        TimelineMarker(
            TimelineCalculator.timeToNormalizedPosition(sunrise),
            "Nascer do sol",
            TimelineCalculator.formatClock(sunrise),
        ),
        TimelineMarker(
            TimelineCalculator.timeToNormalizedPosition(sunset),
            "Pôr do sol",
            TimelineCalculator.formatClock(sunset),
        ),
    )
}

data class TimelineRenderInput(
    val widthPx: Int,
    val heightPx: Int,
    val now: LocalTime,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val solarNoon: LocalTime?,
    val moonPhase: MoonPhase,
    val moonIllumination: Double,
    val moonPosition: CelestialPosition?,
)
