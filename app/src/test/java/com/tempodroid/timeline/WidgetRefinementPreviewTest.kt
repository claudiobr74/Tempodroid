package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.tempodroid.astronomy.CelestialPosition
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.sky.DynamicSkyRenderer
import com.tempodroid.sky.FigmaVisualAssets
import com.tempodroid.sky.SkyRenderInput
import com.tempodroid.weather.WeatherCodeMapper
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WidgetRefinementPreviewTest {

    private data class PreviewCase(
        val fileName: String,
        val time: LocalTime,
        val condition: WeatherCondition,
        val temperature: Int,
        val high: Int,
        val low: Int,
        val rainChance: Int,
        val cloudCover: Int,
        val precipitation: Double,
        val moonPhase: MoonPhase,
        val moonIllumination: Double,
        val moonPosition: CelestialPosition?,
    )

    @Test
    fun generateTimelineAndMoonRefinementPreviews() {
        val context = RuntimeEnvironment.getApplication()
        val output = previewDirectory().apply { mkdirs() }
        val sunrise = LocalTime.of(6, 11)
        val sunset = LocalTime.of(18, 13)
        val solarNoon = LocalTime.of(12, 12)

        val cases = listOf(
            PreviewCase(
                fileName = "01-clear-day-1030.png",
                time = LocalTime.of(10, 30),
                condition = WeatherCondition.CLEAR,
                temperature = 30,
                high = 33,
                low = 20,
                rainChance = 5,
                cloudCover = 5,
                precipitation = 0.0,
                moonPhase = MoonPhase.FIRST_QUARTER,
                moonIllumination = 0.50,
                moonPosition = CelestialPosition(0.56f, 0.26f, true),
            ),
            PreviewCase(
                fileName = "02-partly-cloudy-1756.png",
                time = LocalTime.of(17, 56),
                condition = WeatherCondition.PARTLY_CLOUDY,
                temperature = 28,
                high = 31,
                low = 17,
                rainChance = 15,
                cloudCover = 45,
                precipitation = 0.0,
                moonPhase = MoonPhase.WAXING_CRESCENT,
                moonIllumination = 0.32,
                moonPosition = CelestialPosition(0.48f, 0.24f, true),
            ),
            PreviewCase(
                fileName = "03-sunset-1805.png",
                time = LocalTime.of(18, 5),
                condition = WeatherCondition.CLEAR,
                temperature = 27,
                high = 29,
                low = 16,
                rainChance = 5,
                cloudCover = 10,
                precipitation = 0.0,
                moonPhase = MoonPhase.FIRST_QUARTER,
                moonIllumination = 0.50,
                moonPosition = CelestialPosition(0.54f, 0.25f, true),
            ),
            PreviewCase(
                fileName = "04-clear-night-2230.png",
                time = LocalTime.of(22, 30),
                condition = WeatherCondition.CLEAR,
                temperature = 22,
                high = 30,
                low = 18,
                rainChance = 5,
                cloudCover = 5,
                precipitation = 0.0,
                moonPhase = MoonPhase.FULL_MOON,
                moonIllumination = 1.0,
                moonPosition = CelestialPosition(0.72f, 0.24f, true),
            ),
            PreviewCase(
                fileName = "05-rain-1530.png",
                time = LocalTime.of(15, 30),
                condition = WeatherCondition.RAIN,
                temperature = 24,
                high = 27,
                low = 19,
                rainChance = 80,
                cloudCover = 90,
                precipitation = 2.0,
                moonPhase = MoonPhase.WAXING_GIBBOUS,
                moonIllumination = 0.72,
                moonPosition = CelestialPosition(0.58f, 0.30f, true),
            ),
            PreviewCase(
                fileName = "06-storm-1900.png",
                time = LocalTime.of(19, 0),
                condition = WeatherCondition.STORM,
                temperature = 23,
                high = 28,
                low = 18,
                rainChance = 90,
                cloudCover = 100,
                precipitation = 5.0,
                moonPhase = MoonPhase.WAXING_GIBBOUS,
                moonIllumination = 0.72,
                moonPosition = CelestialPosition(0.62f, 0.28f, true),
            ),
            PreviewCase(
                fileName = "07-near-sunset-1810.png",
                time = LocalTime.of(18, 10),
                condition = WeatherCondition.PARTLY_CLOUDY,
                temperature = 27,
                high = 31,
                low = 17,
                rainChance = 15,
                cloudCover = 38,
                precipitation = 0.0,
                moonPhase = MoonPhase.WAXING_CRESCENT,
                moonIllumination = 0.32,
                moonPosition = CelestialPosition(0.50f, 0.23f, true),
            ),
            PreviewCase(
                fileName = "08-clear-night-0005.png",
                time = LocalTime.of(0, 5),
                condition = WeatherCondition.CLEAR,
                temperature = 20,
                high = 30,
                low = 18,
                rainChance = 5,
                cloudCover = 2,
                precipitation = 0.0,
                moonPhase = MoonPhase.FULL_MOON,
                moonIllumination = 1.0,
                moonPosition = CelestialPosition(0.32f, 0.20f, true),
            ),
        )

        val rendered = cases.map { preview ->
            val state = SolarWeatherWidgetState.Empty.copy(
                city = "Goiânia",
                temperature = preview.temperature,
                condition = preview.condition,
                high = preview.high,
                low = preview.low,
                rainChance = preview.rainChance,
                sunrise = sunrise,
                sunset = sunset,
                solarNoon = solarNoon,
                moonPhase = preview.moonPhase,
                moonIllumination = preview.moonIllumination,
                cloudCover = preview.cloudCover,
                precipitation = preview.precipitation,
                displayTime = preview.time,
                displayDate = "Sex., 18 de set.",
                isDay = preview.time.isAfter(sunrise) && preview.time.isBefore(sunset),
            )
            val bitmap = renderPreview(
                state = state,
                moonPosition = preview.moonPosition,
                width = 640,
                height = 320,
            )
            writePng(bitmap, File(output, preview.fileName))
            bitmap
        }

        val contactSheet = Bitmap.createBitmap(960, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(contactSheet)
        canvas.drawColor(Color.rgb(9, 14, 23))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        rendered.forEachIndexed { index, bitmap ->
            val col = index % 2
            val row = index / 2
            val left = col * 480f
            val top = row * 320f
            canvas.drawBitmap(
                bitmap,
                null,
                android.graphics.RectF(left, top, left + 480f, top + 240f),
                paint,
            )
        }
        writePng(contactSheet, File(output, "00-contact-sheet.png"))

        assertTrue(File(output, "00-contact-sheet.png").isFile)
        assertTrue(cases.all { File(output, it.fileName).isFile })

        rendered.forEach(Bitmap::recycle)
        contactSheet.recycle()
    }

    private fun renderPreview(
        state: SolarWeatherWidgetState,
        moonPosition: CelestialPosition?,
        width: Int,
        height: Int,
    ): Bitmap {
        val context = RuntimeEnvironment.getApplication()
        val time = requireNotNull(state.displayTime)
        val sunrise = requireNotNull(state.sunrise)
        val sunset = requireNotNull(state.sunset)
        val moonTexture = FigmaVisualAssets.moonSurface(context)

        val sky = DynamicSkyRenderer.render(
            context = context,
            input = SkyRenderInput(
                widthPx = width,
                heightPx = height,
                localTime = time,
                sunrise = sunrise,
                sunset = sunset,
                weatherCondition = state.condition,
                cloudCover = state.cloudCover,
                precipitation = state.precipitation,
                sunPosition = null,
                moonPosition = moonPosition,
                moonPhase = state.moonPhase,
                moonIllumination = state.moonIllumination,
                hemisphere = Hemisphere.SOUTH,
                solarNoon = state.solarNoon,
            ),
            moonSurface = moonTexture,
        )

        val result = sky.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(4f, 0f, 2f, 0x77000000)
        }
        val secondary = Paint(white).apply { color = 0xCCFFFFFF.toInt() }

        white.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        white.textSize = 22f
        canvas.drawText(WeatherCodeMapper.labelPt(state.condition, state.isDay), 24f, 38f, white)
        white.textSize = 54f
        canvas.drawText("${state.temperature}°", 24f, 92f, white)

        secondary.textAlign = Paint.Align.RIGHT
        secondary.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        secondary.textSize = 32f
        canvas.drawText(TimelineCalculator.formatClock(time), width - 24f, 43f, secondary)
        secondary.typeface = Typeface.DEFAULT
        secondary.textSize = 18f
        canvas.drawText(state.displayDate, width - 24f, 69f, secondary)
        canvas.drawText(state.city, width - 24f, 93f, secondary)

        val timeline = SolarLunarTimelineRenderer.render(
            widthPx = width - 32,
            heightPx = 104,
            state = state,
            now = time,
            compact = false,
            moonSurface = moonTexture,
            panelDarknessAlpha = 64,
        )
        canvas.drawBitmap(timeline, 16f, 158f, null)

        val summary = "Máx ${state.high}° · Mín ${state.low}° · ${state.rainChance}% chuva"
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f, 0f, 2f, 0x77000000)
        }
        canvas.drawText(summary, width / 2f, 298f, summaryPaint)

        timeline.recycle()
        sky.recycle()
        return result
    }

    private fun writePng(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    private fun previewDirectory(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val appDir = if (cwd.name == "app") cwd else File(cwd, "app")
        return File(appDir, "build/reports/timeline-moon-previews")
    }
}