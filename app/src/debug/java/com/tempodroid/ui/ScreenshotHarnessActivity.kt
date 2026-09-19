package com.tempodroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.Instant
import java.time.LocalTime

/** Debug-only activity used to capture the real production Compose UI in CI. */
class ScreenshotHarnessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra(EXTRA_SCENARIO) ?: SCENARIO_CLEAR
        val state = stateFor(scenario)
        setContent {
            TempodroidControlApp(
                primaryState = state,
                primaryWidgetId = SCREENSHOT_WIDGET_ID,
                installedWidgets = listOf(InstalledWidgetUi(SCREENSHOT_WIDGET_ID, state)),
                networkLabel = "Conectado",
                connectionMessage = "Open-Meteo operacional",
                onRefreshAll = {},
                onRefreshWidget = {},
                onConfigureWidget = {},
                onAddWidget = {},
                onPrivacy = {},
                onCredits = {},
                onTestConnection = {},
            )
        }
    }

    private fun stateFor(scenario: String): SolarWeatherWidgetState {
        val base = SolarWeatherWidgetState.Empty.copy(
            city = "Goiânia",
            high = 33,
            low = 20,
            sunrise = LocalTime.of(6, 11),
            sunset = LocalTime.of(18, 13),
            solarNoon = LocalTime.of(12, 12),
            displayDate = "Sex., 18 de set.",
            updatedAt = Instant.now(),
            apparentTemperature = 31,
            relativeHumidity = 42,
            windSpeedKmh = 13,
            surfacePressureHpa = 1014,
        )
        return when (scenario) {
            SCENARIO_PARTLY -> base.copy(
                temperature = 28,
                condition = WeatherCondition.PARTLY_CLOUDY,
                high = 31,
                low = 17,
                rainChance = 15,
                cloudCover = 46,
                precipitation = 0.0,
                isDay = true,
                displayTime = LocalTime.of(17, 56),
                moonPhase = MoonPhase.FIRST_QUARTER,
                moonIllumination = 0.50,
                apparentTemperature = 29,
                relativeHumidity = 48,
            )
            SCENARIO_SUNSET -> base.copy(
                temperature = 29,
                condition = WeatherCondition.CLEAR,
                high = 32,
                low = 21,
                rainChance = 5,
                cloudCover = 12,
                precipitation = 0.0,
                isDay = true,
                displayTime = LocalTime.of(18, 5),
                moonPhase = MoonPhase.WAXING_GIBBOUS,
                moonIllumination = 0.72,
                apparentTemperature = 30,
            )
            SCENARIO_NIGHT -> base.copy(
                temperature = 22,
                condition = WeatherCondition.CLEAR,
                high = 30,
                low = 19,
                rainChance = 0,
                cloudCover = 4,
                precipitation = 0.0,
                isDay = false,
                displayTime = LocalTime.of(22, 30),
                moonPhase = MoonPhase.WANING_GIBBOUS,
                moonIllumination = 0.72,
                apparentTemperature = 22,
                relativeHumidity = 57,
            )
            SCENARIO_RAIN -> base.copy(
                temperature = 24,
                condition = WeatherCondition.RAIN,
                high = 27,
                low = 19,
                rainChance = 80,
                cloudCover = 93,
                precipitation = 2.8,
                isDay = true,
                displayTime = LocalTime.of(15, 30),
                moonPhase = MoonPhase.WAXING_GIBBOUS,
                moonIllumination = 0.72,
                apparentTemperature = 25,
                relativeHumidity = 86,
                windSpeedKmh = 19,
                surfacePressureHpa = 1008,
            )
            SCENARIO_STORM -> base.copy(
                temperature = 21,
                condition = WeatherCondition.STORM,
                high = 26,
                low = 18,
                rainChance = 95,
                cloudCover = 100,
                precipitation = 7.6,
                isDay = false,
                displayTime = LocalTime.of(19, 0),
                moonPhase = MoonPhase.FULL_MOON,
                moonIllumination = 1.0,
                apparentTemperature = 21,
                relativeHumidity = 91,
                windSpeedKmh = 34,
                surfacePressureHpa = 1004,
            )
            else -> base.copy(
                temperature = 30,
                condition = WeatherCondition.CLEAR,
                rainChance = 5,
                cloudCover = 8,
                precipitation = 0.0,
                isDay = true,
                displayTime = LocalTime.of(10, 30),
                moonPhase = MoonPhase.WAXING_CRESCENT,
                moonIllumination = 0.24,
            )
        }
    }

    companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val SCENARIO_CLEAR = "clear"
        const val SCENARIO_PARTLY = "partly"
        const val SCENARIO_SUNSET = "sunset"
        const val SCENARIO_NIGHT = "night"
        const val SCENARIO_RAIN = "rain"
        const val SCENARIO_STORM = "storm"
        private const val SCREENSHOT_WIDGET_ID = 4242
    }
}
