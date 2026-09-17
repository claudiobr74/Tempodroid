package com.tempodroid.timeline

import com.tempodroid.astronomy.CelestialPosition
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.minutesOf
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
        nowLabel = "%02d:%02d".format(now.hour, now.minute),
        sunrise = sunrise,
        sunset = sunset,
    )
}

data class TimelineMarker(
    val position: Float,
    val title: String,
    val subtitle: String,
)

object TimelineLabels {
    fun defaultMarkers(sunrise: LocalTime, sunset: LocalTime): List<TimelineMarker> = listOf(
        TimelineMarker(0f, "Madrugada", "00h"),
        TimelineMarker(
            TimelineCalculator.timeToNormalizedPosition(sunrise),
            "Nascer do sol",
            "%02dh".format(sunrise.hour),
        ),
        TimelineMarker(0.5f, "Dia", "12h"),
        TimelineMarker(
            TimelineCalculator.timeToNormalizedPosition(sunset),
            "Pôr do sol",
            "%02dh".format(sunset.hour),
        ),
        TimelineMarker(1f, "Noite", "24h"),
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
