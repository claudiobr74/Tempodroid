package com.tempodroid.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NowBadgePlacementTest {

    @Test
    fun nearSunsetBadgeFloatsUpAndLeftWhileChronologicalPointStaysIndependent() {
        val sunsetX = 270f
        val markerX = 268f
        val markerY = 52f

        val placement = SolarLunarTimelineRenderer.collisionAdjustedPlacement(
            desiredCenterX = markerX,
            desiredCenterY = 45f,
            markerX = markerX,
            markerY = markerY,
            sunriseX = 50f,
            sunsetX = sunsetX,
            diameter = 44f,
            left = 10f,
            right = 310f,
            h = 80,
            now = LocalTime.of(18, 10),
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
        )

        assertTrue(placement.displaced)
        assertTrue(placement.centerX < sunsetX)
        assertTrue(placement.centerY < markerY)
        assertEquals(268f, markerX, 0f)
        assertEquals(52f, markerY, 0f)
    }

    @Test
    fun afterSunsetSpatialCollisionFloatsBadgeUpAndRight() {
        val sunsetX = 270f
        val markerX = 278f
        val markerY = 54f

        val placement = SolarLunarTimelineRenderer.collisionAdjustedPlacement(
            desiredCenterX = 278f,
            desiredCenterY = 46f,
            markerX = markerX,
            markerY = markerY,
            sunriseX = 50f,
            sunsetX = sunsetX,
            diameter = 44f,
            left = 10f,
            right = 340f,
            h = 80,
            now = LocalTime.of(19, 0),
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
        )

        assertTrue(placement.displaced)
        assertTrue(placement.centerX > sunsetX)
        assertTrue(placement.centerY < markerY)
    }

    @Test
    fun nearSunriseBadgeFloatsUpAndRight() {
        val sunriseX = 50f
        val markerX = 52f
        val markerY = 52f

        val placement = SolarLunarTimelineRenderer.collisionAdjustedPlacement(
            desiredCenterX = markerX,
            desiredCenterY = 45f,
            markerX = markerX,
            markerY = markerY,
            sunriseX = sunriseX,
            sunsetX = 270f,
            diameter = 44f,
            left = 10f,
            right = 310f,
            h = 80,
            now = LocalTime.of(6, 14),
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
        )

        assertTrue(placement.displaced)
        assertTrue(placement.centerX > sunriseX)
        assertTrue(placement.centerY < markerY)
    }

    @Test
    fun clippedMidnightBadgeFloatsUpAndAwayFromSunriseLabels() {
        val sunriseX = 101f
        val markerX = 28f
        val markerY = 58f

        val placement = SolarLunarTimelineRenderer.collisionAdjustedPlacement(
            desiredCenterX = 59f,
            desiredCenterY = 48f,
            markerX = markerX,
            markerY = markerY,
            sunriseX = sunriseX,
            sunsetX = 470f,
            diameter = 44f,
            left = 27f,
            right = 580f,
            h = 80,
            now = LocalTime.of(0, 5),
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
        )

        assertTrue(placement.displaced)
        assertTrue(placement.centerX < sunriseX)
        assertTrue(placement.centerY < markerY)
    }

    @Test
    fun normalDaytimePositionKeepsBadgeAttachedToTimeline() {
        val placement = SolarLunarTimelineRenderer.collisionAdjustedPlacement(
            desiredCenterX = 150f,
            desiredCenterY = 34f,
            markerX = 150f,
            markerY = 40f,
            sunriseX = 50f,
            sunsetX = 270f,
            diameter = 44f,
            left = 10f,
            right = 310f,
            h = 80,
            now = LocalTime.of(10, 30),
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
        )

        assertFalse(placement.displaced)
        assertEquals(150f, placement.centerX, 0.001f)
        assertEquals(34f, placement.centerY, 0.001f)
    }
}
