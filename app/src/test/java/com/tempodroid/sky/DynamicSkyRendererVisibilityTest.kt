package com.tempodroid.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicSkyRendererVisibilityTest {

    @Test
    fun photographicSurfaceKeepsInvisibleMoonInvisible() {
        assertEquals(0, MoonRenderer.photographicSurfaceAlpha(0f, isSky = true))
    }

    @Test
    fun photographicSurfaceBoostsOnlyTheLitTextureDuringDay() {
        val alpha = MoonRenderer.photographicSurfaceAlpha(0.34f, isSky = true)
        assertTrue(alpha in 145..151)
    }

    @Test
    fun skyEarthshineStaysVerySubtleAtDayVisibility() {
        val alpha = MoonRenderer.earthshineShadeAlpha(
            illumination = 0.65,
            visibility = 0.26f,
            isSky = true,
        )
        assertTrue(alpha <= 2)
    }

    @Test
    fun photographicSurfacePreservesFullNightOpacity() {
        assertEquals(255, MoonRenderer.photographicSurfaceAlpha(1f, isSky = true))
    }
}
