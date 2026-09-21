package com.tempodroid.di

import android.content.Context
import com.tempodroid.BuildConfig
import com.tempodroid.astronomy.AstronomyCalculator
import com.tempodroid.astronomy.AstronomyRepositoryImpl
import com.tempodroid.data.ForecastCacheRepository
import com.tempodroid.data.PreferencesRepository
import com.tempodroid.data.WidgetCacheRepository
import com.tempodroid.location.LocationRepository
import com.tempodroid.weather.HttpGetter
import com.tempodroid.weather.OpenMeteoWeatherProvider
import com.tempodroid.weather.WeatherRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cache = WidgetCacheRepository(appContext)
    val forecastCache = ForecastCacheRepository(appContext)
    val preferences = PreferencesRepository(appContext)

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val http = HttpGetter { url ->
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Tempodroid/${BuildConfig.VERSION_NAME} (Android)")
                .build()
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        }
    }

    val weatherRepository = WeatherRepositoryImpl(OpenMeteoWeatherProvider(http))
    val astronomyRepository = AstronomyRepositoryImpl(AstronomyCalculator())
    val locationRepository = LocationRepository(appContext, cache, preferences, http)

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
