package com.tempodroid.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimelineCalculatorTest {

    @Test
    fun midnightIsZero() {
        assertEquals(0f, TimelineCalculator.timeToNormalizedPosition(LocalTime.MIDNIGHT), 0.0001f)
    }

    @Test
    fun sixAmIsQuarter() {
        assertEquals(0.25f, TimelineCalculator.timeToNormalizedPosition(LocalTime.of(6, 0)), 0.0001f)
    }

    @Test
    fun noonIsHalf() {
        assertEquals(0.50f, TimelineCalculator.timeToNormalizedPosition(LocalTime.NOON), 0.0001f)
    }

    @Test
    fun sixPmIsThreeQuarters() {
        assertEquals(0.75f, TimelineCalculator.timeToNormalizedPosition(LocalTime.of(18, 0)), 0.0001f)
    }

    @Test
    fun almostMidnightIsNearOne() {
        val value = TimelineCalculator.timeToNormalizedPosition(LocalTime.of(23, 59))
        assertEquals(1439 / 1440f, value, 0.0001f)
        assertTrue(value > 0.99f)
        assertTrue(value <= 1f)
    }

    @Test
    fun usesRealSunriseSunsetNotFixedHours() {
        val sunrise = LocalTime.of(6, 10)
        val sunset = LocalTime.of(18, 5)
        val model = TimelineCalculator.build(
            now = LocalTime.of(15, 30),
            sunrise = sunrise,
            sunset = sunset,
            solarNoon = LocalTime.of(12, 8),
            moonrise = LocalTime.of(20, 0),
            moonset = LocalTime.of(8, 0),
        )
        assertTrue(kotlin.math.abs(model.sunrisePosition - 0.25f) > 0.001f)
        assertEquals(TimelineCalculator.timeToNormalizedPosition(sunrise), model.sunrisePosition, 0.0001f)
        assertEquals(TimelineCalculator.timeToNormalizedPosition(sunset), model.sunsetPosition, 0.0001f)
        assertEquals(TimelineCalculator.timeToNormalizedPosition(LocalTime.of(15, 30)), model.currentPosition, 0.0001f)
        assertEquals("15:30", model.nowLabel)
    }

    @Test
    fun timelineLabelsIncludeMinutes() {
        val markers = TimelineLabels.defaultMarkers(
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 5),
        )
        assertEquals(
            listOf("00:00", "06:10", "12:00", "18:05", "24:00"),
            markers.map { it.subtitle },
        )
    }

    @Test
    fun dayChangeKeepsPositionsClamped() {
        val pos = TimelineCalculator.fromMinutes(0)
        val end = TimelineCalculator.fromMinutes(1439)
        assertEquals(0f, pos, 0f)
        assertTrue(end <= 1f)
    }
}
