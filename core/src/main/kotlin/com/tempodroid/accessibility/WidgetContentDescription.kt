package com.tempodroid.accessibility

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.MoonPhaseCalculator
import com.tempodroid.weather.WeatherCodeMapper
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.LocalTime

object WidgetContentDescription {
    fun forState(state: SolarWeatherWidgetState): String {
        val parts = mutableListOf<String>()
        state.temperature?.let { parts += "Temperatura atual $it graus" }
        parts += conditionDescription(state.condition, state.isDay)
        state.displayTime?.let { parts += "Hora ${spokenTime(it)}" }
        if (state.city.isNotBlank()) parts += "Cidade ${state.city}"
        state.high?.let { parts += "Máxima $it graus" }
        state.low?.let { parts += "Mínima $it graus" }
        state.rainChance?.let { parts += rainDescription(it) }
        state.sunrise?.let { parts += "Nascer do sol às ${spokenTime(it)}" }
        state.sunset?.let { parts += "Pôr do sol às ${spokenTime(it)}" }
        parts += MoonPhaseCalculator.labelPt(state.moonPhase)
        parts += "Lua iluminada em ${(state.moonIllumination * 100).toInt()} por cento"
        if (state.hasWeatherError) parts += "Usando último clima válido"
        if (state.isStale) parts += "Dados de clima desatualizados"
        return parts.joinToString(". ")
    }

    fun conditionDescription(condition: WeatherCondition, isDay: Boolean): String {
        val label = WeatherCodeMapper.labelPt(condition, isDay).lowercase()
        return "Clima $label"
    }

    fun rainDescription(percent: Int): String =
        if (percent == 0) "Chance de chuva zero por cento"
        else "Chance de chuva $percent por cento"

    fun spokenTime(time: LocalTime): String {
        val hour = time.hour
        val minute = time.minute
        return if (minute == 0) {
            "$hour horas"
        } else {
            "$hour horas e $minute minutos"
        }
    }

    fun moon(phase: MoonPhase, illumination: Double): String =
        "${MoonPhaseCalculator.labelPt(phase)}. Lua iluminada em ${(illumination * 100).toInt()} por cento"
}
