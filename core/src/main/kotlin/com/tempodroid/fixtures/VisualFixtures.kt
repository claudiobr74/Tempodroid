package com.tempodroid.fixtures

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.Instant
import java.time.LocalTime

data class VisualFixture(
    val id: Int,
    val name: String,
    val time: LocalTime,
    val state: SolarWeatherWidgetState,
)

/**
 * Seis composições aprovadas no Figma (Goiânia, 17 de setembro).
 */
object VisualFixtures {
    private val date = "Qui, 17 de Set"
    private val updated = Instant.parse("2026-09-17T18:30:00Z")

    val diaLimpo = fixture(
        id = 1,
        name = "Dia limpo",
        time = LocalTime.of(15, 30),
        condition = WeatherCondition.CLEAR,
        temperature = 33,
        phase = MoonPhase.WAXING_CRESCENT,
        illumination = 0.18,
        high = 35,
        low = 19,
        rain = 0,
        cloud = 8,
        isDay = true,
        dateLabel = "Qui, 17 de Set",
    )

    val amanhecer = fixture(
        id = 2,
        name = "Amanhecer",
        time = LocalTime.of(6, 20),
        condition = WeatherCondition.PARTLY_CLOUDY,
        temperature = 24,
        phase = MoonPhase.WANING_CRESCENT,
        illumination = 0.16,
        high = 31,
        low = 20,
        rain = 10,
        cloud = 35,
        isDay = true,
        dateLabel = "Qui, 17 de Set",
    )

    val manhaNublada = fixture(
        id = 3,
        name = "Manhã nublada",
        time = LocalTime.of(9, 10),
        condition = WeatherCondition.CLOUDY,
        temperature = 26,
        phase = MoonPhase.FIRST_QUARTER,
        illumination = 0.50,
        high = 29,
        low = 21,
        rain = 25,
        cloud = 78,
        isDay = true,
        dateLabel = "Qui, 17 de Set",
    )

    val chuvaTarde = fixture(
        id = 4,
        name = "Chuva à tarde",
        time = LocalTime.of(16, 40),
        condition = WeatherCondition.RAIN,
        temperature = 27,
        phase = MoonPhase.WAXING_GIBBOUS,
        illumination = 0.78,
        high = 28,
        low = 22,
        rain = 85,
        cloud = 92,
        precipitation = 2.4,
        isDay = true,
        dateLabel = "Qui, 17 de Set",
    )

    val porDoSol = fixture(
        id = 5,
        name = "Pôr do sol",
        time = LocalTime.of(18, 10),
        condition = WeatherCondition.CLEAR,
        temperature = 29,
        phase = MoonPhase.FULL_MOON,
        illumination = 1.0,
        high = 32,
        low = 21,
        rain = 5,
        cloud = 12,
        isDay = false,
        dateLabel = "Qui, 17 de Set",
    )

    val noite = fixture(
        id = 6,
        name = "Noite limpa",
        time = LocalTime.of(21, 45),
        condition = WeatherCondition.CLEAR,
        temperature = 23,
        phase = MoonPhase.WANING_GIBBOUS,
        illumination = 0.88,
        high = 30,
        low = 19,
        rain = 0,
        cloud = 10,
        isDay = false,
        dateLabel = "Qui, 17 Set",
    )

    val all: List<VisualFixture> = listOf(
        diaLimpo, amanhecer, manhaNublada, chuvaTarde, porDoSol, noite,
    )

    private fun fixture(
        id: Int,
        name: String,
        time: LocalTime,
        condition: WeatherCondition,
        temperature: Int,
        phase: MoonPhase,
        illumination: Double,
        high: Int,
        low: Int,
        rain: Int,
        cloud: Int,
        isDay: Boolean,
        dateLabel: String,
        precipitation: Double = 0.0,
    ) = VisualFixture(
        id = id,
        name = name,
        time = time,
        state = SolarWeatherWidgetState(
            city = SolarWeatherWidgetState.FALLBACK_CITY,
            temperature = temperature,
            condition = condition,
            high = high,
            low = low,
            rainChance = rain,
            sunrise = LocalTime.of(6, 10),
            sunset = LocalTime.of(18, 5),
            solarNoon = LocalTime.of(12, 8),
            moonPhase = phase,
            moonIllumination = illumination,
            moonrise = LocalTime.of(20, 10),
            moonset = LocalTime.of(8, 40),
            weatherCode = null,
            cloudCover = cloud,
            precipitation = precipitation,
            isDay = isDay,
            updatedAt = updated,
            hasWeatherError = false,
            isStale = false,
            displayTime = time,
            displayDate = dateLabel,
        ),
    )
}

object DailySummaryFormatter {
    fun normal(high: Int?, low: Int?, rain: Int?): String {
        val max = high?.let { "Máx $it°" } ?: "Máx --"
        val min = low?.let { "Mín $it°" } ?: "Mín --"
        val chance = rain?.let { "$it% chuva" } ?: "-- chuva"
        return "$max · $min · $chance"
    }

    fun compact(high: Int?, low: Int?, rain: Int?): String {
        val max = high?.let { "$it°" } ?: "--"
        val min = low?.let { "$it°" } ?: "--"
        val chance = rain?.let { "$it%" } ?: "--"
        return "$max / $min · $chance"
    }
}
