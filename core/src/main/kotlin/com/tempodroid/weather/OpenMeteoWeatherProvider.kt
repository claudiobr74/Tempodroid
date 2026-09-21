package com.tempodroid.weather

fun interface HttpGetter {
    suspend fun get(url: String): String
}

class OpenMeteoWeatherProvider(
    private val http: HttpGetter,
    private val baseUrl: String = DEFAULT_BASE_URL,
) : WeatherProvider {

    override suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherData {
        val url = buildUrl(latitude, longitude)
        val body = http.get(url)
        return OpenMeteoResponseParser.parse(body)
    }

    fun buildUrl(latitude: Double, longitude: Double): String {
        return "$baseUrl/v1/forecast" +
            "?latitude=$latitude" +
            "&longitude=$longitude" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,surface_pressure,weather_code,cloud_cover,precipitation,is_day" +
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&timezone=auto" +
            "&forecast_days=6"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.open-meteo.com"
    }
}

class WeatherRepositoryImpl(
    private val provider: WeatherProvider,
) : WeatherRepository {
    override suspend fun getWeather(latitude: Double, longitude: Double): WeatherData =
        provider.fetchWeather(latitude, longitude)
}
