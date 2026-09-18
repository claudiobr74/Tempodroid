package com.tempodroid.sky

import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonOrientation
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.MoonPhaseCalculator
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.astronomy.MoonTerminator
import com.tempodroid.fixtures.VisualFixtures
import com.tempodroid.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MoonRendererAndroidTest {

    @Test
    fun allPhasesProduceBitmaps() {
        MoonPhase.entries.forEach { phase ->
            val bitmap = MoonRenderer.render(
                sizePx = 64,
                phase = phase,
                illumination = MoonPhaseCalculator.typicalIllumination(phase),
                hemisphere = Hemisphere.SOUTH,
                style = MoonRenderStyle.SKY,
            )
            assertEquals(64, bitmap.width)
            assertEquals(64, bitmap.height)
            assertTrue(bitmap.byteCount > 0)
            bitmap.recycle()
        }
    }

    @Test
    fun southAndNorthWaxingDiffer() {
        val south = MoonRenderer.illuminatedPath(32f, 32f, 24f, 0.5, lightFromRight = false)
        val north = MoonRenderer.illuminatedPath(32f, 32f, 24f, 0.5, lightFromRight = true)
        assertTrue(!south.isEmpty)
        assertTrue(!north.isEmpty)
        val southBounds = android.graphics.RectF()
        val northBounds = android.graphics.RectF()
        south.computeBounds(southBounds, true)
        north.computeBounds(northBounds, true)
        assertTrue(southBounds.width() > 0)
        assertEquals(MoonOrientation.LIGHT_FROM_LEFT, MoonPhaseCalculator.getMoonOrientation(MoonPhase.WAXING_CRESCENT, Hemisphere.SOUTH))
        assertEquals(MoonOrientation.LIGHT_FROM_RIGHT, MoonPhaseCalculator.getMoonOrientation(MoonPhase.WAXING_CRESCENT, Hemisphere.NORTH))
    }

    @Test
    fun skyRendererCoversRequiredStates() {
        val context = RuntimeEnvironment.getApplication()
        val states = listOf(
            VisualFixtures.diaLimpo.state to WeatherCondition.CLEAR,
            VisualFixtures.manhaNublada.state to WeatherCondition.CLOUDY,
            VisualFixtures.chuvaTarde.state to WeatherCondition.RAIN,
            VisualFixtures.amanhecer.state to WeatherCondition.PARTLY_CLOUDY,
            VisualFixtures.porDoSol.state to WeatherCondition.CLEAR,
            VisualFixtures.noite.state to WeatherCondition.CLEAR,
        )
        states.forEach { (state, condition) ->
            val bitmap = DynamicSkyRenderer.render(
                context = context,
                input = SkyRenderInput(
                    widthPx = 320,
                    heightPx = 160,
                    localTime = state.displayTime ?: java.time.LocalTime.NOON,
                    sunrise = state.sunrise ?: java.time.LocalTime.of(6, 10),
                    sunset = state.sunset ?: java.time.LocalTime.of(18, 5),
                    weatherCondition = condition,
                    cloudCover = state.cloudCover,
                    precipitation = state.precipitation,
                    sunPosition = com.tempodroid.astronomy.SunPositionCalculator.position(
                        state.displayTime ?: java.time.LocalTime.NOON,
                        state.sunrise ?: java.time.LocalTime.of(6, 10),
                        state.sunset ?: java.time.LocalTime.of(18, 5),
                        state.solarNoon,
                    ),
                    moonPosition = state.moonrise?.let {
                        com.tempodroid.astronomy.CelestialPositionCalculator.alongArc(
                            state.displayTime ?: java.time.LocalTime.NOON,
                            it,
                            state.moonset ?: java.time.LocalTime.of(8, 0),
                        )
                    },
                    moonPhase = state.moonPhase,
                    moonIllumination = state.moonIllumination,
                    hemisphere = Hemisphere.SOUTH,
                    solarNoon = state.solarNoon,
                ),
            )
            assertEquals(320, bitmap.width)
            assertEquals(160, bitmap.height)
            assertTrue(bitmap.byteCount > 0)
            bitmap.recycle()
        }
    }

    @Test
    fun moonBelowHorizonIsNotDrawnOnClearDay() {
        val hidden = MoonTerminator.skyVisibility(false, true, WeatherCondition.CLEAR)
        assertEquals(0f, hidden)
    }
}
