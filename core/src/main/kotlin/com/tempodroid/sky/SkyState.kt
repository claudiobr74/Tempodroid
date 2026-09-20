package com.tempodroid.sky

import com.tempodroid.astronomy.minutesOf
import com.tempodroid.weather.WeatherCondition
import java.time.LocalTime

enum class DayPeriod {
    DAWN,
    MORNING,
    DAY,
    AFTERNOON,
    SUNSET,
    TWILIGHT,
    NIGHT
}

object DayPeriodCalculator {
    fun from(
        now: LocalTime,
        sunrise: LocalTime?,
        sunset: LocalTime?,
        solarNoon: LocalTime?,
        sunAboveHorizon: Boolean? = null,
    ): DayPeriod {
        if (sunrise == null || sunset == null) {
            return if (sunAboveHorizon == true) DayPeriod.DAY else DayPeriod.NIGHT
        }
        val nowM = minutesOf(now)
        val rise = minutesOf(sunrise)
        val set = minutesOf(sunset)
        val noon = minutesOf(solarNoon ?: midpoint(sunrise, sunset))
        val dawnStart = rise - 40
        val morningEnd = rise + 90
        val afternoonStart = noon
        val sunsetStart = set - 50
        val twilightEnd = set + 40
        return when {
            nowM < dawnStart || nowM >= twilightEnd -> DayPeriod.NIGHT
            nowM < rise -> DayPeriod.DAWN
            nowM < morningEnd -> DayPeriod.MORNING
            nowM < afternoonStart -> DayPeriod.DAY
            nowM < sunsetStart -> DayPeriod.AFTERNOON
            nowM < set -> DayPeriod.SUNSET
            else -> DayPeriod.TWILIGHT
        }
    }

    private fun midpoint(a: LocalTime, b: LocalTime): LocalTime {
        val mid = (minutesOf(a) + minutesOf(b)) / 2
        return LocalTime.of(mid / 60, mid % 60)
    }

    fun isNightLike(period: DayPeriod): Boolean =
        period == DayPeriod.NIGHT || period == DayPeriod.TWILIGHT || period == DayPeriod.DAWN
}

data class SkyState(
    val period: DayPeriod,
    val weather: WeatherCondition,
    val cloudCover: Int,
    val precipitation: Double,
    val moonIllumination: Double,
)
