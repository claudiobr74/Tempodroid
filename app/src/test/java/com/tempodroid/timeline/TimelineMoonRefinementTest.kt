package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.sky.DayPeriod
import com.tempodroid.sky.MoonRenderer
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TimelineMoonRefinementTest {

    @Test
    fun weatherMarkerUsesApprovedDayNightGlyphs() {
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.SUN,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.CLEAR, DayPeriod.DAY),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.MOON,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.CLEAR, DayPeriod.NIGHT),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.CLOUD_SUN,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.PARTLY_CLOUDY, DayPeriod.AFTERNOON),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.CLOUD_MOON,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.PARTLY_CLOUDY, DayPeriod.NIGHT),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.CLOUD,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.FOG, DayPeriod.DAY),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.RAIN,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.HEAVY_RAIN, DayPeriod.TWILIGHT),
        )
        assertEquals(
            TimelineWeatherMarkerRenderer.Glyph.STORM,
            TimelineWeatherMarkerRenderer.glyphFor(WeatherCondition.STORM, DayPeriod.NIGHT),
        )
    }

    @Test
    fun nowBubbleCanShiftWithoutMovingChronologicalMarker() {
        val left = 10f
        val right = 310f
        val position = TimelineCalculator.timeToNormalizedPosition(LocalTime.of(18, 10))
        val layout = SolarLunarTimelineRenderer.calculateNowMarkerLayout(
            left = left,
            right = right,
            h = 80,
            position = position,
            bubbleWidth = 90f,
            bubbleHeight = 28f,
            arcCy = 46f,
            arcRise = 36f,
        )
        val expectedMarkerX = left + position * (right - left)
        assertEquals(expectedMarkerX, layout.markerX, 0.001f)
        assertTrue(layout.bubbleCenterX <= right - 45f)
    }

    @Test
    fun timelineRendersRequiredTimesWithoutLegacyEndpointIcons() {
        val base = SolarWeatherWidgetState.Empty.copy(
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
            solarNoon = LocalTime.of(12, 12),
            condition = WeatherCondition.PARTLY_CLOUDY,
        )
        listOf(
            LocalTime.of(6, 10),
            LocalTime.of(12, 0),
            LocalTime.of(17, 56),
            LocalTime.of(18, 10),
            LocalTime.of(22, 30),
        ).forEach { now ->
            val bitmap = SolarLunarTimelineRenderer.render(
                widthPx = 608,
                heightPx = 100,
                state = base.copy(displayTime = now),
                now = now,
                compact = false,
                panelDarknessAlpha = 64,
            )
            assertEquals(608, bitmap.width)
            assertEquals(100, bitmap.height)
            assertTrue(bitmap.byteCount > 0)
            bitmap.recycle()
        }
    }

    @Test
    fun earthshineShadeRespectsVisibilityInsteadOfCreatingOpaqueDisc() {
        val dayAlpha = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.25,
            visibility = 0.34f,
            isSky = true,
        )
        val twilightAlpha = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.25,
            visibility = 0.70f,
            isSky = true,
        )
        val nightAlpha = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.25,
            visibility = 1f,
            isSky = true,
        )

        assertTrue(dayAlpha < twilightAlpha)
        assertTrue(twilightAlpha < nightAlpha)
        assertTrue(dayAlpha < 20)
        assertTrue(nightAlpha <= 48)
    }

    @Test
    fun moonHasNoOpaquePixelsOutsideItsDiscWhenHaloIsInactive() {
        val target = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val texture = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.LTGRAY)
        }

        MoonRenderer.draw(
            canvas = canvas,
            cx = 80f,
            cy = 80f,
            radius = 28f,
            phase = MoonPhase.WAXING_CRESCENT,
            illumination = 0.25,
            hemisphere = Hemisphere.SOUTH,
            style = MoonRenderStyle.SKY,
            visibility = 0.34f,
            surfaceTexture = texture,
        )

        assertEquals(0, Color.alpha(target.getPixel(80 + 34, 80)))
        assertEquals(0, Color.alpha(target.getPixel(80 - 34, 80)))
        texture.recycle()
        target.recycle()
    }
}
