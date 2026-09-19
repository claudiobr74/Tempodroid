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
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimelineMoonRefinementTest {

    @Test
    fun weatherMarkerUsesCanonicalDayNightGlyphs() {
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
    fun weatherMarkerPreservesVividFigmaColors() {
        val clear = TimelineWeatherMarkerRenderer.paletteFor(WeatherCondition.CLEAR, DayPeriod.DAY)
        val partly = TimelineWeatherMarkerRenderer.paletteFor(WeatherCondition.PARTLY_CLOUDY, DayPeriod.DAY)
        val rain = TimelineWeatherMarkerRenderer.paletteFor(WeatherCondition.RAIN, DayPeriod.DAY)
        val storm = TimelineWeatherMarkerRenderer.paletteFor(WeatherCondition.STORM, DayPeriod.TWILIGHT)

        assertEquals(0xFFFFD700.toInt(), clear.primary)
        assertEquals(0xFFFFD700.toInt(), partly.secondary)
        assertEquals(0xFF3AADFF.toInt(), rain.accent)
        assertEquals(0xFFFFE000.toInt(), storm.accent)
        assertTrue(partly.primary != partly.secondary)
    }

    @Test
    fun visualProjectionMatchesV2SunriseAndSunsetAnchors() {
        val sunrise = SolarLunarTimelineRenderer.visualPosition(LocalTime.of(6, 11))
        val sunset = SolarLunarTimelineRenderer.visualPosition(LocalTime.of(18, 13))
        assertEquals(0.13f, sunrise, 0.02f)
        assertEquals(0.78f, sunset, 0.02f)
        assertTrue(sunrise < sunset)
    }

    @Test
    fun v2MetricsMakeSolarAnchorsAndTimesReadable() {
        val metrics = SolarLunarTimelineRenderer.visualMetrics
        assertTrue(metrics.sunriseOuterDiameterFraction >= 0.26f)
        assertTrue(metrics.anchorTimeTextFraction >= 0.24f)
        assertTrue(metrics.nowBadgeDiameterFraction >= 0.44f)
        assertTrue(metrics.nowIconFractionOfBadge >= 0.60f)
        assertTrue(metrics.anchorTimeTextFraction > metrics.anchorLabelTextFraction)
    }

    @Test
    fun nowBubbleCanShiftWithoutMovingChronologicalMarker() {
        val left = 10f
        val right = 310f
        val position = SolarLunarTimelineRenderer.visualPosition(LocalTime.of(18, 10))
        val layout = SolarLunarTimelineRenderer.calculateNowMarkerLayout(
            left = left,
            right = right,
            h = 80,
            position = position,
            bubbleWidth = 44f,
            bubbleHeight = 44f,
            arcCy = 72f,
            arcRise = 84f,
        )
        val expectedMarkerX = left + position * (right - left)
        assertEquals(expectedMarkerX, layout.markerX, 0.001f)
        assertTrue(layout.bubbleCenterX in left..right)
    }

    @Test
    fun timelineRendersAllRequiredCanonicalValidationTimes() {
        val base = SolarWeatherWidgetState.Empty.copy(
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
            solarNoon = LocalTime.of(12, 12),
            condition = WeatherCondition.PARTLY_CLOUDY,
        )
        listOf(
            LocalTime.of(6, 11),
            LocalTime.of(10, 30),
            LocalTime.of(12, 0),
            LocalTime.of(15, 30),
            LocalTime.of(17, 56),
            LocalTime.of(18, 5),
            LocalTime.of(18, 10),
            LocalTime.of(19, 43),
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
            assertTrue(countOpaquePixels(bitmap) > 100)
            bitmap.recycle()
        }
    }

    @Test
    fun timelineContainsDistinctSunriseAndSunsetColors() {
        val bitmap = SolarLunarTimelineRenderer.render(
            widthPx = 608,
            heightPx = 100,
            state = SolarWeatherWidgetState.Empty.copy(
                sunrise = LocalTime.of(6, 11),
                sunset = LocalTime.of(18, 13),
                condition = WeatherCondition.CLEAR,
            ),
            now = LocalTime.of(10, 30),
            compact = false,
        )
        assertTrue(countNearColor(bitmap, TimelineWeatherMarkerRenderer.SUN_GOLD) > 8)
        assertTrue(countNearColor(bitmap, TimelineWeatherMarkerRenderer.SUNSET_ORANGE) > 8)
        bitmap.recycle()
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

    private fun countOpaquePixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 12) count++
            }
        }
        return count
    }

    private fun countNearColor(bitmap: Bitmap, target: Int): Int {
        val tr = Color.red(target)
        val tg = Color.green(target)
        val tb = Color.blue(target)
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val c = bitmap.getPixel(x, y)
                if (
                    Color.alpha(c) > 150 &&
                    kotlin.math.abs(Color.red(c) - tr) <= 8 &&
                    kotlin.math.abs(Color.green(c) - tg) <= 8 &&
                    kotlin.math.abs(Color.blue(c) - tb) <= 8
                ) {
                    count++
                }
            }
        }
        return count
    }
}
