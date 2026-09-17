package com.tempodroid.weather

/**
 * Mapeia WMO weather codes (Open-Meteo) para [WeatherCondition].
 *
 * Referência: https://open-meteo.com/en/docs
 */
object WeatherCodeMapper {
    fun fromOpenMeteo(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        51, 53, 56, 61, 66, 80, 85 -> WeatherCondition.RAIN
        55, 57, 63, 65, 67, 81, 82, 86 -> WeatherCondition.HEAVY_RAIN
        71, 73, 75, 77 -> WeatherCondition.CLOUDY
        95, 96, 99 -> WeatherCondition.STORM
        in 50..59 -> WeatherCondition.RAIN
        in 60..69 -> WeatherCondition.RAIN
        in 80..84 -> WeatherCondition.RAIN
        in 90..99 -> WeatherCondition.STORM
        else -> WeatherCondition.CLOUDY
    }

    fun labelPt(condition: WeatherCondition, isDay: Boolean): String = when (condition) {
        WeatherCondition.CLEAR -> if (isDay) "Limpo" else "Noite limpa"
        WeatherCondition.PARTLY_CLOUDY -> "Parcialmente nublado"
        WeatherCondition.CLOUDY -> "Nublado"
        WeatherCondition.RAIN -> "Chuva"
        WeatherCondition.HEAVY_RAIN -> "Chuva forte"
        WeatherCondition.STORM -> "Tempestade"
        WeatherCondition.FOG -> "Neblina"
    }
}
