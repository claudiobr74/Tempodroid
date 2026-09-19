package com.tempodroid.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class WidgetRendererTimeTest {

    @Test
    fun realWidgetIgnoresStaleDisplayTimeAndUsesLiveClock() {
        val state = SolarWeatherWidgetState.Empty.copy(
            displayTime = LocalTime.of(8, 13),
        )
        val liveNow = LocalTime.of(10, 28)

        val resolved = WidgetRenderer.resolveRenderTime(
            state = state,
            useLiveAstronomy = true,
            liveNow = liveNow,
        )

        assertEquals(liveNow, resolved)
    }

    @Test
    fun previewStillUsesSimulatedDisplayTime() {
        val simulated = LocalTime.of(8, 13)
        val state = SolarWeatherWidgetState.Empty.copy(displayTime = simulated)

        val resolved = WidgetRenderer.resolveRenderTime(
            state = state,
            useLiveAstronomy = false,
            liveNow = LocalTime.of(10, 28),
        )

        assertEquals(simulated, resolved)
    }

    @Test
    fun previewFallsBackToLiveClockWhenNoSimulationIsProvided() {
        val liveNow = LocalTime.of(10, 28)

        val resolved = WidgetRenderer.resolveRenderTime(
            state = SolarWeatherWidgetState.Empty.copy(displayTime = null),
            useLiveAstronomy = false,
            liveNow = liveNow,
        )

        assertEquals(liveNow, resolved)
    }
}
