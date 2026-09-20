package com.tempodroid.sky

import android.graphics.Bitmap
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.MoonRenderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MoonCinematicRegressionTest {

    @Test
    fun earthshineIsSubtleByDayAndRicherAtNight() {
        val day = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.67,
            visibility = 0.34f,
            isSky = true,
            ambientDarkness = 0f,
        )
        val night = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.67,
            visibility = 1f,
            isSky = true,
            ambientDarkness = 1f,
        )

        assertTrue(day <= 1)
        assertTrue(night > day)
    }

    @Test
    fun photographicGibbousMoonRendersWithoutBackplate() {
        val context = RuntimeEnvironment.getApplication()
        val texture = FigmaVisualAssets.moonSurface(context)
        val bitmap = MoonRenderer.render(
            sizePx = 96,
            phase = MoonPhase.WAXING_GIBBOUS,
            illumination = 0.67,
            hemisphere = Hemisphere.SOUTH,
            style = MoonRenderStyle.SKY,
            surfaceTexture = texture,
            ambientDarkness = 0.75f,
        )

        assertEquals(96, bitmap.width)
        assertEquals(96, bitmap.height)
        assertTrue(bitmap.byteCount > 0)
        assertEquals(0, bitmap.getPixel(0, 0) ushr 24)
        bitmap.recycle()
    }

    @Test
    fun newMoonDoesNotBecomeDarkArtificialDisc() {
        val bitmap = MoonRenderer.render(
            sizePx = 72,
            phase = MoonPhase.NEW_MOON,
            illumination = 0.01,
            hemisphere = Hemisphere.SOUTH,
            style = MoonRenderStyle.SKY,
            ambientDarkness = 1f,
        )

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue(pixels.all { (it ushr 24) == 0 })
        bitmap.recycle()
    }
}
