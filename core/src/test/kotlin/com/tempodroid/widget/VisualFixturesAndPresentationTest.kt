package com.tempodroid.widget

import com.tempodroid.accessibility.WidgetContentDescription
import com.tempodroid.fixtures.DailySummaryFormatter
import com.tempodroid.fixtures.VisualFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualFixturesAndPresentationTest {

    @Test
    fun sixFixturesMatchBrief() {
        val fixtures = VisualFixtures.all
        assertEquals(6, fixtures.size)
        assertEquals(33, VisualFixtures.diaLimpo.state.temperature)
        assertEquals(24, VisualFixtures.amanhecer.state.temperature)
        assertEquals(26, VisualFixtures.manhaNublada.state.temperature)
        assertEquals(27, VisualFixtures.chuvaTarde.state.temperature)
        assertEquals(29, VisualFixtures.porDoSol.state.temperature)
        assertEquals(23, VisualFixtures.noite.state.temperature)
        assertEquals(0, VisualFixtures.diaLimpo.state.rainChance)
        assertEquals(85, VisualFixtures.chuvaTarde.state.rainChance)
        assertEquals("Goiânia", VisualFixtures.diaLimpo.state.city)
    }

    @Test
    fun summaryFormats() {
        assertEquals("Máx 35° · Mín 19° · 0% chuva", DailySummaryFormatter.normal(35, 19, 0))
        assertEquals("35° / 19° · 0%", DailySummaryFormatter.compact(35, 19, 0))
    }

    @Test
    fun accessibilityPhrases() {
        val text = WidgetContentDescription.forState(VisualFixtures.diaLimpo.state)
        assertTrue(text.contains("Temperatura atual 33 graus"))
        assertTrue(text.contains("Clima limpo"))
        assertTrue(text.contains("Nascer do sol às 6 horas e 10 minutos"))
        assertTrue(text.contains("Pôr do sol às 18 horas e 5 minutos"))
        assertTrue(text.contains("Chance de chuva zero por cento"))
        val full = WidgetContentDescription.moon(
            VisualFixtures.porDoSol.state.moonPhase,
            VisualFixtures.porDoSol.state.moonIllumination,
        )
        assertTrue(full.contains("Lua cheia"))
        assertTrue(full.contains("100 por cento"))
    }
}
