package com.tempodroid.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class MoonPhaseCalculatorTest {

    private val calculator = AstronomyCalculator()

    private fun jd(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Double {
        val zdt = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("UTC"))
        return calculator.julianDate(zdt)
    }

    @Test
    fun newMoonNearKnownEpoch() {
        val jd = MoonPhaseCalculator.KNOWN_NEW_MOON_JD
        assertEquals(MoonPhase.NEW_MOON, MoonPhaseCalculator.phase(jd))
        assertTrue(MoonPhaseCalculator.illumination(jd) < 0.05)
    }

    @Test
    fun fullMoonIlluminationNearOne() {
        val jd = MoonPhaseCalculator.KNOWN_NEW_MOON_JD + MoonPhaseCalculator.SYNODIC_MONTH_DAYS / 2.0
        assertEquals(MoonPhase.FULL_MOON, MoonPhaseCalculator.phase(jd))
        assertTrue(MoonPhaseCalculator.illumination(jd) > 0.95)
    }

    @Test
    fun allEightPhasesAreReached() {
        val seen = mutableSetOf<MoonPhase>()
        val step = MoonPhaseCalculator.SYNODIC_MONTH_DAYS / 64.0
        repeat(64) { i ->
            seen += MoonPhaseCalculator.phase(MoonPhaseCalculator.KNOWN_NEW_MOON_JD + i * step)
        }
        assertEquals(MoonPhase.entries.toSet(), seen)
    }

    @Test
    fun illuminationIsWithinZeroOne() {
        repeat(30) { day ->
            val value = MoonPhaseCalculator.illumination(jd(2026, 9, 1) + day)
            assertTrue(value in 0.0..1.0)
        }
    }

    @Test
    fun southernHemisphereFlipsWaxingOrientation() {
        val south = MoonPhaseCalculator.getMoonOrientation(MoonPhase.WAXING_CRESCENT, Hemisphere.SOUTH)
        val north = MoonPhaseCalculator.getMoonOrientation(MoonPhase.WAXING_CRESCENT, Hemisphere.NORTH)
        assertEquals(MoonOrientation.LIGHT_FROM_LEFT, south)
        assertEquals(MoonOrientation.LIGHT_FROM_RIGHT, north)
        assertTrue(south != north)
    }

    @Test
    fun southernWaningIsOppositeOfWaxing() {
        val waxing = MoonPhaseCalculator.getMoonOrientation(MoonPhase.WAXING_GIBBOUS, Hemisphere.SOUTH)
        val waning = MoonPhaseCalculator.getMoonOrientation(MoonPhase.WANING_GIBBOUS, Hemisphere.SOUTH)
        assertEquals(MoonOrientation.LIGHT_FROM_LEFT, waxing)
        assertEquals(MoonOrientation.LIGHT_FROM_RIGHT, waning)
    }

    @Test
    fun brazilDefaultsToSouth() {
        assertEquals(Hemisphere.SOUTH, hemisphereFromLatitude(-16.6869))
        assertEquals(Hemisphere.NORTH, hemisphereFromLatitude(40.0))
    }

    @Test
    fun terminatorScaleSignsMatchGibbousAndCrescent() {
        assertTrue(MoonTerminator.ellipseHalfWidth(1.0, 10f) > 8f)
        assertTrue(MoonTerminator.ellipseHalfWidth(0.5, 10f) < 1f)
        assertTrue(MoonTerminator.isGibbous(0.82))
        assertFalse(MoonTerminator.isGibbous(0.18))
    }

    @Test
    fun moonHiddenBelowHorizonAndInStorm() {
        assertEquals(0f, MoonTerminator.skyVisibility(false, false, com.tempodroid.weather.WeatherCondition.CLEAR))
        assertEquals(0f, MoonTerminator.skyVisibility(true, false, com.tempodroid.weather.WeatherCondition.STORM))
        assertTrue(MoonTerminator.skyVisibility(true, false, com.tempodroid.weather.WeatherCondition.CLEAR) > 0.9f)
        assertTrue(
            MoonTerminator.skyVisibility(true, true, com.tempodroid.weather.WeatherCondition.CLEAR) <
                MoonTerminator.skyVisibility(true, false, com.tempodroid.weather.WeatherCondition.CLEAR),
        )
    }

    @Test
    fun typicalIlluminationMatchesPhase() {
        assertEquals(1.0, MoonPhaseCalculator.typicalIllumination(MoonPhase.FULL_MOON), 0.0)
        assertTrue(MoonPhaseCalculator.typicalIllumination(MoonPhase.NEW_MOON) < 0.05)
        assertEquals(0.50, MoonPhaseCalculator.typicalIllumination(MoonPhase.FIRST_QUARTER), 0.0)
    }
}
