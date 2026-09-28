package com.tempodroid.astronomy

import com.tempodroid.sky.DayPeriod
import com.tempodroid.sky.DayPeriodCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AstronomyCalculatorTest {

    private val calculator = AstronomyCalculator()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val goianiaLat = -16.6869
    private val goianiaLon = -49.2648
    private val date = LocalDate.of(2026, 9, 17)

    @Test
    fun sunriseSunsetAroundEquinoxInGoiania() {
        val solar = calculator.solarTimes(goianiaLat, goianiaLon, date, zone)
        val sunrise = requireNotNull(solar.sunrise)
        val sunset = requireNotNull(solar.sunset)
        assertTrue("sunrise=$sunrise", sunrise.hour in 5..7)
        assertTrue("sunset=$sunset", sunset.hour in 17..19)
        assertTrue(sunrise.isBefore(solar.solarNoon))
        assertTrue(solar.solarNoon.isBefore(sunset))
    }

    @Test
    fun timezoneShiftsLocalSolarTimes() {
        val utc = calculator.solarTimes(goianiaLat, goianiaLon, date, ZoneId.of("UTC"))
        val sp = calculator.solarTimes(goianiaLat, goianiaLon, date, zone)
        val deltaHours = kotlin.math.abs(sp.solarNoon.hour - utc.solarNoon.hour)
        assertTrue("delta=$deltaHours noonSP=${sp.solarNoon} noonUTC=${utc.solarNoon}", deltaHours in 2..4)
    }

    @Test
    fun sunBelowHorizonBeforeSunriseAndAfterSunset() {
        val solar = calculator.solarTimes(goianiaLat, goianiaLon, date, zone)
        val sunrise = requireNotNull(solar.sunrise)
        val sunset = requireNotNull(solar.sunset)
        val before = SunPositionCalculator.position(sunrise.minusMinutes(20), sunrise, sunset, solar.solarNoon)
        val after = SunPositionCalculator.position(sunset.plusMinutes(20), sunrise, sunset, solar.solarNoon)
        val noon = SunPositionCalculator.position(solar.solarNoon, sunrise, sunset, solar.solarNoon)
        assertFalse(before.aboveHorizon)
        assertFalse(after.aboveHorizon)
        assertTrue(noon.aboveHorizon)
        assertTrue(noon.normalizedY < before.normalizedY)
    }

    @Test
    fun sunRisesThenPeaksThenSets() {
        val sunrise = LocalTime.of(6, 10)
        val noon = LocalTime.of(12, 8)
        val sunset = LocalTime.of(18, 5)
        val morning = SunPositionCalculator.position(LocalTime.of(8, 0), sunrise, sunset, noon)
        val peak = SunPositionCalculator.position(noon, sunrise, sunset, noon)
        val afternoon = SunPositionCalculator.position(LocalTime.of(16, 0), sunrise, sunset, noon)
        assertTrue(morning.aboveHorizon && peak.aboveHorizon && afternoon.aboveHorizon)
        assertTrue(peak.normalizedY < morning.normalizedY)
        assertTrue(peak.normalizedY < afternoon.normalizedY)
        assertTrue(morning.normalizedX < peak.normalizedX)
        assertTrue(peak.normalizedX < afternoon.normalizedX)
    }

    @Test
    fun moonHiddenWhenBelowHorizon() {
        val rise = LocalTime.of(20, 0)
        val set = LocalTime.of(8, 0)
        val day = CelestialPositionCalculator.alongArc(LocalTime.of(15, 0), rise, set)
        val night = CelestialPositionCalculator.alongArc(LocalTime.of(23, 0), rise, set)
        assertFalse(day.aboveHorizon)
        assertTrue(night.aboveHorizon)
    }

    @Test
    fun realHorizontalProjectionKeepsRisingMoonLowAndTransitHigh() {
        val rising = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = 11.6,
            azimuthDegrees = 95.0,
            aboveHorizon = true,
        )
        val transit = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = 81.5,
            azimuthDegrees = 180.0,
            aboveHorizon = true,
        )
        val setting = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = 8.0,
            azimuthDegrees = 265.0,
            aboveHorizon = true,
        )

        assertTrue(rising.normalizedY > 0.65f)
        assertTrue(transit.normalizedY < 0.28f)
        assertTrue(setting.normalizedY > 0.70f)
        assertTrue(rising.normalizedX < transit.normalizedX)
        assertTrue(transit.normalizedX < setting.normalizedX)
    }

    @Test
    fun belowHorizonHorizontalProjectionIsHidden() {
        val hidden = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = -4.0,
            azimuthDegrees = 280.0,
            aboveHorizon = false,
        )
        assertFalse(hidden.aboveHorizon)
        assertTrue(hidden.normalizedY > 1f)
    }

    @Test
    fun september27MoonTrajectoryUsesTopocentricAltitudeInsteadOfSyntheticArc() {
        val targetDate = LocalDate.of(2026, 9, 27)
        val targetTime = LocalTime.of(20, 31)
        val zdt = ZonedDateTime.of(targetDate, targetTime, zone)
        val events = calculator.moonRiseSet(goianiaLat, goianiaLon, targetDate, zone)
        val horizontal = calculator.moonHorizontal(goianiaLat, goianiaLon, calculator.julianDate(zdt))
        val position = calculator.moonPosition(
            goianiaLat,
            goianiaLon,
            zdt,
        )
        val expectedProjection = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = horizontal.altitude,
            azimuthDegrees = horizontal.azimuth,
            aboveHorizon = horizontal.altitude > 0.0,
        )

        assertTrue("moonrise=${events.rise}", requireNotNull(events.rise).hour == 19)
        assertTrue("altitude=${horizontal.altitude}", horizontal.altitude in 28.0..34.0)
        assertTrue("azimuth=${horizontal.azimuth}", horizontal.azimuth in 60.0..90.0)
        assertEquals(expectedProjection.normalizedX, position.normalizedX, 0.0001f)
        assertEquals(expectedProjection.normalizedY, position.normalizedY, 0.0001f)
        assertTrue("normalizedY=${position.normalizedY}", position.normalizedY < 0.56f)
    }

    @Test
    fun computeReturnsConsistentHemisphereAndPhase() {
        val data = calculator.compute(goianiaLat, goianiaLon, date, zone, LocalTime.of(21, 45))
        assertEquals(Hemisphere.SOUTH, data.hemisphere)
        assertTrue(data.moonIllumination in 0.0..1.0)
        assertTrue(requireNotNull(data.sunrise).isBefore(requireNotNull(data.sunset)))
    }

    @Test
    fun polarSummerHasNoFakeSunriseOrSunset() {
        val tromsøZone = ZoneId.of("Europe/Oslo")
        val summerDate = LocalDate.of(2026, 6, 21)
        val solar = calculator.solarTimes(69.6492, 18.9553, summerDate, tromsøZone)
        val data = calculator.compute(69.6492, 18.9553, summerDate, tromsøZone, LocalTime.NOON)

        assertNull(solar.sunrise)
        assertNull(solar.sunset)
        assertTrue(data.sunPosition?.aboveHorizon == true)
        assertEquals(
            DayPeriod.DAY,
            DayPeriodCalculator.from(LocalTime.NOON, null, null, solar.solarNoon, data.sunPosition?.aboveHorizon),
        )
    }

    @Test
    fun polarWinterHasNoFakeSunriseOrSunset() {
        val tromsøZone = ZoneId.of("Europe/Oslo")
        val winterDate = LocalDate.of(2026, 12, 21)
        val solar = calculator.solarTimes(69.6492, 18.9553, winterDate, tromsøZone)
        val data = calculator.compute(69.6492, 18.9553, winterDate, tromsøZone, LocalTime.NOON)

        assertNull(solar.sunrise)
        assertNull(solar.sunset)
        assertFalse(data.sunPosition?.aboveHorizon == true)
        assertEquals(
            DayPeriod.NIGHT,
            DayPeriodCalculator.from(LocalTime.NOON, null, null, solar.solarNoon, data.sunPosition?.aboveHorizon),
        )
    }

    @Test
    fun dayPeriodUsesRealSunriseSunset() {
        val sunrise = LocalTime.of(6, 10)
        val sunset = LocalTime.of(18, 5)
        val noon = LocalTime.of(12, 8)
        assertEquals(DayPeriod.DAWN, DayPeriodCalculator.from(LocalTime.of(5, 50), sunrise, sunset, noon))
        assertEquals(DayPeriod.MORNING, DayPeriodCalculator.from(LocalTime.of(7, 0), sunrise, sunset, noon))
        assertEquals(DayPeriod.DAY, DayPeriodCalculator.from(LocalTime.of(10, 0), sunrise, sunset, noon))
        assertEquals(DayPeriod.AFTERNOON, DayPeriodCalculator.from(LocalTime.of(15, 30), sunrise, sunset, noon))
        assertEquals(DayPeriod.SUNSET, DayPeriodCalculator.from(LocalTime.of(17, 40), sunrise, sunset, noon))
        assertEquals(DayPeriod.TWILIGHT, DayPeriodCalculator.from(LocalTime.of(18, 20), sunrise, sunset, noon))
        assertEquals(DayPeriod.NIGHT, DayPeriodCalculator.from(LocalTime.of(21, 45), sunrise, sunset, noon))
    }
}
