package com.tempodroid.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicSkyRendererVisibilityTest {

    @Test
    fun photographicVisibilityKeepsInvisibleMoonInvisible() {
        assertEquals(0f, DynamicSkyRenderer.photographicMoonVisibility(0f), 0f)
    }

    @Test
    fun photographicVisibilityBoostsDayTextureWithoutMakingItOpaque() {
        val boosted = DynamicSkyRenderer.photographicMoonVisibility(0.34f)
        assertTrue(boosted > 0.55f)
        assertTrue(boosted < 0.65f)
    }

    @Test
    fun photographicVisibilityPreservesFullNightOpacity() {
        assertEquals(1f, DynamicSkyRenderer.photographicMoonVisibility(1f), 0f)
    }
}
