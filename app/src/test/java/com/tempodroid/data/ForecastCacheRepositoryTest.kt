package com.tempodroid.data

import android.app.Application
import com.tempodroid.weather.DailyForecast
import com.tempodroid.weather.ForecastSnapshot
import com.tempodroid.weather.HourlyForecast
import com.tempodroid.weather.WeatherCondition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ForecastCacheRepositoryTest {

    private val widgetId = 9201
    private lateinit var cache: ForecastCacheRepository

    @Before
    fun setUp() = runBlocking {
        cache = ForecastCacheRepository(RuntimeEnvironment.getApplication())
        cache.deleteForecast(widgetId)
    }

    @After
    fun tearDown() = runBlocking {
        cache.deleteForecast(widgetId)
    }

    @Test
    fun savesLoadsAndDeletesForecastByWidget() = runBlocking {
        val expected = ForecastSnapshot(
            hourly = listOf(
                HourlyForecast(
                    time = Instant.parse("2026-09-21T12:00:00Z"),
                    temperature = 29,
                    rainChance = 15,
                    condition = WeatherCondition.PARTLY_CLOUDY,
                ),
            ),
            daily = listOf(
                DailyForecast(
                    date = LocalDate.parse("2026-09-22"),
                    high = 32,
                    low = 21,
                    rainChance = 35,
                    condition = WeatherCondition.CLOUDY,
                ),
            ),
        )

        cache.saveForecast(expected, widgetId)
        assertEquals(expected, cache.loadForecast(widgetId))

        cache.deleteForecast(widgetId)
        val deleted = cache.loadForecast(widgetId)
        assertTrue(deleted.hourly.isEmpty())
        assertTrue(deleted.daily.isEmpty())
    }
}
