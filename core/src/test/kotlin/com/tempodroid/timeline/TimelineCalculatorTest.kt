package com.tempodroid.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun refinedTimelineHasOnlySunriseAndSunsetFixedAnchors() {
        val markers = TimelineLabels.defaultMarkers(
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 5),
        )
        assertEquals(2, markers.size)
        assertEquals(listOf("Nascer do sol", "Pôr do sol"), markers.map { it.title })
        assertEquals(listOf("06:10", "18:05"), markers.map { it.subtitle })

        val legacy = markers.flatMap { listOf(it.title, it.subtitle) }
        assertFalse(legacy.contains("Madrugada"))
        assertFalse(legacy.contains("Dia"))
        assertFalse(legacy.contains("Noite"))
        assertFalse(legacy.contains("00:00"))
        assertFalse(legacy.contains("12:00"))
        assertFalse(legacy.contains("24:00"))
    }

    @Test
    fun requiredNowCasesKeepExact24HourPositionsAndMinutes() {
        val cases = listOf(
            LocalTime.of(6, 10),
            LocalTime.of(12, 0),
            LocalTime.of(17, 56),
            LocalTime.of(18, 10),
            LocalTime.of(22, 30),
        )
        val sunrise = LocalTime.of(6, 11)
        val sunset = LocalTime.of(18, 13)

        cases.forEach { now ->
            val model = TimelineCalculator.build(
                now = now,
                sunrise = sunrise,
                sunset = sunset,
                solarNoon = LocalTime.of(12, 12),
                moonrise = null,
                moonset = null,
            )
            assertEquals(
                TimelineCalculator.timeToNormalizedPosition(now),
                model.currentPosition,
                0.0001f,
            )
            assertEquals(TimelineCalculator.formatClock(now), model.nowLabel)
        }
    }

    @Test
    fun sunsetCollisionCaseDoesNotChangeChronologicalPosition() {
        val now = LocalTime.of(18, 10)
        val model = TimelineCalculator.build(
            now = now,
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
            solarNoon = null,
            moonrise = null,
            moonset = null,
        )
        assertEquals(1090 / 1440f, model.currentPosition, 0.0001f)
        assertTrue(model.currentPosition < model.sunsetPosition)
    }

    @Test
    fun dayChangeKeepsPositionsClamped() {
        val pos = TimelineCalculator.fromMinutes(0)
        val end = TimelineCalculator.fromMinutes(1439)
        assertEquals(0f, pos, 0f)
        assertTrue(end <= 1f)
    }
}
