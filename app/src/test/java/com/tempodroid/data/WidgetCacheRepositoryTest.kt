package com.tempodroid.data

import android.app.Application
import com.tempodroid.location.LocationSource
import com.tempodroid.location.ResolvedLocation
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.weather.WeatherData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WidgetCacheRepositoryTest {

    private val firstId = 9101
    private val secondId = 9102
    private lateinit var cache: WidgetCacheRepository

    @Before
    fun setUp() = runBlocking {
        cache = WidgetCacheRepository(RuntimeEnvironment.getApplication())
        cache.deleteWidgetState(firstId)
        cache.deleteWidgetState(secondId)
    }

    @After
    fun tearDown() = runBlocking {
        cache.deleteWidgetState(firstId)
        cache.deleteWidgetState(secondId)
    }

    @Test
    fun locationsAreIsolatedByWidgetId() = runBlocking {
        cache.saveLocation(
            ResolvedLocation("Goiânia", -16.6869, -49.2648, "America/Sao_Paulo", LocationSource.MANUAL_CITY),
            firstId,
        )
        cache.saveLocation(
            ResolvedLocation("Recife", -8.0476, -34.8770, "America/Recife", LocationSource.MANUAL_CITY),
            secondId,
        )

        assertEquals("Goiânia", cache.savedLocationExact(firstId)?.city)
        assertEquals("Recife", cache.savedLocationExact(secondId)?.city)
        assertTrue(cache.isConfigured(firstId))
        assertTrue(cache.isConfigured(secondId))
    }

    @Test
    fun weatherFromOneWidgetNeverLeaksIntoAnother() = runBlocking {
        cache.saveLocation(
            ResolvedLocation("Goiânia", -16.6869, -49.2648, "America/Sao_Paulo", LocationSource.MANUAL_CITY),
            firstId,
        )
        cache.saveLocation(
            ResolvedLocation("Recife", -8.0476, -34.8770, "America/Recife", LocationSource.MANUAL_CITY),
            secondId,
        )
        cache.saveWeather(
            WeatherData(
                temperature = 33,
                condition = WeatherCondition.CLEAR,
                high = 35,
                low = 19,
                rainChance = 0,
                cloudCover = 10,
                precipitation = 0.0,
                weatherCode = 0,
                isDay = true,
                timestamp = Instant.parse("2026-09-17T18:00:00Z"),
                timezoneId = "America/Sao_Paulo",
                apparentTemperature = 34,
                windSpeedKmh = 12,
            ),
            firstId,
        )

        val firstState = cache.loadState(firstId)
        assertEquals(33, firstState.temperature)
        assertEquals(34, firstState.apparentTemperature)
        assertEquals(12, firstState.windSpeedKmh)
        assertNull(cache.loadState(secondId).temperature)
    }

    @Test
    fun backgroundLocationWriteDoesNotMarkWidgetConfigured() = runBlocking {
        cache.saveLocation(
            ResolvedLocation("Goiânia", -16.6869, -49.2648, "America/Sao_Paulo", LocationSource.FALLBACK),
            firstId,
            markConfigured = false,
        )

        assertFalse(cache.isConfigured(firstId))
        assertEquals("Goiânia", cache.savedLocationExact(firstId)?.city)
    }

    @Test
    fun deletingOneWidgetDoesNotDeleteTheOther() = runBlocking {
        cache.saveLocation(
            ResolvedLocation("Goiânia", -16.6869, -49.2648, "America/Sao_Paulo", LocationSource.MANUAL_CITY),
            firstId,
        )
        cache.saveLocation(
            ResolvedLocation("Recife", -8.0476, -34.8770, "America/Recife", LocationSource.MANUAL_CITY),
            secondId,
        )

        cache.deleteWidgetState(firstId)

        assertNull(cache.savedLocationExact(firstId))
        assertEquals("Recife", cache.savedLocationExact(secondId)?.city)
        assertFalse(cache.isConfigured(firstId))
        assertTrue(cache.isConfigured(secondId))
    }
}
