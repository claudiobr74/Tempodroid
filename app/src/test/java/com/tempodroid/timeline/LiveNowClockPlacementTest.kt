package com.tempodroid.timeline

import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class LiveNowClockPlacementTest {

    private val state = SolarWeatherWidgetState.Empty.copy(
        sunrise = LocalTime.of(6, 10),
        sunset = LocalTime.of(18, 13),
        solarNoon = LocalTime.of(12, 11),
        condition = WeatherCondition.CLEAR,
    )

    @Test
    fun liveClockPlacementStaysInsideTimelineAtMidday() {
        val placement = SolarLunarTimelineRenderer.nowClockOverlayPlacement(
            widthPx = 608,
            heightPx = 100,
            state = state,
            now = LocalTime.of(13, 37),
            compact = false,
        )
        assertTrue(placement.centerXFraction in 0.05f..0.95f)
        assertTrue(placement.topFraction in 0f..0.90f)
    }

    @Test
    fun liveClockPlacementHandlesSunriseAndSunsetCollisionWindows() {
        listOf(LocalTime.of(6, 10), LocalTime.of(18, 10)).forEach { now ->
            val placement = SolarLunarTimelineRenderer.nowClockOverlayPlacement(
                widthPx = 608,
                heightPx = 100,
                state = state,
                now = now,
                compact = false,
            )
            assertTrue(placement.centerXFraction in 0f..1f)
            assertTrue(placement.topFraction in 0f..1f)
        }
    }
}
